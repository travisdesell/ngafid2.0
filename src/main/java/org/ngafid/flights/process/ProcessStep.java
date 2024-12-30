package org.ngafid.flights.process;

import java.util.Set;
import java.sql.Connection;
import java.sql.SQLException;

import org.ngafid.flights.FatalFlightFileException;
import org.ngafid.flights.MalformedFlightFileException;

public abstract class ProcessStep {

    public interface Factory {
        ProcessStep create(Connection connection, FlightBuilder builder);
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

    public ProcessStep(Connection connection, FlightBuilder builder) {
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

    // Whether or not this ProcessStep can be performed for a given airframe
    public abstract boolean airframeIsValid(String airframe);

    public final boolean applicable() {
        return airframeIsValid(builder.meta.airframe.getName())
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

        if (!airframeIsValid(builder.meta.airframe.getName())) {
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
    final public <T> T withConnection(ConnectionFunctor<T> functor) throws SQLException {
        T value = null;

        synchronized (connection) {
            value = functor.compute(connection);
        }

        return value;
    }

    public abstract void compute() throws SQLException, MalformedFlightFileException, FatalFlightFileException;
}
