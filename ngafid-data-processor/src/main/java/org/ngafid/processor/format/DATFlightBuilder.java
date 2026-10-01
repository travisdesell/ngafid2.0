package org.ngafid.processor.format;

import java.util.Map;
import org.ngafid.core.flights.DoubleTimeSeries;
import org.ngafid.core.flights.FlightMeta;
import org.ngafid.core.flights.StringTimeSeries;

public class DATFlightBuilder extends FlightBuilder {

    /**
     * Constructs a DAT (DJI) flight builder from already-parsed series.
     *
     * @param meta the flight metadata
     * @param doubleTimeSeries the double time series keyed by name
     * @param stringTimeSeries the string time series keyed by name
     */
    public DATFlightBuilder(
            FlightMeta meta,
            Map<String, DoubleTimeSeries> doubleTimeSeries,
            Map<String, StringTimeSeries> stringTimeSeries) {
        super(meta, doubleTimeSeries, stringTimeSeries);
    }
}
