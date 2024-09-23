package org.ngafid.flights.process;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
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
import java.sql.SQLException;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Objects;
import java.util.Queue;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashSet;
import java.util.LinkedList;
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

/**
 * Provides the relevant methods and book keeping for a parallelized flight
 * processing pipeline.
 * See `Pipeline::run` for how these methods are put together.
 *
 * @author Joshua Karns (josh@karns.dev)
 */
public class Pipeline implements AutoCloseable {
    private static Logger LOG = Logger.getLogger(Pipeline.class.getName());

    private static final int BATCH_SIZE = 1;
    private static int parallelism = 1;

    private static ForkJoinPool pool = null;

    /**
     * Creates the thread pool used for flight file processing pipelines
     */
    private static void initialize() {
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

    private final Connection connection;

    private final Upload upload;
    private final ZipFile zipFile;

    // ZipFileSystem
    private FileSystem derivedFileSystem = null;
    private Upload derivedUpload = null;

    private final Map<String, FlightFileProcessor.Factory> factories;

    private final AtomicInteger validFlightsCount = new AtomicInteger(0);
    private final AtomicInteger warningFlightsCount = new AtomicInteger(0);

    private final ConcurrentHashMap<String, UploadException> flightErrors = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, FlightInfo> flightInfo = new ConcurrentHashMap<>();

    public Pipeline(Connection connection, Upload upload, ZipFile zipFile) {
        this.connection = connection;
        this.upload = upload;
        this.zipFile = zipFile;

        this.factories = Map.of(
                "csv", CSVFileProcessor::new,
                "dat", DATFileProcessor::new,
                "json", JSONFileProcessor::new,
                "gpx", GPXFileProcessor::new);
    }

    @Override
    public void close() throws IOException {
        if (derivedFileSystem != null)
            derivedFileSystem.close();
    }

    public void execute() throws IOException {
        if (Pipeline.pool == null)
            initialize();

        LOG.info("Creating pipeline to process upload id " + upload.id + " / " + upload.filename);

        var flights = new ArrayBlockingQueue<Flight>(BATCH_SIZE * 2);

        var processHandle = pool.submit(() -> this.parallelStream()
                .filter(Objects::nonNull)
                .forEach(s -> {
                    List<Flight> f = this.parse(s).sequential()
                            .map(this::build)
                            .filter(Objects::nonNull)
                            .map(this::finalize)
                            .collect(Collectors.toList());
                    try (Connection connection = Database.getConnection()) {
                        LOG.info("Writing " + f.size() + " flights to database:");
                        for (var flight : f) {
                            LOG.info("STATUS: " + flight.getStatus());
                            LOG.info("ERRORS: " + flight.getExceptions().size());
                        }
                        Flight.batchUpdateDatabase(connection, upload, f);
                    } catch (SQLException e) {
                        LOG.info("Encountered SQLException trying to get database connection...");
                        e.printStackTrace();
                        this.fail();
                    }
                }));

        processHandle.join();
        if (flights.size() > 0) {
            Flight.batchUpdateDatabase(connection, upload, flights);
        }
    }

    /**
     * Mark this upload as having failed in an irrecoverable way, marking it for
     * re-processing
     */
    public void fail() {
        // TODO: Implement
    }

    public FileSystem createDerivedFileSystem() throws IOException {
        Path derivedZipPath = Paths.get(derivedUpload.getDerivedDirectory(), derivedUpload.getDerivedFilename());

        // May need to create this directory
        Files.createDirectories(derivedZipPath.getParent());

        File f = new File(derivedZipPath.toString());
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(f));
        out.close();

        Map<String, String> zipENV = new HashMap<>();
        zipENV.put("create", "true");

        Path zipFilePath = Paths.get(derivedUpload.getDerivedDirectory() + "/" + derivedUpload.getDerivedFilename());

        URI zipURI = URI.create("jar:" + zipFilePath.toUri());
        return FileSystems.newFileSystem(zipURI, zipENV);
    }

    public synchronized void addDerivedFile(String filename, byte[] data) throws IOException, SQLException {
        if (derivedFileSystem == null) {
            derivedUpload = Upload.createDerivedUpload(connection, upload.uploaderId, upload.fleetId, upload.filename,
                    upload.identifier, upload.md5Hash);
            derivedFileSystem = createDerivedFileSystem();
        }

        Path zipFileSystemPath = derivedFileSystem.getPath(filename);
        Path parentPath = zipFileSystemPath.getParent();
        if (parentPath != null && !Files.exists(parentPath)) {
            Files.createDirectories(parentPath);
        }else{
            String path = upload.getDerivedDirectory();
            Path derivedPath = Paths.get(path);
            Files.createDirectories(derivedPath);
        }

        Files.write(zipFileSystemPath, data, StandardOpenOption.CREATE);
    }

    public Map<String, FlightInfo> getFlightInfo() {
        return flightInfo;
    }

    public Map<String, UploadException> getFlightErrors() {
        return Collections.unmodifiableMap(flightErrors);
    }

    private Stream<? extends ZipEntry> getValidFilesStream() {
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        Stream<? extends ZipEntry> validFiles = StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(entries.asIterator(), Spliterator.ORDERED),
                false)
                .filter(z -> !z.getName().contains("__MACOSX"))
                .filter(z -> !z.isDirectory());
        return validFiles;
    }

    private Stream<FlightFileProcessor> stream() {
        return getValidFilesStream().map(this::create).filter(Objects::nonNull).collect(Collectors.toList()).stream();
    }

    private Stream<FlightFileProcessor> parallelStream() {
        var validFiles = getValidFilesStream().collect(Collectors.toList());

        return validFiles.parallelStream().map(this::create);
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
                return f.create(connection, zipFile.getInputStream(entry), filename, this);
            } catch (Exception e) {
                e.printStackTrace();
                flightErrors.put(filename, new UploadException(e.getMessage(), e, filename));
            }
        } else {
            flightErrors.put(filename,
                    new UploadException("Unknown file type '" + extension + "' contained in zip file.", filename));
        }

        return null;
    }

    public Flight finalize(Flight flight) {
        if (flight.getStatus().equals("WARNING")) {
            warningFlightsCount.incrementAndGet();
        } else {
            validFlightsCount.incrementAndGet();
        }
        LOG.info("FLIGHT STATUS = " + flight.getStatus());
        for (var e : flight.getExceptions()) {
            e.printStackTrace();
        }
        flightInfo.put(flight.getFilename(),
                new FlightInfo(flight.getId(), flight.getNumberRows(), flight.getFilename(), flight.getExceptions()));

        return flight;
    }

    public int getWarningFlightsCount() {
        return warningFlightsCount.get();
    }

    public int getValidFlightsCount() {
        return validFlightsCount.get();
    }

    public int getDerivedUploadId() {
        return derivedUpload.id;
    }

    public Upload getUpload() {
        return upload;
    }
}
