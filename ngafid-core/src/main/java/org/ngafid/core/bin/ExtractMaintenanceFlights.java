package org.ngafid.core.bin;

import org.ngafid.core.Config;
import org.ngafid.core.Database;
import org.ngafid.core.agl_converter.MSLtoAGLConverter;
import org.ngafid.core.flights.DoubleTimeSeries;
import org.ngafid.core.flights.Flight;
import org.ngafid.core.flights.Parameters;
import org.ngafid.core.flights.StringTimeSeries;
import org.ngafid.core.flights.export.CachedCSVWriter;
import org.ngafid.core.flights.maintenance.AircraftTimeline;
import org.ngafid.core.flights.maintenance.MaintenanceRecord;

import java.io.*;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import static org.ngafid.core.Config.NGAFID_ARCHIVE_DIR;

/**
 * Extracts flight CSV files around maintenance events into before/during/after folders for analysis.
 * <p>
 * <b>Flow:</b> (1) Load maintenance records from CSV. (2) For each workorder, build a timeline of flights
 * in the window [open−10, close+10]. (3) Assign each flight to a phase (before/during/after) using
 * {@link #computeMaintenancePhase}: before = flight start &lt; date_time_opened; during = between open and close;
 * after = flight start &gt; date_time_closed. (4) Count up to 3 "before" and 3 "after" flights per workorder
 * (setFlightsToNextFlights). (5) Export only flights that pass filters (AGL, left ground, cruise altitude,
 * phase computation) to before/during/after CSVs. (6) Write manifest.json.
 * <p>
 * Modes: normal extraction (outputDir CSV [CSV ...]); --validate inputCsv (writes valid-/invalid- CSVs);
 * --validate-verify inputCsv (checks valid CSV against DB).
 */
public final class ExtractMaintenanceFlights {
    /** Maintenance records are UTC; DB is GMT. All timeline logic uses GMT. */
    private static final DateTimeFormatter MYSQL_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final HashMap<Integer, MaintenanceRecord> RECORDS_BY_WORKORDER = new HashMap<>();
    private static final TreeSet<MaintenanceRecord> ALL_RECORDS = new TreeSet<>();
    private static final TreeSet<String> TAIL_NUMBERS = new TreeSet<>();
    private static final TreeSet<String> AIRFRAMES = new TreeSet<>();
    private static int extractedFlightsTotal = 0;
    private static int extractedBeforeTotal = 0;
    private static int extractedDuringTotal = 0;
    private static int extractedDuringSingleDayTotal = 0;
    private static int extractedAfterTotal = 0;
    private static final double CRUISE_ALTITUDE_AGL_FT = 600.0;

    /** Log file for per-workorder time trace; all dates in GMT. */
    private static PrintWriter timeLogWriter = null;

    /** Writes time-trace line to maintenance_flight_sort_log.txt only (no console output). */
    private static void timeLog(String line) {
        if (timeLogWriter != null) {
            timeLogWriter.println(line);
        }
    }

    private ExtractMaintenanceFlights() {
        throw new UnsupportedOperationException("Utility class");
    }

    // -------------------------------------------------------------------------
    // Input: load maintenance records from CSV
    // -------------------------------------------------------------------------

    /**
     * Reads the cluster files and populates RECORDS_BY_WORKORDER, ALL_RECORDS, TAIL_NUMBERS, and AIRFRAMES.
     *
     * @param allClusters the list of all cluster files
     */
    private static void readClusterFiles(String[] allClusters) {
        int lineCount = 0;
        for (String allCluster : allClusters) {
            BufferedReader reader;

            try {
                System.out.println("READING FILE: '" + allCluster + "'");
                reader = new BufferedReader(new FileReader(allCluster));
                String line = reader.readLine();
                
                // Skip header row (starts with "workorder" or "workorder,")
                if (line != null && line.toLowerCase().startsWith("workorder")) {
                    line = reader.readLine();
                }

                while (line != null) {
                    // Skip blank lines and header rows
                    if (line.trim().isEmpty()) {
                        line = reader.readLine();
                        continue;
                    }
                    if (line.toLowerCase().startsWith("workorder")) {
                        line = reader.readLine();
                        continue;
                    }

                    lineCount++;
                    MaintenanceRecord record = new MaintenanceRecord(line);

                    MaintenanceRecord existingRecord = RECORDS_BY_WORKORDER.get(record.getWorkorderNumber());
                    if (existingRecord == null) {
                        // this is a record we have not yet seen before
                        RECORDS_BY_WORKORDER.put(record.getWorkorderNumber(), record);

                        // Include tail if airframe matches filter or is empty (new format has no airframe)
                        if (record.getAirframe().isEmpty() || record.getAirframe().equals("C172")
                                || record.getAirframe().equals("ARCH") || record.getAirframe().equals("SEMI")) {
                            TAIL_NUMBERS.add(record.getTailNumber());
                        }

                        AIRFRAMES.add(record.getAirframe());

                        ALL_RECORDS.add(record);
                    } else {
                        existingRecord.combine(record);
                    }

                    line = reader.readLine();
                }

                reader.close();
            } catch (IOException e) {
                System.err.println("Could not read cluster file: '" + allCluster + "': " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        }

        System.out.println("\n\n\n");
        System.out.println("Number of record lines: " + lineCount);
        System.out.println("Number of workorders: " + ALL_RECORDS.size());
        if (!ALL_RECORDS.isEmpty()) {
            System.out.println("earliest date: " + ALL_RECORDS.first().getOpenDate());
            System.out.println("latest date: " + ALL_RECORDS.last().getCloseDate());
        } else {
            System.out.println("No records found: cannot print earliest/latest date.");
        }
        System.out.println("unique tails: ");
        System.out.println("\t" + TAIL_NUMBERS);
    }

    // -------------------------------------------------------------------------
    // Date helpers and phase classification
    // -------------------------------------------------------------------------

    /** Treats the given date as UTC and returns start-of-day in GMT (same instant), formatted for MySQL. */
    private static String utcDateToGmtStartOfDay(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).format(MYSQL_DATETIME);
    }

    /** Treats the given date as UTC and returns end-of-day (23:59:59) in GMT, formatted for MySQL. */
    private static String utcDateToGmtEndOfDay(LocalDate date) {
        return date.atTime(23, 59, 59).atZone(ZoneOffset.UTC).format(MYSQL_DATETIME);
    }

    /** Phase of a flight relative to a maintenance window: before, during, or after. */
    private static final String PHASE_BEFORE = "before";
    private static final String PHASE_DURING = "during";
    private static final String PHASE_AFTER = "after";

    /**
     * Determines the maintenance phase for a flight given a maintenance record.
     * Uses date_time_opened and date_time_closed for precise comparison:
     * <ul>
     *   <li><b>before</b>: flight start &lt; date_time_opened</li>
     *   <li><b>during</b>: date_time_opened &lt;= flight start &lt;= date_time_closed</li>
     *   <li><b>after</b>: flight start &gt; date_time_closed</li>
     * </ul>
     *
     * @param flightStartGmt flight start datetime (GMT)
     * @param openGmt        maintenance open datetime (GMT)
     * @param closeGmt       maintenance close datetime (GMT)
     * @return PHASE_BEFORE, PHASE_DURING, or PHASE_AFTER
     */
    private static String computeMaintenancePhase(LocalDateTime flightStartGmt,
                                                   LocalDateTime openGmt,
                                                   LocalDateTime closeGmt) {
        if (flightStartGmt.isBefore(openGmt)) {
            return PHASE_BEFORE;
        }
        if (flightStartGmt.isAfter(closeGmt)) {
            return PHASE_AFTER;
        }
        return PHASE_DURING;
    }

    /**
     * Counts flights in the database that overlap the extended maintenance window:
     * 10 days before open date through 10 days after close date (matches extraction window).
     * Maintenance dates are in UTC; converts window to GMT for query (DB stores flights in GMT; GMT = UTC).
     *
     * @param connection the database connection
     * @param tailNumber  the tail number
     * @param openDate    maintenance open date (UTC)
     * @param closeDate   maintenance close date (UTC)
     * @return number of flights in the extended window, or 0 if tail not found
     * @throws SQLException if there is an error with the SQL query
     */
    private static int countFlightsInMaintenancePeriod(Connection connection, String tailNumber,
                                                       LocalDate openDate, LocalDate closeDate)
            throws SQLException {
        LocalDate windowStart = openDate.minusDays(10);
        LocalDate windowEnd = closeDate.plusDays(10);
        String windowStartGmt = utcDateToGmtStartOfDay(windowStart);
        String windowEndGmt = utcDateToGmtEndOfDay(windowEnd);
        PreparedStatement stmt = connection.prepareStatement(
                "SELECT COUNT(*) FROM flights f " +
                "JOIN tails t ON f.fleet_id = t.fleet_id AND f.system_id = t.system_id " +
                "WHERE t.tail = ? AND f.start_time <= ? AND f.end_time >= ?");
        stmt.setString(1, tailNumber);
        stmt.setString(2, windowEndGmt);
        stmt.setString(3, windowStartGmt);
        ResultSet rs = stmt.executeQuery();
        int count = 0;
        if (rs.next()) {
            count = rs.getInt(1);
        }
        rs.close();
        stmt.close();
        return count;
    }

    /**
     * Validates maintenance records by checking that each record has at least one flight
     * in the extended window (10 days before open through 10 days after close). Writes valid rows to
     * valid-{basename}.csv and invalid rows to invalid-{basename}.csv in the same directory
     * as the input. Run this before extraction so only valid records are used.
     *
     * @param connection   the database connection
     * @param inputCsvPath path to the input maintenance CSV (e.g. test_maintenance.csv)
     * @throws IOException  if reading or writing files fails
     * @throws SQLException if a query fails
     */
    private static void validateMaintenanceRecords(Connection connection, String inputCsvPath)
            throws IOException, SQLException {
        int total = 0;
        int valid = 0;
        int invalid = 0;

        File inputFile = new File(inputCsvPath);
        String dir = inputFile.getParent();
        String baseName = inputFile.getName();
        String validCsvPath = (dir != null ? dir + File.separator : "") + "valid-" + baseName;
        String invalidCsvPath = (dir != null ? dir + File.separator : "") + "invalid-" + baseName;

        try (BufferedReader reader = new BufferedReader(new FileReader(inputCsvPath));
             PrintWriter validWriter = new PrintWriter(new FileWriter(validCsvPath));
             PrintWriter invalidWriter = new PrintWriter(new FileWriter(invalidCsvPath))) {

            String line = reader.readLine();
            if (line == null) {
                System.err.println("Input file is empty.");
                return;
            }
            // Write header (with or without "workorder" header row)
            if (line != null && line.trim().isEmpty()) {
                line = reader.readLine();
            }
            if (line != null && line.toLowerCase().startsWith("workorder")) {
                validWriter.println(line);
                invalidWriter.println(line);
                line = reader.readLine();
            }

            while (line != null) {
                if (line.trim().isEmpty()) {
                    line = reader.readLine();
                    continue;
                }
                if (line.toLowerCase().startsWith("workorder")) {
                    line = reader.readLine();
                    continue;
                }

                total++;
                try {
                    MaintenanceRecord record = new MaintenanceRecord(line);
                    int flightCount = countFlightsInMaintenancePeriod(connection,
                            record.getTailNumber(), record.getOpenDate(), record.getCloseDate());
                    if (flightCount > 0) {
                        validWriter.println(line);
                        valid++;
                    } else {
                        invalidWriter.println(line);
                        invalid++;
                    }
                } catch (Exception e) {
                    invalidWriter.println(line);
                    invalid++;
                    System.err.println("Skip invalid line (parse error): " + e.getMessage());
                }
                line = reader.readLine();
            }
        }

        System.out.println("Validation: total=" + total + " valid=" + valid + " invalid=" + invalid +
                " -> valid: " + validCsvPath + ", invalid: " + invalidCsvPath);
    }

    /**
     * Verifies that validation logic is consistent: re-queries the DB for each record in the
     * input and output CSVs and checks that every row in the output has flight count &gt; 0 and
     * every row only in the input has flight count 0. Use after --validate to double-check.
     *
     * @param connection   the database connection
     * @param inputCsvPath path to the original maintenance CSV
     * @param outputCsvPath path to the valid_maintenance.csv produced by --validate
     * @return true if verification passed, false if any mismatch
     */
    private static boolean verifyValidationResults(Connection connection, String inputCsvPath,
                                                    String outputCsvPath)
            throws IOException, SQLException {
        Set<String> validKeys = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(outputCsvPath))) {
            String line = reader.readLine();
            while (line != null && line.toLowerCase().startsWith("workorder")) {
                line = reader.readLine();
            }
            while (line != null) {
                if (!line.trim().isEmpty() && !line.toLowerCase().startsWith("workorder")) {
                    try {
                        MaintenanceRecord r = new MaintenanceRecord(line);
                        validKeys.add(r.getWorkorderNumber() + "|" + r.getTailNumber() + "|"
                                + r.getOpenDate() + "|" + r.getCloseDate());
                    } catch (Exception ignored) { }
                }
                line = reader.readLine();
            }
        }

        List<String> mismatches = new ArrayList<>();
        int checked = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(inputCsvPath))) {
            String line = reader.readLine();
            while (line != null && (line.trim().isEmpty() || line.toLowerCase().startsWith("workorder"))) {
                line = reader.readLine();
            }
            while (line != null) {
                if (line.trim().isEmpty() || line.toLowerCase().startsWith("workorder")) {
                    line = reader.readLine();
                    continue;
                }
                try {
                    MaintenanceRecord record = new MaintenanceRecord(line);
                    String key = record.getWorkorderNumber() + "|" + record.getTailNumber() + "|"
                            + record.getOpenDate() + "|" + record.getCloseDate();
                    int count = countFlightsInMaintenancePeriod(connection,
                            record.getTailNumber(), record.getOpenDate(), record.getCloseDate());
                    boolean inValid = validKeys.contains(key);
                    if (inValid && count <= 0) {
                        mismatches.add("WO " + record.getWorkorderNumber() + " in valid CSV but count=" + count + " (expected > 0)");
                    } else if (!inValid && count > 0) {
                        mismatches.add("WO " + record.getWorkorderNumber() + " excluded from valid CSV but count=" + count + " (expected 0)");
                    }
                    checked++;
                } catch (Exception e) {
                    mismatches.add("Parse error: " + e.getMessage());
                }
                line = reader.readLine();
            }
        }

        if (mismatches.isEmpty()) {
            System.out.println("VERIFY PASS: " + checked + " records; logic matches DB.");
            return true;
        }
        System.err.println("VERIFY FAIL: " + mismatches.size() + " mismatch(es)");
        for (String m : mismatches) {
            System.err.println("  " + m);
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Timeline: fetch flights in [open-10, close+10] for each tail
    // -------------------------------------------------------------------------

    /**
     * Populates the timeline list with the flights for the given tail set.
     * Maintenance dates are UTC; window is converted to GMT for query (DB stores flights in GMT; GMT = UTC).
     * Flight start/end from DB are left in GMT so the extracted calendar date matches maintenance (UTC).
     *
     * @param connection the database connection
     * @param tailSet    the set of tails
     * @param startDate  start of window in UTC (e.g. openDate - 10)
     * @param endDate    end of window in UTC (e.g. closeDate + 10)
     * @return the list of aircraft timelines
     * @throws SQLException if there is an error with the SQL query
     */
    private static List<AircraftTimeline> buildTimeline(Connection connection, ResultSet tailSet, LocalDate startDate,
                                                        LocalDate endDate) throws SQLException {
        List<AircraftTimeline> timeline = new ArrayList<>();
        String windowStartGmt = utcDateToGmtStartOfDay(startDate);
        String windowEndGmt = utcDateToGmtEndOfDay(endDate);

        while (tailSet.next()) {
            String systemId = tailSet.getString(1);
            int fleetId = tailSet.getInt(2);

            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT id, start_time, end_time, airframe_id FROM flights " +
                            "WHERE fleet_id = ? AND system_id = ? AND start_time <= ? AND end_time >= ? ORDER BY start_time");
            stmt.setInt(1, fleetId);
            stmt.setString(2, systemId);
            stmt.setString(3, windowEndGmt);
            stmt.setString(4, windowStartGmt);

            ResultSet resultSet = stmt.executeQuery();
            while (resultSet.next()) {
                int flightId = resultSet.getInt(1);
                String flightStartTime = resultSet.getString(2);
                String flightEndTime = resultSet.getString(3);

                // DB is GMT; maintenance is UTC (same calendar date). Keep flight times in GMT so
                // AircraftTimeline.getStartTime() yields the GMT/UTC date for before/during/after comparison.
                timeline.add(new AircraftTimeline(flightId, flightStartTime, flightEndTime));
            }

            resultSet.close();
            stmt.close();
        }

        return timeline;
    }


    private static final class AltAGLResult {
        final double[] values;
        final String failureReason;

        AltAGLResult(double[] values, String failureReason) {
            this.values = values;
            this.failureReason = failureReason;
        }
    }

    /**
     * Obtains AltAGL values for a flight. Tries DB first, then fallback: AltMSL/AltB + Lat/Lon + terrain.
     * @return AltAGLResult with values or null failureReason; if unobtainable, values is null and failureReason describes what is missing
     */
    private static AltAGLResult getAltAGLForExtraction(Connection connection, int flightId, int maxRows) {
        try {
            double[] values = FlightPhaseProcessor.getAltAGLValues(connection, flightId, maxRows);
            if (values != null && values.length > 0) {
                return new AltAGLResult(values, null);
            }
            return new AltAGLResult(null, "AltAGL empty or zero rows in DB");
        } catch (Exception ignored) {
            /* fall through to terrain-based fallback: AltB/AltMSL + Lat/Lon + terrain */
        }
        DoubleTimeSeries lat = null;
        DoubleTimeSeries lon = null;
        DoubleTimeSeries altMSL = null;
        DoubleTimeSeries altB = null;
        try {
            lat = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, Parameters.LATITUDE);
            lon = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, Parameters.LONGITUDE);
            altMSL = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, Parameters.ALT_MSL);
            altB = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, Parameters.ALT_B);
        } catch (SQLException e) {
            return new AltAGLResult(null, "AltAGL not in DB; fallback failed: error fetching series - " + e.getMessage());
        }
        DoubleTimeSeries altSource = altMSL != null ? altMSL : altB;
        if (altSource == null) {
            return new AltAGLResult(null, "AltAGL not in DB; cannot compute from AltB+terrain: AltMSL and AltB both missing");
        }
        if (lat == null) {
            return new AltAGLResult(null, "AltAGL not in DB; cannot compute from AltB+terrain: Latitude missing (needed for terrain lookup)");
        }
        if (lon == null) {
            return new AltAGLResult(null, "AltAGL not in DB; cannot compute from AltB+terrain: Longitude missing (needed for terrain lookup)");
        }
        if (Config.NGAFID_TERRAIN_DIR == null || Config.NGAFID_TERRAIN_DIR.trim().isEmpty()) {
            return new AltAGLResult(null, "AltAGL not in DB; cannot compute from AltB+terrain: terrain not configured (ngafid.terrain.dir empty)");
        }
        int n = Math.min(altSource.size(), Math.min(lat.size(), lon.size()));
        n = Math.min(n, maxRows);
        if (n == 0) {
            return new AltAGLResult(null, "AltAGL not in DB; cannot compute from AltB+terrain: no data rows in altitude/position series");
        }
        double[] agl = new double[n];
        int terrainFailCount = 0;
        for (int i = 0; i < n; i++) {
            double msl = altSource.get(i);
            double la = lat.get(i);
            double lo = lon.get(i);
            if (Double.isNaN(msl) || Double.isNaN(la) || Double.isNaN(lo)) {
                agl[i] = Double.NaN;
            } else {
                double a = MSLtoAGLConverter.convertMSLToAGL(msl, la, lo);
                agl[i] = a;
                if (Double.isNaN(a)) terrainFailCount++;
            }
        }
        if (terrainFailCount == n) {
            return new AltAGLResult(null, "AltAGL not in DB; cannot compute from AltB+terrain: terrain lookup returned NaN for all " + n + " points (tile missing or coordinates out of range)");
        }
        System.err.println("Flight " + flightId + ": AltAGL computed from " + altSource.getName() + " + terrain");
        return new AltAGLResult(agl, null);
    }

    /**
     * Check if the flight actually took off (not a ground run)
     * by checking if there are any takeoff or landing entries in the itinerary table
     *
     * @param connection the database connection
     * @param flightId   the flight id
     * @return true if the flight took off, false if it's a ground run
     * @throws SQLException if there is an error with the SQL query
     */
    private static boolean flightTookOff(Connection connection, int flightId) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(
                "SELECT COUNT(*) FROM itinerary WHERE flight_id = ? AND type IN ('takeoff', 'landing')");
        stmt.setInt(1, flightId);
        ResultSet rs = stmt.executeQuery();
        boolean tookOff = false;
        if (rs.next()) {
            tookOff = rs.getInt(1) > 0;
        }
        rs.close();
        stmt.close();
        return tookOff;
    }

    // -------------------------------------------------------------------------
    // Phase assignment: before / during / after (by date_time_opened, date_time_closed)
    // -------------------------------------------------------------------------

    /**
     * Assigns each flight to a maintenance phase (before/during/after) by comparing flight start
     * datetime with date_time_opened and date_time_closed.
     *
     * @param timeline    sorted list of aircraft timelines (flights)
     * @param tailRecords sorted list of maintenance records for this tail in the cluster
     */
    private static void assignFlightsToPhases(List<AircraftTimeline> timeline,
                                              List<MaintenanceRecord> tailRecords) {
        for (MaintenanceRecord r : tailRecords) {
            LocalDateTime openGmt = r.getOpenDateTime();
            LocalDateTime closeGmt = r.getCloseDateTime();
            timeLog("[TIME] WO " + r.getWorkorderNumber());
            timeLog("[TIME]   Raw from CSV:              open  = \"" + r.getRawOpenDate() + "\"   close = \"" + r.getRawCloseDate() + "\"");
            timeLog("[TIME]   Maintenance record (GMT):  open  = " + openGmt + "   close = " + closeGmt);
            timeLog("[TIME]   Extracted flights (after filtering: AGL, cruise, phases):");
        }

        for (AircraftTimeline ac : timeline) {
            LocalDateTime flightStartGmt = ac.getStartDateTime();
            MaintenanceRecord duringRecord = null;
            MaintenanceRecord beforeRecord = null;
            MaintenanceRecord afterRecord = null;

            for (MaintenanceRecord r : tailRecords) {
                LocalDateTime openGmt = r.getOpenDateTime();
                LocalDateTime closeGmt = r.getCloseDateTime();
                String phase = computeMaintenancePhase(flightStartGmt, openGmt, closeGmt);

                if (PHASE_DURING.equals(phase)) {
                    if (duringRecord == null || r.getOpenDateTime().isBefore(duringRecord.getOpenDateTime())) {
                        duringRecord = r;
                    }
                } else if (PHASE_BEFORE.equals(phase)) {
                    if (beforeRecord == null || r.getOpenDateTime().isBefore(beforeRecord.getOpenDateTime())) {
                        beforeRecord = r;
                    }
                } else if (PHASE_AFTER.equals(phase)) {
                    if (afterRecord == null || r.getCloseDateTime().isAfter(afterRecord.getCloseDateTime())) {
                        afterRecord = r;
                    }
                }
            }

            LocalDate flightDateGmt = ac.getStartTime();
            if (duringRecord != null) {
                ac.setPreviousEvent(duringRecord, 0);
                ac.setNextEvent(duringRecord, 0);
            } else if (beforeRecord != null) {
                long daysToNext = ChronoUnit.DAYS.between(flightDateGmt, beforeRecord.getOpenDate());
                ac.setPreviousEvent(null, -1);
                ac.setNextEvent(beforeRecord, daysToNext);
            } else if (afterRecord != null) {
                long daysSincePrev = ChronoUnit.DAYS.between(afterRecord.getCloseDate(), flightDateGmt);
                ac.setPreviousEvent(afterRecord, daysSincePrev);
                ac.setNextEvent(null, -1);
            } else {
                ac.setPreviousEvent(null, -1);
                ac.setNextEvent(null, -1);
            }
        }
    }

    /**
     * Sets the flights to next and flights since previous for the given timeline
     * Skips ground-run flights (flights with no itinerary takeoff/landing entries) when counting
     *
     * @param connection the database connection
     * @param timeline the list of aircraft timelines
     * @throws SQLException if there is an error with the SQL query
     */
    private static void setFlightsToNextFlights(Connection connection, List<AircraftTimeline> timeline) throws SQLException {
        int numberOfExtractions = 3;
        for (int currentAircraft = 0; currentAircraft < timeline.size(); currentAircraft++) {
            AircraftTimeline ac = timeline.get(currentAircraft);

            // Trigger counting when: 
            // 1. Flight is during maintenance (between open and close; daysToNext==0 or daysSincePrevious==0)
            // 2. OR when transitioning between maintenance events (nextEvent changes from previous flight)
            boolean isOnMaintenanceDay = ac.getDaysToNext() == 0 || ac.getDaysSincePrevious() == 0;
            boolean isAfterMaintenanceGap = false;
            
            if (!isOnMaintenanceDay && currentAircraft > 0 && ac.getNextEvent() != null) {
                AircraftTimeline prev = timeline.get(currentAircraft - 1);
                // Check if this is the first flight after a maintenance event (next event changed)
                isAfterMaintenanceGap = prev.getNextEvent() != ac.getNextEvent();
            }
            
            if (isOnMaintenanceDay || isAfterMaintenanceGap) {
                MaintenanceRecord recordForBefore = ac.getNextEvent(); // during or transition: record for "before" count
                MaintenanceRecord recordForAfter = ac.getPreviousEvent(); // during or transition: record for "after" count

                int i = 1;
                int flightCount = 0;
                while ((currentAircraft - i) >= 0 && flightCount < numberOfExtractions) {
                    AircraftTimeline a = timeline.get(currentAircraft - i);
                    if (a.getDaysToNext() == 0) {
                        // True during = both events set and same; reclassified first-day has only nextEvent
                        boolean isTrueDuring = a.getPreviousEvent() != null && a.getNextEvent() != null
                                && a.getPreviousEvent() == a.getNextEvent();
                        if (isTrueDuring) {
                            a.setFlightsToNext(-1); // -1 means during
                            i++;
                            continue;
                        }
                    }
                    // Only count flights that are "before" for the same record
                    if (recordForBefore == null || a.getNextEvent() != recordForBefore) {
                        i++;
                        continue;
                    }

                    if (!flightTookOff(connection, a.getFlightId())) {
                        a.setFlightsToNext(-2); // -2 means ground run, don't extract
                        i++;
                        continue;
                    }

                    a.setFlightsToNext(flightCount);
                    i++;
                    flightCount++;
                }

                i = 1;
                flightCount = 0;

                while ((currentAircraft + i) < timeline.size() && flightCount < numberOfExtractions) {
                    AircraftTimeline a = timeline.get(currentAircraft + i);
                    if (a.getDaysSincePrevious() == 0) {
                        // True during = both events set and same; reclassified last-day has only previousEvent
                        boolean isTrueDuring = a.getPreviousEvent() != null && a.getNextEvent() != null
                                && a.getPreviousEvent() == a.getNextEvent();
                        if (isTrueDuring) {
                            a.setFlightsSincePrevious(-1); // -1 means during
                            i++;
                            continue;
                        }
                    }
                    // Only count flights that are "after" for the same record
                    if (recordForAfter == null || a.getPreviousEvent() != recordForAfter) {
                        i++;
                        continue;
                    }

                    if (!flightTookOff(connection, a.getFlightId())) {
                        a.setFlightsSincePrevious(-2); // -2 means ground run, don't extract
                        i++;
                        continue;
                    }

                    a.setFlightsSincePrevious(flightCount);
                    i++;
                    flightCount++;
                }
            }
        }
        
        // Second pass: Handle maintenance events that had no flights on their day
        // Find flights that have nextEvent or previousEvent set but no flightsToNext/flightsSincePrevious counted
        for (int i = 0; i < timeline.size(); i++) {
            AircraftTimeline ac = timeline.get(i);
            
            // Check if this flight has a nextEvent but flightsToNext was never set
            if (ac.getNextEvent() != null && ac.getFlightsToNext() < 0 && ac.getDaysToNext() > 0) {
                // Find other flights with the same nextEvent and set their flightsToNext
                MaintenanceRecord nextEvent = ac.getNextEvent();
                int flightCount = 0;
                for (int j = i; j >= 0 && flightCount < numberOfExtractions; j--) {
                    AircraftTimeline a = timeline.get(j);
                    if (a.getNextEvent() == nextEvent && a.getDaysToNext() > 0) {
                        if (!flightTookOff(connection, a.getFlightId())) {
                            a.setFlightsToNext(-2);
                        } else if (a.getFlightsToNext() < 0) {
                            a.setFlightsToNext(flightCount);
                            flightCount++;
                        }
                    } else if (a.getNextEvent() != nextEvent) {
                        break; // Stop when we hit flights pointing to a different event
                    }
                }
            }
            
            // Check if this flight has a previousEvent but flightsSincePrevious was never set
            if (ac.getPreviousEvent() != null && ac.getFlightsSincePrevious() < 0 && ac.getDaysSincePrevious() > 0) {
                // Find other flights with the same previousEvent and set their flightsSincePrevious
                MaintenanceRecord prevEvent = ac.getPreviousEvent();
                int flightCount = 0;
                for (int j = i; j < timeline.size() && flightCount < numberOfExtractions; j++) {
                    AircraftTimeline a = timeline.get(j);
                    if (a.getPreviousEvent() == prevEvent && a.getDaysSincePrevious() > 0) {
                        if (!flightTookOff(connection, a.getFlightId())) {
                            a.setFlightsSincePrevious(-2);
                        } else if (a.getFlightsSincePrevious() < 0) {
                            a.setFlightsSincePrevious(flightCount);
                            flightCount++;
                        }
                    } else if (a.getPreviousEvent() != prevEvent) {
                        break; // Stop when we hit flights pointing to a different event
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Export: filter (AGL, cruise, phases), write CSV + optional debug _phases.csv
    // -------------------------------------------------------------------------

    /**
     * Writes a debug _phases.csv file for a flight (or a segment). Used when --phases is set.
     */
    private static void writeDebugPhasesCsv(Connection connection, Flight flight,
                                           FlightPhaseProcessor.FlightPhaseData phaseData,
                                           String debugFilePath, String headerFirstLine,
                                           int startRow, int endRowExclusive) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(debugFilePath))) {
            writer.println(headerFirstLine);
            writer.println("# Row_Index\tTimestamp\tAltAGL_ft\tGround_Speed_kts\tRPM\tAirport_Distance_ft\tNearest_Airport\tFlight_Phase");
            writer.println("# ----------------------------------------");
            DoubleTimeSeries altAgl = flight.getDoubleTimeSeries(connection, Parameters.ALT_AGL);
            DoubleTimeSeries groundSpeed = flight.getDoubleTimeSeries(connection, Parameters.GND_SPD);
            DoubleTimeSeries rpm = null;
            try { rpm = flight.getDoubleTimeSeries(connection, Parameters.E1_RPM); } catch (Exception e) {}
            DoubleTimeSeries airportDist = null;
            DoubleTimeSeries nearestAirport = null;
            try { airportDist = flight.getDoubleTimeSeries(connection, Parameters.AIRPORT_DISTANCE); } catch (Exception e) {}
            try { nearestAirport = flight.getDoubleTimeSeries(connection, Parameters.NEAREST_AIRPORT); } catch (Exception e) {}
            StringTimeSeries timestamps = null;
            try { timestamps = flight.getStringTimeSeries(connection, Parameters.UTC_DATE_TIME); } catch (Exception e) {}
            int endRow = Math.min(endRowExclusive, altAgl.size());
            for (int i = startRow; i < endRow; i++) {
                String timestamp = (timestamps != null && i < timestamps.size()) ? timestamps.get(i) : "";
                double alt = altAgl.get(i);
                double gs = groundSpeed.get(i);
                double rpmVal = (rpm != null) ? rpm.get(i) : Double.NaN;
                double airportDistVal = (airportDist != null) ? airportDist.get(i) : Double.NaN;
                String nearestAirportVal = (nearestAirport != null) ? String.valueOf(nearestAirport.get(i)) : "";
                String phaseStr = (phaseData != null) ? phaseData.getPhaseStringAt(i) : "UNKNOWN";
                writer.printf("%d\t%s\t%.1f\t%.1f\t%.0f\t%.1f\t%s\t%s\n",
                    i, timestamp, alt, gs, rpmVal, airportDistVal, nearestAirportVal, phaseStr);
            }
        } catch (Exception e) {
            System.err.println("Error writing debug phases file: " + e.getMessage());
        }
    }

    /**
     * Writes the files for the given event, flight and when
     *
     * @param connection      the database connection
     * @param outputDirectory the output directory
     * @param event           the maintenance record
     * @param flight          the flight
     * @param when            the when string
     * @param fileSplitIndices indices where to split CSV into multiple files (prolonged taxi), empty = single file
     * @throws IOException  if there is an error writing the file
     * @throws SQLException if there is an error with the SQL query
     */
    private static void writeFiles(Connection connection, String outputDirectory,
                                   MaintenanceRecord event, Flight flight, String when,
                                   List<Integer> fileSplitIndices,
                                   FlightPhaseProcessor.FlightPhaseData phaseData,
                                   boolean debugPhases) 
            throws IOException, SQLException {
        assert flight != null;

        // Parse phase (before/during/after) from 'when' string
        String phase;
        if (when.equals("_during")) {
            phase = "during";
        } else if (when.startsWith("_before_")) {
            phase = "before";
        } else if (when.startsWith("_after_")) {
            phase = "after";
        } else {
            phase = "unknown";
        }

        // Create directory structure: <label_id>/<workorder>_<tail>/phase/
        String clusterDir = event.getLabelId();
        String workorderTailDir = event.getWorkorderNumber() + "_" + event.getTailNumber();
        String fullDir = outputDirectory + "/" + clusterDir + "/" + workorderTailDir + "/" + phase;
        
        // Create directories if they don't exist
        File directory = new File(fullDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Generate JSON record file (once per workorder_tail combination)
        File jsonFile = new File(outputDirectory + "/" + clusterDir + "/" + workorderTailDir + "/" + 
                                 workorderTailDir + "_record.json");
        try (FileWriter jsonWriter = new FileWriter(jsonFile)) {
            jsonWriter.write(event.toJSON());
        }

        // Write CSV with flight phase column
        String outfile = fullDir + "/" + flight.getId() + ".csv";

        String zipRoot = NGAFID_ARCHIVE_DIR + "/" + flight.getFleetId() + "/" + flight.getUploaderId() + "/";

        // First, write the original CSV
        try {
            CachedCSVWriter csvWriter = new CachedCSVWriter(zipRoot, flight, Optional.of(new File(outfile + ".tmp")), false);
            csvWriter.writeToFile();
        } catch (java.sql.SQLException e) {
            System.err.println("Warning: SQL error while writing CSV for flight " + flight.getId() + " | Tail: " + flight.getTailNumber() + " | Upload ID: " + flight.getUploadId() + " | Event: " + (event != null ? event.getLabel() : "null") + ": " + e.getMessage());
            return;
        } catch (Exception e) {
            System.err.println("Warning: Unexpected error while writing CSV for flight " + flight.getId() + " | Tail: " + flight.getTailNumber() + " | Upload ID: " + flight.getUploadId() + " | Event: " + (event != null ? event.getLabel() : "null") + ": " + e.getMessage());
            return;
        }

        // phaseData is pre-computed and passed in (required for extraction)

        // Read the temporary CSV and write with FlightPhase column
        // Split into multiple files only for prolonged taxi (30+ sec in middle); not touch-and-go
        if (fileSplitIndices != null && !fileSplitIndices.isEmpty()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(outfile + ".tmp"))) {
                // Read all lines
                List<String> allLines = new ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    allLines.add(line);
                }
                
                // Extract header lines (lines starting with '#' and the column names line)
                // Second # line is data types; append ", enum" so it matches column count of header+FlightPhase
                List<String> headerLines = new ArrayList<>();
                int dataStartIndex = 0;
                int sharpLineCount = 0;
                for (int i = 0; i < allLines.size(); i++) {
                    String currentLine = allLines.get(i);
                    if (currentLine.startsWith("#")) {
                        sharpLineCount++;
                        headerLines.add(sharpLineCount == 2 ? currentLine + ", enum" : currentLine);
                        dataStartIndex = i + 1;
                    } else if (i == dataStartIndex) {
                        headerLines.add(currentLine + ",FlightPhase");
                        dataStartIndex = i + 1;
                        break;
                    }
                }
                
                List<String> dataLines = allLines.subList(dataStartIndex, allLines.size());
                
                // Write segments
                int segmentNumber = 1;
                int startRow = 0;
                
                for (int splitIndex : fileSplitIndices) {
                    // Determine output file name
                    String segmentFile = fullDir + "/" + flight.getId() + "-" + segmentNumber + ".csv";

                    try (PrintWriter writer = new PrintWriter(new FileWriter(segmentFile))) {
                        // Write all header lines
                        for (String headerLine : headerLines) {
                            writer.println(headerLine);
                        }

                        for (int rowIndex = startRow; rowIndex < splitIndex && rowIndex < dataLines.size(); rowIndex++) {
                            String phaseString = "UNKNOWN";
                            if (phaseData != null) {
                                phaseString = phaseData.getPhaseStringAt(rowIndex);
                            }
                            writer.println(dataLines.get(rowIndex) + "," + phaseString);
                        }
                    }

                    // Generate debug phases CSV for this segment if enabled
                    if (debugPhases) {
                        String debugFile = segmentFile.replace(".csv", "_phases.csv");
                        writeDebugPhasesCsv(connection, flight, phaseData, debugFile,
                                "# Flight " + flight.getId() + " Segment " + segmentNumber, startRow, splitIndex);
                    }

                    startRow = splitIndex;
                    segmentNumber++;
                }
                
                // Write final segment
                String finalFile = fullDir + "/" + flight.getId() + "-" + segmentNumber + ".csv";
                try (PrintWriter writer = new PrintWriter(new FileWriter(finalFile))) {
                    // Write all header lines
                    for (String headerLine : headerLines) {
                        writer.println(headerLine);
                    }

                    for (int rowIndex = startRow; rowIndex < dataLines.size(); rowIndex++) {
                        String phaseString = "UNKNOWN";
                        if (phaseData != null) {
                            phaseString = phaseData.getPhaseStringAt(rowIndex);
                        }
                        writer.println(dataLines.get(rowIndex) + "," + phaseString);
                    }
                }

                // Generate debug phases CSV for final segment if enabled
                if (debugPhases) {
                    String debugFile = finalFile.replace(".csv", "_phases.csv");
                    writeDebugPhasesCsv(connection, flight, phaseData, debugFile,
                            "# Flight " + flight.getId() + " Segment " + segmentNumber, startRow, Integer.MAX_VALUE);
                }
            }
        } else {
            // No prolonged taxi split, write single file
            try (BufferedReader reader = new BufferedReader(new FileReader(outfile + ".tmp"));
                 PrintWriter writer = new PrintWriter(new FileWriter(outfile))) {
                
                // Read and write all header lines (metadata # line as-is; data types # line + ", enum"; column names + ",FlightPhase")
                String line = reader.readLine();
                int rowIndex = 0;
                int sharpLineCount = 0;
                while (line != null && line.startsWith("#")) {
                    sharpLineCount++;
                    writer.println(sharpLineCount == 2 ? line + ", enum" : line);
                    line = reader.readLine();
                }
                if (line != null) {
                    writer.println(line + ",FlightPhase");
                    
                    // Write data rows
                    while ((line = reader.readLine()) != null) {
                        String phaseString = "UNKNOWN";
                        if (phaseData != null) {
                            phaseString = phaseData.getPhaseStringAt(rowIndex);
                        }
                        writer.println(line + "," + phaseString);
                        rowIndex++;
                    }
                }
            }
        }

        // Delete temporary file
        new File(outfile + ".tmp").delete();

        // DEBUG PHASES CSV OUTPUT
        if (debugPhases) {
            String debugFile = outfile.replace(".csv", "_phases.csv");
            writeDebugPhasesCsv(connection, flight, phaseData, debugFile,
                    "# Flight " + flight.getId(), 0, Integer.MAX_VALUE);
        }
    }

    /**
     * Exports flights to CSV files. Returns the number of flights actually extracted (written).
     *
     * @param connection      the database connection
     * @param timeline        the list of aircraft timelines
     * @param targetLabelId   the label_id of the cluster to export (e.g. c_1, c_13)
     * @param outputDirectory the output directory
     * @param debugPhases     whether to write debug _phases.csv files
     * @return number of flights written
     * @throws SQLException if there is an error with the SQL query
     * @throws IOException  if there is an error writing the file
     */
    private static int exportFiles(Connection connection, List<AircraftTimeline> timeline, String targetLabelId,
                                    String outputDirectory, boolean debugPhases) throws SQLException, IOException {
        int extractedCount = 0;
        int logCountBefore = 0, logCountDuring = 0, logCountAfter = 0;
        String lastLoggedPhase = null;
        for (int currentAircraft = 0; currentAircraft < timeline.size(); currentAircraft++) {
            AircraftTimeline ac = timeline.get(currentAircraft);

            // Extract flights if:
            // 1. During: flight between open and close inclusive (daysSincePrevious >= 0 AND daysToNext >= 0)
            // 2. Before: one of 3 flights in [openDate-10, openDate) (flightsToNext 0-2)
            // 3. After: one of 3 flights in (closeDate, closeDate+10] (flightsSincePrevious 0-2), unless nearby maintenance
            boolean isDuringMaintenance = ac.getDaysSincePrevious() >= 0 && ac.getDaysToNext() >= 0;
            boolean isBeforeMaintenance = ac.getFlightsToNext() >= 0 && ac.getFlightsToNext() <= 2;
            boolean isAfterMaintenance = ac.getFlightsSincePrevious() >= 0 && ac.getFlightsSincePrevious() <= 2;
            
            // Check if there's another maintenance event within 10 days after current event
            boolean hasNearbyNextMaintenance = false;
            if (isAfterMaintenance && ac.getPreviousEvent() != null) {
                // Look ahead to see if there's another maintenance event soon
                for (int i = currentAircraft + 1; i < timeline.size(); i++) {
                    AircraftTimeline futureAc = timeline.get(i);
                    if (futureAc.getDaysSincePrevious() == 0 && futureAc.getPreviousEvent() != ac.getPreviousEvent()) {
                        // Found another maintenance event
                        long daysBetween = ChronoUnit.DAYS.between(ac.getPreviousEvent().getCloseDate(), 
                                                                   futureAc.getPreviousEvent().getOpenDate());
                        if (daysBetween <= 10) {
                            hasNearbyNextMaintenance = true;
                            break;
                        }
                    }
                }
            }

            if (isDuringMaintenance || isBeforeMaintenance || (isAfterMaintenance && !hasNearbyNextMaintenance)) {

                Flight flight = Flight.getFlight(connection, ac.getFlightId());

                // 1. Obtain AltAGL (DB or fallback: AltMSL/AltB + terrain)
                AltAGLResult altAGLResult = getAltAGLForExtraction(connection, flight.getId(), Integer.MAX_VALUE);
                if (altAGLResult.values == null || altAGLResult.values.length == 0) {
                    System.err.println("Skipping flight " + flight.getId() + ": could not obtain AltAGL (" + altAGLResult.failureReason + ")");
                    continue;
                }
                double[] altAGLValues = altAGLResult.values;

                // 2. Validate: must have left ground (max AGL > 10 ft)
                FlightPhaseProcessor.FlightValidationResult validation =
                        FlightPhaseProcessor.validateAndDetectTouchAndGo(altAGLValues);
                if (!validation.isValid) {
                    System.err.println("Skipping flight " + flight.getId() + ": never left ground (max AGL <= 10 ft)");
                    continue;
                }

                // 3. Must reach cruise altitude (600 ft AGL)
                double maxAGL = Double.NEGATIVE_INFINITY;
                for (double v : altAGLValues) {
                    if (!Double.isNaN(v) && v > maxAGL) maxAGL = v;
                }
                if (maxAGL < CRUISE_ALTITUDE_AGL_FT) {
                    System.err.println("Skipping flight " + flight.getId() + ": never reached cruise (max AGL " + String.format("%.0f", maxAGL) + " ft < " + (int) CRUISE_ALTITUDE_AGL_FT + " ft)");
                    continue;
                }

                // 4. Compute flight phases (required for extraction)
                FlightPhaseProcessor.FlightPhaseData phaseData;
                try {
                    phaseData = FlightPhaseProcessor.computeCompleteFlightPhasesFromAltAGLArray(
                            connection, flight.getId(), altAGLValues, validation);
                } catch (Exception e) {
                    System.err.println("Skipping flight " + flight.getId() + ": could not compute flight phases (" + e.getMessage() + ")");
                    continue;
                }

                // 5. File splitting: prolonged taxi (30+ sec in middle)
                List<Integer> fileSplitIndices = new ArrayList<>();
                try {
                    fileSplitIndices = FlightPhaseProcessor.detectProlongedTaxiSplits(connection, flight.getId());
                } catch (Exception ignored) { }

                // Only split if every resulting segment reaches cruise (max AGL >= 600 ft)
                if (!fileSplitIndices.isEmpty() && altAGLValues != null) {
                    List<Integer> sortedSplits = new ArrayList<>(fileSplitIndices);
                    Collections.sort(sortedSplits);
                    int n = altAGLValues.length;
                    int start = 0;
                    boolean allReachCruise = true;
                    for (int split : sortedSplits) {
                        double segMax = Double.NEGATIVE_INFINITY;
                        for (int i = start; i < split && i < n; i++) {
                            if (!Double.isNaN(altAGLValues[i]) && altAGLValues[i] > segMax) segMax = altAGLValues[i];
                        }
                        if (segMax < CRUISE_ALTITUDE_AGL_FT) {
                            allReachCruise = false;
                            break;
                        }
                        start = split;
                    }
                    if (allReachCruise && start < n) {
                        double segMax = Double.NEGATIVE_INFINITY;
                        for (int i = start; i < n; i++) {
                            if (!Double.isNaN(altAGLValues[i]) && altAGLValues[i] > segMax) segMax = altAGLValues[i];
                        }
                        if (segMax < CRUISE_ALTITUDE_AGL_FT) allReachCruise = false;
                    }
                    if (!allReachCruise) fileSplitIndices = new ArrayList<>();
                }

                String when = null;

                MaintenanceRecord event = null;
                if (isDuringMaintenance) {
                    // Flight is between open and close (maintenance period)
                    event = ac.getPreviousEvent();
                    when = "_during";
                } else if (isAfterMaintenance && !hasNearbyNextMaintenance) {
                    event = ac.getPreviousEvent();
                    when = "_after_" + ac.getFlightsSincePrevious();
                } else if (isBeforeMaintenance) {
                    event = ac.getNextEvent();
                    when = "_before_" + ac.getFlightsToNext();
                }

                if (event == null) {
                    System.err.println("ERROR: event is null! currentAircraft: " + currentAircraft +
                            ", timeline.size(): " + timeline.size());
                    continue;
                }

                if (!targetLabelId.equals(event.getLabelId())) {
                    continue;
                }

                extractedCount++;
                if (when != null) {
                    if (when.startsWith("_before_")) {
                        extractedBeforeTotal++;
                    } else if ("_during".equals(when)) {
                        extractedDuringTotal++;
                        if (event.getOpenDate().equals(event.getCloseDate())) {
                            extractedDuringSingleDayTotal++;
                        }
                    } else if (when.startsWith("_after_")) {
                        extractedAfterTotal++;
                    }
                }

                // Time log: only flights that pass all filters (AGL, cruise, phases)
                String phaseForLog = when.startsWith("_before_") ? "before" : ("_during".equals(when) ? "during" : "after");
                if (phaseForLog.equals("before")) logCountBefore++;
                else if (phaseForLog.equals("during")) logCountDuring++;
                else logCountAfter++;
                if (lastLoggedPhase != null && !phaseForLog.equals(lastLoggedPhase)) {
                    timeLog("");
                }
                if (lastLoggedPhase == null || !phaseForLog.equals(lastLoggedPhase)) {
                    timeLog("[TIME]   --- " + phaseForLog + " ---");
                    lastLoggedPhase = phaseForLog;
                }
                LocalDate flightDateGmt = ac.getStartTime();
                timeLog("[TIME]     flightId=" + ac.getFlightId() + "  flightDate(GMT)=" + flightDateGmt
                        + "  start(GMT)=" + ac.getStartDateTimeUtc() + "  end(GMT)=" + ac.getEndDateTimeUtc() + "  -> " + phaseForLog);

                writeFiles(connection, outputDirectory, event, flight, when, fileSplitIndices, phaseData, debugPhases);
            }
        }
        timeLog("");
        timeLog("[TIME]   Counts:  before=" + logCountBefore + "  during=" + logCountDuring + "  after=" + logCountAfter);
        return extractedCount;
    }

    // -------------------------------------------------------------------------
    // Manifest: manifest.json with workorders and flight paths
    // -------------------------------------------------------------------------

    /**
     * Generates a manifest JSON file containing information about all extracted flights
     *
     * @param outputDirectory the base directory where extracted flights are stored
     */
    private static void generateManifest(String outputDirectory) {
        try {
            
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"generated_at\": \"").append(LocalDateTime.now().toString()).append("\",\n");
            
            // Calculate statistics
            int totalFlights = 0;
            HashMap<String, Integer> clusterFlightCounts = new HashMap<>();
            HashMap<String, String> clusterNames = new HashMap<>();
            
            // Build label name map from records (label_id -> label)
            for (MaintenanceRecord record : ALL_RECORDS) {
                if (record.getLabelId() != null && record.getLabel() != null) {
                    clusterNames.put(record.getLabelId(), record.getLabel());
                }
            }
            
            json.append("  \"statistics\": {\n");
            json.append("    \"total_workorders\": ").append(RECORDS_BY_WORKORDER.size()).append(",\n");
            
            // Count flights per cluster by scanning directories
            int totalBefore = 0;
            int totalDuring = 0;
            int totalDuringSameDay = 0;
            int totalAfter = 0;
            File baseDir = new File(outputDirectory);
            if (baseDir.exists() && baseDir.isDirectory()) {
                for (File clusterDir : baseDir.listFiles()) {
                    if (clusterDir.isDirectory() && clusterDir.getName().startsWith("c_")) {
                        int clusterCount = 0;
                        for (File workorderDir : clusterDir.listFiles()) {
                            if (workorderDir.isDirectory()) {
                                // Count CSV files in before/during/after subdirectories
                                for (String phase : new String[]{"before", "during", "after"}) {
                                    File phaseDir = new File(workorderDir, phase);
                                    if (phaseDir.exists() && phaseDir.isDirectory()) {
                                        File[] csvFiles = phaseDir.listFiles((dir, name) -> name.endsWith(".csv"));
                                        if (csvFiles != null) {
                                            clusterCount += csvFiles.length;
                                            totalFlights += csvFiles.length;
                                            if ("before".equals(phase)) {
                                                totalBefore += csvFiles.length;
                                            } else if ("during".equals(phase)) {
                                                totalDuring += csvFiles.length;
                                                // Count during flights for records where open_date == close_date
                                                try {
                                                    int woNum = Integer.parseInt(workorderDir.getName().split("_")[0]);
                                                    MaintenanceRecord rec = RECORDS_BY_WORKORDER.get(woNum);
                                                    if (rec != null && rec.getOpenDate().equals(rec.getCloseDate())) {
                                                        totalDuringSameDay += csvFiles.length;
                                                    }
                                                } catch (NumberFormatException ignored) { }
                                            } else if ("after".equals(phase)) {
                                                totalAfter += csvFiles.length;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (clusterCount > 0) {
                            clusterFlightCounts.put(clusterDir.getName(), clusterCount);
                        }
                    }
                }
            }
            
            json.append("    \"total_flights\": ").append(totalFlights).append(",\n");
            json.append("    \"total_before\": ").append(totalBefore).append(",\n");
            json.append("    \"total_during\": { \"all\": ").append(totalDuring).append(", \"same_day\": ").append(totalDuringSameDay).append(" },\n");
            json.append("    \"total_after\": ").append(totalAfter).append(",\n");
            json.append("    \"by_cluster\": {\n");
            
            int clusterIndex = 0;
            for (Map.Entry<String, Integer> entry : clusterFlightCounts.entrySet()) {
                String clusterId = entry.getKey();
                String clusterName = clusterNames.getOrDefault(clusterId, "Unknown");
                json.append("      \"").append(clusterId).append("\": {\n");
                json.append("        \"name\": \"").append(escapeJson(clusterName)).append("\",\n");
                json.append("        \"count\": ").append(entry.getValue()).append("\n");
                json.append("      }");
                if (clusterIndex < clusterFlightCounts.size() - 1) {
                    json.append(",");
                }
                json.append("\n");
                clusterIndex++;
            }
            
            json.append("    }\n");
            json.append("  },\n");
            
            // Single-day during paths: flights that occurred on same-day maintenance (open==close), grouped by label_id for manual review
            Map<String, java.util.List<String>> singleDayDuringByLabel = new LinkedHashMap<>();
            for (String cid : clusterNames.keySet()) {
                singleDayDuringByLabel.put(cid, new java.util.ArrayList<>());
            }
            for (MaintenanceRecord record : ALL_RECORDS) {
                if (!record.getOpenDate().equals(record.getCloseDate())) continue;
                String labelId = record.getLabelId();
                String workorderTail = record.getWorkorderNumber() + "_" + record.getTailNumber();
                File workorderDir = new File(outputDirectory, labelId + "/" + workorderTail);
                java.util.List<String> duringPaths = new java.util.ArrayList<>();
                collectPhasePaths(labelId, workorderTail, workorderDir, "during", duringPaths);
                singleDayDuringByLabel.get(labelId).addAll(duringPaths);
            }
            json.append("  \"single_day_during_paths\": {\n");
            int labelIdx = 0;
            for (Map.Entry<String, java.util.List<String>> e : singleDayDuringByLabel.entrySet()) {
                String labelId = e.getKey();
                String labelName = clusterNames.getOrDefault(labelId, "Unknown");
                json.append("    \"").append(labelId).append("\": {\n");
                json.append("      \"name\": \"").append(escapeJson(labelName)).append("\",\n");
                json.append("      \"paths\": ").append(pathListToJson(e.getValue())).append("\n");
                json.append("    }");
                if (labelIdx < singleDayDuringByLabel.size() - 1) json.append(",");
                json.append("\n");
                labelIdx++;
            }
            json.append("  },\n");
            
            // Generate workorders array (only workorders that have at least one extracted flight)
            json.append("  \"workorders\": [\n");
            
            boolean firstWorkorder = true;
            for (MaintenanceRecord record : ALL_RECORDS) {
                String labelId = record.getLabelId();
                String workorderTail = record.getWorkorderNumber() + "_" + record.getTailNumber();
                File workorderDir = new File(outputDirectory, labelId + "/" + workorderTail);

                java.util.List<String> beforePaths = new java.util.ArrayList<>();
                java.util.List<String> duringPaths = new java.util.ArrayList<>();
                java.util.List<String> afterPaths = new java.util.ArrayList<>();
                collectPhasePaths(labelId, workorderTail, workorderDir, "before", beforePaths);
                collectPhasePaths(labelId, workorderTail, workorderDir, "during", duringPaths);
                collectPhasePaths(labelId, workorderTail, workorderDir, "after", afterPaths);
                int totalFlightsForWorkorder = beforePaths.size() + duringPaths.size() + afterPaths.size();

                if (totalFlightsForWorkorder == 0) {
                    continue; // Skip workorders with no extracted flights; no manifest entry, no record_json
                }

                if (!firstWorkorder) {
                    json.append(",\n");
                }
                firstWorkorder = false;

                json.append("    {\n");
                json.append("      \"workorder\": ").append(record.getWorkorderNumber()).append(",\n");
                json.append("      \"label_id\": \"").append(record.getLabelId()).append("\",\n");
                json.append("      \"label\": \"").append(escapeJson(record.getLabel())).append("\",\n");
                json.append("      \"tail_number\": \"").append(record.getTailNumber()).append("\",\n");
                json.append("      \"airframe\": \"").append(record.getAirframe()).append("\",\n");
                json.append("      \"open_date\": \"").append(record.getOpenDate().toString()).append("\",\n");
                json.append("      \"close_date\": \"").append(record.getCloseDate().toString()).append("\",\n");
                json.append("      \"open_date_time\": \"").append(record.getOpenDateTime().toString()).append("\",\n");
                json.append("      \"close_date_time\": \"").append(record.getCloseDateTime().toString()).append("\",\n");
                json.append("      \"original_action\": \"").append(escapeJson(record.getOriginalAction())).append("\",\n");
                json.append("      \"flights\": {\n");
                json.append("        \"before\": ").append(pathListToJson(beforePaths)).append(",\n");
                json.append("        \"during\": ").append(pathListToJson(duringPaths)).append(",\n");
                json.append("        \"after\": ").append(pathListToJson(afterPaths)).append("\n");
                json.append("      },\n");
                String recordJsonPath = labelId + "/" + workorderTail + "/" + workorderTail + "_record.json";
                json.append("      \"record_json\": \"").append(recordJsonPath).append("\"\n");
                json.append("    }");
            }
            
            json.append("\n  ]\n");
            json.append("}\n");
            
            // Write manifest file to data/maintenance/manifest.json
            File manifestDir = new File(outputDirectory).getParentFile();
            File manifestFile = new File(manifestDir, "manifest.json");
            
            try (FileWriter writer = new FileWriter(manifestFile)) {
                writer.write(json.toString());
            }
            
            System.out.println("Manifest: " + manifestFile.getName() + " (" + RECORDS_BY_WORKORDER.size() + " workorders, " + totalFlights + " flights)");
            
        } catch (IOException e) {
            System.err.println("Error generating manifest: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Escapes special characters in JSON strings
     */
    private static String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    /**
     * Collects sorted CSV paths for one phase into the given list (used for manifest).
     */
    private static void collectPhasePaths(String labelId, String workorderTail,
            File workorderDir, String phase, java.util.List<String> outPaths) {
        File phaseDir = new File(workorderDir, phase);
        if (!phaseDir.exists() || !phaseDir.isDirectory()) return;
        File[] csvFiles = phaseDir.listFiles((dir, name) -> name.endsWith(".csv"));
        if (csvFiles == null) return;
        java.util.Arrays.sort(csvFiles, (f1, f2) -> {
            String n1 = f1.getName();
            String n2 = f2.getName();
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d+)(?:-(\\d+))?.*\\.csv");
            java.util.regex.Matcher m1 = p.matcher(n1);
            java.util.regex.Matcher m2 = p.matcher(n2);
            int base1 = 0, base2 = 0, suffix1 = 0, suffix2 = 0;
            if (m1.matches()) {
                base1 = Integer.parseInt(m1.group(1));
                suffix1 = m1.group(2) != null ? Integer.parseInt(m1.group(2)) : 0;
            }
            if (m2.matches()) {
                base2 = Integer.parseInt(m2.group(1));
                suffix2 = m2.group(2) != null ? Integer.parseInt(m2.group(2)) : 0;
            }
            if (base1 != base2) return Integer.compare(base1, base2);
            return Integer.compare(suffix1, suffix2);
        });
        java.util.regex.Pattern baseRegex = java.util.regex.Pattern.compile("^(\\d+)(\\.csv)$");
        java.util.regex.Pattern derivedRegex = java.util.regex.Pattern.compile("^(\\d+)-(\\d+)(\\.csv)$");
        for (int i = 0; i < csvFiles.length; i++) {
            String fileName = csvFiles[i].getName();
            java.util.regex.Matcher mBase = baseRegex.matcher(fileName);
            String baseNum = null;
            boolean hasDerived = false;
            if (mBase.matches()) {
                baseNum = mBase.group(1);
                for (int j = 0; j < csvFiles.length; j++) {
                    if (j == i) continue;
                    java.util.regex.Matcher mDerived = derivedRegex.matcher(csvFiles[j].getName());
                    if (mDerived.matches() && mDerived.group(1).equals(baseNum)) {
                        hasDerived = true;
                        break;
                    }
                }
            }
            String relativePath = (mBase.matches() && hasDerived)
                    ? (labelId + "/" + workorderTail + "/" + phase + "/" + baseNum + "-1.csv")
                    : (labelId + "/" + workorderTail + "/" + phase + "/" + fileName);
            outPaths.add(relativePath);
        }
    }

    /** Returns JSON array of quoted path strings, e.g. ["a/b/c.csv"]. */
    private static String pathListToJson(java.util.List<String> paths) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < paths.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(escapeJson(paths.get(i))).append("\"");
        }
        return sb.append("]").toString();
    }

    /**
     * Main method for the ExtractMaintenanceFlights class
     *
     * @param arguments the command line arguments
     * @throws SQLException if there is an error with the SQL query
     */
    public static void main(String[] arguments) throws SQLException {
        Connection connection = Database.getConnection();

        // Run only validation: --validate <input_csv>
        // Writes valid-maintenance.csv and invalid-maintenance.csv in the same directory as input.
        if (arguments.length >= 2 && "--validate".equals(arguments[0])) {
            String inputCsv = arguments[1];
            try {
                validateMaintenanceRecords(connection, inputCsv);
            } catch (IOException e) {
                System.err.println("Validation failed: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
            return;
        }

        // Verify validation results: --validate-verify <input_csv>
        // Expects valid-{basename}.csv in same directory as input.
        if (arguments.length >= 2 && "--validate-verify".equals(arguments[0])) {
            String inputCsv = arguments[1];
            File inputFile = new File(inputCsv);
            String dir = inputFile.getParent();
            String baseName = inputFile.getName();
            String outputCsv = (dir != null ? dir + File.separator : "") + "valid-" + baseName;
            try {
                boolean ok = verifyValidationResults(connection, inputCsv, outputCsv);
                System.exit(ok ? 0 : 1);
            } catch (IOException e) {
                System.err.println("Verify failed: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
            return;
        }

        boolean debugPhases = false;
        List<String> csvFileList = new ArrayList<>();
        for (int i = 1; i < arguments.length; i++) {
            if ("--phases".equals(arguments[i])) {
                debugPhases = true;
            } else {
                csvFileList.add(arguments[i]);
            }
        }
        String[] csvFiles = csvFileList.toArray(new String[0]);
        String outputDirectory = arguments[0];

        File outDir = new File(outputDirectory);
        if (!outDir.exists()) {
            if (!outDir.mkdirs()) {
                System.err.println("Could not create output directory: " + outputDirectory);
                System.exit(1);
            }
        }
        System.out.println("Output directory: " + outDir.getAbsolutePath());

        System.out.println("CSV files:");
        for (String f : csvFiles) {
            System.out.println("\t'" + f + "'");
        }

        readClusterFiles(csvFiles);

        System.out.println("unique airframes: ");
        System.out.println("\t" + AIRFRAMES);
        System.out.println("Processing " + ALL_RECORDS.size() + " workorder(s)");

        try {
            // Write flight-sort log next to the CSV input (e.g. data/maintenance/) so it appears with extract.log
            File logDir = csvFiles.length > 0
                    ? new File(csvFiles[0]).getAbsoluteFile().getParentFile()
                    : new File(System.getProperty("user.dir"));
            File logFile = new File(logDir, "maintenance_flight_sort_log.txt");
            try {
                timeLogWriter = new PrintWriter(new FileWriter(logFile));
                timeLogWriter.println("Maintenance extraction time log");
                timeLogWriter.println("All dates in GMT (maintenance record and flight dates).");
                timeLogWriter.println();
            } catch (IOException e) {
                System.err.println("Could not create time log file: " + logFile.getAbsolutePath() + " - " + e.getMessage());
                timeLogWriter = null;
            }

            boolean firstWorkorder = true;
            for (MaintenanceRecord record : ALL_RECORDS) {
                if (!firstWorkorder) {
                    timeLog("");
                }
                firstWorkorder = false;

                LocalDate startDate = record.getOpenDate().minusDays(10);
                LocalDate endDate = record.getCloseDate().plusDays(10);
                String tailNumber = record.getTailNumber();
                String labelId = record.getLabelId();

                // Directories and record JSON are created in writeFiles() when the first CSV is written.
                // Do not create them here so workorders with no extracted flights leave no empty structure.

                PreparedStatement tailStmt =
                        connection.prepareStatement("SELECT system_id, fleet_id FROM tails WHERE tail = ?");
                tailStmt.setString(1, tailNumber);

                ResultSet tailSet = tailStmt.executeQuery();
                List<AircraftTimeline> timeline = buildTimeline(connection, tailSet, startDate, endDate);
                tailSet.close();
                tailStmt.close();

                Collections.sort(timeline);

                List<MaintenanceRecord> tailRecords = Collections.singletonList(record);
                assignFlightsToPhases(timeline, tailRecords);
                setFlightsToNextFlights(connection, timeline);

                int extracted = exportFiles(connection, timeline, labelId, outputDirectory, debugPhases);
                extractedFlightsTotal += extracted;

                System.out.println("  WO " + record.getWorkorderNumber() + " " + tailNumber + " [" + labelId + "] " +
                        startDate + " to " + endDate + " -> " + extracted + " flight(s)");
            }

            if (timeLogWriter != null) {
                timeLogWriter.close();
                timeLogWriter = null;
                System.out.println("Time log: " + logFile.getAbsolutePath());
            }

            System.out.println("Total: " + extractedFlightsTotal + " flights extracted (before: " + extractedBeforeTotal
                    + ", during: " + extractedDuringTotal + ", during single-day: " + extractedDuringSingleDayTotal
                    + ", after: " + extractedAfterTotal + ")");
        } catch (SQLException e) {
            System.err.println("SQLException: " + e);
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("IOException: " + e);
            e.printStackTrace();
        }

        // Generate manifest file after all clusters are processed
        try {
            generateManifest(outputDirectory);
        } catch (Exception e) {
            System.err.println("Error during manifest generation: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
