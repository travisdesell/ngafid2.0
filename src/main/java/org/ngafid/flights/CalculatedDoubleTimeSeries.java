package org.ngafid.flights;

/**
 * This is a {@link DoubleTimeSeries} that requires further analysis, such
 * as a {@link Calculation}. This class is meant to be expandable
 *
 * @author <a href = mailto:apl1341@cs.rit.edu>Aidan LaBella @ RIT CS</a>
 */

public class CalculatedDoubleTimeSeries extends DoubleTimeSeries {
    private final Flight flight;

    /**
     * Default Constructor
     *
     * @param name the new name of the time series
     * @param dataType the dataType of the new timeseries, represented by a {@link String}
     * @param cache indicates if the new series should be stored in the database after all analysis is complete
     * @param flight the flight instance the timeseries is being calcualted for
     */
    public CalculatedDoubleTimeSeries(String name, String dataType, boolean cache, Flight flight) {
        super(name, dataType, cache);
        this.flight = flight;
    }

    /**
     * Runs the calculation
     *
     * @param calculation the calculation to use to get the new {@link DoubleTimeSeries}
     */
    public void create(Calculation calculation) {
        System.out.println("Performing " + super.getName() + " calculation");

        for (int i = 0; i < this.flight.getNumberRows(); i++) {
            super.add(calculation.calculate(i));
        }

        flight.addDoubleTimeSeries(super.getName(), this);
    }
}
