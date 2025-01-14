/**
 * This is a {@link DoubleTimeSeries} that requires further analysis, such
 * as a {@link Calculation}. This class is meant to be expandable
 *
 * @author <a href = mailto:apl1341@cs.rit.edu>Aidan LaBella @ RIT CS</a>
 */
package org.ngafid.flights.calculations;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Connection;
import org.ngafid.flights.*;

public class CalculatedDoubleTimeSeries extends DoubleTimeSeries {
    private final Flight flight;
    private final boolean cache;

    /**
     * Default Constructor
     *
     * @param connection the connection to the database
     * @param name the new name of the time series
     * @param dataType the dataType of the new time series, represented by a {@link String}
     * @param cache indicates if the new series should be stored in the database after all analysis is complete
     * @param flight the flight instance the time series is being calculated for
     */
    public CalculatedDoubleTimeSeries(Connection connection, String name,
                                      String dataType, boolean cache, Flight flight) throws SQLException {
        super(connection, name, dataType);
        this.flight = flight;
        this.cache = cache;
    }

    public CalculatedDoubleTimeSeries(String name, String dataType, boolean cache, Flight flight) throws SQLException {
        super(name, dataType);
        this.cache = cache;
        this.flight = flight;
    }

    /**
     * Runs the calculation
     *
     * @param calculation the calculation to use to get the new {@link DoubleTimeSeries}
     */
    public void create(Calculation calculation) throws IOException, SQLException {
        System.out.println("Performing " + super.getName() + " calculation");

        for (int i = 0; i < this.flight.getNumberRows(); i++) {
            super.add(calculation.calculate(i));
        }

        if (cache)
            flight.addDoubleTimeSeries(super.getName(), this);
    }
}
