// ngafid-data-processor/src/main/java/org/ngafid/processor/format/FlightWarningChecks.java
package org.ngafid.processor.format;

import static java.util.Map.entry;
import static org.ngafid.core.flights.Airframes.*;
import static org.ngafid.core.flights.Parameters.*;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import org.ngafid.core.flights.DoubleTimeSeries;
import org.ngafid.core.flights.MalformedFlightFileException;
import org.ngafid.core.flights.StringTimeSeries;
import org.ngafid.core.util.TimeUtils;

final class FlightWarningChecks {
    private static final double LOW_FREQUENCY_THRESHOLD_SECONDS = 1.10;

    private static final Set<String> REMOTE_AIRFRAME_BLACKLIST = Set.of(AIRFRAME_SCAN_EAGLE, AIRFRAME_DJI);
    private static final List<String> TOTAL_FUEL_INPUTS = List.of(FUEL_QTY_LEFT, FUEL_QTY_RIGHT);
    private static final List<String> AGL_INPUTS = List.of(ALT_MSL, LATITUDE, LONGITUDE);
    private static final List<String> AIRPORT_PROXIMITY_INPUTS = List.of(LATITUDE, LONGITUDE, ALT_AGL);
    private static final List<String> ITINERARY_DOUBLE_INPUTS =
            List.of(ALT_AGL, LATITUDE, LONGITUDE, AIRPORT_DISTANCE, RUNWAY_DISTANCE, GND_SPD, E1_RPM);
    private static final List<String> ITINERARY_STRING_INPUTS = List.of(NEAREST_AIRPORT, NEAREST_RUNWAY);

    private record DivergenceConfig(List<String> parameters, String output) {}

    private static final List<DivergenceConfig> CESSNA_CONFIG = List.of(
            new DivergenceConfig(List.of("E1 CHT1", "E1 CHT2", "E1 CHT3", "E1 CHT4"), "E1 CHT Divergence"),
            new DivergenceConfig(List.of("E1 EGT1", "E1 EGT2", "E1 EGT3", "E1 EGT4"), "E1 EGT Divergence"));

    private static final List<DivergenceConfig> PA_28_CONFIG =
            List.of(new DivergenceConfig(List.of("E1 EGT1", "E1 EGT2", "E1 EGT3", "E1 EGT4"), "E1 EGT Divergence"));

    private static final List<DivergenceConfig> PA_44_CONFIG = List.of(
            new DivergenceConfig(List.of("E1 EGT1", "E1 EGT2", "E1 EGT3", "E1 EGT4"), "E1 EGT Divergence"),
            new DivergenceConfig(List.of("E2 EGT1", "E2 EGT2", "E2 EGT3", "E2 EGT4"), "E2 EGT Divergence"));

    private static final List<DivergenceConfig> SIX_CYLINDER_CONFIG = List.of(
            new DivergenceConfig(
                    List.of("E1 CHT1", "E1 CHT2", "E1 CHT3", "E1 CHT4", "E1 CHT5", "E1 CHT6"), "E1 CHT Divergence"),
            new DivergenceConfig(
                    List.of("E1 EGT1", "E1 EGT2", "E1 EGT3", "E1 EGT4", "E1 EGT5", "E1 EGT6"), "E1 EGT Divergence"));

    private static final List<DivergenceConfig> DIAMOND_CONFIG = List.of(
            new DivergenceConfig(List.of("E1 CHT1", "E1 CHT2", "E1 CHT3", "E1 CHT4"), "E1 CHT Divergence"),
            new DivergenceConfig(List.of("E1 EGT1", "E1 EGT2", "E1 EGT3", "E1 EGT4"), "E1 EGT Divergence"));

    private static final Map<String, List<DivergenceConfig>> DIVERGENCE_CONFIGS = Map.ofEntries(
            entry(AIRFRAME_CESSNA_172R, CESSNA_CONFIG),
            entry(AIRFRAME_CESSNA_172S, CESSNA_CONFIG),
            entry(AIRFRAME_PA_28_181, PA_28_CONFIG),
            entry(AIRFRAME_PA_44_180, PA_44_CONFIG),
            entry(AIRFRAME_CIRRUS_SR20, SIX_CYLINDER_CONFIG),
            entry(AIRFRAME_CESSNA_T182T, SIX_CYLINDER_CONFIG),
            entry(AIRFRAME_CESSNA_182T, SIX_CYLINDER_CONFIG),
            entry(AIRFRAME_BEECHCRAFT_A36_G36, SIX_CYLINDER_CONFIG),
            entry(AIRFRAME_CIRRUS_SR22, SIX_CYLINDER_CONFIG),
            entry(AIRFRAME_CESSNA_400, SIX_CYLINDER_CONFIG),
            entry(AIRFRAME_DIAMOND_DA_40_F, DIAMOND_CONFIG),
            entry(AIRFRAME_DIAMOND_DA_40, DIAMOND_CONFIG),
            entry(AIRFRAME_DIAMOND_DA40, DIAMOND_CONFIG));

    private FlightWarningChecks() {}

    static void addWarnings(FlightBuilder builder, boolean includeLegacyOptionalCalculationWarnings) {
        addFrequencyWarning(builder);

        if (includeLegacyOptionalCalculationWarnings) {
            addLegacyOptionalCalculationWarnings(builder);
        }
    }

    private static void addFrequencyWarning(FlightBuilder builder) {
        List<Double> timestamps = getUnixTimestamps(builder);
        if (timestamps.isEmpty()) timestamps = getUtcTimestamps(builder);

        double previous = Double.NaN;
        double totalInterval = 0;
        int intervalCount = 0;

        for (double timestamp : timestamps) {
            if (Double.isFinite(previous) && timestamp > previous) {
                totalInterval += timestamp - previous;
                intervalCount++;
            }
            previous = timestamp;
        }

        if (intervalCount == 0) return;

        double averageInterval = totalInterval / intervalCount;
        if (averageInterval > LOW_FREQUENCY_THRESHOLD_SECONDS) {
            addUniqueWarning(
                    builder,
                    String.format(
                            Locale.US, "Time series has frequency below 1Hz (avg interval %.2fs)", averageInterval));
        }
    }

    private static List<Double> getUnixTimestamps(FlightBuilder builder) {
        DoubleTimeSeries unixTime = builder.getDoubleTimeSeries(UNIX_TIME_SECONDS);
        if (unixTime == null) return Collections.emptyList();

        ArrayList<Double> timestamps = new ArrayList<>(unixTime.size());
        for (int i = 0; i < unixTime.size(); i++) {
            double value = unixTime.get(i);
            if (Double.isFinite(value)) timestamps.add(value);
        }
        return timestamps;
    }

    private static List<Double> getUtcTimestamps(FlightBuilder builder) {
        StringTimeSeries utcTime = builder.getStringTimeSeries(UTC_DATE_TIME);
        if (utcTime == null) return Collections.emptyList();

        ArrayList<Double> timestamps = new ArrayList<>(utcTime.size());
        for (int i = 0; i < utcTime.size(); i++) {
            String value = utcTime.get(i);
            if (value == null || value.isBlank()) continue;

            try {
                OffsetDateTime timestamp = OffsetDateTime.parse(value.trim(), TimeUtils.ISO_8601_FORMAT);
                timestamps.add(timestamp.toEpochSecond() + timestamp.getNano() / 1_000_000_000.0);
            } catch (DateTimeParseException ignored) {
                // Invalid timestamp cells are ignored by the warning pass.
            }
        }
        return timestamps;
    }

    private static void addLegacyOptionalCalculationWarnings(FlightBuilder builder) {
        addAltAglWarning(builder);
        addAirportProximityWarning(builder);
        addTotalFuelWarning(builder);
        addLaggedAltMslWarning(builder);
        addDivergenceWarnings(builder);
        addLociWarning(builder);
        addItineraryWarning(builder);
    }

    private static void addAltAglWarning(FlightBuilder builder) {
        if (hasDouble(builder, ALT_AGL)) return;

        List<String> missingInputs = missingDoubleInputs(builder, AGL_INPUTS);
        if (!missingInputs.isEmpty()) {
            addUniqueWarning(builder, missingColumnsMessage("Cannot calculate AGL", missingInputs));
        }
    }

    private static void addAirportProximityWarning(FlightBuilder builder) {
        if (hasDouble(builder, AIRPORT_DISTANCE)
                && hasDouble(builder, RUNWAY_DISTANCE)
                && hasString(builder, NEAREST_AIRPORT)
                && hasString(builder, NEAREST_RUNWAY)) {
            return;
        }

        List<String> missingInputs = missingDoubleInputs(builder, AIRPORT_PROXIMITY_INPUTS);
        if (!missingInputs.isEmpty()) {
            addUniqueWarning(
                    builder, missingColumnsMessage("Cannot calculate airport and runway distances", missingInputs));
        }
    }

    private static void addTotalFuelWarning(FlightBuilder builder) {
        if (hasDouble(builder, TOTAL_FUEL) || remoteAirframe(builder)) return;

        for (String input : TOTAL_FUEL_INPUTS) {
            if (missingDouble(builder, input)) {
                addUniqueWarning(builder, "Cannot calculate 'Total Fuel' as fuel parameter '" + input + "' was missing.");
                return;
            }
        }
    }

    private static void addLaggedAltMslWarning(FlightBuilder builder) {
        if (hasDouble(builder, ALT_MSL_LAG_DIFF) || remoteAirframe(builder) || hasDouble(builder, ALT_MSL)) return;

        addUniqueWarning(builder, "Cannot calculate 'AltMSL Lag Diff' as parameter 'AltMSL' was missing.");
    }

    private static void addDivergenceWarnings(FlightBuilder builder) {
        List<DivergenceConfig> configs = DIVERGENCE_CONFIGS.get(airframeName(builder));
        if (configs == null) return;

        for (DivergenceConfig config : configs) {
            if (hasDouble(builder, config.output)) continue;

            for (String parameter : config.parameters) {
                if (missingDouble(builder, parameter)) {
                    addUniqueWarning(builder, "Cannot calculate '" + config.output + "' as parameter '" + parameter
                            + "' was missing.");
                    break;
                }
            }
        }
    }

    private static void addLociWarning(FlightBuilder builder) {
        if (hasDouble(builder, LOCI) || !airframeName(builder).contains("C172")) return;

        for (String parameter : LOCI_DEPENDENCIES) {
            if (missingDouble(builder, parameter)) {
                addUniqueWarning(
                        builder, "Cannot calculate 'LOC-I Index' as parameter '" + parameter + "' was missing.");
                return;
            }
        }

        if (missingDouble(builder, STALL_PROB)) {
            addUniqueWarning(builder, "Cannot calculate 'LOC-I Index' as parameter '" + STALL_PROB + "' was missing.");
        }
    }

    private static void addItineraryWarning(FlightBuilder builder) {
        if (!builder.getItinerary().isEmpty() || missingDouble(builder, E1_RPM)) return;

        for (String parameter : ITINERARY_DOUBLE_INPUTS) {
            if (missingDouble(builder, parameter)) {
                addUniqueWarning(builder, "Cannot calculate itinerary as parameter '" + parameter + "' was missing.");
                return;
            }
        }

        for (String parameter : ITINERARY_STRING_INPUTS) {
            if (missingString(builder, parameter)) {
                addUniqueWarning(builder, "Cannot calculate itinerary as parameter '" + parameter + "' was missing.");
                return;
            }
        }
    }

    private static String missingColumnsMessage(String prefix, List<String> missingInputs) {
        String columns = quoteJoin(missingInputs);
        return prefix + ", flight file had empty or missing " + columns + " column"
                + (missingInputs.size() == 1 ? "." : "s.");
    }

    private static String quoteJoin(List<String> values) {
        if (values.size() == 1) return "'" + values.get(0) + "'";
        if (values.size() == 2) return "'" + values.get(0) + "' and '" + values.get(1) + "'";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(i == values.size() - 1 ? " and " : ", ");
            sb.append("'").append(values.get(i)).append("'");
        }
        return sb.toString();
    }

    private static List<String> missingDoubleInputs(FlightBuilder builder, List<String> inputs) {
        ArrayList<String> missing = new ArrayList<>();
        for (String input : inputs) {
            if (missingDouble(builder, input)) missing.add(input);
        }
        return missing;
    }

    private static boolean remoteAirframe(FlightBuilder builder) {
        String airframe = airframeName(builder);
        for (String blacklisted : REMOTE_AIRFRAME_BLACKLIST) {
            if (airframe.contains(blacklisted)) return true;
        }
        return false;
    }

    private static String airframeName(FlightBuilder builder) {
        if (builder.meta.getAirframe() == null || builder.meta.getAirframe().getName() == null) return "";
        return builder.meta.getAirframe().getName();
    }

    private static boolean hasDouble(FlightBuilder builder, String name) {
        DoubleTimeSeries series = builder.getDoubleTimeSeries(name);
        return series != null && series.validCount() > 0;
    }

    private static boolean hasString(FlightBuilder builder, String name) {
        StringTimeSeries series = builder.getStringTimeSeries(name);
        return series != null && series.validCount() > 0;
    }

    private static boolean missingDouble(FlightBuilder builder, String name) {
        return !hasDouble(builder, name);
    }

    private static boolean missingString(FlightBuilder builder, String name) {
        return !hasString(builder, name);
    }

    private static void addUniqueWarning(FlightBuilder builder, String message) {
        for (MalformedFlightFileException exception : builder.exceptions) {
            if (message.equals(exception.getMessage())) return;
        }

        builder.exceptions.add(new MalformedFlightFileException(message));
    }
}
