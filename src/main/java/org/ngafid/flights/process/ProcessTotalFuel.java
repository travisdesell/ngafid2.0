package org.ngafid.flights.process;

import java.util.Set;
import java.util.logging.Logger;
import java.util.Collections;
import java.sql.Connection;
import java.sql.SQLException;

import java.nio.file.NoSuchFileException;

import org.ngafid.flights.Flight;
import org.ngafid.flights.DoubleTimeSeries;
import static org.ngafid.flights.Parameters.*;
import static org.ngafid.flights.Airframes.*;
import org.ngafid.flights.FatalFlightFileException;
import org.ngafid.flights.MalformedFlightFileException;

public class ProcessTotalFuel extends ProcessStep {
    private static final Logger LOG = Logger.getLogger(ProcessTotalFuel.class.getName());

    private static Set<String> REQUIRED_DOUBLE_COLUMNS = Set.of(FUEL_QTY_LEFT, FUEL_QTY_RIGHT);
    private static Set<String> OUTPUT_COLUMNS = Set.of(TOTAL_FUEL);
    private static Set<String> AIRFRAME_BLACKLIST = Set.of(AIRFRAME_SCAN_EAGLE, AIRFRAME_DJI);

    public ProcessTotalFuel(Connection connection, FlightBuilder builder) {
        super(connection, builder);
    }

    public Set<String> getRequiredDoubleColumns() { return REQUIRED_DOUBLE_COLUMNS; }
    public Set<String> getRequiredStringColumns() { return Collections.<String>emptySet(); }
    public Set<String> getRequiredColumns() { return REQUIRED_DOUBLE_COLUMNS; }
    public Set<String> getOutputColumns() { return OUTPUT_COLUMNS; }
    
    public boolean airframeIsValid(String airframe) {
        return !AIRFRAME_BLACKLIST.contains(airframe);
    }

    public void compute() throws SQLException, MalformedFlightFileException, FatalFlightFileException {
        double[] totalFuel = null;

        for (var columnName : REQUIRED_DOUBLE_COLUMNS) {
            DoubleTimeSeries fuelTS = doubleTS.get(columnName);
            if (totalFuel == null)
                totalFuel = new double[fuelTS.size()];

            for (int i = 0; i < fuelTS.size(); i++)
                totalFuel[i] += fuelTS.get(i);
        }

        DoubleTimeSeries totalFuelTS = new DoubleTimeSeries(TOTAL_FUEL, UNIT_GALLONS, totalFuel);
        doubleTS.put(TOTAL_FUEL, totalFuelTS);
    }

}
