package org.ngafid.processor.format;

import org.ngafid.core.flights.DoubleTimeSeries;
import org.ngafid.core.flights.FlightMeta;
import org.ngafid.core.flights.StringTimeSeries;

import java.util.Map;

public class DATFlightBuilder extends FlightBuilder {

    public DATFlightBuilder(FlightMeta meta, Map<String, DoubleTimeSeries> doubleTimeSeries,
                            Map<String, StringTimeSeries> stringTimeSeries) {
        super(meta, doubleTimeSeries, stringTimeSeries);
    }
}
