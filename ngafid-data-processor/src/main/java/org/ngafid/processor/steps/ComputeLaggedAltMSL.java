package org.ngafid.processor.steps;

import static org.ngafid.core.flights.Airframes.AIRFRAME_DJI;
import static org.ngafid.core.flights.Airframes.AIRFRAME_SCAN_EAGLE;
import static org.ngafid.core.flights.Parameters.ALT_MSL;
import static org.ngafid.core.flights.Parameters.ALT_MSL_LAG_DIFF;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Set;
import org.ngafid.core.flights.DoubleTimeSeries;
import org.ngafid.core.flights.FatalFlightFileException;
import org.ngafid.core.flights.MalformedFlightFileException;
import org.ngafid.core.flights.Parameters;
import org.ngafid.processor.format.FlightBuilder;

/**
 * Computes a double time series that contains the altitude above sea level 10 seconds ago
 * (i.e. alt msl lagged by 10 seconds).
 */
public class ComputeLaggedAltMSL extends ComputeStep {
    private static final Set<String> REQUIRED_DOUBLE_COLUMNS = Set.of(ALT_MSL);
    private static final Set<String> OUTPUT_COLUMNS = Set.of(ALT_MSL_LAG_DIFF);
    private static final Set<String> AIRFRAME_BLACKLIST = Set.of(AIRFRAME_SCAN_EAGLE, AIRFRAME_DJI);
    private static final int LAG = 10;

    /**
     * Constructs the lagged-altitude-MSL compute step.
     *
     * @param connection the database connection the step may use
     * @param builder the flight builder this step reads from and writes to
     */
    public ComputeLaggedAltMSL(Connection connection, FlightBuilder builder) {
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

    /**
     * Reports whether this step applies to the given airframe name.
     *
     * @param airframe the airframe name to check
     * @return true if the step applies to the airframe
     */
    public boolean airframeIsValid(String airframe) {
        for (String blacklisted : AIRFRAME_BLACKLIST) if (airframe.contains(blacklisted)) return false;

        return true;
    }

    /**
     * Builds the {@code ALT_MSL_LAG_DIFF} series: for each sample it stores the change in MSL altitude over the
     * preceding {@value #LAG}-sample (10-second) window, i.e. {@code altMSL[i] - altMSL[i - LAG]}. The first
     * {@value #LAG} samples have no prior value to compare against and are filled with 0. The resulting series is
     * added to the flight builder.
     *
     * @throws SQLException if a database operation fails
     * @throws MalformedFlightFileException if the flight data is malformed for this step
     * @throws FatalFlightFileException if an unrecoverable error occurs processing the flight
     */
    @Override
    public void compute() throws SQLException, MalformedFlightFileException, FatalFlightFileException {
        DoubleTimeSeries altMSL = builder.getDoubleTimeSeries(ALT_MSL);
        DoubleTimeSeries laggedAltMSL = new DoubleTimeSeries(ALT_MSL_LAG_DIFF, Parameters.Unit.FT_AGL, altMSL.size());

        for (int i = 0; i < LAG; i++) laggedAltMSL.add(0.0);
        for (int i = LAG; i < altMSL.size(); i++) laggedAltMSL.add(altMSL.get(i) - altMSL.get(i - LAG));

        builder.addTimeSeries(laggedAltMSL);
    }
}
