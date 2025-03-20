package org.ngafid.processor.steps;

import org.ngafid.core.flights.Airframes;
import org.ngafid.core.flights.FatalFlightFileException;
import org.ngafid.core.flights.MalformedFlightFileException;
import org.ngafid.processor.format.FlightBuilder;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

/**
 * The processing of flights is broken up into small, discrete steps -- a ComputeStep object computes one of these steps.
 * <p>
 * The applicability of a step to a given flight is determined by a few things: the required double series, string series,
 * and valid airframes. Some steps are only applicable to certain aircraft, and every step will have a set of columns
 * required to compute it.
 * <p>
 * Compute steps modify a flight builder object (an intermediate representation of a flight) as they see fit, but the primary output
 * comes in the form of columns. Steps should generally not mess with anything they are not directly intended to in the flight builder,
 * e.g. {@link ComputeUTCTime} computes the start and end time of the flight and
 * modifies the metadata in the flight builder to do so, but it shouldn't set the airframe name or type.
 * <p>
 * So, compute steps have required input columns and output columns. We can use these relationships to form a graph of the compute steps which can then be executed in parallel.
 * We can also linearize this graph and execute it sequentially while ensuring all dependencies are met. This is done in
 * {@link org.ngafid.uploads.process.DependencyGraph} -- see it for more details.
 */
public abstract class ComputeStep {

    public interface Factory {
        ComputeStep create(Connection connection, FlightBuilder builder);
    }

    public static Factory required(Factory factory) {
        return (c, b) -> {
            var step = factory.create(c, b);
            step.required = true;
            return step;
        };
    }

    protected final FlightBuilder builder;

    // Connection is not accessible by subclasses directly by design, instead use the `withConnection` function.
    // This grabs the lock on the object so only one thread is using the connection at any given point in time.
    private Connection connection;

    public ComputeStep(Connection connection, FlightBuilder builder) {
        this.connection = connection;
        this.builder = builder;
    }

    // These should probably return references to static immutable Sets.
    public abstract Set<String> getRequiredDoubleColumns();

    public abstract Set<String> getRequiredStringColumns();

    public abstract Set<String> getRequiredColumns();

    public abstract Set<String> getOutputColumns();

    private boolean required = false;

    // Whether or not this ProcessStep is required / mandatory
    // If a required step cannot be computed, a MalformedFlightFileException will be raised
    public final boolean isRequired() {
        return required;
    }

    // Whether this ProcessStep can be performed for a given airframe
    public boolean airframeIsValid(Airframes.Airframe airframe) {
        return true;
    }

    public boolean applicable() {
        return airframeIsValid(builder.meta.airframe)
                && builder
                .getStringTimeSeriesKeySet()
                .containsAll(getRequiredStringColumns())
                && builder
                .getDoubleTimeSeriesKeySet()
                .containsAll(getRequiredDoubleColumns());
    }

    public final String explainApplicability() {
        if (applicable()) {
            return "is applicable - all required columns are present and the airframeName is valid)";
        }

        String className = this.getClass().getSimpleName();
        StringBuilder sb = new StringBuilder(
                "Step '" + className + "' cannot be applied for the following reason(s):\n");

        if (!airframeIsValid(builder.meta.airframe)) {
            sb.append("  - airframeName '" + builder.meta.airframe.getName() + "' is invalid ("
                    + className + "::airframeIsValid returned false for airframeName '" + className + "')\n");
        }

        for (String key : getRequiredStringColumns()) {
            if (builder.getStringTimeSeries(key) == null)
                sb.append("  - The required string column '" + key + "' is not available.\n");
        }

        for (String key : getRequiredDoubleColumns()) {
            if (builder.getDoubleTimeSeries(key) == null)
                sb.append("  - The required double column '" + key + "' is not available.\n");
        }

        return sb.toString();
    }

    protected interface ConnectionFunctor<T> {
        T compute(Connection connection) throws SQLException;
    }

    // This interface must be used to access the connection so that we can guarantee that only one
    // thread is using it at any given time.
    public final <T> T withConnection(ConnectionFunctor<T> functor) throws SQLException {
        T value = null;

        synchronized (connection) {
            value = functor.compute(connection);
        }

        return value;
    }

    public abstract void compute() throws SQLException, MalformedFlightFileException, FatalFlightFileException;
}
