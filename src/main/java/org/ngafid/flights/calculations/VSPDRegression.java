package org.ngafid.flights.calculations;

import org.ngafid.flights.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import static org.ngafid.flights.Parameters.*;

/**
 * This class is an instance of a {@link Calculation} that gets a derived VSI using linear regression 
 *
 * @author <a href = "mailto:apl1341@cs.rit.edu">Aidan LaBella @ RIT CS</a>
 */

public class VSPDRegression implements Calculation {
    private final DoubleTimeSeries altB;
    private final DoubleTimeSeries altBLag;
    private final DoubleTimeSeries altBLead;

    static final double FPM_CONV = 60.d;

    /**
     * This is a linear regression calculation to get a more instantaneous VSI
     *
     * @param flight the {@link Flight} to perform a regression on
     */
    public VSPDRegression(Connection connection, Flight flight) throws SQLException, IOException {
        this.altB = flight.getDoubleTimeSeries(ALT_B);
        this.altBLag = altB.lag(connection, VSI_LAG_DIFF);
        this.altBLead = altB.lead(connection, VSI_LAG_DIFF);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double calculate(int index) {
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

    /**
     * Used to normalize altitudes 
     *
     * @param yValues the array of y values (altitudes)
     * @param yA the average y value 
     */
    public static void normalizeAltitudes(double [] yValues, double yA) {
        double stdDev = stdDev(yValues, yA);

        for (int i = 1; i < yValues.length; i++) {
            if (Math.abs(yValues[i] - yValues[i - 1]) > (3 * stdDev)) {

            }

        }
    }
           
    /**
     * Takes the standard deviation of the yValues
     *
     * @param yValues the array of yValues (altitudes)
     * @param yA the average y value
     */
    public static double stdDev(double [] yValues, double yA) {
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
     *
     * @return the average of the y values as a double
     */
    public static double average(double ... yValues) {
        double sum = 0.d;

        for (int i = 0; i < yValues.length; i++) {
            sum += yValues[i];
        }

        return sum / yValues.length;
    }

    /**
     * Performs a linear regression on any data point such that the lengths of the datasets is 3
     *
     * @param xValues the x values to use for the regression 
     * @param yValues the x values to use for the regression 
     * @param yA the average of the y values
     * @param xA the average of the x values
     *
     * @return the regression coefeccient (derivative) of the functon portryaed through the x and y values
     */
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
