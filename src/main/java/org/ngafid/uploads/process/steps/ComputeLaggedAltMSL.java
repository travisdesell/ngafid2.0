package org.ngafid.uploads.process.steps;

import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.uploads.process.FatalFlightFileException;
import org.ngafid.uploads.process.MalformedFlightFileException;
import org.ngafid.uploads.process.format.FlightBuilder;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Set;

import static org.ngafid.flights.Airframes.AIRFRAME_DJI;
import static org.ngafid.flights.Airframes.AIRFRAME_SCAN_EAGLE;
import static org.ngafid.flights.Parameters.*;

/**
 * Computes a double time series that contains the altitude above sea level 10 seconds ago (i.e. alt msl lagged by 10 seconds).
 */
public class ComputeLaggedAltMSL extends ComputeStep {
    private static final Set<String> REQUIRED_DOUBLE_COLUMNS = Set.of(ALT_MSL);
    private static final Set<String> OUTPUT_COLUMNS = Set.of(ALT_MSL_LAG_DIFF);
    private static final Set<String> AIRFRAME_BLACKLIST = Set.of(AIRFRAME_SCAN_EAGLE, AIRFRAME_DJI);
    private static final int LAG = 10;

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

    public boolean airframeIsValid(String airframe) {
        for (String blacklisted : AIRFRAME_BLACKLIST)
            if (airframe.contains(blacklisted))
                return false;

        return true;
    }

    public void compute() throws SQLException, MalformedFlightFileException, FatalFlightFileException {
        DoubleTimeSeries altMSL = builder.getDoubleTimeSeries(ALT_MSL);
        DoubleTimeSeries laggedAltMSL = new DoubleTimeSeries(ALT_MSL_LAG_DIFF, Unit.FT_AGL, altMSL.size());

        for (int i = 0; i < LAG; i++)
            laggedAltMSL.add(0.0);
        for (int i = LAG; i < altMSL.size(); i++)
            laggedAltMSL.add(altMSL.get(i) - altMSL.get(i - LAG));

        builder.addTimeSeries(laggedAltMSL);
    }

}
