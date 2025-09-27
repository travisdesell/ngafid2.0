package org.ngafid.processor.format;

import org.ngafid.core.Database;
import org.ngafid.core.event.Event;
import org.ngafid.core.flights.FatalFlightFileException;
import org.ngafid.core.flights.Flight;
import org.ngafid.core.flights.FlightProcessingException;
import org.ngafid.core.flights.TurnToFinal;
import org.ngafid.processor.Pipeline;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * A flight file processor handles the initial parsing of data and metadata from a flight file.
 */
public abstract class FlightFileProcessor implements Callable<Void> {

    private static final Logger LOG = Logger.getLogger(FlightFileProcessor.class.getName());

    /**
     * Factory type for the creation of a {@link FlightFileProcessor}, mirroring the base constructor.
     */
    public interface Factory {
        FlightFileProcessor create(Connection connection, InputStream is, String filename, Pipeline pipeline)
                throws IOException, FatalFlightFileException, SQLException;
    }

    public final Connection connection;
    public final InputStream stream;
    public final String filename;
    public final Pipeline pipeline;

    /**
     * Creates a new flight file processor. If the provided stream is not a ByteArrayInput stream, the stream will be
     * fully read into memory and a ByteArrayInput stream will be created.
     *
     * @param connection
     * @param stream
     * @param filename
     * @param pipeline
     * @throws IOException
     */
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

    public abstract Stream<FlightBuilder> parse() throws FlightProcessingException;

    @Override
    public Void call() {
        try (Connection connection = Database.getConnection()) {
            var builders = pipeline
                    .parse(this)
                    .parallel()
                    .filter(Objects::nonNull)
                    .map(fbs -> pipeline.build(connection, fbs))
                    .toList();

            if (builders.isEmpty())
                return null;

            List<Flight> flights = builders.stream().map(FlightBuilder::getFlight).toList();
            Flight.batchUpdateDatabase(connection, flights);
            for (FlightBuilder builder : builders) {
                Event.batchInsertion(connection, builder.getFlight(), builder.getEvents());
                TurnToFinal.cacheTurnToFinal(connection, builder.getFlight().getId(), builder.getTurnToFinals());
                builder.getFlight().insertComputedEvents(connection, builder.getEventDefinitions());
            }

        } catch (SQLException | IOException e) {
            pipeline.fail(filename, e);
        }
        return null;
    }

    public Stream<Flight> flights = null;
}
