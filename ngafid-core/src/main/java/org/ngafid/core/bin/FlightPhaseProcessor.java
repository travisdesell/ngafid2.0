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
 * 1. Initial Phase Detection (2-pass approach)
 *    Pass 1 - Sequential: TAXI → TAKEOFF → CLIMB
 *    Pass 2 - Consolidated: CRUISE, DESCENT, LANDING, GROUND
 * 
 * 2. Touch-and-Go Detection
 *    - Altitude < 5 ft for 10+ consecutive rows after climbing above 200 ft
 *    - Marks ±10 rows around split point
 * 
 * 3. Go-Around Detection
 *    - Valley pattern: descent below 100 ft, then climb ≥50 ft without landing
 *    - Marks ±10 rows around valley bottom
 * 
 * 4. Post-Processing
 *    - Altitude smoothing (5-foot threshold for noise filtering)
 *    - Phase reclassification
 *    - Final taxi detection after landing
 * 
 * PERFORMANCE: O(n) with ~3-4 passes through data
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
     */
    public static class FlightValidationResult {
        public final boolean isValid;           // false if flight never exceeds 10ft AGL
        public final List<Integer> splitIndices; // indices where to split for touch-and-gos (empty if none)
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
     * Validates flight and detects ALL touch-and-gos in a single pass.
     * 
     * NEW CRITERIA:
     * - Invalid (ground-only): If max AltAGL never exceeds 10 ft
     * - Touch-and-go: At least 10 consecutive rows where AltAGL = 0 ft, then goes up
     *   * Only counts as touch-and-go if aircraft has climbed above 200 ft first (excludes initial taxi)
     *   * Splits the zeros in half: first half ends first flight, second half starts next flight
     *   * Both segments are marked as TOUCH_AND_GO phase
     * 
     * @param altAGLValues Array of altitude above ground values
     * @return FlightValidationResult containing validation status and all split points
     */
    public static FlightValidationResult validateAndDetectTouchAndGo(double[] altAGLValues) {
        if (altAGLValues == null || altAGLValues.length == 0) {
            return new FlightValidationResult(false, new ArrayList<>(), 0.0);
        }
        
        final double GROUND_THRESHOLD = 10.0;     // Must exceed this to be valid flight
        final double CLIMB_THRESHOLD = 200.0;     // Must exceed this before touch-and-gos are counted
        final double TOUCH_THRESHOLD = 0.0;       // Must be = 0ft to confirm ground contact
        final int MIN_ZERO_ROWS = 10;             // Minimum rows on ground for touch-and-go
        
        double maxAltAGL = Double.NEGATIVE_INFINITY;
        boolean hasLeftGround = false;
        boolean hasClimbedHigh = false;           // Only count touch-and-gos after climbing high
        int consecutiveZeroCount = 0;
        int zeroStartIndex = -1;
        List<Integer> splitIndices = new ArrayList<>();
        
        // Single pass through the data
        for (int i = 0; i < altAGLValues.length; i++) {
            double alt = altAGLValues[i];
            
            // Skip NaN values
            if (Double.isNaN(alt)) {
                continue;
            }
            
            // Track maximum altitude
            if (alt > maxAltAGL) {
                maxAltAGL = alt;
            }
            
            // Check if aircraft has left the ground
            if (alt > GROUND_THRESHOLD) {
                hasLeftGround = true;
            }
            
            // Check if aircraft has climbed high enough to start counting touch-and-gos
            if (alt > CLIMB_THRESHOLD) {
                hasClimbedHigh = true;
            }
            
            // Count consecutive LOW altitudes (< 5 feet) ONLY after climbing high
            // This detects touch-and-go even with sensor noise (0-3 feet oscillations)
            if (hasClimbedHigh && alt < 5.0) {
                if (consecutiveZeroCount == 0) {
                    zeroStartIndex = i;  // Mark start of low altitude sequence
                }
                consecutiveZeroCount++;
            } else if (hasClimbedHigh && alt >= 5.0 && consecutiveZeroCount >= MIN_ZERO_ROWS) {
                // Aircraft left ground after sufficient time at low altitude - this is a touch-and-go!
                // Split point is at the MIDDLE of the low altitude sequence
                int zeroEndIndex = i - 1;  // Last low altitude row
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
        
        // Return all detected touch-and-gos
        boolean isValid = maxAltAGL > GROUND_THRESHOLD;
        return new FlightValidationResult(isValid, splitIndices, maxAltAGL);
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
        final double LANDING_THRESHOLD = 5.0;  // Valley bottom must be above 5 ft (not landing)
        final int MIN_CLIMB_WINDOW = 10;  // Must sustain climb for 10 rows to confirm go-around
        
        for (int i = 1; i < altAglArray.length - MIN_CLIMB_WINDOW; i++) {
            double alt = altAglArray[i];
            
            // Skip NaN values
            if (Double.isNaN(alt)) {
                continue;
            }
            
            // Look for valley pattern:
            // 1. Current altitude is low (below 100 ft)
            // 2. Current altitude is above landing threshold (> 5 ft, not actually landing)
            // 3. Previous altitude was higher (descending)
            // 4. Future altitude will be higher (climbing)
            // 5. CRITICAL: Altitude never drops to landing level during entire valley
            
            if (alt < LOW_ALTITUDE_THRESHOLD && alt > LANDING_THRESHOLD) {
                // Check if descending (previous altitude higher)
                boolean isDescending = false;
                if (i > 0 && !Double.isNaN(altAglArray[i-1])) {
                    isDescending = altAglArray[i-1] > alt;
                }
                
                // Check if this is a valley (altitude will climb back up)
                boolean isClimbing = false;
                double maxFutureAlt = alt;
                int climbEndIndex = -1;
                
                // Look ahead to see if altitude climbs significantly
                for (int j = i + 1; j < Math.min(i + 30, altAglArray.length); j++) {
                    if (!Double.isNaN(altAglArray[j])) {
                        maxFutureAlt = Math.max(maxFutureAlt, altAglArray[j]);
                        
                        // Check if we've climbed enough from the valley
                        if (altAglArray[j] - alt >= CLIMB_RECOVERY_THRESHOLD) {
                            // Verify sustained climb (at least 10 rows climbing)
                            int climbRows = 0;
                            for (int k = i + 1; k <= j; k++) {
                                if (!Double.isNaN(altAglArray[k]) && altAglArray[k] > alt + 10) {
                                    climbRows++;
                                }
                            }
                            
                            if (climbRows >= MIN_CLIMB_WINDOW) {
                                isClimbing = true;
                                climbEndIndex = j;
                                break;
                            }
                        }
                    }
                }
                
                // CRITICAL FIX: Verify aircraft never actually lands during the valley
                // Scan BOTH backward AND forward from detection point to ensure altitude
                // NEVER drops to landing level. If it does anywhere in the valley, this
                // is a touch-and-go, not a go-around.
                boolean actuallyLanded = false;
                if (isClimbing && climbEndIndex > 0) {
                    // Scan backward (last 30 rows) to check if aircraft was on ground before this point
                    for (int k = Math.max(0, i - 30); k < i; k++) {
                        if (!Double.isNaN(altAglArray[k]) && altAglArray[k] <= LANDING_THRESHOLD) {
                            actuallyLanded = true;
                            break;
                        }
                    }
                    
                    // Scan forward (from current point to climb end) to check if aircraft lands
                    if (!actuallyLanded) {
                        for (int k = i; k <= climbEndIndex; k++) {
                            if (!Double.isNaN(altAglArray[k]) && altAglArray[k] <= LANDING_THRESHOLD) {
                                actuallyLanded = true;
                                break;
                            }
                        }
                    }
                }
                
                // If we found a valley pattern (descending, low point, then climbing)
                // AND the aircraft never actually landed (stayed above 5 ft throughout)
                if (isDescending && isClimbing && !actuallyLanded) {
                    // Find the exact valley bottom (minimum altitude in local window)
                    int valleyIndex = i;
                    double valleyAlt = alt;
                    
                    // Search ±5 rows for the exact minimum
                    for (int j = Math.max(0, i - 5); j < Math.min(i + 5, altAglArray.length); j++) {
                        if (!Double.isNaN(altAglArray[j]) && altAglArray[j] < valleyAlt) {
                            valleyAlt = altAglArray[j];
                            valleyIndex = j;
                        }
                    }
                    
                    goAroundIndices.add(valleyIndex);
                    
                    // Skip ahead to avoid detecting multiple go-arounds in the same pattern
                    i = valleyIndex + MIN_CLIMB_WINDOW;
                }
            }
        }
        
        return goAroundIndices;
    }

    /**
     * Diagnostic method to retrieve and inspect AltAGL values from a flight.
     * Useful for verifying altitude data before implementing ground detection logic.
     * 
     * @param connection the database connection
     * @param flightId the flight ID to inspect
     * @param maxRows maximum number of rows to retrieve (default: all rows if <= 0)
     * @return array of AltAGL values, or null if not available
     * @throws SQLException if there is an error with the SQL query
     */
    public static double[] getAltAGLValues(Connection connection, int flightId, int maxRows) 
            throws SQLException {
        DoubleTimeSeries altAgl = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, Parameters.ALT_AGL);
        
        if (altAgl == null) {
            System.err.println("Warning: AltAGL not available for flight " + flightId);
            return null;
        }
        
        int numRows = (maxRows > 0 && maxRows < altAgl.size()) ? maxRows : altAgl.size();
        double[] values = new double[numRows];
        
        for (int i = 0; i < numRows; i++) {
            values[i] = altAgl.get(i);
        }
        
        return values;
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
     * Uses 2-pass algorithm:
     * - Pass 1: Sequential detection (TAXI → TAKEOFF → CLIMB)
     * - Pass 2: Consolidated classification (CRUISE, DESCENT, LANDING, GROUND)
     * 
     * Note: TOUCH_AND_GO and GO_AROUND phases are detected in post-processing.
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
        
        int numRows = altAgl.size();
        List<FlightPhase> phases = new ArrayList<>(numRows);

        // Initialize all phases to UNKNOWN
        for (int i = 0; i < numRows; i++) {
            phases.add(FlightPhase.UNKNOWN);
        }

        int currentIndex = 0;
        
        // PHASE 1: TAXI - From start until takeoff criteria is first met
        // Criteria: All rows from beginning until RPM >= 2100 AND groundSpeed > 14.5 AND groundSpeed < 80
        while (currentIndex < numRows) {
            if (!Double.isNaN(altAgl.get(currentIndex)) && !Double.isNaN(groundSpeed.get(currentIndex))) {
                // Check if takeoff criteria is met
                if (rpm != null && !Double.isNaN(rpm.get(currentIndex)) && 
                    rpm.get(currentIndex) >= 2100 && groundSpeed.get(currentIndex) > 14.5 && groundSpeed.get(currentIndex) < 80) {
                    // Takeoff criteria met - taxi phase ends HERE
                    break;
                }
                // Only classify as TAXI if on ground (altitude <= 5 feet)
                if (altAgl.get(currentIndex) <= 5.0) {
                    phases.set(currentIndex, FlightPhase.TAXI);
                }
            }
            currentIndex++;
        }

        // PHASE 2: TAKEOFF - First 15 consecutive rows meeting takeoff criteria
        // Criteria: RPM >= 2100 AND groundSpeed > 14.5 AND groundSpeed < 80
        int takeoffCounter = 0;
        while (currentIndex < numRows && takeoffCounter < 15) {
            if (!Double.isNaN(altAgl.get(currentIndex)) && !Double.isNaN(groundSpeed.get(currentIndex))) {
                if (rpm != null && !Double.isNaN(rpm.get(currentIndex)) && 
                    rpm.get(currentIndex) >= 2100 && groundSpeed.get(currentIndex) > 14.5 && groundSpeed.get(currentIndex) < 80) {
                    phases.set(currentIndex, FlightPhase.TAKEOFF);
                    takeoffCounter++;
                } else {
                    break;  // Criteria not met - stop takeoff phase
                }
            }
            currentIndex++;
        }

        // PHASE 3: CLIMB - After takeoff until reaching 600 ft AGL
        // Begins after 15 takeoff rows, criteria: RPM >= 2100 AND groundSpeed > 14.5 AND groundSpeed <= 80
        // Ends when AGL >= 600 feet
        while (currentIndex < numRows) {
            if (!Double.isNaN(altAgl.get(currentIndex)) && !Double.isNaN(groundSpeed.get(currentIndex))) {
                // End climb when reaching 600 ft
                if (altAgl.get(currentIndex) >= 600) {
                    currentIndex++;
                    break;
                }
                
                // Check climb criteria
                if (rpm != null && !Double.isNaN(rpm.get(currentIndex)) && 
                    rpm.get(currentIndex) >= 2100 && groundSpeed.get(currentIndex) > 14.5 && groundSpeed.get(currentIndex) <= 80) {
                    phases.set(currentIndex, FlightPhase.CLIMB);
                } else {
                    // Criteria not met but still climbing
                    phases.set(currentIndex, FlightPhase.CLIMB);
                }
            }
            currentIndex++;
        }

        // PHASE 4-8: CONSOLIDATED PASS - Classify remaining phases based on altitude and vertical trend
        for (int i = 0; i < numRows; i++) {
            if (phases.get(i) == FlightPhase.UNKNOWN) {
                double alt = altAgl.get(i);
                double speed = groundSpeed.get(i);
                
                if (!Double.isNaN(alt) && !Double.isNaN(speed)) {
                    // Check if descending (compare with previous altitude)
                    boolean isDescending = false;
                    if (i > 0 && !Double.isNaN(altAgl.get(i-1))) {
                        isDescending = alt < altAgl.get(i-1);
                    }
                    
                    // Classify based on altitude thresholds and vertical trend
                    if (alt >= 600) {
                        phases.set(i, FlightPhase.CRUISE);
                    } else if (alt >= 100 && alt < 600) {
                        // Pattern altitude: classify as descent
                        phases.set(i, FlightPhase.DESCENT);
                    } else if (alt < 100 && alt > 5) {
                        // Low altitude: default to landing
                        phases.set(i, FlightPhase.LANDING);
                    } else if (alt <= 5) {
                        // On ground: stationary = GROUND, moving = LANDING (taxi detected in post-processing)
                        phases.set(i, speed == 0.0 ? FlightPhase.GROUND : FlightPhase.LANDING);
                    }
                }
            }
        }

        return new FlightPhaseData(phases, numRows);
    }

    /**
     * Compute complete flight phases with post-processing.
     * 
     * @param altAgl altitude AGL time series
     * @param groundSpeed ground speed time series  
     * @param rpm RPM time series (can be null if not available)
     * @return FlightPhaseData containing phase information for each row
     */
    private static FlightPhaseData computeFlightPhasesInternal(
            DoubleTimeSeries altAgl,
            DoubleTimeSeries groundSpeed,
            DoubleTimeSeries rpm) {
        
        int numRows = altAgl.size();
        List<FlightPhase> phases = new ArrayList<>(numRows);

        // Initialize all phases to UNKNOWN
        for (int i = 0; i < numRows; i++) {
            phases.add(FlightPhase.UNKNOWN);
        }

        int currentIndex = 0;
        
        // PHASE 1: TAXI - From start until takeoff criteria is first met
        // Criteria: All rows from beginning until RPM >= 2100 AND groundSpeed > 14.5 AND groundSpeed < 80
        while (currentIndex < numRows) {
            if (!Double.isNaN(altAgl.get(currentIndex)) && !Double.isNaN(groundSpeed.get(currentIndex))) {
                // Check if takeoff criteria is met
                if (rpm != null && !Double.isNaN(rpm.get(currentIndex)) && 
                    rpm.get(currentIndex) >= 2100 && groundSpeed.get(currentIndex) > 14.5 && groundSpeed.get(currentIndex) < 80) {
                    // Takeoff criteria met - taxi phase ends HERE
                    break;
                }
                // Only classify as TAXI if on ground (altitude <= 5 feet)
                if (altAgl.get(currentIndex) <= 5.0) {
                    phases.set(currentIndex, FlightPhase.TAXI);
                }
            }
            currentIndex++;
        }

        // PHASE 2: TAKEOFF - First 15 consecutive rows meeting takeoff criteria
        // Criteria: RPM >= 2100 AND groundSpeed > 14.5 AND groundSpeed < 80
        int takeoffCounter = 0;
        while (currentIndex < numRows && takeoffCounter < 15) {
            if (!Double.isNaN(altAgl.get(currentIndex)) && !Double.isNaN(groundSpeed.get(currentIndex))) {
                if (rpm != null && !Double.isNaN(rpm.get(currentIndex)) && 
                    rpm.get(currentIndex) >= 2100 && groundSpeed.get(currentIndex) > 14.5 && groundSpeed.get(currentIndex) < 80) {
                    phases.set(currentIndex, FlightPhase.TAKEOFF);
                    takeoffCounter++;
                } else {
                    break;  // Criteria not met - stop takeoff phase
                }
            }
            currentIndex++;
        }

        // PHASE 3: CLIMB - After takeoff until reaching 600 ft AGL
        // Begins after 15 takeoff rows, criteria: RPM >= 2100 AND groundSpeed > 14.5 AND groundSpeed <= 80
        // Ends when AGL >= 600 feet
        while (currentIndex < numRows) {
            if (!Double.isNaN(altAgl.get(currentIndex)) && !Double.isNaN(groundSpeed.get(currentIndex))) {
                // End climb when reaching 600 ft
                if (altAgl.get(currentIndex) >= 600) {
                    currentIndex++;
                    break;
                }
                
                // Check climb criteria
                if (rpm != null && !Double.isNaN(rpm.get(currentIndex)) && 
                    rpm.get(currentIndex) >= 2100 && groundSpeed.get(currentIndex) > 14.5 && groundSpeed.get(currentIndex) <= 80) {
                    phases.set(currentIndex, FlightPhase.CLIMB);
                } else {
                    // Criteria not met but still climbing
                    phases.set(currentIndex, FlightPhase.CLIMB);
                }
            }
            currentIndex++;
        }

        // PHASE 4-8: CONSOLIDATED PASS - Classify remaining phases based on altitude and vertical trend
        for (int i = 0; i < numRows; i++) {
            if (phases.get(i) == FlightPhase.UNKNOWN) {
                double alt = altAgl.get(i);
                double speed = groundSpeed.get(i);
                
                if (!Double.isNaN(alt) && !Double.isNaN(speed)) {
                    // Check if descending (compare with previous altitude)
                    boolean isDescending = false;
                    if (i > 0 && !Double.isNaN(altAgl.get(i-1))) {
                        isDescending = alt < altAgl.get(i-1);
                    }
                    
                    // Classify based on altitude thresholds and vertical trend
                    if (alt >= 600) {
                        phases.set(i, FlightPhase.CRUISE);
                    } else if (alt >= 100 && alt < 600) {
                        // Pattern altitude: default to descent unless clearly climbing
                        phases.set(i, isDescending ? FlightPhase.DESCENT : FlightPhase.DESCENT);
                    } else if (alt < 100 && alt > 5) {
                        // Low altitude: default to landing
                        phases.set(i, FlightPhase.LANDING);
                    } else if (alt <= 5) {
                        // On ground: stationary = GROUND, moving = LANDING (taxi detected in post-processing)
                        phases.set(i, speed == 0.0 ? FlightPhase.GROUND : FlightPhase.LANDING);
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
                // Mark a wider window around the split point as TOUCH_AND_GO
                // This covers the entire ground contact and acceleration period
                for (int i = Math.max(0, splitIndex - 10); 
                     i <= Math.min(phaseData.getPhases().size() - 1, splitIndex + 10); i++) {
                    phaseData.getPhases().set(i, FlightPhase.TOUCH_AND_GO);
                }
            }
        }
        
        // Detect and mark go-around zones (±10 rows around valleys)
        List<Integer> goAroundIndices = detectGoArounds(altAglArray);
        for (int goAroundIndex : goAroundIndices) {
            // Mark ±10 rows around the go-around valley as GO_AROUND phase
            // This covers the low approach and climb-out
            for (int i = Math.max(0, goAroundIndex - 10); 
                 i <= Math.min(phaseData.getPhases().size() - 1, goAroundIndex + 10); i++) {
                phaseData.getPhases().set(i, FlightPhase.GO_AROUND);
            }
        }
        
        // Apply altitude smoothing and reclassify phases
        final double ALTITUDE_CHANGE_THRESHOLD = 5.0;
        
        for (int i = 0; i < phaseData.getPhases().size() && i < altAglArray.length; i++) {
            FlightPhase currentPhase = phaseData.getPhases().get(i);
            
            // Skip already classified special phases
            if (currentPhase == FlightPhase.TOUCH_AND_GO ||
                currentPhase == FlightPhase.GO_AROUND ||
                currentPhase == FlightPhase.TAXI ||
                currentPhase == FlightPhase.TAKEOFF) {
                continue;
            }
            
            double alt = altAglArray[i];
            if (!Double.isNaN(alt)) {
                // Determine vertical trend with 5-row window for noise filtering
                boolean isClimbing = false;
                boolean isDescending = false;
                
                if (i >= 5 && i < altAglArray.length) {
                    double startAlt = altAglArray[i - 5];
                    if (!Double.isNaN(startAlt)) {
                        double altChange = alt - startAlt;
                        // 15 feet over 5 rows = 3 ft/row average
                        if (altChange > 15.0) {
                            isClimbing = true;
                        } else if (altChange < -15.0) {
                            isDescending = true;
                        }
                    }
                } else if (i > 0) {
                    double prevAlt = altAglArray[i - 1];
                    if (!Double.isNaN(prevAlt)) {
                        double altChange = alt - prevAlt;
                        if (altChange > ALTITUDE_CHANGE_THRESHOLD) {
                            isClimbing = true;
                        } else if (altChange < -ALTITUDE_CHANGE_THRESHOLD) {
                            isDescending = true;
                        }
                    }
                }
                
                // Reclassify based on altitude and trend
                if (alt >= 600) {
                    phaseData.getPhases().set(i, FlightPhase.CRUISE);
                } else if (alt >= 100 && alt < 600) {
                    // Pattern altitude
                    if (isClimbing) {
                        phaseData.getPhases().set(i, FlightPhase.CLIMB);
                    } else if (isDescending) {
                        phaseData.getPhases().set(i, FlightPhase.DESCENT);
                    } else {
                        // Level flight: check context from previous phase
                        FlightPhase prevPhase = (i > 0) ? phaseData.getPhases().get(i - 1) : FlightPhase.UNKNOWN;
                        if (prevPhase == FlightPhase.TAKEOFF || 
                            prevPhase == FlightPhase.CLIMB) {
                            phaseData.getPhases().set(i, FlightPhase.CLIMB);
                        } else {
                            phaseData.getPhases().set(i, FlightPhase.DESCENT);
                        }
                    }
                } else if (alt < 100) {
                    // Low altitude
                    if (isDescending) {
                        phaseData.getPhases().set(i, FlightPhase.LANDING);
                    } else if (isClimbing) {
                        phaseData.getPhases().set(i, FlightPhase.CLIMB);
                    } else {
                        // Level flight: use context from previous phase
                        FlightPhase prevPhase = (i > 0) ? phaseData.getPhases().get(i - 1) : FlightPhase.UNKNOWN;
                        if (prevPhase == FlightPhase.TAKEOFF || 
                            prevPhase == FlightPhase.TAXI || 
                            prevPhase == FlightPhase.CLIMB || 
                            prevPhase == FlightPhase.TOUCH_AND_GO) {
                            phaseData.getPhases().set(i, FlightPhase.CLIMB);
                        } else if (prevPhase == FlightPhase.LANDING || 
                                   prevPhase == FlightPhase.DESCENT || 
                                   prevPhase == FlightPhase.CRUISE) {
                            phaseData.getPhases().set(i, FlightPhase.LANDING);
                        } else {
                            phaseData.getPhases().set(i, FlightPhase.LANDING);
                        }
                    }
                }
            }
        }
        
        // Final pass: Detect taxi after landing
        if (groundSpeed != null) {
            for (int i = 0; i < phaseData.getPhases().size() && i < altAglArray.length && i < groundSpeed.size(); i++) {
                FlightPhase currentPhase = phaseData.getPhases().get(i);
                
                // Only reclassify LANDING phases
                if (currentPhase == FlightPhase.LANDING) {
                    double alt = altAglArray[i];
                    double speed = groundSpeed.get(i);
                    
                    if (!Double.isNaN(alt) && !Double.isNaN(speed)) {
                        // On ground and moving = taxi
                        if (alt <= 5.0 && speed > 0.0) {
                            phaseData.getPhases().set(i, FlightPhase.TAXI);
                        }
                    }
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
}
