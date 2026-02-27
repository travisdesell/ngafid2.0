package org.ngafid.core.bin;

import org.ngafid.core.flights.DoubleTimeSeries;
import org.ngafid.core.flights.Flight;
import org.ngafid.core.flights.Parameters;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Labels flight phases from altitude, ground speed, and RPM time series.
 * <p>
 * Pipeline: (1) Initial phases from RPM/speed/altitude — TAXI, TAKEOFF, CLIMB, CRUISE, DESCENT, LANDING, GROUND.
 * (2) Touch-and-go and go-around detection; (3) Post-process: smooth noise, reclassify UNKNOWNs, lock ground/sustained trend.
 */
public final class FlightPhaseProcessor {

    // ----- Altitude (ft AGL) -----
    private static final double GROUND_ALT_FT = 5.0;
    private static final double GROUND_CONTEXT_ALT_FT = 10.0;   // also "valid flight" min (max alt > this)
    private static final double MAX_ALT_AGL_FOR_SHORT_RUN_RECLASSIFY_FT = 20.0;
    private static final double LANDING_ALT_FT = 100.0;
    private static final double PATTERN_ALT_FT = 200.0;          // pattern / touch-and-go "climbed high"
    private static final double CRUISE_ALT_FT = 600.0;
    private static final double DESCENT_ALT_CHANGE_FT = 10.0;
    private static final double SUSTAINED_TREND_THRESHOLD_FT = 5.0;
    private static final double GO_AROUND_CLIMB_RECOVERY_FT = 50.0;

    // ----- Speed (kts) -----
    private static final double GROUND_SPEED_STATIONARY_KTS = 5.0;
    private static final double TAXI_SPEED_MAX_KTS = 8.0;
    private static final double MIN_TAKEOFF_ROLL_SPEED_KTS = 15.0;  // takeoff phase + touch-and-go rolling
    private static final double TAKEOFF_SPEED_MAX_KTS = 80.0;

    // ----- RPM -----
    private static final double TAKEOFF_RPM = 2100.0;

    // ----- Row counts -----
    private static final int NOISE_WINDOW_ROWS = 5;              // sustained trend + ground-context window
    private static final int TAKEOFF_DURATION_ROWS = 15;
    private static final int TOUCH_AND_GO_MIN_GROUND_ROWS = 10;
    private static final int GO_AROUND_WINDOW_ROWS = 10;
    private static final int MIN_TAXI_ROWS = 30;
    private static final int MAX_SHORT_AIRBORNE_RUN = NOISE_WINDOW_ROWS - 1;
    private static final int MIN_GROUND_SURROUND_FOR_SHORT_RUN = NOISE_WINDOW_ROWS / 2;

    /**
     * Flight phase enumeration representing different stages of flight
     */
    public enum FlightPhase {
        GROUND,         // Aircraft on ground, not moving (altitude <= 5 ft, speed < 5 knots)
        TAXI,           // Aircraft taxiing on ground
        TAKEOFF,        // Aircraft taking off (first 15 rows meeting takeoff criteria)
        CLIMB,          // Aircraft climbing (below 600 ft and climbing)
        CRUISE,         // Aircraft in cruise phase (altitude >= 600 ft)
        DESCENT,        // Aircraft descending (below 600 ft and descending)
        LANDING,        // Aircraft landing (below 100 ft and descending)
        TOUCH_AND_GO,   // Ground contact followed by immediate takeoff (altitude < 5 ft for 10+ rows)
        GO_AROUND,      // Aborted landing - valley pattern (descent to <100 ft, then climb without landing)
        UNKNOWN         // Phase cannot be determined (should be eliminated by post-processing)
    }

    /** Phase at each time index; used for CSV export and analysis. */
    public static class FlightPhaseData {
        private final List<FlightPhase> phases;
        private final int numberOfRows;

        public FlightPhaseData(List<FlightPhase> phases, int numberOfRows) {
            this.phases = phases;
            this.numberOfRows = numberOfRows;
        }

        public List<FlightPhase> getPhases() {
            return phases;
        }

        public int getNumberOfRows() {
            return numberOfRows;
        }

        /**
         * Get the phase at a specific row index
         * @param index the row index
         * @return the flight phase at that index
         */
        public FlightPhase getPhaseAt(int index) {
            if (index < 0 || index >= phases.size()) {
                return FlightPhase.UNKNOWN;
            }
            return phases.get(index);
        }

        /** Phase name for CSV export. */
        public String getPhaseStringAt(int index) {
            return getPhaseAt(index).name();
        }
    }

    /** Validation result: isValid, touch-and-go split indices (for phase marking), maxAltAGL. File splitting uses {@link #detectProlongedTaxiSplits}. */
    public static class FlightValidationResult {
        public final boolean isValid;
        public final List<Integer> splitIndices;
        public final double maxAltAGL;
        
        public FlightValidationResult(boolean isValid, List<Integer> splitIndices, double maxAltAGL) {
            this.isValid = isValid;
            this.splitIndices = splitIndices;
            this.maxAltAGL = maxAltAGL;
        }
        
        public boolean hasTouchAndGo() {
            return !splitIndices.isEmpty();
        }
    }

    private FlightPhaseProcessor() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Validates flight (max AltAGL &gt; 10 ft) and detects touch-and-go indices for phase marking.
     * Touch-and-go: 10+ consecutive rows with AltAGL &lt; 5 ft after having climbed above 200 ft.
     * splitIndices are for TOUCH_AND_GO marking only; file splitting uses {@link #detectProlongedTaxiSplits}.
     */
    public static FlightValidationResult validateAndDetectTouchAndGo(double[] altAGLValues) {
        if (altAGLValues == null || altAGLValues.length == 0) {
            return new FlightValidationResult(false, new ArrayList<>(), 0.0);
        }

        double maxAltAGL = Double.NEGATIVE_INFINITY;
        boolean hasClimbedHigh = false;
        int consecutiveZeroCount = 0;
        int zeroStartIndex = -1;
        List<Integer> splitIndices = new ArrayList<>();

        for (int i = 0; i < altAGLValues.length; i++) {
            double alt = altAGLValues[i];
            if (Double.isNaN(alt)) continue;
            if (alt > maxAltAGL) maxAltAGL = alt;
            if (alt > PATTERN_ALT_FT) hasClimbedHigh = true;
            if (hasClimbedHigh && alt < GROUND_ALT_FT) {
                if (consecutiveZeroCount == 0) zeroStartIndex = i;
                consecutiveZeroCount++;
            } else if (hasClimbedHigh && alt >= GROUND_ALT_FT && consecutiveZeroCount >= TOUCH_AND_GO_MIN_GROUND_ROWS) {
                splitIndices.add(zeroStartIndex + consecutiveZeroCount / 2);
                consecutiveZeroCount = 0;
                zeroStartIndex = -1;
            } else if (alt >= GROUND_ALT_FT) {
                consecutiveZeroCount = 0;
                zeroStartIndex = -1;
            }
        }
        return new FlightValidationResult(maxAltAGL > GROUND_CONTEXT_ALT_FT, splitIndices, maxAltAGL);
    }

    /**
     * Detects prolonged taxi (30+ s) in the middle of a flight for splitting one CSV into multiple files.
     * Taxi = alt &lt; 5 ft, speed &lt; 8 kts, RPM &lt; 2100. Excludes initial and final taxi.
     * @return Split indices (midpoints of prolonged taxi windows), empty if none
     */
    public static List<Integer> detectProlongedTaxiSplits(Connection connection, int flightId)
            throws SQLException, IOException {
        double[] altAGL = getAltAGLValues(connection, flightId, Integer.MAX_VALUE);
        double[] groundSpeed = getDoubleSeriesValues(connection, flightId, Parameters.GND_SPD, Integer.MAX_VALUE);
        double[] rpm = null;
        try {
            rpm = getDoubleSeriesValues(connection, flightId, Parameters.E1_RPM, Integer.MAX_VALUE);
        } catch (Exception ignored) {
            // RPM may not be available for all aircraft
        }
        return detectProlongedTaxiSplitsFromSeries(altAGL, groundSpeed, rpm);
    }

    /** Taxi criteria: alt &lt; 5 ft, speed &lt; 8 kts, RPM &lt; 2100. Only after having climbed above 200 ft. */
    public static List<Integer> detectProlongedTaxiSplitsFromSeries(
            double[] altAGLValues, double[] groundSpeedValues, double[] rpmValues) {
        List<Integer> splits = new ArrayList<>();
        if (altAGLValues == null || altAGLValues.length == 0) return splits;
        int n = altAGLValues.length;
        if (groundSpeedValues == null || groundSpeedValues.length == 0) return splits;
        n = Math.min(n, groundSpeedValues.length);

        boolean hasClimbedHigh = false;
        int consecCount = 0;
        int startIdx = -1;
        int rpmLen = (rpmValues != null) ? rpmValues.length : 0;

        for (int i = 0; i < n; i++) {
            double alt = altAGLValues[i];
            double gs = (i < groundSpeedValues.length) ? groundSpeedValues[i] : Double.NaN;
            double r = (rpmValues != null && i < rpmLen) ? rpmValues[i] : Double.NaN;

            if (Double.isNaN(alt)) { consecCount = 0; startIdx = -1; continue; }
            if (alt > PATTERN_ALT_FT) hasClimbedHigh = true;

            boolean isTaxi = alt < GROUND_ALT_FT
                    && !Double.isNaN(gs) && gs < TAXI_SPEED_MAX_KTS
                    && (Double.isNaN(r) || r < TAKEOFF_RPM);

            if (hasClimbedHigh && isTaxi) {
                if (consecCount == 0) startIdx = i;
                consecCount++;
            } else if (hasClimbedHigh && !isTaxi && consecCount >= MIN_TAXI_ROWS) {
                splits.add(startIdx + consecCount / 2);
                consecCount = 0;
                startIdx = -1;
            } else if (!isTaxi) {
                consecCount = 0;
                startIdx = -1;
            }
        }
        return splits;
    }

    private static double[] getDoubleSeriesValues(Connection connection, int flightId,
                                                   String columnName, int maxRows)
            throws SQLException, IOException {
        DoubleTimeSeries series = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, columnName);
        int len = Math.min(series.size(), maxRows);
        double[] arr = new double[len];
        for (int i = 0; i < len; i++) arr[i] = series.get(i);
        return arr;
    }

    /**
     * Detects go-arounds: descent below 100 ft, valley &gt; 5 ft (no touchdown), then climb ≥50 ft.
     * @return Indices of valley bottoms (go-around points)
     */
    public static List<Integer> detectGoArounds(double[] altAglArray) {
        List<Integer> goAroundIndices = new ArrayList<>();
        final int minClimbWindow = 10;

        for (int i = 1; i < altAglArray.length - minClimbWindow; i++) {
            double alt = altAglArray[i];
            if (Double.isNaN(alt)) continue;
            if (alt >= LANDING_ALT_FT || alt <= GROUND_ALT_FT) continue;
            if (Double.isNaN(altAglArray[i - 1]) || Double.isNaN(altAglArray[i + 1])) continue;
            if (alt >= altAglArray[i - 1] || alt >= altAglArray[i + 1]) continue;

            int climbEnd = -1;
            for (int j = i + 1; j < Math.min(i + 30, altAglArray.length); j++) {
                if (!Double.isNaN(altAglArray[j]) && altAglArray[j] - alt >= GO_AROUND_CLIMB_RECOVERY_FT) {
                    climbEnd = j;
                    break;
                }
            }
            if (climbEnd == -1) continue;

            int windowStart = Math.max(0, i - GO_AROUND_WINDOW_ROWS);
            int windowEnd = Math.min(altAglArray.length - 1, i + GO_AROUND_WINDOW_ROWS);
            boolean touchesGround = false;
            for (int k = windowStart; k <= windowEnd; k++) {
                if (!Double.isNaN(altAglArray[k]) && altAglArray[k] <= GROUND_ALT_FT) {
                    touchesGround = true;
                    break;
                }
            }
            if (!touchesGround) {
                goAroundIndices.add(i);
                i = i + 11;
            }
        }
        return goAroundIndices;
    }

    /** Computes phases from DB time series (no post-processing). Prefer {@link #computeCompleteFlightPhases} when validation is available. */
    public static FlightPhaseData computeFlightPhases(Connection connection, Flight flight)
            throws SQLException, IOException {
        DoubleTimeSeries altAgl = flight.getDoubleTimeSeries(connection, Parameters.ALT_AGL);
        DoubleTimeSeries groundSpeed = flight.getDoubleTimeSeries(connection, Parameters.GND_SPD);
        DoubleTimeSeries rpm = null;
        try {
            rpm = flight.getDoubleTimeSeries(connection, Parameters.E1_RPM);
        } catch (Exception ignored) {
            System.err.println("Warning: RPM not available for flight " + flight.getId() + ", using alternative phase detection");
        }
        return computeFlightPhasesFromTimeSeries(altAgl, groundSpeed, rpm);
    }

    /**
     * Compute flight phases from already-loaded time series.
     * Order: TAXI → TAKEOFF (15 rows) → CLIMB until 600 ft → CRUISE (≥600 ft) → DESCENT / LANDING → GROUND.
     * TOUCH_AND_GO and GO_AROUND are applied later in post-processing.
     */
    public static FlightPhaseData computeFlightPhasesFromTimeSeries(
            DoubleTimeSeries altAgl,
            DoubleTimeSeries groundSpeed,
            DoubleTimeSeries rpm) {

        int numRows = altAgl.size();
        List<FlightPhase> phases = new ArrayList<>(numRows);
        for (int i = 0; i < numRows; i++) {
            phases.add(FlightPhase.UNKNOWN);
        }

        int takeoffEnd = markTaxiAndTakeoff(phases, altAgl, groundSpeed, rpm, numRows);
        int climbEnd = markClimb(phases, altAgl, groundSpeed, takeoffEnd, numRows);
        markCruise(phases, altAgl, climbEnd, numRows);
        markDescentAndLanding(phases, altAgl, numRows);
        markGround(phases, altAgl, groundSpeed, numRows);

        return new FlightPhaseData(phases, numRows);
    }

    /** TAXI from start until takeoff criteria; TAKEOFF = next 15 rows with RPM ≥2100, 14.5 &lt; speed &lt; 80 kts. Returns index after TAKEOFF block. */
    private static int markTaxiAndTakeoff(List<FlightPhase> phases, DoubleTimeSeries altAgl,
                                           DoubleTimeSeries groundSpeed, DoubleTimeSeries rpm, int numRows) {
        int taxiEndIdx = -1;
        for (int i = 0; i < numRows; i++) {
            if (rpm != null && !Double.isNaN(rpm.get(i)) && !Double.isNaN(groundSpeed.get(i))
                    && rpm.get(i) >= TAKEOFF_RPM && groundSpeed.get(i) > MIN_TAKEOFF_ROLL_SPEED_KTS && groundSpeed.get(i) < TAKEOFF_SPEED_MAX_KTS) {
                taxiEndIdx = i;
                break;
            }
        }
        for (int i = 0; i < numRows && (taxiEndIdx < 0 || i < taxiEndIdx); i++) {
            phases.set(i, FlightPhase.TAXI);
        }
        if (taxiEndIdx < 0) return 0;
        int takeoffEnd = Math.min(taxiEndIdx + TAKEOFF_DURATION_ROWS, numRows);
        boolean meetsTakeoff = rpm != null && !Double.isNaN(rpm.get(taxiEndIdx)) && !Double.isNaN(groundSpeed.get(taxiEndIdx))
                && rpm.get(taxiEndIdx) >= TAKEOFF_RPM && groundSpeed.get(taxiEndIdx) > MIN_TAKEOFF_ROLL_SPEED_KTS && groundSpeed.get(taxiEndIdx) < TAKEOFF_SPEED_MAX_KTS;
        if (meetsTakeoff) {
            for (int i = taxiEndIdx; i < takeoffEnd; i++) {
                phases.set(i, FlightPhase.TAKEOFF);
            }
        }
        return takeoffEnd;
    }

    /** CLIMB from takeoffEnd until AGL ≥ 600 ft. Rows at or above 600 ft are set to CRUISE. Returns first index at or above cruise altitude. */
    private static int markClimb(List<FlightPhase> phases, DoubleTimeSeries altAgl,
                                 DoubleTimeSeries groundSpeed, int takeoffEnd, int numRows) {
        int climbIdx = takeoffEnd;
        while (climbIdx < numRows) {
            if (phases.get(climbIdx) != FlightPhase.UNKNOWN) {
                climbIdx++;
                continue;
            }
            if (!Double.isNaN(altAgl.get(climbIdx)) && !Double.isNaN(groundSpeed.get(climbIdx))) {
                if (altAgl.get(climbIdx) >= CRUISE_ALT_FT) {
                    phases.set(climbIdx, FlightPhase.CRUISE);
                    climbIdx++;
                    break;
                }
                phases.set(climbIdx, FlightPhase.CLIMB);
            }
            climbIdx++;
        }
        return climbIdx;
    }

    /** CRUISE: all UNKNOWN rows with AGL ≥ 600 ft (e.g. after climb or later in flight). */
    private static void markCruise(List<FlightPhase> phases, DoubleTimeSeries altAgl, int fromIdx, int numRows) {
        for (int j = fromIdx; j < numRows; j++) {
            if (phases.get(j) == FlightPhase.UNKNOWN && !Double.isNaN(altAgl.get(j)) && altAgl.get(j) >= CRUISE_ALT_FT) {
                phases.set(j, FlightPhase.CRUISE);
            }
        }
    }

    /** DESCENT: 100–600 ft descending or &gt;200 ft with drop &gt;10 ft. LANDING: &lt;100 ft and descending. Single pass. */
    private static void markDescentAndLanding(List<FlightPhase> phases, DoubleTimeSeries altAgl, int numRows) {
        for (int i = 1; i < numRows; i++) {
            if (phases.get(i) == FlightPhase.TAKEOFF) continue;
            if (phases.get(i) != FlightPhase.UNKNOWN) continue;
            double alt = altAgl.get(i);
            double prevAlt = altAgl.get(i - 1);
            if (Double.isNaN(alt) || Double.isNaN(prevAlt)) continue;
            double altChange = alt - prevAlt;

            if (alt < LANDING_ALT_FT && alt > GROUND_ALT_FT && alt < prevAlt) {
                phases.set(i, FlightPhase.LANDING);
            } else if (alt < CRUISE_ALT_FT && alt >= LANDING_ALT_FT && (altChange < -DESCENT_ALT_CHANGE_FT || phases.get(i - 1) == FlightPhase.DESCENT)) {
                phases.set(i, FlightPhase.DESCENT);
            } else if (alt > PATTERN_ALT_FT && altChange < -DESCENT_ALT_CHANGE_FT) {
                phases.set(i, FlightPhase.DESCENT);
            }
        }
    }

    /** GROUND: AGL ≤ 5 ft and speed = 0. */
    private static void markGround(List<FlightPhase> phases, DoubleTimeSeries altAgl,
                                   DoubleTimeSeries groundSpeed, int numRows) {
        for (int i = 0; i < numRows; i++) {
            if (phases.get(i) == FlightPhase.TAKEOFF) continue;
            if (phases.get(i) != FlightPhase.UNKNOWN) continue;
            if (!Double.isNaN(altAgl.get(i)) && !Double.isNaN(groundSpeed.get(i))
                    && altAgl.get(i) <= GROUND_ALT_FT && groundSpeed.get(i) == GROUND_SPEED_STATIONARY_KTS) {
                phases.set(i, FlightPhase.GROUND);
            }
        }
    }

    /** Full pipeline: initial phases + touch-and-go/go-around marking + reclassify UNKNOWNs + smooth short airborne runs. */
    public static FlightPhaseData computeCompleteFlightPhases(
            Connection connection,
            Flight flight,
            FlightValidationResult validation) throws SQLException, IOException {
        FlightPhaseData phaseData = computeFlightPhases(connection, flight);
        if (validation == null) return phaseData;
        double[] altAglArray = getAltAGLValues(connection, flight.getId(), Integer.MAX_VALUE);
        DoubleTimeSeries groundSpeed = flight.getDoubleTimeSeries(connection, Parameters.GND_SPD);
        applyCompletePhaseProcessing(phaseData, altAglArray, groundSpeed, validation);
        return phaseData;
    }

    /**
     * Compute complete flight phases from a pre-computed AltAGL array (e.g. from terrain fallback).
     * Use when AltAGL is not in DB but was computed from AltMSL/AltB + terrain.
     */
    public static FlightPhaseData computeCompleteFlightPhasesFromAltAGLArray(
            Connection connection, int flightId, double[] altAglArray,
            FlightValidationResult validation) throws SQLException, IOException {
        Flight flight = Flight.getFlight(connection, flightId);
        DoubleTimeSeries altAglTS = new DoubleTimeSeries(Parameters.ALT_AGL, Parameters.Unit.FT_AGL, altAglArray);
        DoubleTimeSeries groundSpeed = flight.getDoubleTimeSeries(connection, Parameters.GND_SPD);
        DoubleTimeSeries rpm = null;
        try {
            rpm = flight.getDoubleTimeSeries(connection, Parameters.E1_RPM);
        } catch (Exception ignored) { }
        FlightPhaseData phaseData = computeFlightPhasesFromTimeSeries(altAglTS, groundSpeed, rpm);
        if (validation != null) {
            applyCompletePhaseProcessing(phaseData, altAglArray, groundSpeed, validation);
        }
        return phaseData;
    }

    /**
     * True if at least NOISE_WINDOW_ROWS of the last NOISE_WINDOW_ROWS rows have altitude <= GROUND_CONTEXT_ALT_FT.
     */
    private static boolean isInGroundContext(double[] altAglArray, int i) {
        int start = Math.max(0, i - NOISE_WINDOW_ROWS + 1);
        int count = 0;
        for (int k = start; k <= i && k < altAglArray.length; k++) {
            if (!Double.isNaN(altAglArray[k]) && altAglArray[k] <= GROUND_CONTEXT_ALT_FT) {
                count++;
            }
        }
        return count >= NOISE_WINDOW_ROWS;
    }

    /** Trend over last NOISE_WINDOW_ROWS rows: "climb", "descent", or "flat" from net altitude change. */
    private static String getSustainedTrend(double[] altAglArray, int i) {
        int start = Math.max(0, i - NOISE_WINDOW_ROWS + 1);
        if (start >= i || i >= altAglArray.length) return "flat";
        double first = Double.NaN, last = Double.NaN;
        for (int k = start; k <= i && k < altAglArray.length; k++) {
            if (Double.isNaN(altAglArray[k])) continue;
            if (Double.isNaN(first)) first = altAglArray[k];
            last = altAglArray[k];
        }
        if (Double.isNaN(first) || Double.isNaN(last)) return "flat";
        double change = last - first;
        if (change > SUSTAINED_TREND_THRESHOLD_FT) return "climb";
        if (change < -SUSTAINED_TREND_THRESHOLD_FT) return "descent";
        return "flat";
    }

    /**
     * Reclassify short runs of CLIMB or LANDING (1 to MAX_SHORT_AIRBORNE_RUN rows) that are
     * surrounded by at least MIN_GROUND_SURROUND_FOR_SHORT_RUN TAXI/GROUND as TAXI (or GROUND if speed 0).
     * Only reclassifies when the run is at ground level (max Alt AGL in run <= MAX_ALT_AGL_FOR_SHORT_RUN_RECLASSIFY_FT)
     * so real low approaches are not converted to TAXI.
     */
    private static void smoothShortAirborneInGround(
            List<FlightPhase> phases, double[] altAglArray, DoubleTimeSeries groundSpeed, int n) {
        int i = 0;
        while (i < n) {
            FlightPhase p = phases.get(i);
            if (p != FlightPhase.CLIMB && p != FlightPhase.LANDING) {
                i++;
                continue;
            }
            int runStart = i;
            while (i < n && (phases.get(i) == FlightPhase.CLIMB || phases.get(i) == FlightPhase.LANDING)) {
                i++;
            }
            int runLen = i - runStart;
            if (runLen > MAX_SHORT_AIRBORNE_RUN) {
                continue;
            }
            // Check backward: at least MIN_GROUND_SURROUND_FOR_SHORT_RUN of TAXI/GROUND before runStart
            int groundBefore = 0;
            for (int k = runStart - 1; k >= 0 && groundBefore < MIN_GROUND_SURROUND_FOR_SHORT_RUN; k--) {
                FlightPhase q = phases.get(k);
                if (q == FlightPhase.TAXI || q == FlightPhase.GROUND) groundBefore++;
                else break;
            }
            if (groundBefore < MIN_GROUND_SURROUND_FOR_SHORT_RUN) continue;
            // Check forward: at least MIN_GROUND_SURROUND_FOR_SHORT_RUN of TAXI/GROUND after run
            int groundAfter = 0;
            for (int k = i; k < n && groundAfter < MIN_GROUND_SURROUND_FOR_SHORT_RUN; k++) {
                FlightPhase q = phases.get(k);
                if (q == FlightPhase.TAXI || q == FlightPhase.GROUND) groundAfter++;
                else break;
            }
            if (groundAfter < MIN_GROUND_SURROUND_FOR_SHORT_RUN) continue;
            // Only reclassify if run is at ground level (avoid converting real low approach to TAXI)
            if (altAglArray != null) {
                double maxAltInRun = Double.NEGATIVE_INFINITY;
                for (int j = runStart; j < i && j < altAglArray.length; j++) {
                    if (!Double.isNaN(altAglArray[j]) && altAglArray[j] > maxAltInRun) maxAltInRun = altAglArray[j];
                }
                if (maxAltInRun > MAX_ALT_AGL_FOR_SHORT_RUN_RECLASSIFY_FT) continue;
            }
            // Reclassify run to TAXI or GROUND
            for (int j = runStart; j < i; j++) {
                double gs = (groundSpeed != null && j < groundSpeed.size()) ? groundSpeed.get(j) : Double.NaN;
                phases.set(j, (!Double.isNaN(gs) && gs > 0.0) ? FlightPhase.TAXI : FlightPhase.GROUND);
            }
        }
    }

    /**
     * Post-processing: touch-and-go and go-around marking, reclassify UNKNOWNs, smooth short airborne runs.
     * Modifies phaseData in place. Safe to call with validation null (only go-around and reclassify run).
     */
    public static void applyCompletePhaseProcessing(
            FlightPhaseData phaseData,
            double[] altAglArray,
            DoubleTimeSeries groundSpeed,
            FlightValidationResult validation) {
        
        // 1. Touch-and-go: ±10 rows around split points; TOUCH_AND_GO only if rolling speed ≥ 15 kts, else GROUND
        if (validation != null && validation.hasTouchAndGo()) {
            for (int splitIndex : validation.splitIndices) {
                int start = Math.max(0, splitIndex - GO_AROUND_WINDOW_ROWS);
                int end = Math.min(phaseData.getPhases().size() - 1, splitIndex + GO_AROUND_WINDOW_ROWS);
                boolean hasRollingSpeed = false;
                if (groundSpeed != null) {
                    for (int i = start; i <= end && i < groundSpeed.size(); i++) {
                        if (!Double.isNaN(groundSpeed.get(i)) && groundSpeed.get(i) >= MIN_TAKEOFF_ROLL_SPEED_KTS) {
                            hasRollingSpeed = true;
                            break;
                        }
                    }
                }
                if (hasRollingSpeed) {
                    for (int i = start; i <= end; i++) {
                        phaseData.getPhases().set(i, FlightPhase.TOUCH_AND_GO);
                    }
                } else {
                    for (int i = start; i <= end; i++) {
                        double alt = (i < altAglArray.length) ? altAglArray[i] : Double.NaN;
                        if (!Double.isNaN(alt) && alt <= GROUND_ALT_FT) {
                            phaseData.getPhases().set(i, FlightPhase.GROUND);
                        }
                    }
                }
            }
        }

        // 2. Go-around: ±10 rows around valley indices
        List<Integer> goAroundIndices = detectGoArounds(altAglArray);
        int lastMarkedEnd = -1;
        for (int goAroundIndex : goAroundIndices) {
            int start = Math.max(0, goAroundIndex - GO_AROUND_WINDOW_ROWS);
            int end = Math.min(phaseData.getPhases().size() - 1, goAroundIndex + GO_AROUND_WINDOW_ROWS);
            if (start > lastMarkedEnd) {
                for (int i = start; i <= end; i++) {
                    phaseData.getPhases().set(i, FlightPhase.GO_AROUND);
                }
                lastMarkedEnd = end;
            }
        }

        // 3. Reclassify UNKNOWNs using sustained trend and ground context
        List<FlightPhase> phases = phaseData.getPhases();
        int n = phases.size();
        for (int i = 0; i < n; i++) {
            if (phases.get(i) == FlightPhase.UNKNOWN) {
                double alt = (i < altAglArray.length) ? altAglArray[i] : Double.NaN;
                double gndSpd = (groundSpeed != null && i < groundSpeed.size()) ? groundSpeed.get(i) : Double.NaN;
                boolean onGroundAlt = !Double.isNaN(alt) && alt <= GROUND_ALT_FT;
                boolean inGroundContext = isInGroundContext(altAglArray, i);
                if (onGroundAlt || (inGroundContext && !Double.isNaN(alt) && alt <= GROUND_CONTEXT_ALT_FT)) {
                    phases.set(i, (!Double.isNaN(gndSpd) && gndSpd > 0.0) ? FlightPhase.TAXI : FlightPhase.GROUND);
                    continue;
                }
                String trend = getSustainedTrend(altAglArray, i);
                if (inGroundContext && alt < LANDING_ALT_FT && !"climb".equals(trend)) {
                    phases.set(i, FlightPhase.TAXI);
                    continue;
                }
                if ("climb".equals(trend)) {
                    phases.set(i, FlightPhase.CLIMB);
                } else if ("descent".equals(trend)) {
                    phases.set(i, alt < LANDING_ALT_FT ? FlightPhase.LANDING : FlightPhase.DESCENT);
                } else if (!Double.isNaN(alt) && alt >= CRUISE_ALT_FT) {
                    phases.set(i, FlightPhase.CRUISE);
                } else {
                    phases.set(i, (inGroundContext || (!Double.isNaN(alt) && alt < 50))
                            ? ((!Double.isNaN(gndSpd) && gndSpd > 0.0) ? FlightPhase.TAXI : FlightPhase.GROUND)
                            : FlightPhase.CLIMB);
                }
            }
        }

        // 4. Smooth short CLIMB/LANDING runs surrounded by TAXI/GROUND at ground level
        smoothShortAirborneInGround(phases, altAglArray, groundSpeed, n);
    }

    /** AltAGL time series as double array. */
    public static double[] getAltAGLValues(Connection connection, int flightId, int maxRows) throws SQLException, IOException {
        DoubleTimeSeries altAgl = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, Parameters.ALT_AGL);
        int n = Math.min(altAgl.size(), maxRows);
        double[] values = new double[n];
        for (int i = 0; i < n; i++) {
            values[i] = altAgl.get(i);
        }
        return values;
    }
}
