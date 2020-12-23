/**
 * This is a {@link DoubleTimeSeries} that requires further analysis, such
 * as a Calculation. This class is meant to be expandable
 *
 * @author <a href = mailto:apl1341@cs.rit.edu>Aidan LaBella</a>
 */

package org.ngafid.flights;

import java.sql.Connection;
import java.util.Map;

import org.ngafid.Database;

public class CalculatedDoubleTimeSeries extends DoubleTimeSeries {
    static Connection connection = Database.getConnection();

    private Calculation calculation;
    private String [] depNames;

    /**
     * Default Constructor
     *
     * By instantiating a {@link CalculatedDoubleTimeSeries} object, it performs the {@link Calculation} 
     * supplied to it
     *
     * @param calculation the calculation to perform
     */
    public CalculatedDoubleTimeSeries(Calculation calculation, String [] depNames) {
        super(calculation.getCalculationName(), "double");
        this.calculation = calculation;
        this.depNames = depNames;

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

            updateDatabase(connection, flightId);
        } else {
            System.err.println("WARNING: flight #" + flightId + " is not calculatable for " + calculation.getCalculationName() + "!");
        }
    }

    /**
     * Updates the database and its dependencies 
     *
     * @param connection the database {@link Connection}
     * @param flightId the flight ID
     */
    @Override
    public void updateDatabase(Connection connection, int flightId) {
        super.updateDatabase(connection, flightId);
        Map<String, DoubleTimeSeries> params = calculation.getParameters();

        for (String name : depNames) { 
            params.get(name).updateDatabase(connection, flightId);
        }
    }


    public boolean notCalculated() {
        return this.calculation.isNotCalculatable();
    }
}
