/**
 * A {@link CalculationSeries} is a value object containing info pertaining to a flight and its respective 
 * {@link DoubleTimeSeries} data
 *
 * @author <a href = mailto:apl1341@cs.rit.edu>Aidan LaBella @ RIT CS</a>
 */

package org.ngafid.flights;

import java.util.Map;

public class CalculationSeries {
    private Flight flight;
    private Map<String, DoubleTimeSeries> parameters;
    private int length;

    /**
     * Constructor
     *
     * @param flight the flight that is being analyzed
     * @param parameters the set of parameters that are used for the calculation
     * @param refSize the size of the {@link DoubleTimeSeries} in readable seconds
     */
    public CalculationSeries(Flight flight, Map<String, DoubleTimeSeries> parameters, int refSize) {
        this.flight = flight;
        this.parameters = parameters;
        this.length = refSize;
    }

    /**
     * Gets the size that a new {@link DoubleTimeSeries} or {@link CalculatedDoubleTimeSeries} should be
     *
     * @return an int containing the size
     */
    public int size() {
        return this.length;
    }

    /**
     * Gets the id of the flight being analyzed
     *
     * @return an int containing the {@link Flight} id
     */
    public int getFlightId() {
        return this.flight.getId();
    }

    /**
     * Gets the parameters used for this calculation
     *
     * @return a {@link Map} containing the parameters
     */
    public Map<String, DoubleTimeSeries> getParameters() {
        return this.parameters;
    }

    /**
     * Adds a parameter to the parameters map
     *
     * @param name the name of the new {@link DoubleTimeSeries}
     * @param series the {@link DoubleTimeSeries} itself
     */
    public void add(String name, DoubleTimeSeries series) {
        this.parameters.put(name, series);
    }
}
