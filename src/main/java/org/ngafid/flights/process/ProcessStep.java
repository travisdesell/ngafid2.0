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
    
    protected FlightBuilder builder;
    
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
    
    // Whether or not this ProcessStep is required / mandatory
    // If a required step cannot be computed, a MalformedFlightFileException will be raised
    public abstract boolean isRequired();

    // Whether or not this ProcessStep can be performed for a given airframe
    public abstract boolean airframeIsValid(String airframe);

    final public boolean applicable() {
        return 
            airframeIsValid(builder.meta.airframeName)
            && builder
                .stringTimeSeries
                .keySet()
                .containsAll(getRequiredStringColumns()) 
            && builder
                .doubleTimeSeries
                .keySet()
                .containsAll(getRequiredDoubleColumns());
    }

    protected interface ConnectionFunctor<T> {
        public T compute(Connection connection) throws SQLException;
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
