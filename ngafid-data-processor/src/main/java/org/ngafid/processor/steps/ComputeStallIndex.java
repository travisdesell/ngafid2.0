package org.ngafid.processor.steps;

import org.ngafid.core.flights.DoubleTimeSeries;
import org.ngafid.core.flights.FatalFlightFileException;
import org.ngafid.core.flights.MalformedFlightFileException;
import org.ngafid.processor.format.FlightBuilder;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;

import static org.ngafid.core.flights.Airframes.AIRFRAME_CESSNA_172S;
import static org.ngafid.core.flights.Parameters.*;

/**
 * Computes the stall index -- see the paper references in {@link ComputeLOCI} for details.
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

    /**
     * This class is an instance of a {@link DoubleTimeSeries.TimeStepCalculation} that gets a derived VSI using linear regression
     *
     * @author <a href = "mailto:apl1341@cs.rit.edu">Aidan LaBella @ RIT CS</a>
     */

    public static class VSPDRegression implements DoubleTimeSeries.TimeStepCalculation {
        static final double FPM_CONV = 60.d;
        private final DoubleTimeSeries altB;
        private final DoubleTimeSeries altBLag;
        private final DoubleTimeSeries altBLead;

        /**
         * This is a linear regression calculation to get a more instantaneous VSI
         *
         * @param altB the altitude time series to use for the calculation
         */
        public VSPDRegression(DoubleTimeSeries altB) {
            this.altB = altB;
            this.altBLag = altB.lag(VSI_LAG_DIFF);
            this.altBLead = altB.lead(VSI_LAG_DIFF);
        }

        /**
         * Takes the standard deviation of the yValues
         *
         * @param yValues the array of yValues (altitudes)
         * @param yA      the average y value
         * @return the standard deviation of the y values as a double
         */
        public static double stdDev(double[] yValues, double yA) {
            double n = 0.d;
            int k = yValues.length;

            for (int i = 0; i < k; i++) {
                n += Math.pow((yValues[i] - yA), 2);
            }

            return Math.sqrt(n / k);
        }

        /**
         * Takes the average of the y values
         *
         * @param yValues the array of y values to average
         * @return the average of the y values as a double
         */
        public static double average(double... yValues) {
            double sum = 0.d;

            for (double yValue : yValues) {
                sum += yValue;
            }

            return sum / yValues.length;
        }

        /**
         * Performs a linear regression on any data point such that the lengths of the datasets is 3
         *
         * @param xValues the x values to use for the regression
         * @param yValues the x values to use for the regression
         * @param yA      the average of the y values
         * @param xA      the average of the x values
         * @return the regression coefeccient (derivative) of the functon portryaed through the x and y values
         */
        public static double vsiLinearRegression(double[] xValues, double[] yValues, double yA, double xA) {
            double n = 0.d;
            double d = 0.d;

            assert yValues.length == xValues.length;

            for (int i = 0; i < yValues.length; i++) {
                double chi = (xValues[i] - xA);
                n += chi * (yValues[i] - yA);
                d += (chi * chi);
            }

            return n / d;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public double compute(int index) {
            if (index < 1 || index >= altB.size() - 1) {
                return Double.NaN;
            } else {
                double[] yValues = new double[3];
                double[] xValues = new double[3];

                xValues[0] = index - 1;
                xValues[1] = index;
                xValues[2] = index + 1;

                yValues[0] = altBLag.get(index);
                yValues[1] = altB.get(index);
                yValues[2] = altBLead.get(index);

                double yA = average(yValues);
                double xA = average(xValues);

                double m = vsiLinearRegression(xValues, yValues, yA, xA);
                double vsi = m / VSI_LAG_DIFF;

                vsi *= FPM_CONV;

                return vsi;
            }
        }
    }
}
