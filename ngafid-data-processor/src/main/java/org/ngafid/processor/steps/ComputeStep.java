package org.ngafid.processor.steps;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;
import org.ngafid.core.flights.Airframes;
import org.ngafid.core.flights.FatalFlightFileException;
import org.ngafid.core.flights.MalformedFlightFileException;
import org.ngafid.processor.format.FlightBuilder;

/**
 * The processing of flights is broken up into small, discrete steps -- a ComputeStep object computes one of these
 * steps.
 * <p>
 * The applicability of a step to a given flight is determined by a few things: the required double series, string
 * series, and valid airframes. Some steps are only applicable to certain aircraft, and every step will have a set
 * of columns required to compute it.
 * <p>
 * Compute steps modify a flight builder object (an intermediate representation of a flight) as they see fit, but
 * the primary output comes in the form of columns. Steps should generally not mess with anything they are not
 * directly intended to in the flight builder, e.g. {@link ComputeUTCTime} computes the start and end time of the
 * flight and modifies the metadata in the flight builder to do so, but it shouldn't set the airframe name or type.
 * <p>
 * So, compute steps have required input columns and output columns. We can use these relationships to form a graph
 * of the compute steps which can then be executed in parallel. We can also linearize this graph and execute it
 * sequentially while ensuring all dependencies are met. This is done in
 * {@link org.ngafid.uploads.process.DependencyGraph} -- see it for more details.
 */
public abstract class ComputeStep {

    /**
     * Factory for constructing a {@link ComputeStep} bound to a specific database connection and flight builder.
     *
     * <p>Supplying steps as factories lets the dependency graph defer construction until it has the connection and
     * builder for a given flight, and lets {@link #required(Factory)} wrap a factory to mark its steps mandatory.
     */
    public interface Factory {
        /**
         * Creates a compute step bound to the given connection and flight builder.
         *
         * @param connection the database connection the step may use
         * @param builder the flight builder the step reads from and writes to
         * @return the constructed compute step
         */
        ComputeStep create(Connection connection, FlightBuilder builder);
    }

    /**
     * Wraps a factory so that every step it creates is marked as required (a required step that cannot be
     * computed causes a {@link MalformedFlightFileException}).
     *
     * @param factory the factory to wrap
     * @return a factory that produces required steps
     */
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

    /**
     * Constructs a compute step.
     *
     * @param connection the database connection the step may use (accessed only via {@link #withConnection})
     * @param builder the flight builder the step reads from and writes to
     */
    public ComputeStep(Connection connection, FlightBuilder builder) {
        this.connection = connection;
        this.builder = builder;
    }

    // These should probably return references to static immutable Sets.

    /**
     * Returns the double-series columns this step requires as input.
     *
     * @return the set of required double-series column names
     */
    public abstract Set<String> getRequiredDoubleColumns();

    /**
     * Returns the string-series columns this step requires as input.
     *
     * @return the set of required string-series column names
     */
    public abstract Set<String> getRequiredStringColumns();

    /**
     * Returns all columns this step requires as input.
     *
     * @return the set of required column names
     */
    public abstract Set<String> getRequiredColumns();

    /**
     * Returns the columns this step produces.
     *
     * @return the set of output column names
     */
    public abstract Set<String> getOutputColumns();

    private boolean required = false;

    // Whether or not this ProcessStep is required / mandatory
    // If a required step cannot be computed, a MalformedFlightFileException will be raised
    public final boolean isRequired() {
        return required;
    }

    /**
     * Reports whether this step can be performed for the given airframe. The default implementation applies to
     * all airframes; subtypes restricted to specific aircraft override this.
     *
     * @param airframe the airframe to check
     * @return true if the step applies to the airframe
     */
    public boolean airframeIsValid(Airframes.Airframe airframe) {
        return true;
    }

    /**
     * Reports whether this step is applicable to the current flight: its airframe is valid and all required
     * string and double columns are present.
     *
     * @return true if the step can be computed for the current flight
     */
    public boolean applicable() {
        return airframeIsValid(builder.meta.getAirframe())
                && builder.getStringTimeSeriesKeySet().containsAll(getRequiredStringColumns())
                && builder.getDoubleTimeSeriesKeySet().containsAll(getRequiredDoubleColumns());
    }

    /**
     * Returns a human-readable explanation of whether this step is applicable to the current flight, listing the
     * reasons it cannot be applied (invalid airframe or missing required columns) when it is not.
     *
     * @return a description of the step's applicability
     */
    public final String explainApplicability() {
        if (applicable()) {
            return "is applicable - all required columns are present and the airframeName is valid)";
        }

        String className = this.getClass().getSimpleName();
        StringBuilder sb =
                new StringBuilder("Step '" + className + "' cannot be applied for the following reason(s):\n");

        if (!airframeIsValid(builder.meta.getAirframe())) {
            sb.append("  - airframeName '" + builder.meta.getAirframe().getName() + "' is invalid (" + className
                    + "::airframeIsValid returned false for airframeName '" + className + "')\n");
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
        /**
         * Performs work using the supplied connection.
         *
         * @param connection the database connection to use
         * @return the computed value
         * @throws SQLException if the database operation fails
         */
        T compute(Connection connection) throws SQLException;
    }

    /**
     * Runs the given functor with exclusive access to the step's database connection. This is the only
     * supported way to use the connection; it synchronizes on the connection so at most one thread uses it at a
     * time.
     *
     * @param functor the work to perform with the connection
     * @param <T> the type of value the functor returns
     * @return the value computed by the functor
     * @throws SQLException if the functor's database operation fails
     */
    public final <T> T withConnection(ConnectionFunctor<T> functor) throws SQLException {
        T value = null;

        synchronized (connection) {
            value = functor.compute(connection);
        }

        return value;
    }

    /**
     * Performs this step's computation, modifying the flight builder (typically by adding its output columns).
     *
     * @throws SQLException if a database operation fails
     * @throws MalformedFlightFileException if the flight data is malformed for this step
     * @throws FatalFlightFileException if an unrecoverable error occurs processing the flight
     */
    public abstract void compute() throws SQLException, MalformedFlightFileException, FatalFlightFileException;
}
