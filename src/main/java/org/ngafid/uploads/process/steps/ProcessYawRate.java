package org.ngafid.uploads.process.steps;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;

import org.ngafid.flights.DoubleTimeSeries;
import static org.ngafid.flights.Parameters.HDG;
import static org.ngafid.flights.Parameters.IAS;
import org.ngafid.flights.Parameters.Unit;
import static org.ngafid.flights.Parameters.YAW_RATE;
import static org.ngafid.flights.Parameters.YAW_RATE_DEPENDENCIES;
import static org.ngafid.flights.Parameters.YAW_RATE_LAG;
import org.ngafid.uploads.process.FatalFlightFileException;
import org.ngafid.uploads.process.MalformedFlightFileException;
import org.ngafid.uploads.process.format.FlightBuilder;

public class ProcessYawRate extends ProcessStep {
    private static final Logger LOG = Logger.getLogger(ProcessYawRate.class.getName());

    private static final Set<String> REQUIRED_DOUBLE_COLUMNS = Set.of(YAW_RATE_DEPENDENCIES);
    private static final Set<String> OUTPUT_COLUMNS = Set.of(YAW_RATE);

    public ProcessYawRate(Connection connection, FlightBuilder builder) {
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
        return OUTPUT_COLUMNS;
    }

    public boolean airframeIsValid(String airframe) {
        return true;
    }

    public void compute() throws SQLException, MalformedFlightFileException, FatalFlightFileException {

        /*
            LOG.info("ProcessYawRate -- Computing yaw rate");
        */

        DoubleTimeSeries ias = builder.getDoubleTimeSeries(IAS);
        int length = ias.size();

        //Get HDG
        DoubleTimeSeries hdg = builder.getDoubleTimeSeries(HDG);

        //Calculate Yaw Rate
        DoubleTimeSeries yawRate;
        yawRate = DoubleTimeSeries.computed(
            YAW_RATE, Unit.DEGREES, length, index -> {

                double hdgValue = hdg.get(index);

                /*
                    if (Double.isNaN(hdgValue))
                        LOG.info("[EX] ProcessStallIndex -- hdgValue is NaN at index: " + index);
                */

                DoubleTimeSeries hdgLagged = hdg.lag(YAW_RATE_LAG);
                double hdgLaggedValue = hdgLagged.get(index);

                /*
                    if (Double.isNaN(hdgLaggedValue))
                        LOG.info("[EX] ProcessStallIndex -- hdgLaggedValue is NaN at index: " + index);
                */

                double yawRateValue =
                    Double.isNaN(hdgLaggedValue)
                    ? 0
                    : 180 - Math.abs(180 - Math.abs(hdgValue - hdgLaggedValue) % 360);

                /*
                    LOG.info(
                        "ProcessStallIndex -- Calculated Yaw Rate: " + yawRateValue
                        + " at index: " + index
                        + ", hdgValue: " + hdgValue
                        + ", hdgLaggedValue: " + hdgLaggedValue
                    );
                */

                return yawRateValue;

            }

        );

        builder.addTimeSeries(yawRate);  

        /*
            LOG.info("ProcessYawRate -- Finished computing yaw rate");
        */

    }
}
