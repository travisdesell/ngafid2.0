package org.ngafid.flights.process;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.ngafid.Database;
import org.ngafid.ProcessUpload.FlightInfo;
import org.ngafid.UploadException;
import org.ngafid.flights.Flight;
import org.ngafid.flights.Upload;


public class Pipeline {
    private static Logger LOG = Logger.getLogger(Pipeline.class.getName());
    
    private static final int BATCH_SIZE = 1;
    private static int parallelism = 1;
    
    private static ForkJoinPool pool = null;

    static {
        try {
            parallelism = Integer.parseInt(System.getenv("PARALLELISM"));
        } catch (NullPointerException | NumberFormatException e) {
            parallelism = Runtime.getRuntime().availableProcessors();
        }

        if (parallelism <= 0)
            pool = new ForkJoinPool();
        else
            pool = new ForkJoinPool(parallelism);

        LOG.info("Created pool with " + parallelism + " threads");
    }
    
    public static Pipeline run(Connection connection, Upload upload, ZipFile zipFile) throws IOException {
        LOG.info("Creating pipeline to process upload id" + upload.id + " / " + upload.filename);
        Pipeline pipeline = new Pipeline(connection, upload, zipFile);

        var flights = new ArrayBlockingQueue<Flight>(BATCH_SIZE * 2);

        var processHandle = pool.submit(() ->
            pipeline
                .parallelStream()
                .forEach(s -> {
                    List<Flight> f =  pipeline.parse(s).sequential()
                        .map(pipeline::build)
                        .filter(Objects::nonNull)
                        .map(pipeline::finalize)
                        .collect(Collectors.toList());

                    if (BATCH_SIZE > 1) {
                        flights.addAll(f);
                        if (flights.size() >= BATCH_SIZE) {
                            var batch = new ArrayList<Flight>(128);
                            flights.drainTo(batch);
                            Flight.batchUpdateDatabase(Database.getConnection(), upload, batch);
                        }
                    } else {
                        Flight.batchUpdateDatabase(Database.getConnection(), upload, f);
                    }
                })
        );
        
        processHandle.join();

        if (flights.size() > 0) {
            Flight.batchUpdateDatabase(connection, upload, flights);
        }
        
        URI zipURI = URI.create("jar:" + Paths.get(upload.filename).toUri());
        pipeline.insertConvertedFiles(zipURI);
        
        return pipeline;
    }

    private final Connection connection;
    private final ZipFile zipFile;
    private final Map<String, FlightFileProcessor.Factory> factories;
    private final Upload upload;
    private final AtomicInteger validFlightsCount = new AtomicInteger(0);
    private final AtomicInteger warningFlightsCount = new AtomicInteger(0);

    private final ConcurrentHashMap<String, UploadException> flightErrors = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, FlightInfo> flightInfo = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<String> convertedFiles = new ConcurrentLinkedQueue<>();
    
    public Pipeline(Connection connection, Upload upload, ZipFile zipFile) {
        this.connection = connection;
        this.upload = upload;
        this.zipFile = zipFile;

        this.factories = Map.of(
            "csv",  this::createCSVFileProcessor,
            "dat",  this::createDATFileProcessor,
            "json", JSONFileProcessor::new,
            "gpx",  GPXFileProcessor::new
        );
    }

    public Map<String, FlightInfo> getFlightInfo() {
        return flightInfo;
    }

    public Map<String, UploadException> getFlightErrors() {
        return Collections.unmodifiableMap(flightErrors);
    }

    private FlightFileProcessor createDATFileProcessor(Connection connection, InputStream is, String filename) {
        return new DATFileProcessor(connection, is, filename, zipFile);
    }

    private FlightFileProcessor createCSVFileProcessor(Connection connection, InputStream is, String filename) throws IOException {
        return new CSVFileProcessor(connection, is, filename, upload);
    }

    private Stream<? extends ZipEntry> getValidFilesStream() {
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        Stream<? extends ZipEntry> validFiles = 
            StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(entries.asIterator(), Spliterator.ORDERED),
                false
            )
            .filter(z -> !z.getName().contains("__MACOSX"))
            .filter(z -> !z.isDirectory());
        return validFiles;
    }

    private Stream<FlightFileProcessor> stream() {
        // TODO: This reads all files into memory sequentially because ZipFile is not thread safe.
        // TODO: We should in the future interleave the loading of files and processing of files.
        return getValidFilesStream().map(this::create).filter(Objects::nonNull).collect(Collectors.toList()).stream();
    }

    private Stream<FlightFileProcessor> parallelStream() {
        var validFiles = getValidFilesStream().collect(Collectors.toList());
        var queue = new ArrayBlockingQueue<FlightFileProcessor>(validFiles.size());
        var handle = pool.submit(() -> {
            validFiles.stream().map(this::create).filter(Objects::nonNull).forEach(queue::add);
        });

        return IntStream.range(0, validFiles.size()).parallel().mapToObj(x ->{
            while (true) {
                try {
                    return queue.take(); 
                } catch (InterruptedException e) {

                }
            }
        });
    }

    public Stream<FlightBuilder> parse(FlightFileProcessor processor) {
        try {
            return processor.parse();
        } catch (FlightProcessingException e) {
            flightErrors.put(processor.filename, new UploadException(e.getMessage(), e, processor.filename));
            return Stream.of();
        }
    }

    public Flight build(FlightBuilder fb) {
        try {
            return fb.build(connection);
        } catch (FlightProcessingException e) {
            e.printStackTrace();
            flightErrors.put(fb.meta.filename, new UploadException(e.getMessage(), e, fb.meta.filename));
            return null;
        }
    }

    public List<Flight> build(Stream<FlightBuilder> fbs) {
        return fbs.map(this::build).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private FlightFileProcessor create(ZipEntry entry) {
        String filename = entry.getName();

        int index = filename.lastIndexOf('.');
        String extension = index >= 0 ? filename.substring(index + 1).toLowerCase() : "";
        FlightFileProcessor.Factory f = factories.get(extension);

        if (f != null) {
            try {
                return f.create(connection, zipFile.getInputStream(entry), filename);
            } catch (IOException e) {
                flightErrors.put(filename, new UploadException(e.getMessage(), e, filename));
            }
        } else {
            flightErrors.put(filename, new UploadException("Unknown file type '" + extension + "' contained in zip file.", filename));
        }

        return null;
    }

    public Flight finalize(Flight flight) {
        if (flight.getStatus().equals("WARNING")) {
            warningFlightsCount.incrementAndGet();
        } else {
            validFlightsCount.incrementAndGet();
        }
        
        flightInfo.put(flight.getFilename(), new FlightInfo(flight.getId(), flight.getNumberRows(), flight.getFilename(), flight.getExceptions()));

        return flight;
    }

    public void insertConvertedFiles(URI zipURI) throws IOException {
        Map<String, String> zipENV = new HashMap<>();
        zipENV.put("create", "true");
        
        try (FileSystem fileSystem = FileSystems.newFileSystem(zipURI, zipENV)) {
            for (String convertedFile : convertedFiles) {
                Path csvFilePath = Paths.get(convertedFile);
                Path zipFileSystemPath = fileSystem.getPath(convertedFile.substring(convertedFile.lastIndexOf("/") + 1));
                Files.write(zipFileSystemPath, Files.readAllBytes(csvFilePath), StandardOpenOption.CREATE);
            }
        }
    }

    public int getWarningFlightsCount() {
        return warningFlightsCount.get();
    }

    public int getValidFlightsCount() {
        return validFlightsCount.get();
    }
}
