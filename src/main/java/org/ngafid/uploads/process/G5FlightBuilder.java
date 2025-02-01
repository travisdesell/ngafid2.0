package org.ngafid.uploads.process;

import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.StringTimeSeries;

import java.util.Map;
import java.util.Set;

/**
 * Flight builder for G5FlightBuilder
 */
public class G5FlightBuilder extends FlightBuilder {

    public G5FlightBuilder(FlightMeta meta, Map<String, DoubleTimeSeries> doubleTimeSeries,
                           Map<String, StringTimeSeries> stringTimeSeries) {
        super(meta, doubleTimeSeries, stringTimeSeries);
    }

    private final Map<String, Set<String>> aliases = Map.of(
            "AltAGL", Set.of("Altitude Above Ground Level"));

    @Override
    protected final Map<String, Set<String>> getAliases() {
        return aliases;
    }

}
