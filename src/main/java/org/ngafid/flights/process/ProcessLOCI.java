package org.ngafid.flights.process;

import java.util.Set;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.logging.Logger;

import static org.ngafid.flights.Flight.calculateLOCI;
import static org.ngafid.flights.Parameters.*;
import static org.ngafid.flights.Airframes.*;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.MalformedFlightFileException;
import org.ngafid.flights.FatalFlightFileException;

public class ProcessLOCI extends ProcessStep {
    private static final Logger LOG = Logger.getLogger(ProcessLOCI.class.getName());

    public static Set<String> REQUIRED_DOUBLE_COLUMNS = Set.of(LOCI_DEPENDENCIES);

    public ProcessLOCI(Connection connection, FlightBuilder builder) {
        super(connection, builder);
    }

    public Set<String> getRequiredDoubleColumns() {
        return REQUIRED_DOUBLE_COLUMNS;
    }

    public Set<String> getRequiredStringColumns() {
        return Collections.emptySet();
    }

    public Set<String> getRequiredColumns() {
        return REQUIRED_DOUBLE_COLUMNS;
    }

    public Set<String> getOutputColumns() {
        return Collections.emptySet();
    }

    public boolean airframeIsValid(String airframe) {
        return airframe.equals(AIRFRAME_CESSNA_172S);
    }

    public void compute() throws SQLException, MalformedFlightFileException, FatalFlightFileException {
        DoubleTimeSeries hdg = builder.getDoubleTimeSeries(HDG);
        DoubleTimeSeries hdgLagged = hdg.lag(YAW_RATE_LAG);
        DoubleTimeSeries roll = builder.getDoubleTimeSeries(ROLL);
        DoubleTimeSeries tas = builder.getDoubleTimeSeries(TAS_FTMIN);
        DoubleTimeSeries stallIndex = builder.getDoubleTimeSeries(STALL_PROB);

        int length = roll.size();

        DoubleTimeSeries coordIndex = DoubleTimeSeries.computed(PRO_SPIN_FORCE, "index", length,
                (int index) -> {
                    double laggedHdg = hdgLagged.get(index);
                    return calculateLOCI(hdg, index, roll, tas, laggedHdg);
                });
        DoubleTimeSeries loci = DoubleTimeSeries.computed(LOCI, "index", length,
                index -> {
                    double prob = stallIndex.get(index) * coordIndex.get(index);
                    return prob / 100;
                });

        builder.addTimeSeries(coordIndex);
        builder.addTimeSeries(loci);
    }
}
