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
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.ngafid.UploadException;
import org.ngafid.flights.Flight;
import org.ngafid.flights.Upload;

public abstract class FlightFileProcessor {
    
    private static Logger LOG = Logger.getLogger(FlightFileProcessor.class.getName());

    interface Factory {
        FlightFileProcessor create(Connection connection, InputStream is, String filename) throws IOException;
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
