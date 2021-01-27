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

    private Flight flight;
    private Map<String, DoubleTimeSeries> calculationDeps;
    private int refSize;

    /**
     * Default Constructor
     *
     * @param name the new name of the time series
     * @param flight the flight to calculate for
     * @param calculationDeps the dependencies for this calculation in a {@link Map}
     * @param refSize the size that the new series should be
     */
    public CalculatedDoubleTimeSeries(String name, Flight flight, Map<String, DoubleTimeSeries> calculationDeps, int refSize) {
        super(name, "double");
        this.flight = flight;
        this.calculationDeps = calculationDeps;
        this.refSize = refSize;
    }

    /**
     * Runs the calculation
     *
     * @param calculation the calculation to use to get the new {@link DoubleTimeSeries}
     */
    public void create(Calculation calculation) {
        System.out.println("Performing " + super.getName() + " calculation on flight #" + flight.getId());

        for (int i = 0; i < refSize; i++) {
            super.add(calculation.calculate(this.calculationDeps, i));
        }

        this.calculationDeps.put(super.getName(), this);
        super.updateDatabase(connection, this.flight.getId());
    }
}
