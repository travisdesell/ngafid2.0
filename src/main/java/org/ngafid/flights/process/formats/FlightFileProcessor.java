package org.ngafid.flights.process.formats;

import org.ngafid.flights.Flight;
import org.ngafid.flights.process.FlightBuilder;
import org.ngafid.flights.process.FlightProcessingException;
import org.ngafid.flights.process.Pipeline;

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

    protected final Connection connection;
    protected final InputStream stream;
    public final String filename;
    protected final Pipeline pipeline;

    public FlightFileProcessor(Connection connection, InputStream stream, String filename, Pipeline pipeline) {
        this.connection = connection;
        this.stream = stream;
        this.filename = filename;
        this.pipeline = pipeline;
    }

    // If an exception occurs, it will be stored here.
    FlightProcessingException parseException = null;

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
