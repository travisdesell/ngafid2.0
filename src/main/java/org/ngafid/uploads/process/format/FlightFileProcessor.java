package org.ngafid.uploads.process.format;

import org.ngafid.flights.Flight;
import org.ngafid.uploads.process.FlightProcessingException;
import org.ngafid.uploads.process.Pipeline;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Stream;

public abstract class FlightFileProcessor {

    private static final Logger LOG = Logger.getLogger(FlightFileProcessor.class.getName());

    public interface Factory {
        FlightFileProcessor create(Connection connection, InputStream is, String filename, Pipeline pipeline)
                throws Exception;
    }

    public final Connection connection;
    public final InputStream stream;
    public final String filename;
    public final Pipeline pipeline;

    public FlightFileProcessor(Connection connection, InputStream stream, String filename, Pipeline pipeline) throws IOException {
        this.connection = connection;
        if (!(stream instanceof ByteArrayInputStream)) {
            this.stream = new ByteArrayInputStream(stream.readAllBytes());
        } else {
            this.stream = stream;
        }
        this.filename = filename;
        this.pipeline = pipeline;
    }

    // If an exception occurs, it will be stored here.
    private FlightProcessingException parseException = null;

    /**
     * Parses the file for flight data to be processed
     *
     * @return A stream of FlightBuilders
     * @throws FlightProcessingException
     */
    private Stream<FlightBuilder> parsedFlightBuilders = null;

    public abstract Stream<FlightBuilder> parse() throws FlightProcessingException;

    public FlightFileProcessor pipelinedParse() {
        try {
            parsedFlightBuilders = parse();
            assert parsedFlightBuilders != null;
        } catch (FlightProcessingException e) {
            parseException = e;
        }

        return this;
    }

    public Stream<Flight> flights = null;
    public final ArrayList<FlightProcessingException> buildExceptions = new ArrayList<>();

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
