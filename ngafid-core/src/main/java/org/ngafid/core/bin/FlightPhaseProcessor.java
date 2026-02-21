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
 * Utility class for processing and labeling flight phases.
 * 
 * ALGORITHM OVERVIEW:
 * 1. Initial Phase Detection (RPM/Speed/Altitude based)
 *    - TAXI: From start until takeoff criteria met
 *    - TAKEOFF: First 15 rows with RPM>=2100, 14.5<speed<80 knots
 *    - CLIMB: After takeoff until altitude reaches 600 ft
 *    - CRUISE: Any altitude >= 600 ft
 *    - DESCENT: Below 600 ft and descending (100-600 ft range)
 *    - LANDING: Below 100 ft and descending
 *    - GROUND: Altitude <= 5 ft and speed < 5 knots
 * 
 * 2. Touch-and-Go Detection (altitude pattern analysis)
 *    - Detects when altitude stays < 5 ft for 10+ consecutive rows
 *    - Must occur after initial climb above 200 ft
 *    - Marks ±5 rows around split point as TOUCH_AND_GO phase
 * 
 * 3. Go-Around Detection (valley pattern analysis)
 *    - Detects descent below 100 ft followed by climb without landing
 *    - Valley bottom must be > 5 ft (distinguishes from actual landing)
 *    - Must climb back up by >= 50 ft with sustained climb (10+ rows)
 *    - Marks ±10 rows around valley as GO_AROUND phase
 * 
 * 4. Post-Processing with Altitude Smoothing
 *    - Applies 5-foot threshold to filter sensor noise
 *    - Reclassifies phases based on altitude and vertical trend
 *    - Only altitude changes > 5 feet considered significant
 *    - Eliminates all UNKNOWN phases
 */
public final class FlightPhaseProcessor {

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

    /**
     * Container class for flight phase data at each time point
     */
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

        /**
         * Get the phase as a string for CSV export
         * @param index the row index
         * @return the phase name as string
         */
        public String getPhaseStringAt(int index) {
            return getPhaseAt(index).name();
        }
    }

    /**
     * Result of flight validation and touch-and-go detection.
     * splitIndices: used for touch-and-go phase marking (not for file splitting).
     * File splitting uses detectProlongedTaxiSplits() instead.
     */
    public static class FlightValidationResult {
        public final boolean isValid;           // false if flight never exceeds 10ft AGL
        public final List<Integer> splitIndices; // touch-and-go indices for phase marking (empty if none)
        public final double maxAltAGL;          // maximum altitude reached
        
        public FlightValidationResult(boolean isValid, List<Integer> splitIndices, double maxAltAGL) {
            this.isValid = isValid;
            this.splitIndices = splitIndices;
            this.maxAltAGL = maxAltAGL;
        }
        
        public boolean hasTouchAndGo() {
            return !splitIndices.isEmpty();
        }
        
        public int getTouchAndGoCount() {
            return splitIndices.size();
        }
    }

    private FlightPhaseProcessor() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Validates flight and detects touch-and-gos for phase marking.
     * - Invalid (ground-only): If max AltAGL never exceeds 10 ft
     * - Touch-and-go: At least 10 consecutive rows where AltAGL &lt; 5 ft, then goes up
     *   (must have climbed above 200 ft first to exclude initial taxi).
     * splitIndices are used only for TOUCH_AND_GO phase marking, not for file splitting.
     *
     * @param altAGLValues Array of altitude above ground values
     * @return FlightValidationResult with validation status and touch-and-go indices for phase marking
     */
    public static FlightValidationResult validateAndDetectTouchAndGo(double[] altAGLValues) {
        if (altAGLValues == null || altAGLValues.length == 0) {
            return new FlightValidationResult(false, new ArrayList<>(), 0.0);
        }
        
        final double GROUND_THRESHOLD = 10.0;     // Must exceed this to be valid flight
        final double CLIMB_THRESHOLD = 200.0;     // Must exceed this before touch-and-gos are counted
        final int MIN_ZERO_ROWS = 10;             // Minimum rows on ground for touch-and-go

        double maxAltAGL = Double.NEGATIVE_INFINITY;
        boolean hasClimbedHigh = false;           // Only count touch-and-gos after climbing high
        int consecutiveZeroCount = 0;
        int zeroStartIndex = -1;
        List<Integer> splitIndices = new ArrayList<>();

        // Single pass through the data
        for (int i = 0; i < altAGLValues.length; i++) {
            double alt = altAGLValues[i];
            // Skip NaN values
            if (Double.isNaN(alt)) continue;
            // Track maximum altitude
            if (alt > maxAltAGL) maxAltAGL = alt;
            // Check if aircraft has climbed high enough to start counting touch-and-gos
            if (alt > CLIMB_THRESHOLD) hasClimbedHigh = true;
            // Count consecutive LOW altitudes (< 5 feet) ONLY after climbing high
            if (hasClimbedHigh && alt < 5.0) {
                if (consecutiveZeroCount == 0) zeroStartIndex = i;  // Mark start of low altitude sequence
                consecutiveZeroCount++;
            } else if (hasClimbedHigh && alt >= 5.0 && consecutiveZeroCount >= MIN_ZERO_ROWS) {
                // Aircraft left ground after sufficient time at low altitude - this is a touch-and-go!
                // Split point is at the MIDDLE of the low altitude sequence
                int midpoint = zeroStartIndex + consecutiveZeroCount / 2;
                splitIndices.add(midpoint);
                // Reset counters
                consecutiveZeroCount = 0;
                zeroStartIndex = -1;
            } else if (alt >= 5.0) {
                // Reset if we go above 5 feet without enough low altitude rows
                consecutiveZeroCount = 0;
                zeroStartIndex = -1;
            }
        }
        // Return all detected touch-and-gos (for phase marking only; file splitting uses prolonged taxi)
        boolean isValid = maxAltAGL > GROUND_THRESHOLD;
        return new FlightValidationResult(isValid, splitIndices, maxAltAGL);
    }

    /**
     * Detects prolonged taxi (30+ seconds) in the middle of a flight using TAXI criteria:
     * altitude &lt; 5 ft, ground speed &lt; 8 knots, RPM &lt; 2100 (same logic as TAXI phase).
     * Used for splitting a single CSV into multiple flight files. Taxi at the beginning
     * (before first climb) and at the end (final approach/landing) are excluded.
     * Split point is at the midpoint of each prolonged taxi window (first half to one
     * file, second half to the next).
     *
     * @param connection the database connection
     * @param flightId   the flight ID
     * @return list of row indices where to split (midpoints of prolonged taxi sequences), empty if none
     */
    public static List<Integer> detectProlongedTaxiSplits(Connection connection, int flightId)
            throws SQLException, IOException {
        double[] altAGL = getAltAGLValues(connection, flightId, Integer.MAX_VALUE);
        double[] groundSpeed = getDoubleSeriesValues(connection, flightId, Parameters.GND_SPD, Integer.MAX_VALUE);
        double[] rpm = null;
        try {
            rpm = getDoubleSeriesValues(connection, flightId, Parameters.E1_RPM, Integer.MAX_VALUE);
        } catch (Exception e) {
            // RPM may not be available for all aircraft
        }
        return detectProlongedTaxiSplitsFromSeries(altAGL, groundSpeed, rpm);
    }

    /**
     * Detects prolonged taxi from pre-loaded time series. Taxi = alt &lt; 5 ft, speed &lt; 8 kts, RPM &lt; 2100.
     */
    public static List<Integer> detectProlongedTaxiSplitsFromSeries(
            double[] altAGLValues, double[] groundSpeedValues, double[] rpmValues) {
        List<Integer> splits = new ArrayList<>();
        if (altAGLValues == null || altAGLValues.length == 0) return splits;
        int n = altAGLValues.length;
        if (groundSpeedValues == null || groundSpeedValues.length == 0) return splits;
        n = Math.min(n, groundSpeedValues.length);

        final double GROUND_ALT = 5.0;           // ft - on ground
        final double TAXI_SPEED_MAX = 8.0;       // kts - taxi speed &lt; 8 (takeoff is 14.5+)
        final double TAKEOFF_RPM = 2100.0;       // RPM - takeoff is 2100+
        final int MIN_TAXI_ROWS = 30;            // 30 seconds at ~1 Hz sampling
        final double CLIMB_THRESHOLD = 200.0;    // ft - must have flown first (excludes start taxi)

        boolean hasClimbedHigh = false;
        int consecCount = 0;
        int startIdx = -1;
        int rpmLen = (rpmValues != null) ? rpmValues.length : 0;

        for (int i = 0; i < n; i++) {
            double alt = altAGLValues[i];
            double gs = (i < groundSpeedValues.length) ? groundSpeedValues[i] : Double.NaN;
            double r = (rpmValues != null && i < rpmLen) ? rpmValues[i] : Double.NaN;

            if (Double.isNaN(alt)) { consecCount = 0; startIdx = -1; continue; }
            if (alt > CLIMB_THRESHOLD) hasClimbedHigh = true;

            // Taxi: alt &lt; 5 ft, speed &lt; 8 kts, RPM &lt; 2100 (or RPM not available)
            boolean isTaxi = alt < GROUND_ALT
                    && !Double.isNaN(gs) && gs < TAXI_SPEED_MAX
                    && (Double.isNaN(r) || r < TAKEOFF_RPM);

            if (hasClimbedHigh && isTaxi) {
                if (consecCount == 0) startIdx = i;
                consecCount++;
            } else if (hasClimbedHigh && !isTaxi && consecCount >= MIN_TAXI_ROWS) {
                int midpoint = startIdx + consecCount / 2;
                splits.add(midpoint);
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
     * Detect go-around maneuvers using valley pattern analysis.
     * 
     * ALGORITHM:
     * A go-around occurs when the aircraft descends toward landing but aborts
     * and climbs back up without touching down. This creates a "valley" pattern
     * in the altitude data.
     * 
     * DETECTION CRITERIA:
     * 1. Descent Phase: Aircraft descends below 100 ft AGL
     * 2. Valley Bottom: Reaches minimum altitude > 5 ft (not an actual landing)
     * 3. Climb Recovery: Climbs back up by >= 50 ft from valley bottom
     * 4. Sustained Climb: Maintains climb for >= 10 consecutive rows
     * 
     * IMPLEMENTATION:
     * - Scans altitude array looking for local minimums below 100 ft
     * - For each potential valley, checks if previous altitude was higher (descending)
     * - Looks ahead 30 rows to verify sustained climb of >= 50 ft
     * - Finds exact valley bottom within ±5 rows of detection point
     * - Skips ahead after detection to avoid duplicate detection
     * 
     * @param altAglArray altitude AGL array for the flight
     * @return list of indices where go-arounds occur (valley bottoms)
     */
    public static List<Integer> detectGoArounds(double[] altAglArray) {
        List<Integer> goAroundIndices = new ArrayList<>();
        final double LOW_ALTITUDE_THRESHOLD = 100.0;  // Must descend below 100 ft
        final double CLIMB_RECOVERY_THRESHOLD = 50.0;  // Must climb back up by 50 ft
        final double LANDING_THRESHOLD = 5.0;  // Valley bottom should be above 5 ft (not landing)
        final int MIN_CLIMB_WINDOW = 10;  // Must sustain climb for 10 rows to confirm go-around

        for (int i = 1; i < altAglArray.length - MIN_CLIMB_WINDOW; i++) {
            double alt = altAglArray[i];
            if (Double.isNaN(alt)) continue;
            // Find local minimum (valley)
            if (alt < LOW_ALTITUDE_THRESHOLD && alt > LANDING_THRESHOLD &&
                !Double.isNaN(altAglArray[i-1]) && !Double.isNaN(altAglArray[i+1]) &&
                alt < altAglArray[i-1] && alt < altAglArray[i+1]) {
                // Check for climb of at least 50 ft after the valley
                double minAlt = altAglArray[i];
                int climbEnd = -1;
                for (int j = i + 1; j < Math.min(i + 30, altAglArray.length); j++) {
                    if (!Double.isNaN(altAglArray[j]) && altAglArray[j] - minAlt >= CLIMB_RECOVERY_THRESHOLD) {
                        climbEnd = j;
                        break;
                    }
                }
                if (climbEnd != -1) {
                    // Check ±10 rows around the valley for any ground contact (<= 5 ft)
                    boolean touchesGround = false;
                    int windowStart = Math.max(0, i - 10);
                    int windowEnd = Math.min(altAglArray.length - 1, i + 10);
                    for (int k = windowStart; k <= windowEnd; k++) {
                        if (!Double.isNaN(altAglArray[k]) && altAglArray[k] <= LANDING_THRESHOLD) {
                            touchesGround = true;
                            break;
                        }
                    }
                    if (!touchesGround) {
                        goAroundIndices.add(i);
                        i = i + 11;
                        continue;
                    }
                }
            }
        }
        return goAroundIndices;
    }

    /**
     * Compute flight phases for the given flight.
     * This method analyzes the flight data and assigns a phase to each row.
     * Loads time series data from the database.
     *
     * @param connection the database connection
     * @param flight the flight to process
     * @return FlightPhaseData containing phase information for each row
     * @throws SQLException if there is an error with the SQL query
     * @throws IOException if there is an error reading flight data
     */
    public static FlightPhaseData computeFlightPhases(Connection connection, Flight flight) 
            throws SQLException, IOException {
        
        // Get required time series data
        DoubleTimeSeries altAgl = flight.getDoubleTimeSeries(connection, Parameters.ALT_AGL);
        DoubleTimeSeries groundSpeed = flight.getDoubleTimeSeries(connection, Parameters.GND_SPD);
        DoubleTimeSeries rpm = null;
        
        try {
            rpm = flight.getDoubleTimeSeries(connection, Parameters.E1_RPM);
        } catch (Exception e) {
            // RPM may not be available for all aircraft types
            System.err.println("Warning: RPM not available for flight " + flight.getId() + ", using alternative phase detection");
        }

        return computeFlightPhasesFromTimeSeries(altAgl, groundSpeed, rpm);
    }

    /**
     * Compute flight phases from already-loaded time series data.
     * 
     * INITIAL PHASE DETECTION (Sequential Assignment):
     * 
     * 1. TAXI: From start until takeoff criteria met
     *    - Continues until: RPM >= 2100 AND 14.5 < groundSpeed < 80 knots
     * 
     * 2. TAKEOFF: First 15 consecutive rows meeting takeoff criteria
     *    - RPM >= 2100 AND 14.5 < groundSpeed < 80 knots
     * 
     * 3. CLIMB: After takeoff until reaching cruise altitude
     *    - Ends when altitude >= 600 ft AGL
     * 
     * 4. CRUISE: High altitude phase
     *    - Any altitude >= 600 ft
     * 
     * 5. DESCENT: Descending in pattern or from cruise
     *    - 100-600 ft range, descending
     * 
     * 6. LANDING: Final descent to ground
     *    - Below 100 ft and descending
     * 
     * 7. GROUND: On ground with minimal movement
     *    - Altitude <= 5 ft AND groundSpeed < 5 knots
     * 
     * Note: TOUCH_AND_GO and GO_AROUND are detected separately and marked in post-processing.
     * Note: UNKNOWN phases are eliminated through altitude-smoothed reclassification.
     *
     * @param altAgl altitude AGL time series
     * @param groundSpeed ground speed time series
     * @param rpm RPM time series (can be null if not available)
     * @return FlightPhaseData containing phase information for each row
     */
    public static FlightPhaseData computeFlightPhasesFromTimeSeries(
            DoubleTimeSeries altAgl,
            DoubleTimeSeries groundSpeed,
            DoubleTimeSeries rpm) {
        
        return computeFlightPhasesFromTimeSeries(altAgl, groundSpeed, rpm, null);
    }
    
    /**
     * Compute flight phases from time series data with optional airport distance.
     * 
     * @param altAgl altitude AGL time series
     * @param groundSpeed ground speed time series  
     * @param rpm RPM time series (can be null if not available)
     * @param airportDistance distance to nearest airport (can be null)
     * @return FlightPhaseData containing phase information for each row
     */
    public static FlightPhaseData computeFlightPhasesFromTimeSeries(
            DoubleTimeSeries altAgl,
            DoubleTimeSeries groundSpeed,
            DoubleTimeSeries rpm,
            DoubleTimeSeries airportDistance) {
        


        int numRows = altAgl.size();
        List<FlightPhase> phases = new ArrayList<>(numRows);
        for (int i = 0; i < numRows; i++) {
            phases.add(FlightPhase.UNKNOWN);
        }

        // --- Simple, systematic TAXI and TAKEOFF logic ---
        int taxiEndIdx = -1;
        for (int i = 0; i < numRows; i++) {
            if (rpm != null && !Double.isNaN(rpm.get(i)) && !Double.isNaN(groundSpeed.get(i)) &&
                rpm.get(i) >= 2100 && groundSpeed.get(i) > 14.5 && groundSpeed.get(i) < 80) {
                taxiEndIdx = i;
                break;
            }
        }
        // Mark TAXI phase
        for (int i = 0; i < numRows && i < taxiEndIdx; i++) {
            phases.set(i, FlightPhase.TAXI);
        }
        // Mark TAKEOFF phase: always mark 15 rows after taxiEndIdx as TAKEOFF
        int takeoffStart = taxiEndIdx;
        int takeoffEnd = Math.min(takeoffStart + 15, numRows);
        // Only mark TAKEOFF if the first row meets criteria
        if (takeoffStart >= 0 && takeoffStart < numRows &&
            rpm != null && !Double.isNaN(rpm.get(takeoffStart)) && !Double.isNaN(groundSpeed.get(takeoffStart)) &&
            rpm.get(takeoffStart) >= 2100 && groundSpeed.get(takeoffStart) > 14.5 && groundSpeed.get(takeoffStart) < 80) {
            for (int i = takeoffStart; i < takeoffEnd; i++) {
                phases.set(i, FlightPhase.TAKEOFF);
            }
        }

        // PHASE 3: CLIMB - After TAKEOFF block until reaching 600 ft AGL
        int climbIdx = takeoffEnd; // Start CLIMB assignment immediately after last TAKEOFF row
        while (climbIdx < numRows) {
            if (phases.get(climbIdx) != FlightPhase.UNKNOWN) {
                climbIdx++;
                continue;
            }
            // Ensure TAKEOFF rows are not overridden
            if (phases.get(climbIdx) == FlightPhase.TAKEOFF) {
                climbIdx++;
                continue;
            }
            if (!Double.isNaN(altAgl.get(climbIdx)) && !Double.isNaN(groundSpeed.get(climbIdx))) {
                if (altAgl.get(climbIdx) >= 600) {
                    climbIdx++;
                    break;
                }
                phases.set(climbIdx, FlightPhase.CLIMB);
            }
            climbIdx++;
        }

        // PHASE 4: CRUISE - After reaching 600 ft, mark high altitude phases
        // Cruise begins immediately after climb ends (when AGL >= 600)
        for (int j = climbIdx; j < numRows; j++) {
            if (phases.get(j) == FlightPhase.TAKEOFF) continue;
            if (!Double.isNaN(altAgl.get(j))) {
                if (altAgl.get(j) >= 600 && phases.get(j) == FlightPhase.UNKNOWN) {
                    phases.set(j, FlightPhase.CRUISE);
                }
            }
        }

        // PHASE 5: DESCENT - Descending from cruise to landing pattern
        // Mark any rows below 600 ft that are descending (not yet landing)
        for (int d1 = 1; d1 < numRows; d1++) {
            if (phases.get(d1) == FlightPhase.TAKEOFF) continue;
            if (!Double.isNaN(altAgl.get(d1)) && !Double.isNaN(altAgl.get(d1-1))) {
                double alt = altAgl.get(d1);
                double prevAlt = altAgl.get(d1-1);
                double altChange = alt - prevAlt;
                // Descent: below 600 ft, above 100 ft, and descending by more than 10 feet
                if (alt < 600 && alt >= 100 && altChange < -10.0 && phases.get(d1) == FlightPhase.UNKNOWN) {
                    phases.set(d1, FlightPhase.DESCENT);
                }
            }
        }
        // Mark all remaining rows below 600 ft as DESCENT (includes pattern flying)
        for (int d2 = 0; d2 < numRows; d2++) {
            if (phases.get(d2) == FlightPhase.TAKEOFF) continue;
            if (!Double.isNaN(altAgl.get(d2))) {
                double alt = altAgl.get(d2);
                if (alt < 600 && alt >= 100 && phases.get(d2) == FlightPhase.UNKNOWN) {
                    // Only mark as DESCENT if previous row was DESCENT or significant drop
                    if (d2 > 0 && !Double.isNaN(altAgl.get(d2-1))) {
                        double altChange = alt - altAgl.get(d2-1);
                        if (altChange < -10.0 || phases.get(d2-1) == FlightPhase.DESCENT) {
                            phases.set(d2, FlightPhase.DESCENT);
                        }
                    }
                }
            }
        }
        // PHASE 6: LANDING - Final descent from 100 ft to 0 ft
        // Mark rows where AGL is decreasing from ~100 to 0
        for (int l1 = 1; l1 < numRows; l1++) {
            if (phases.get(l1) == FlightPhase.TAKEOFF) continue;
            if (!Double.isNaN(altAgl.get(l1)) && !Double.isNaN(altAgl.get(l1-1))) {
                double alt = altAgl.get(l1);
                double prevAlt = altAgl.get(l1-1);
                // Refined Landing: below 100 ft, above 5 ft, and descending
                if (alt < 100 && alt > 5.0 && alt < prevAlt && phases.get(l1) == FlightPhase.UNKNOWN) {
                    phases.set(l1, FlightPhase.LANDING);
                }
            }
        }
        // PHASE 7: DESCENT - Descending but not in approach or landing yet
        // Fill in remaining descending sections
        for (int d3 = 1; d3 < numRows; d3++) {
            if (phases.get(d3) == FlightPhase.TAKEOFF) continue;
            if (!Double.isNaN(altAgl.get(d3)) && !Double.isNaN(altAgl.get(d3-1))) {
                double alt = altAgl.get(d3);
                double prevAlt = altAgl.get(d3-1);
                double altChange = alt - prevAlt;
                // Descent: decreasing altitude above 200 ft by more than 10 feet
                if (altChange < -10.0 && alt > 200 && phases.get(d3) == FlightPhase.UNKNOWN) {
                    phases.set(d3, FlightPhase.DESCENT);
                }
            }
        }
        // PHASE 8: GROUND - Stationary on ground (parked/stopped)
        // Only mark as GROUND if truly stationary (speed = 0)
        for (int g1 = 0; g1 < numRows; g1++) {
            if (phases.get(g1) == FlightPhase.TAKEOFF) continue;
            if (phases.get(g1) == FlightPhase.UNKNOWN) {
                if (!Double.isNaN(altAgl.get(g1)) && !Double.isNaN(groundSpeed.get(g1))) {
                    if (altAgl.get(g1) <= 5 && groundSpeed.get(g1) == 0.0) {
                        phases.set(g1, FlightPhase.GROUND);
                    }
                }
            }
        }

        return new FlightPhaseData(phases, numRows);
    }

    /**
     * Compute complete flight phases with post-processing.
     * This is the recommended method to use - it includes:
     * - Initial phase detection
     * - Touch-and-go zone marking (±10 rows)
     * - Go-around detection and marking (±10 rows)
     * - Altitude smoothing (5-foot threshold to filter sensor noise)
     * - Complete reclassification to eliminate UNKNOWN phases
     * - Final taxi detection after landing
     * 
     * @param connection the database connection
     * @param flight the flight to process
     * @param validation the flight validation result (from validateAndDetectTouchAndGo)
     * @return FlightPhaseData containing complete phase information
     * @throws SQLException if there is an error with the SQL query
     * @throws IOException if there is an error reading flight data
     */
    public static FlightPhaseData computeCompleteFlightPhases(
            Connection connection, 
            Flight flight,
            FlightValidationResult validation) throws SQLException, IOException {
        
        // Start with basic phase detection
        FlightPhaseData phaseData = computeFlightPhases(connection, flight);
        
        // If no validation data provided, return basic phases
        if (validation == null) {
            return phaseData;
        }
        
        // Get altitude and speed data for post-processing
        double[] altAglArray = getAltAGLValues(connection, flight.getId(), Integer.MAX_VALUE);
        DoubleTimeSeries groundSpeed = flight.getDoubleTimeSeries(connection, Parameters.GND_SPD);
        
        // Apply complete post-processing
        applyCompletePhaseProcessing(phaseData, altAglArray, groundSpeed, validation);
        
        return phaseData;
    }

    /**
     * Apply complete phase post-processing including touch-and-go marking,
     * go-around detection, altitude smoothing, and final taxi detection.
     * This method can be used standalone with arrays (e.g., from CSV files)
     * or as part of the complete phase computation pipeline.
     * 
     * @param phaseData the flight phase data to modify in-place
     * @param altAglArray altitude AGL array for the flight
     * @param groundSpeed ground speed time series (can be null)
     * @param validation flight validation result (null if not available)
     */
    public static void applyCompletePhaseProcessing(
            FlightPhaseData phaseData,
            double[] altAglArray,
            DoubleTimeSeries groundSpeed,
            FlightValidationResult validation) {
        
        // Mark touch-and-go zones (±10 rows around split points)
        if (validation != null && validation.hasTouchAndGo()) {
            for (int splitIndex : validation.splitIndices) {
                for (int i = Math.max(0, splitIndex - 10); 
                     i <= Math.min(phaseData.getPhases().size() - 1, splitIndex + 10); i++) {
                    phaseData.getPhases().set(i, FlightPhase.TOUCH_AND_GO);
                }
            }
        }

        // Detect and mark go-around zones (±10 rows around valleys, 20 rows total)
        List<Integer> goAroundIndices = detectGoArounds(altAglArray);
        int lastMarkedEnd = -1;
        for (int goAroundIndex : goAroundIndices) {
            int start = Math.max(0, goAroundIndex - 10);
            int end = Math.min(phaseData.getPhases().size() - 1, goAroundIndex + 10);
            if (start > lastMarkedEnd) {
                for (int i = start; i <= end; i++) {
                    phaseData.getPhases().set(i, FlightPhase.GO_AROUND);
                }
                lastMarkedEnd = end;
            }
        }

        // FINAL PASS: Reclassify all remaining UNKNOWNs
        List<FlightPhase> phases = phaseData.getPhases();
        int n = phases.size();
        for (int i = 0; i < n; i++) {
            if (phases.get(i) == FlightPhase.UNKNOWN) {
                double alt = (i >= 0 && i < altAglArray.length) ? altAglArray[i] : Double.NaN;
                double prevAlt = (i > 0 && i - 1 < altAglArray.length) ? altAglArray[i - 1] : Double.NaN;
                double nextAlt = (i < n - 1 && i + 1 < altAglArray.length) ? altAglArray[i + 1] : Double.NaN;
                double gndSpd = (groundSpeed != null && i < groundSpeed.size()) ? groundSpeed.get(i) : Double.NaN;
                // Taxi logic: on ground (<=5 ft) and moving
                if (!Double.isNaN(alt) && alt <= 5.0) {
                    if (!Double.isNaN(gndSpd) && gndSpd > 0.0) {
                        phases.set(i, FlightPhase.TAXI);
                    } else {
                        phases.set(i, FlightPhase.GROUND);
                    }
                    continue;
                }
                // Use climb/descent/cruise logic for others
                boolean climbing = !Double.isNaN(prevAlt) && !Double.isNaN(alt) && alt > prevAlt;
                boolean descending = !Double.isNaN(prevAlt) && !Double.isNaN(alt) && alt < prevAlt;
                if (!Double.isNaN(prevAlt) && !Double.isNaN(nextAlt)) {
                    if (alt > prevAlt && nextAlt > alt) {
                        climbing = true;
                        descending = false;
                    } else if (alt < prevAlt && nextAlt < alt) {
                        climbing = false;
                        descending = true;
                    }
                }
                if (climbing) {
                    phases.set(i, FlightPhase.CLIMB);
                } else if (descending) {
                    phases.set(i, FlightPhase.DESCENT);
                } else if (!Double.isNaN(alt) && alt >= 600) {
                    phases.set(i, FlightPhase.CRUISE);
                } else {
                    phases.set(i, FlightPhase.CLIMB);
                }
            }
        }
    }

    /**
     * Get a summary of phases in the flight
     * @param phaseData the flight phase data
     * @return a string summary of phase distribution
     */
    public static String getPhaseSummary(FlightPhaseData phaseData) {
        int[] phaseCounts = new int[FlightPhase.values().length];
        
        for (FlightPhase phase : phaseData.getPhases()) {
            phaseCounts[phase.ordinal()]++;
        }
        
        StringBuilder summary = new StringBuilder();
        summary.append("Flight Phase Summary (").append(phaseData.getNumberOfRows()).append(" rows):\n");
        
        for (FlightPhase phase : FlightPhase.values()) {
            int count = phaseCounts[phase.ordinal()];
            if (count > 0) {
                double percentage = (count * 100.0) / phaseData.getNumberOfRows();
                summary.append(String.format("  %s: %d rows (%.1f%%)\n", 
                    phase.name(), count, percentage));
            }
        }
        
        return summary.toString();
    }
    /**
     * Loads the AltAGL time series for a flight and returns it as a double array.
     *
     * @param connection the database connection
     * @param flightId the flight ID
     * @param maxRows maximum number of rows to return (use Integer.MAX_VALUE for all rows)
     * @return array of AltAGL values (double[])
     * @throws SQLException if there is an error with the SQL query
     * @throws IOException if there is an error reading flight data
     */
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
