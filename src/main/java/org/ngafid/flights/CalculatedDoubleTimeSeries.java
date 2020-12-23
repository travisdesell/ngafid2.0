/**
 * This is a {@link DoubleTimeSeries} that requires further analysis, such
 * as a Calculation. This class is meant to be expandable
 *
 * @author <a href = mailto:apl1341@cs.rit.edu>Aidan LaBella</a>
 */

package org.ngafid.flights;

import org.ngafid.Database;

public class CalculatedDoubleTimeSeries extends DoubleTimeSeries {
    private Calculation calculation;

    /**
     * Default Constructor
     *
     * By instantiating a {@link CalculatedDoubleTimeSeries} object, it performs the {@link Calculation} 
     * supplied to it
     *
     * @param calculation the calculation to perform
     */
    public CalculatedDoubleTimeSeries(Calculation calculation) {
        super(calculation.getCalculationName(), "double");
        this.calculation = calculation;

        runCalculation();
    }

    /**
     * Runs the calculation
     */
    private void runCalculation() {
        int flightId = calculation.getFlightId();
        if (!calculation.isNotCalculatable()) {
            System.out.println("Performing " + calculation.getCalculationName() + " calculation on flight #" + flightId);
            calculation.calculate(this);

            super.updateDatabase(Database.getConnection(), flightId);
        } else {
            System.err.println("WARNING: flight #" + flightId + " is not calculatable for " + calculation.getCalculationName() + "!");
        }
    }

    public boolean notCalculated() {
        return this.calculation.isNotCalculatable();
    }
}
