package org.ngafid.flights;

import static org.ngafid.flights.CalculationParameters.*;

import java.util.Map;

import org.ngafid.Database;

public class VSPDCalculation extends Calculation {
    private static final int DIFF = 1;
    
    static final double FPM_CONV = 60.f;

    public VSPDCalculation(Flight flight, Map<String, DoubleTimeSeries> parameters) {
        super(flight, vsiParamStrings, parameters);
    }

    /**
     * This is a linear regression calculation to get a more instantaneous VSI
     */
    public void calculate(DoubleTimeSeries doubleSeries) {
        System.out.println("Calcuating VSPD from altB data");

        DoubleTimeSeries alt = this.parameters.get(ALT_B);
        DoubleTimeSeries altLagged = alt.lag(DIFF);
        DoubleTimeSeries altLead = alt.lead(DIFF);
        this.parameters.put(altLead.getName(), altLead);

        for (int i = DIFF; i < alt.size(); i++) {
            if (i < 1 || i >= alt.size() - 1) {
                doubleSeries.add(Double.NaN);
            } else {
                double [] yValues = new double[3];
                double [] xValues = new double[3];

                xValues[0] = i - 1;
                xValues[1] = i;
                xValues[2] = i + 1;

                yValues[0] = altLagged.get(i);
                yValues[1] = alt.get(i);
                yValues[2] = altLead.get(i);

                //TODO: filter out bad readings here
                //
                double yA = average(yValues);
                double xA = average(xValues);

                double m = vsiLinearRegression(xValues, yValues, yA, xA);
                double vsi = m / DIFF;

                vsi *= FPM_CONV;

                doubleSeries.add(vsi);
            }
        }

        this.parameters.put(VSPD_CALCULATED, doubleSeries);
    }

    public static void normalizeAltitudes(double [] yValues, double yA) {
        double stdDev = stdDev(yValues, yA);

        for (int i = 1; i < yValues.length; i++) {
            if (Math.abs(yValues[i] - yValues[i - 1]) > (5 * stdDev)) {

            }

        }
    }
            
    public void updateDatabase() {
        DoubleTimeSeries vsi = this.parameters.get(VSPD_CALCULATED);
        vsi.updateDatabase(Database.getConnection(), this.flight.getId());
    }

    public static double stdDev(double [] yValues, double yA) {
        double n = 0.f;
        int k = yValues.length;

        for (int i = 0; i < k; i++) {
            n += Math.pow((yValues[i] - yA), 2);
        }

        return Math.sqrt(n / k);
    }

    public static double average(double ... yValues) {
        double sum = 0.f;

        for (int i = 0; i < yValues.length; i++) {
            sum += yValues[i];
        }

        return sum / yValues.length;
    }

    public static double vsiLinearRegression(double [] xValues, double [] yValues, double yA, double xA) {
        double n = 0.f;
        double d = 0.f;

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
    public String getCalculationName() {
        return VSPD_CALCULATED;
    }
}
