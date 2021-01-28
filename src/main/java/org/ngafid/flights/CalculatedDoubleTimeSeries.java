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

    private CalculationSeries calculationSeries;

    // Indicates whether or not to store this new series in the database
    private boolean cache;

    /**
     * Default Constructor
     *
     * @param name the new name of the time series
     * @param cache indicates if the new series should be stored in the database after all analysis is complete
     * @param calculationSeries is the {@link CalculationSeries} object containg information about this new series and the flight itself
     */
    public CalculatedDoubleTimeSeries(String name, boolean cache, CalculationSeries calculationSeries) {
        super(name, "double");
        this.cache = cache;
        this.calculationSeries = calculationSeries;
    }

    /**
     * Runs the calculation
     *
     * @param calculation the calculation to use to get the new {@link DoubleTimeSeries}
     */
    public void create(Calculation calculation) {
        System.out.println("Performing " + super.getName() + " calculation on flight #" + calculationSeries.getFlightId());

        for (int i = 0; i < calculationSeries.size(); i++) {
            super.add(calculation.calculate(calculationSeries.getParameters(), i));
        }

        calculationSeries.add(super.getName(), this);
        if (this.cache) super.updateDatabase(connection, calculationSeries.getFlightId());
    }
}
