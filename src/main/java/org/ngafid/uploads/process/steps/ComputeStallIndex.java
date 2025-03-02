package org.ngafid.uploads.process.steps;

import org.ngafid.events.calculations.VSPDRegression;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.uploads.process.FatalFlightFileException;
import org.ngafid.uploads.process.MalformedFlightFileException;
import org.ngafid.uploads.process.format.FlightBuilder;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;

import static org.ngafid.flights.Airframes.AIRFRAME_CESSNA_172S;
import static org.ngafid.flights.Parameters.*;

/**
 * Computes the stall index -- see the paper references in {@link org.ngafid.uploads.process.steps.ComputeLOCI} for details.
 */
public class ComputeStallIndex extends ComputeStep {
    private static final Logger LOG = Logger.getLogger(ComputeStallIndex.class.getName());

    private static final Set<String> REQUIRED_DOUBLE_COLUMNS = Set.of(STALL_DEPENDENCIES);
    private static final Set<String> OUTPUT_COLUMNS = Set.of(STALL_PROB, TAS_FTMIN, VSPD_CALCULATED, CAS);

    public ComputeStallIndex(Connection connection, FlightBuilder builder) {
        super(connection, builder);
    }

    @Override
    public Set<String> getRequiredDoubleColumns() {
        return REQUIRED_DOUBLE_COLUMNS;
    }

    @Override
    public Set<String> getRequiredStringColumns() {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getRequiredColumns() {
        return REQUIRED_DOUBLE_COLUMNS;
    }

    @Override
    public Set<String> getOutputColumns() {
        return OUTPUT_COLUMNS;
    }

    public boolean airframeIsValid(String airframe) {
        return true;
    }

    @Override
    public void compute() throws SQLException, MalformedFlightFileException, FatalFlightFileException {
        DoubleTimeSeries ias = builder.getDoubleTimeSeries(IAS);
        int length = ias.size();

        if (builder.meta.airframe.getName().equals(AIRFRAME_CESSNA_172S)) {
            DoubleTimeSeries cas = DoubleTimeSeries.computed(CAS, Unit.KNOTS, length,
                    index -> {
                        double iasValue = ias.get(index);

                        if (iasValue < 70.d)
                            iasValue = (0.7d * iasValue) + 20.667;

                        return iasValue;
                    });
            cas.setTemporary(true);
            builder.addTimeSeries(cas);
        }

        DoubleTimeSeries vspdCalculated = DoubleTimeSeries.computed(VSPD_CALCULATED, Unit.FT_PER_MINUTE, length,
                new VSPDRegression(builder.getDoubleTimeSeries(ALT_B)));
        vspdCalculated.setTemporary(true);
        builder.addTimeSeries(vspdCalculated);

        DoubleTimeSeries baroA = builder.getDoubleTimeSeries(BARO_A);
        DoubleTimeSeries oat = builder.getDoubleTimeSeries(OAT);
        DoubleTimeSeries densityRatio = DoubleTimeSeries.computed(DENSITY_RATIO, Unit.RATIO, length,
                index -> {
                    double pressRatio = baroA.get(index) / STD_PRESS_INHG;
                    double tempRatio = (273 + oat.get(index)) / 288;

                    return pressRatio / tempRatio;
                });

        DoubleTimeSeries airspeed = builder.meta.airframe.getName().equals(AIRFRAME_CESSNA_172S)
                ? builder.getDoubleTimeSeries(CAS)
                : builder.getDoubleTimeSeries(IAS);
        DoubleTimeSeries tasFtMin = DoubleTimeSeries.computed(TAS_FTMIN, Unit.FT_PER_MINUTE, length,
                index -> {
                    return (airspeed.get(index) * Math.pow(densityRatio.get(index), -0.5)) * ((double) 6076 / 60);
                });
        tasFtMin.setTemporary(true);

        DoubleTimeSeries pitch = builder.getDoubleTimeSeries(PITCH);
        DoubleTimeSeries aoaSimple = DoubleTimeSeries.computed(AOA_SIMPLE, Unit.DEGREES, length,
                index -> {

                    double vspdGeo = vspdCalculated.get(index) * Math.pow(densityRatio.get(index), -0.5);
                    double fltPthAngle = Math.asin(vspdGeo / tasFtMin.get(index));
                    fltPthAngle = fltPthAngle * (180 / Math.PI);
                    double value = pitch.get(index) - fltPthAngle;

                    return value;
                });

        DoubleTimeSeries stallIndex = DoubleTimeSeries.computed(STALL_PROB, Unit.INDEX, length,
                index -> {
                    return (Math.min(((Math.abs(aoaSimple.get(index) / AOA_CRIT)) * 100), 100)) / 100;
                });

        builder.addTimeSeries(stallIndex);
        builder.addTimeSeries(tasFtMin);
    }
}
