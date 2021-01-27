package org.ngafid.flights;

import static org.ngafid.flights.CalculationParameters.*;

import java.util.Map;

public class VSPDRegression implements Calculation {
    private final DoubleTimeSeries altB;
    private final DoubleTimeSeries altBLag;
    private final DoubleTimeSeries altBLead;

    static final double FPM_CONV = 60.d;

    public VSPDRegression(Map<String, DoubleTimeSeries> parameters) {
        this.altB = parameters.get(ALT_B);

        this.altBLag = altB.lag(VSI_LAG_DIFF);
        this.altBLead = altB.lead(VSI_LAG_DIFF);

        parameters.put(ALT_B + LAG_SUFFIX + VSI_LAG_DIFF, altBLag);
        parameters.put(ALT_B + LEAD_SUFFIX + VSI_LAG_DIFF, altBLead);
    }

    /**
     * This is a linear regression calculation to get a more instantaneous VSI
     */
    public double calculate(Map<String, DoubleTimeSeries> parameters, int index) {
        if (index < 1 || index >= altB.size() - 1) {
            return Double.NaN;
        } else {
            double [] yValues = new double[3];
            double [] xValues = new double[3];

            xValues[0] = index - 1;
            xValues[1] = index;
            xValues[2] = index + 1;

            yValues[0] = altBLag.get(index);
            yValues[1] = altB.get(index);
            yValues[2] = altBLead.get(index);

            //TODO: filter out bad readings here
            //possible solution to improve accuracy if research is extended
            
            double yA = average(yValues);
            double xA = average(xValues);

            double m = vsiLinearRegression(xValues, yValues, yA, xA);
            double vsi = m / VSI_LAG_DIFF;

            vsi *= FPM_CONV;

            return vsi;
        }
    }

    public static void normalizeAltitudes(double [] yValues, double yA) {
        double stdDev = stdDev(yValues, yA);

        for (int i = 1; i < yValues.length; i++) {
            if (Math.abs(yValues[i] - yValues[i - 1]) > (3 * stdDev)) {

            }

        }
    }
           
    public static double stdDev(double [] yValues, double yA) {
        double n = 0.d;
        int k = yValues.length;

        for (int i = 0; i < k; i++) {
            n += Math.pow((yValues[i] - yA), 2);
        }

        return Math.sqrt(n / k);
    }

    public static double average(double ... yValues) {
        double sum = 0.d;

        for (int i = 0; i < yValues.length; i++) {
            sum += yValues[i];
        }

        return sum / yValues.length;
    }

    public static double vsiLinearRegression(double [] xValues, double [] yValues, double yA, double xA) {
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
}
