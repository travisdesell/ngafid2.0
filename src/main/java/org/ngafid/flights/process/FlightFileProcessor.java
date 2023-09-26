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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.ngafid.UploadException;
import org.ngafid.flights.Flight;
import org.ngafid.flights.Upload;

public abstract class FlightFileProcessor {
    
    interface Factory {
        FlightFileProcessor create(Connection connection, InputStream is, String filename);
    }
    
    // Right now this is only for zip files but this could easily be extended to handle other types of archives.
    // Most of the code is reusable.
    public static class Pipeline {
        final Connection connection;
        final ZipFile zipFile;
        final Map<String, FlightFileProcessor.Factory> factories;
        final Upload upload;
        private AtomicInteger validFlightsCount = new AtomicInteger(0);
        private AtomicInteger warningFlightsCount = new AtomicInteger(0);

        private ConcurrentHashMap<String, UploadException> flightErrors = new ConcurrentHashMap<>();
        private ConcurrentLinkedQueue<String> convertedFiles = new ConcurrentLinkedQueue<>();
        
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

        public Map<String, UploadException> getFlightErrors() {
            return Collections.unmodifiableMap(flightErrors);
        }

        private FlightFileProcessor createDATFileProcessor(Connection connection, InputStream is, String filename) {
            return new DATFileProcessor(connection, is, filename, zipFile);
        }

        private FlightFileProcessor createCSVFileProcessor(Connection connection, InputStream is, String filename) {
            return new CSVFileProcessor(connection, is, filename, upload);
        }

        public Stream<FlightFileProcessor> stream() {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            Stream<? extends ZipEntry> validFiles = 
                StreamSupport.stream(
                    Spliterators.spliteratorUnknownSize(entries.asIterator(), Spliterator.ORDERED),
                    false
                )
                .filter(z -> !z.getName().contains("__MACOSX"))
                .filter(z -> !z.isDirectory());

            return validFiles.map(this::create).filter(Objects::nonNull).collect(Collectors.toList()).stream();
        }

        public Stream<FlightFileProcessor> parallelStream() {
            return this.stream().parallel();
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
            Factory f = factories.get(extension);
            if (f != null) {
//                try {
//                    InputStream zipEntryInputStream = zipFile.getInputStream(entry);
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//
//                InputStream reusableInputStream = new ByteArrayInputStream(inputStream.readAllBytes());

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
            if (flight.getStatus().equals("WARNING"))
                warningFlightsCount.incrementAndGet();
            else
                validFlightsCount.incrementAndGet();

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

    protected final Connection connection;
    protected final InputStream stream;
    protected final String filename;

    public FlightFileProcessor(Connection connection, InputStream stream, String filename) {
        this.connection = connection;
        this.stream = stream;
        this.filename = filename;
    }

    // If an exception occurs, it will be stored here.
    FlightProcessingException parseException = null;

    /**
     * Parses the file for flight data to be processed
     * @return A stream of FlightBuilders
     * @throws FlightProcessingException
     */
    private Stream<FlightBuilder> parsedFlightBuilders = null;
    protected abstract Stream<FlightBuilder> parse() throws FlightProcessingException;

    public FlightFileProcessor pipelinedParse() {
        try {
            parsedFlightBuilders = parse();
            assert parsedFlightBuilders != null;
        } catch (FlightProcessingException e) {
            parseException = e;
        }

        return this;
    }

    protected Stream<Flight> flights = null;
    protected final ArrayList<FlightProcessingException> buildExceptions = new ArrayList<>();

    private Flight build(FlightBuilder fb) {
        try {
            return fb.build(connection);
        } catch (FlightProcessingException e) {
            buildExceptions.add(e);
        }
        return null;
    }

    public FlightFileProcessor pipelinedBuild() {
        if (parseException == null) {
            flights = parsedFlightBuilders.map(this::build).filter(Objects::nonNull);
        }

        return this;
    }
}
