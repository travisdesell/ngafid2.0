package org.ngafid.uploads.process.steps;

import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.uploads.process.FatalFlightFileException;
import org.ngafid.uploads.process.format.FlightBuilder;
import org.ngafid.uploads.process.MalformedFlightFileException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;

import static org.ngafid.flights.Airframes.AIRFRAME_DJI;
import static org.ngafid.flights.Airframes.AIRFRAME_SCAN_EAGLE;
import static org.ngafid.flights.Parameters.*;

public class ProcessTotalFuel extends ProcessStep {
    private static final Logger LOG = Logger.getLogger(ProcessTotalFuel.class.getName());

    private static Set<String> REQUIRED_DOUBLE_COLUMNS = Set.of(FUEL_QTY_LEFT, FUEL_QTY_RIGHT);
    private static Set<String> OUTPUT_COLUMNS = Set.of(TOTAL_FUEL);
    private static Set<String> AIRFRAME_BLACKLIST = Set.of(AIRFRAME_SCAN_EAGLE, AIRFRAME_DJI);

    public ProcessTotalFuel(Connection connection, FlightBuilder builder) {
        super(connection, builder);
    }

    public Set<String> getRequiredDoubleColumns() {
        return REQUIRED_DOUBLE_COLUMNS;
    }

    public Set<String> getRequiredStringColumns() {
        return Collections.<String>emptySet();
    }

    public Set<String> getRequiredColumns() {
        return REQUIRED_DOUBLE_COLUMNS;
    }

    public Set<String> getOutputColumns() {
        return OUTPUT_COLUMNS;
    }

    public boolean airframeIsValid(String airframe) {
        return !AIRFRAME_BLACKLIST.contains(airframe);
    }

    public void compute() throws SQLException, MalformedFlightFileException, FatalFlightFileException {
        double[] totalFuel = null;

        for (var columnName : REQUIRED_DOUBLE_COLUMNS) {
            DoubleTimeSeries fuelTS = builder.getDoubleTimeSeries(columnName);
            if (totalFuel == null)
                totalFuel = new double[fuelTS.size()];

            for (int i = 0; i < fuelTS.size(); i++)
                totalFuel[i] += fuelTS.get(i);
        }

        DoubleTimeSeries totalFuelTS = new DoubleTimeSeries(TOTAL_FUEL, Unit.GALLONS, totalFuel);
        builder.addTimeSeries(totalFuelTS);
    }

}
