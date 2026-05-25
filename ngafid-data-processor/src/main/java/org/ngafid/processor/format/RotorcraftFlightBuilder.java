package org.ngafid.processor.format;

import java.util.Map;
import java.util.Set;
import org.ngafid.core.flights.DoubleTimeSeries;
import org.ngafid.core.flights.FlightMeta;
import org.ngafid.core.flights.Parameters;
import org.ngafid.core.flights.StringTimeSeries;

/**
 * Flight builder for rotorcraft CSV uploads. Maps recorder-specific column names (e.g. Appareo) to
 * {@link Parameters} names expected by compute steps.
 */
public final class RotorcraftFlightBuilder extends FlightBuilder {

    private static final Map<String, Set<String>> ALIASES = Map.ofEntries(
            Map.entry(Parameters.UNIX_TIME_SECONDS, Set.of("UNIX Time")),
            Map.entry(Parameters.GND_SPD, Set.of("Groundspeed")),
            Map.entry(Parameters.VSPD, Set.of("Vertical Speed")),
            Map.entry(Parameters.HDG, Set.of("True Heading")),
            Map.entry(Parameters.PITCH, Set.of("Pitch")),
            Map.entry(Parameters.ROLL, Set.of("Roll")),
            Map.entry(Parameters.YAW_RATE, Set.of("Yaw Rate")),
            Map.entry(
                    Parameters.ALT_AGL,
                    Set.of("Height Above Airfield", "Altitude Above Ground Level")),
            Map.entry(Parameters.ALT_B, Set.of("Pressure Altitude")),
            Map.entry(Parameters.LATITUDE, Set.of("Latitude")),
            Map.entry(Parameters.LONGITUDE, Set.of("Longitude")),
            Map.entry(Parameters.OAT, Set.of("TAT")),
            Map.entry(Parameters.LAT_AC, Set.of("Lateral Acceleration")),
            Map.entry(Parameters.NORM_AC, Set.of("Longitudinal Acceleration")));

    public RotorcraftFlightBuilder(
            FlightMeta meta,
            Map<String, DoubleTimeSeries> doubleTimeSeries,
            Map<String, StringTimeSeries> stringTimeSeries) {
        super(meta, doubleTimeSeries, stringTimeSeries);
    }

    @Override
    protected Map<String, Set<String>> getAliases() {
        return ALIASES;
    }
}
