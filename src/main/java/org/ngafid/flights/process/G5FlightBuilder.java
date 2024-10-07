package org.ngafid.flights.process;

import java.util.Map;
import java.util.Set;

import org.ngafid.flights.*;

/**
 * Flight builder for G5FlightBuilder
 */
public class G5FlightBuilder extends FlightBuilder {

    public G5FlightBuilder(FlightMeta meta, Map<String, DoubleTimeSeries> doubleTimeSeries,
            Map<String, StringTimeSeries> stringTimeSeries) {
        super(meta, doubleTimeSeries, stringTimeSeries);
    }

    private Map<String, Set<String>> ALIASES = Map.of(
            "AltAGL", Set.of("Altitude Above Ground Level"));

    @Override
    protected final Map<String, Set<String>> getAliases() {
        return ALIASES;
    }

}
