package org.ngafid.flights.process;

import org.ngafid.flights.Flight;
import org.ngafid.flights.StringTimeSeries;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.FatalFlightFileException;
import org.ngafid.flights.MalformedFlightFileException;

import java.util.Map;
import java.util.Set;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;


public abstract class ProcessStep {

    public interface Factory {
        ProcessStep create(Flight flight);
    }
    
    protected Flight flight;
    
    // Connection is not accessible by subclasses directly by design, instead use the `withConnection` function.
    // This grabs the lock on the object so only one thread is using the connection at any given point in time.
    private Connection connection;

    // References to the corresponding fields in `flight` 
    protected Map<String, DoubleTimeSeries> doubleTimeSeries;
    protected Map<String, StringTimeSeries> stringTimeSeries;

    public ProcessStep(Connection connection, Flight flight) {
        this.connection = connection;
        this.flight = flight;

        this.doubleTimeSeries = flight.getDoubleTimeSeriesMap();
        this.stringTimeSeries = flight.getStringTimeSeriesMap();
    }

    // These should probably return references to static immutable Sets.
    public abstract Set<String> getRequiredDoubleColumns();
    public abstract Set<String> getRequiredStringColumns();
    public abstract Set<String> getRequiredColumns();
    public abstract Set<String> getOutputColumns();
    
    // Whether or not this ProcessStep is required / mandatory
    public abstract boolean isRequired();

    // Whether or not this ProcessStep can be performed for a given airframe
    public abstract boolean airframeIsValid(String airframe);

    final public boolean applicable() {
        return 
            airframeIsValid(flight.getAirframeName())
            && stringTimeSeries
                .keySet()
                .containsAll(getRequiredStringColumns()) 
            && doubleTimeSeries
                .keySet()
                .containsAll(getRequiredDoubleColumns());
    }

    protected interface ConnectionFunctor<T> {
        public T compute(Connection connection) throws SQLException;
    }
    
    final public <T> T withConnection(ConnectionFunctor<T> functor) throws SQLException {
        T value = null;
        synchronized (connection) {
            value = functor.compute(connection);
        }
        return value;
    }

    public abstract void compute() throws SQLException, MalformedFlightFileException, FatalFlightFileException;
}
