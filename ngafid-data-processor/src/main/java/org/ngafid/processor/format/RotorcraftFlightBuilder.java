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

    /**
     * Lat/lon column pairs in priority order. Navigation-solution (NAV) columns are intentionally omitted —
     * they are unsuitable for map tracks on Appareo-style exports. The first pair with at least one valid
     * sample wins ({@link #promotePositionPair}). Metro OTL/HAA uses {@code Latitude}/{@code Longitude} via
     * {@link RotorcraftCSVFileProcessor}.
     */
    private static final String[][] POSITION_SOURCE_PAIRS = {
        {"GPS-PP_LAT", "GPS-PP_LNG"},
        {"GPS.PP_Latitude", "GPS.PP_Longitude"},
        {"GeneralPurpose-PP_LAT", "GeneralPurpose-PP_LNG"},
        {"Latitude", "Longitude"},
        {"Latitude (1)", "Longitude (1)"},
    };

    private static final Map<String, Set<String>> ALIASES = Map.ofEntries(
            Map.entry(Parameters.UNIX_TIME_SECONDS, Set.of("UNIX Time")),
            Map.entry(
                    Parameters.IAS,
                    Set.of("Airspeed", "GeneralPurpose-IAS", "GeneralPurpose-TRUE_AS",
                            "IAS1", "IAS2", "GP.CAS", "AP.IAS")),
            Map.entry(
                    Parameters.GND_SPD,
                    Set.of(
                            "Groundspeed",
                            "Ground Speed",
                            "GeneralPurpose-GS",
                            "GPS-GS",
                            "PNAV_GndSpd",
                            "PNAV GndSpd",
                            "PNAV_Tru_A/S",
                            "PNAV Tru A/S",
                            "GPS.Ground_Speed")),
            Map.entry(
                    Parameters.VSPD,
                    Set.of(
                            "Vertical Speed",
                            "Vertical Speed Inertial (AHRS)",
                            "GeneralPurpose-VS",
                            "ANALOG.VS",
                            "GYRO.Climb_Rate",
                            "Gyro-VS",
                            "Alt_Rate1",
                            "Alt Rate1",
                            "Alt_Rate2",
                            "Alt Rate2")),
            Map.entry(
                    Parameters.HDG,
                    Set.of(
                            "True Heading",
                            "Heading",
                            "Track",
                            "GeneralPurpose-MAG_HDG",
                            "Gyro-MAG_HDG",
                            "PNAV_Tr_Hdg",
                            "PNAV Tr Hdg",
                            "TruHdg-Bl-1",
                            "MagHdg-Bl-1",
                            "AFCS1 Mag Hdg (320)",
                            "AFCS2 Mag Hdg (320)",
                            "EFIS Mag Hdg (320)",
                            "GPS.Magnetic_Heading",
                            "ANALOG.Magnetic_Heading",
                            "AP.Magnetic_Heading",
                            "GYRO.Magnetic_Heading")),
            Map.entry(
                    Parameters.PITCH,
                    Set.of(
                            "Pitch",
                            "GeneralPurpose-PITCH",
                            "ANALOG.Pitch",
                            "GYRO.Pitch",
                            "Gyro-PITCH",
                            "PtchAn-Bl-1",
                            "PtchAn-In-1")),
            Map.entry(
                    Parameters.ROLL,
                    Set.of(
                            "Roll",
                            "GeneralPurpose-ROLL",
                            "ANALOG.Roll",
                            "GYRO.Roll",
                            "Gyro-ROLL",
                            "RollAn-Bl-1",
                            "RollAn-In-1")),
            Map.entry(Parameters.YAW_RATE, Set.of("Yaw Rate", "Gyro-YAW_RATE")),
            Map.entry(
                    Parameters.ENGINE_1_TORQUE,
                    Set.of(
                            "TRQ_1",
                            "E1 Torq",
                            "Eng (1) Torque",
                            "Eng 1 Torque",
                            "1_Torque",
                            "Torque 1(%)")),
            Map.entry(
                    Parameters.ENGINE_2_TORQUE,
                    Set.of(
                            "TRQ_2",
                            "E2 torque",
                            "Eng (2) Torque",
                            "Eng 2 Torque",
                            "2_Torque",
                            "Torque 2(%)")),
            Map.entry(
                    Parameters.ALT_AGL,
                    Set.of(
                            "Height Above Airfield",
                            "Altitude Above Ground Level",
                            "Altitude Radio (A)",
                            "Altitude Radio (B)",
                            "GeneralPurpose-RA",
                            "RadAlt_Inht")),
            Map.entry(
                    Parameters.ALT_MSL,
                    Set.of(
                            "Altitude",
                            "Altitude Baro (1)",
                            "Altitude Baro (2)",
                            "GeneralPurpose-BARO_ALT",
                            "GeneralPurpose-STD_ALT",
                            "GPS-BARO_ALT",
                            "Press_Alt1",
                            "Press Alt1",
                            "Press_Alt2",
                            "Press Alt2")),
            Map.entry(
                    Parameters.ALT_B,
                    Set.of("Pressure Altitude", "Press_Alt1", "Press Alt1", "Press_Alt2", "Press Alt2")),
            Map.entry(Parameters.FUEL_QTY_LEFT, Set.of("Fuel_Qty_1")),
            Map.entry(Parameters.FUEL_QTY_RIGHT, Set.of("Fuel_Qty_2")),
            Map.entry(Parameters.E1_RPM, Set.of("Eng1_N1", "Eng1_N2", "Nr1")),
            Map.entry(
                    Parameters.LATITUDE,
                    Set.of(
                            "Latitude",
                            "Latitude (1)",
                            "GPS-PP_LAT",
                            "GPS.PP_Latitude",
                            "GeneralPurpose-PP_LAT")),
            Map.entry(
                    Parameters.LONGITUDE,
                    Set.of(
                            "Longitude",
                            "Longitude (1)",
                            "GPS-PP_LNG",
                            "GPS.PP_Longitude",
                            "GeneralPurpose-PP_LNG")),
            Map.entry(Parameters.OAT, Set.of("TAT", "AFCS1 OAT (233)", "AFCS2 OAT (233)", "DAU OAT (233)")),
            Map.entry(
                    Parameters.LAT_AC,
                    Set.of(
                            "Lateral Acceleration",
                            "Acceleration Lateral (AHRS)",
                            "Gyro-LAT_ACC",
                            "GeneralPurpose-LAT_ACC")),
            Map.entry(
                    Parameters.NORM_AC,
                    Set.of(
                            "Acceleration Normal",
                            "Gyro-NRM_ACC",
                            "GeneralPurpose-NRM_ACC",
                            "Longitudinal Acceleration",
                            "Acceleration Longitudinal (AHRS)",
                            "Gyro-LNG_ACC",
                            "GeneralPurpose-LNG_ACC")));

    /**
     * Creates a rotorcraft flight builder that aliases recorder columns to canonical parameter keys.
     *
     * @param meta             the flight metadata
     * @param doubleTimeSeries numeric time series keyed by recorder column name
     * @param stringTimeSeries string time series keyed by recorder column name
     */
    public RotorcraftFlightBuilder(
            FlightMeta meta,
            Map<String, DoubleTimeSeries> doubleTimeSeries,
            Map<String, StringTimeSeries> stringTimeSeries) {
        super(meta, doubleTimeSeries, stringTimeSeries);
    }

    /**
     * Promotes recorder columns into canonical {@link Parameters} keys for storage/map APIs (numeric series only).
     *
     * @param doubleTimeSeries numeric time series to promote in-place
     */
    static void promoteForPersistence(Map<String, DoubleTimeSeries> doubleTimeSeries) {
        promoteForPersistence(doubleTimeSeries, null);
    }

    /**
     * Promotes recorder columns into canonical {@link Parameters} keys (numeric series only, plus optional USCG DMS).
     *
     * @param doubleTimeSeries numeric time series to promote in-place
     * @param stringTimeSeries string time series used for USCG DMS position conversion, may be null
     */
    static void promoteForPersistence(
            Map<String, DoubleTimeSeries> doubleTimeSeries, Map<String, StringTimeSeries> stringTimeSeries) {
        if (stringTimeSeries != null) {
            RotorcraftCSVFileProcessor.addCanonicalPositionFromPnavDms(doubleTimeSeries, stringTimeSeries);
        }
        promotePositionPair(doubleTimeSeries);
        for (Map.Entry<String, Set<String>> entry : ALIASES.entrySet()) {
            String canonical = entry.getKey();
            if (Parameters.UNIX_TIME_SECONDS.equals(canonical)
                    || Parameters.LATITUDE.equals(canonical)
                    || Parameters.LONGITUDE.equals(canonical)
                    || doubleTimeSeries.containsKey(canonical)) {
                continue;
            }
            for (String alias : entry.getValue()) {
                DoubleTimeSeries source = doubleTimeSeries.get(alias);
                if (source == null) {
                    continue;
                }
                doubleTimeSeries.put(canonical, copySeries(canonical, source));
                break;
            }
        }
    }

    /**
     * If canonical {@link Parameters#LATITUDE} is missing, copies the first lat/lon pair from
     * {@link #POSITION_SOURCE_PAIRS} with at least one non-zero, non-NaN sample.
     *
     * @param doubleTimeSeries numeric time series to search and update in-place
     */
    private static void promotePositionPair(Map<String, DoubleTimeSeries> doubleTimeSeries) {
        if (doubleTimeSeries.containsKey(Parameters.LATITUDE)) {
            return;
        }
        for (String[] pair : POSITION_SOURCE_PAIRS) {
            DoubleTimeSeries latSource = doubleTimeSeries.get(pair[0]);
            DoubleTimeSeries lonSource = doubleTimeSeries.get(pair[1]);
            if (latSource == null || lonSource == null || latSource.size() != lonSource.size()) {
                continue;
            }
            if (countValidPositions(latSource, lonSource) < 1) {
                continue;
            }
            doubleTimeSeries.put(Parameters.LATITUDE, copySeries(Parameters.LATITUDE, latSource));
            doubleTimeSeries.put(Parameters.LONGITUDE, copySeries(Parameters.LONGITUDE, lonSource));
            return;
        }
    }

    private static int countValidPositions(DoubleTimeSeries latSource, DoubleTimeSeries lonSource) {
        int validPoints = 0;
        for (int i = 0; i < latSource.size(); i++) {
            double lat = latSource.get(i);
            double lon = lonSource.get(i);
            if (!Double.isNaN(lat) && !Double.isNaN(lon) && lat != 0.0 && lon != 0.0) {
                validPoints++;
            }
        }
        return validPoints;
    }

    /**
     * Deep-copies a {@link DoubleTimeSeries} into a new series name (canonical parameter key).
     *
     * @param canonicalName the canonical parameter name for the new series
     * @param source        the source series to copy values from
     * @return a new {@link DoubleTimeSeries} with the same values as {@code source}
     */
    private static DoubleTimeSeries copySeries(String canonicalName, DoubleTimeSeries source) {
        DoubleTimeSeries canonical = new DoubleTimeSeries(canonicalName, source.getDataType(), source.size());
        for (int i = 0; i < source.size(); i++) {
            canonical.add(source.get(i));
        }
        return canonical;
    }

    /** Alias set consumed by the base {@link FlightBuilder} when computing derived parameters. */
    @Override
    protected Map<String, Set<String>> getAliases() {
        return ALIASES;
    }
}
