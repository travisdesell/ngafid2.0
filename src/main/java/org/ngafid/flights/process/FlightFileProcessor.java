package org.ngafid.flights.process;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.util.Map;
import java.util.HashMap;
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

import org.ngafid.filters.Pair;
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
        private int validFlightsCount = 0;
        private int warningFlightsCount = 0;

        private HashMap<String, UploadException> flightErrors = new HashMap<>();

        public Pipeline(Connection connection, Upload upload, ZipFile zipFile) {
            this.connection = connection;
            this.upload = upload;
            this.zipFile = zipFile;

            this.factories = Map.of(
                "csv",  CSVFileProcessor::new,
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

        public Stream<FlightFileProcessor> stream() {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            Stream<? extends ZipEntry> validFiles = 
                StreamSupport.stream(
                    Spliterators.spliteratorUnknownSize(entries.asIterator(), Spliterator.ORDERED),
                    false
                )
                .filter(z -> !z.getName().contains("__MACOSX"))
                .filter(z -> !z.isDirectory());

            return validFiles.map(this::create).filter(Objects::nonNull);
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

        public Stream<Flight> build(Stream<FlightBuilder> fbs) {
            return fbs.map(this::build).filter(Objects::nonNull);
        }

        private FlightFileProcessor create(ZipEntry entry) {
            String filename = entry.getName();

            int index = filename.lastIndexOf('.');
            String extension = index >= 0 ? filename.substring(index + 1).toLowerCase() : "";
            System.out.println("Extension: " + extension); 
            Factory f = factories.get(extension);
            if (f != null) {
                try {
                    return f.create(connection, zipFile.getInputStream(entry), zipFile.getName());
                } catch (IOException e) {
                    flightErrors.put(filename, new UploadException(e.getMessage(), e, filename));
                }
            } else {
                flightErrors.put(filename, new UploadException("Unknown file type '" + extension + "' contained in zip file.", filename));
            }

            return null;
        }

        public Flight insert(Flight flight) {
            flight.updateDatabase(connection, upload.getId(), upload.getUploaderId(), upload.getFleetId());
            return flight;
        }

        public void tabulateFlightStatus(Flight flight) {
            if (flight.getStatus().equals("WARNING"))
                warningFlightsCount++;
            else
                validFlightsCount++;
        }

        public int getWarningFlightsCount() {
            return warningFlightsCount;
        }

        public int getValidFlightsCount() {
            return validFlightsCount;
        }
    }

    public final Connection connection;
    public final String filename;
    public final InputStream stream;

    public FlightFileProcessor(Connection connection, InputStream stream, String filename) {
        this.connection = connection;
        this.filename = filename;
        this.stream = stream;
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
