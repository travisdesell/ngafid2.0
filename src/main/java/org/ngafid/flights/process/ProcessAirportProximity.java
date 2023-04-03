package org.ngafid.flights.process;

import java.util.Set;
import java.util.Collections;
import java.sql.Connection;
import java.sql.SQLException;

import org.ngafid.flights.Flight;
import org.ngafid.flights.DoubleTimeSeries;
import static org.ngafid.flights.Parameters.*;
import org.ngafid.flights.process.ProcessStep;
import org.ngafid.flights.FatalFlightFileException;
import org.ngafid.flights.MalformedFlightFileException;

public class ProcessAirportProximity extends ProcessStep {
    private static Set<String> REQUIRED_DOUBLE_COLUMNS = Set.of(LATITUDE, LONGITUDE, ALT_AGL);
    private static Set<String> OUTPUT_COLUMNS = Set.of(NEAREST_RUNWAY, AIRPORT_DISTANCE, NEAREST_RUNWAY, RUNWAY_DISTANCE);

    public ProcessAirportProximity(Connection connection, Flight flight) {
        super(connection, flight);
    }

    public Set<String> getRequiredDoubleColumns() { return REQUIRED_DOUBLE_COLUMNS; }
    public Set<String> getRequiredStringColumns() { return Collections.<String>emptySet(); }
    public Set<String> getRequiredColumns() { return Collections.<String>emptySet(); }
    public Set<String> getOutputColumns() { return OUTPUT_COLUMNS; }
    
    public boolean airframeIsValid(String airframe) { return true; }
    public boolean isRequired() { return true; }

    public void compute() throws SQLException, MalformedFlightFileException, FatalFlightFileException {
        
    }
}
