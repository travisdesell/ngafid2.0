package org.ngafid.flights;

import static org.ngafid.flights.CalculationParameters.*;

import java.util.Map;

import org.ngafid.Database;

public class VSPDCalculation extends Calculation {
    private final int DIFF = 1;
    
    static final double FPM_CONV = 60.f;

    public VSPDCalculation(Flight flight, Map<String, DoubleTimeSeries> parameters) {
        super(flight, vsiParamStrings, parameters);
    }

    /**
     * This is a linear regression calculation to get a more instantaneous VSI
     */
    public void calculate() {
        System.out.println("Calcuating VSPD from MSL data");

        DoubleTimeSeries msl = this.parameters.get(ALT_MSL);
        DoubleTimeSeries mslLagged = msl.lag(DIFF);
        DoubleTimeSeries mslLead = msl.lead(DIFF);
        this.parameters.put(mslLead.getName(), mslLead);

        DoubleTimeSeries vsiCalculated = new DoubleTimeSeries(VSPD_CALCULATED, "double");
        
        for (int i = DIFF; i < msl.size(); i++) {
            if (i < 1 || i >= msl.size() - 1) {
                vsiCalculated.add(Double.NaN);
            } else {
                double [] yValues = new double[3];
                double [] xValues = new double[3];

                xValues[0] = i - 1;
                xValues[1] = i;
                xValues[2] = i + 1;

                yValues[0] = mslLagged.get(i);
                yValues[1] = msl.get(i);
                yValues[2] = mslLead.get(i);

                //TODO: filter out bad readings here

                double m = vsiLinearRegression(xValues, yValues);
                double vsi = m / DIFF;

                vsi *= FPM_CONV;

                vsiCalculated.add(vsi);
            }
        }

        this.parameters.put(VSPD_CALCULATED, vsiCalculated);
    }

    public static double average(double ... yValues) {
        double sum = 0.f;

        for (int i = 0; i < yValues.length; i++) {
            sum += yValues[i];
        }

        return sum / yValues.length;
    }

    public static double vsiLinearRegression(double [] xValues, double [] yValues) {
        double n = 0.f;
        double d = 0.f;
        double xA = average(xValues);
        double yA = average(yValues);

        assert yValues.length == xValues.length;

        for (int i = 0; i < yValues.length; i++) {
            double chi = (xValues[i] - xA);
            n += chi * (yValues[i] - yA);
            d += (chi * chi);
        }

        return n / d;
    }
            
    public void updateDatabase() {
        DoubleTimeSeries vsi = this.parameters.get(VSPD_CALCULATED);
        vsi.updateDatabase(Database.getConnection(), this.flight.getId());
    }
}
