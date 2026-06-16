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

    /** First pair with enough valid samples wins ({@link #promoteForPersistence}). */
    private static final String[][] POSITION_SOURCE_PAIRS = {
        {"GPS-NAV_LAT", "GPS-NAV_LNG"},
        {"GPS.NAV_Latitude", "GPS.NAV_Longitude"},
        {"GPS.PP_Latitude", "GPS.PP_Longitude"},
        {"GeneralPurpose-PP_LAT", "GeneralPurpose-PP_LNG"},
        {"Latitude (1)", "Longitude (1)"},
        {"GeneralPurpose-NAV_LAT", "GeneralPurpose-NAV_LNG"},
    };

    private static final Map<String, Set<String>> ALIASES = Map.ofEntries(
            Map.entry(Parameters.UNIX_TIME_SECONDS, Set.of("UNIX Time")),
            Map.entry(
                    Parameters.IAS,
                    Set.of(
                            "Airspeed",
                            "GeneralPurpose-IAS",
                            "GeneralPurpose-TRUE_AS",
                            "IAS1",
                            "IAS2",
                            "GP.CAS",
                            "AP.IAS")),
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
                            "GPS-NAV_LAT",
                            "GPS.NAV_Latitude",
                            "GPS.PP_Latitude",
                            "GeneralPurpose-PP_LAT",
                            "GeneralPurpose-NAV_LAT")),
            Map.entry(
                    Parameters.LONGITUDE,
                    Set.of(
                            "Longitude",
                            "Longitude (1)",
                            "GPS-NAV_LNG",
                            "GPS.NAV_Longitude",
                            "GPS.PP_Longitude",
                            "GeneralPurpose-PP_LNG",
                            "GeneralPurpose-NAV_LNG")),
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

    /** Creates a rotorcraft flight builder that aliases recorder columns to canonical parameter keys. */
    public RotorcraftFlightBuilder(
            FlightMeta meta,
            Map<String, DoubleTimeSeries> doubleTimeSeries,
            Map<String, StringTimeSeries> stringTimeSeries) {
        super(meta, doubleTimeSeries, stringTimeSeries);
    }

    /**
     * Promotes recorder columns into canonical {@link Parameters} keys for storage/map APIs (numeric series only).
     */
    static void promoteForPersistence(Map<String, DoubleTimeSeries> doubleTimeSeries) {
        promoteForPersistence(doubleTimeSeries, null);
    }

    /**
     * Promotes recorder columns into canonical {@link Parameters} keys (numeric series only, plus optional USCG DMS).
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
     * If canonical {@link Parameters#LATITUDE} is missing, builds it from the first lat/lon recorder pair
     * with at least one non-zero, non-NaN sample.
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
            int validPoints = 0;
            for (int i = 0; i < latSource.size(); i++) {
                double lat = latSource.get(i);
                double lon = lonSource.get(i);
                if (!Double.isNaN(lat) && !Double.isNaN(lon) && lat != 0.0 && lon != 0.0) {
                    validPoints++;
                }
            }
            if (validPoints < 1) {
                continue;
            }
            doubleTimeSeries.put(Parameters.LATITUDE, copySeries(Parameters.LATITUDE, latSource));
            doubleTimeSeries.put(Parameters.LONGITUDE, copySeries(Parameters.LONGITUDE, lonSource));
            return;
        }
    }

    /** Deep-copies a {@link DoubleTimeSeries} into a new series name (canonical parameter key). */
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
