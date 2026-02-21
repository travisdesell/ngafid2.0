package org.ngafid.core.bin;

import org.ngafid.core.Database;
import org.ngafid.core.flights.DoubleTimeSeries;
import org.ngafid.core.flights.Flight;
import org.ngafid.core.flights.Parameters;
import org.ngafid.core.flights.StringTimeSeries;
import org.ngafid.core.flights.export.CachedCSVWriter;
import org.ngafid.core.flights.maintenance.AircraftTimeline;
import org.ngafid.core.flights.maintenance.MaintenanceRecord;
import org.ngafid.core.util.TimeUtils;

import java.io.*;
import java.sql.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import static org.ngafid.core.Config.NGAFID_ARCHIVE_DIR;

public final class ExtractMaintenanceFlights {
    private static final HashMap<Integer, MaintenanceRecord> RECORDS_BY_WORKORDER = new HashMap<>();
    private static final HashMap<String, ArrayList<MaintenanceRecord>> RECORDS_BY_LABEL = new HashMap<>();
    private static final HashMap<String, ArrayList<MaintenanceRecord>> RECORDS_BY_TAIL_NUMBER = new HashMap<>();
    private static final TreeSet<MaintenanceRecord> ALL_RECORDS = new TreeSet<>();
    private static final TreeSet<String> TAIL_NUMBERS = new TreeSet<>();
    private static final TreeSet<String> AIRFRAMES = new TreeSet<>();
    private static final HashMap<String, String> CLUSTER_TO_LABEL = new HashMap<>();
    private static final HashMap<String, String> LABEL_TO_CLUSTER = new HashMap<>();
    private static int systemIdCount = 0;
    private static int extractedFlightsTotal = 0;

    private ExtractMaintenanceFlights() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Reads the cluster files and populates the recordsByWorkorder, recordsByLabel,
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
                
                // Skip header row if this is the first line (starts with "workorder")
                if (line != null && line.startsWith("workorder")) {
                    line = reader.readLine();
                }

                while (line != null) {
                    // Skip blank lines and header rows
                    if (line.trim().isEmpty()) {
                        line = reader.readLine();
                        continue;
                    }
                    if (line.startsWith("workorder")) {
                        line = reader.readLine();
                        continue;
                    }

                    lineCount++;
                    MaintenanceRecord record = new MaintenanceRecord(line);

                    // Each record's originalAction becomes its own cluster name
                    String clusterName = record.getOriginalAction();

                    // Add cluster mapping for this record
                    if (!CLUSTER_TO_LABEL.containsKey(clusterName)) {
                        CLUSTER_TO_LABEL.put(clusterName, record.getLabel());
                        LABEL_TO_CLUSTER.put(record.getLabel(), clusterName);
                    }

                    MaintenanceRecord existingRecord = RECORDS_BY_WORKORDER.get(record.getWorkorderNumber());
                    if (existingRecord == null) {
                        // this is a record we have not yet seen before
                        RECORDS_BY_WORKORDER.put(record.getWorkorderNumber(), record);

                        if ((record.getAirframe().equals("C172") || record.getAirframe().equals("ARCH") ||
                                record.getAirframe().equals("SEMI"))) {
                            // only add tail numbers from C172, PA28 or PA44
                            TAIL_NUMBERS.add(record.getTailNumber());
                        }

                        AIRFRAMES.add(record.getAirframe());

                        // add it to the list of records by tails
                        ArrayList<MaintenanceRecord> tailSet =
                                RECORDS_BY_TAIL_NUMBER.computeIfAbsent(record.getTailNumber(), k -> new ArrayList<>());
                        tailSet.add(record);

                        // add it to the list of records by labels
                        ArrayList<MaintenanceRecord> labelList =
                                RECORDS_BY_LABEL.computeIfAbsent(record.getLabel(), k -> new ArrayList<>());
                        labelList.add(record);

                        ALL_RECORDS.add(record);
                    } else {
                        existingRecord.combine(record);
                    }

                    line = reader.readLine();
                }

                reader.close();
            } catch (IOException e) {
                System.err.println("Could not read cluster file: '" + allCluster + "'");
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

    /**
     * Counts flights in the database that overlap the extended maintenance window:
     * 10 days before open date through 10 days after close date (matches extraction window).
     *
     * @param connection the database connection
     * @param tailNumber  the tail number
     * @param openDate    maintenance open date
     * @param closeDate   maintenance close date
     * @return number of flights in the extended window, or 0 if tail not found
     * @throws SQLException if there is an error with the SQL query
     */
    private static int countFlightsInMaintenancePeriod(Connection connection, String tailNumber,
                                                       LocalDate openDate, LocalDate closeDate)
            throws SQLException {
        LocalDate windowStart = openDate.minusDays(10);
        LocalDate windowEnd = closeDate.plusDays(10);
        String windowEndOfDay = windowEnd.toString() + " 23:59:59";
        PreparedStatement stmt = connection.prepareStatement(
                "SELECT COUNT(*) FROM flights f " +
                "JOIN tails t ON f.system_id = t.system_id " +
                "WHERE t.tail = ? AND f.start_time <= ? AND f.end_time >= ?");
        stmt.setString(1, tailNumber);
        stmt.setString(2, windowEndOfDay);
        stmt.setString(3, windowStart.toString());
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
            if (line.trim().isEmpty()) {
                line = reader.readLine();
            }
            if (line != null && line.startsWith("workorder")) {
                validWriter.println(line);
                invalidWriter.println(line);
                line = reader.readLine();
            }

            while (line != null) {
                if (line.trim().isEmpty()) {
                    line = reader.readLine();
                    continue;
                }
                if (line.startsWith("workorder")) {
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
            while (line != null && line.startsWith("workorder")) {
                line = reader.readLine();
            }
            while (line != null) {
                if (!line.trim().isEmpty() && !line.startsWith("workorder")) {
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
            while (line != null && (line.trim().isEmpty() || line.startsWith("workorder"))) {
                line = reader.readLine();
            }
            while (line != null) {
                if (line.trim().isEmpty() || line.startsWith("workorder")) {
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

    /** Returns the set of tail numbers present in the given records. */
    private static Set<String> getTailNumbersForRecords(List<MaintenanceRecord> targetRecords) {
        Set<String> tails = new HashSet<>();
        for (MaintenanceRecord record : targetRecords) {
            tails.add(record.getTailNumber());
        }
        return tails;
    }

    /**
     * Populates the timeline list with the flights for the given tail set
     *
     * @param connection the database connection
     * @param tailSet    the set of tails
     * @param startDate  the start date
     * @param endDate    the end date
     * @return the list of aircraft timelines
     * @throws SQLException if there is an error with the SQL query
     */
    private static List<AircraftTimeline> buildTimeline(Connection connection, ResultSet tailSet, LocalDate startDate,
                                                        LocalDate endDate) throws SQLException, TimeUtils.UnrecognizedDateTimeFormatException {
        List<AircraftTimeline> timeline = new ArrayList<>();

        while (tailSet.next()) {
            String systemId = tailSet.getString(1);

            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT id, start_time, end_time, airframe_id FROM flights " +
                            "WHERE system_id = ? AND start_time > ? AND end_time < ? ORDER BY start_time");
            stmt.setString(1, systemId);
            stmt.setString(2, startDate.toString());
            stmt.setString(3, endDate.toString());

            ResultSet resultSet = stmt.executeQuery();
            while (resultSet.next()) {
                int flightId = resultSet.getInt(1);
                String flightStartTime = resultSet.getString(2);
                String flightEndTime = resultSet.getString(3);

                // convert the start time (which is in GMT) to CST
                flightStartTime = TimeUtils.toString(TimeUtils.convertToOffset(flightStartTime, "+00:00", "-06:00"));
                flightEndTime = TimeUtils.toString(TimeUtils.convertToOffset(flightEndTime, "+00:00", "-06:00"));

                timeline.add(new AircraftTimeline(flightId, flightStartTime, flightEndTime));
            }

            systemIdCount++;
            resultSet.close();
            stmt.close();
        }

        return timeline;
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

    /**
     * Assigns each flight to a maintenance phase (before/during/after) based on open and close dates.
     * Maintenance can span multiple days, so the full open-close range is treated as during.
     * <ul>
     *   <li><b>before</b>: flight date in [openDate-10, openDate)</li>
     *   <li><b>during</b>: flight date between open and close inclusive [openDate, closeDate]</li>
     *   <li><b>after</b>: flight date in (closeDate, closeDate+10]</li>
     * </ul>
     *
     * @param timeline    sorted list of aircraft timelines (flights)
     * @param tailRecords sorted list of maintenance records for this tail in the cluster
     */
    private static void assignFlightsToPhases(List<AircraftTimeline> timeline,
                                              List<MaintenanceRecord> tailRecords) {
        for (AircraftTimeline ac : timeline) {
            LocalDate flightDate = ac.getStartTime();
            MaintenanceRecord duringRecord = null;
            MaintenanceRecord beforeRecord = null;
            MaintenanceRecord afterRecord = null;

            for (MaintenanceRecord r : tailRecords) {
                LocalDate openMinus10 = r.getOpenDate().minusDays(10);
                LocalDate closePlus10 = r.getCloseDate().plusDays(10);

                if (!flightDate.isBefore(r.getOpenDate()) && !flightDate.isAfter(r.getCloseDate())) {
                    // flightDate in [open, close] - during maintenance (can span multiple days)
                    if (duringRecord == null || r.getOpenDate().isBefore(duringRecord.getOpenDate())) {
                        duringRecord = r; // pick earliest matching if overlap
                    }
                } else if (!flightDate.isBefore(openMinus10) && flightDate.isBefore(r.getOpenDate())) {
                    // flightDate in [open-10, open); pick record with smallest openDate (closest)
                    if (beforeRecord == null || r.getOpenDate().isBefore(beforeRecord.getOpenDate())) {
                        beforeRecord = r;
                    }
                } else if (flightDate.isAfter(r.getCloseDate()) && !flightDate.isAfter(closePlus10)) {
                    // flightDate in (close, close+10]; pick record with largest closeDate (closest)
                    if (afterRecord == null || r.getCloseDate().isAfter(afterRecord.getCloseDate())) {
                        afterRecord = r;
                    }
                }
            }

            // DURING takes precedence; otherwise BEFORE or AFTER
            if (duringRecord != null) {
                ac.setPreviousEvent(duringRecord, 0);
                ac.setNextEvent(duringRecord, 0);
            } else if (beforeRecord != null) {
                long daysToNext = ChronoUnit.DAYS.between(flightDate, beforeRecord.getOpenDate());
                ac.setPreviousEvent(null, -1);
                ac.setNextEvent(beforeRecord, daysToNext);
            } else if (afterRecord != null) {
                long daysSincePrev = ChronoUnit.DAYS.between(afterRecord.getCloseDate(), flightDate);
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
                        a.setFlightsToNext(-1); // -1 means during (flight between open and close)
                        i++;
                        continue;
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
                        a.setFlightsSincePrevious(-1); // -1 means during (flight between open and close)
                        i++;
                        continue;
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

    /**
     * Writes the files for the given event, flight and when
     *
     * @param connection      the database connection
     * @param outputDirectory the output directory
     * @param event           the maintenance record
     * @param flight          the flight
     * @param when            the when string
     * @param validation      the flight validation result (for phase marking)
     * @param fileSplitIndices indices where to split CSV into multiple files (prolonged taxi), empty = single file
     * @throws IOException  if there is an error writing the file
     * @throws SQLException if there is an error with the SQL query
     */
    private static void writeFiles(Connection connection, String outputDirectory,
                                   MaintenanceRecord event, Flight flight, String when,
                                   FlightPhaseProcessor.FlightValidationResult validation,
                                   List<Integer> fileSplitIndices,
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
        if (!jsonFile.exists()) {
            try (FileWriter jsonWriter = new FileWriter(jsonFile)) {
                jsonWriter.write(event.toJSON());
            }
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

        // Compute complete flight phases (includes touch-and-go/go-around marking and altitude smoothing)
        FlightPhaseProcessor.FlightPhaseData phaseData = null;
        try {
            phaseData = FlightPhaseProcessor.computeCompleteFlightPhases(connection, flight, validation);
        } catch (Exception e) {
            System.err.println("Warning: Could not compute flight phases for flight " + flight.getId() + ": " + e.getMessage());
        }

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
                List<String> headerLines = new ArrayList<>();
                int dataStartIndex = 0;
                
                for (int i = 0; i < allLines.size(); i++) {
                    String currentLine = allLines.get(i);
                    if (currentLine.startsWith("#")) {
                        // Metadata header line - append ,FlightPhase
                        headerLines.add(currentLine + ",FlightPhase");
                        dataStartIndex = i + 1;
                    } else if (i == dataStartIndex) {
                        // This is the column names line (first non-# line after # lines)
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
                        try (PrintWriter writer = new PrintWriter(new FileWriter(debugFile))) {
                            writer.println("# Flight " + flight.getId() + " Segment " + segmentNumber);
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
                            for (int i = startRow; i < splitIndex && i < altAgl.size(); i++) {
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
                            System.err.println("Error writing debug phases file for segment: " + e.getMessage());
                        }
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
                    try (PrintWriter writer = new PrintWriter(new FileWriter(debugFile))) {
                        writer.println("# Flight " + flight.getId() + " Segment " + segmentNumber);
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
                        for (int i = startRow; i < altAgl.size(); i++) {
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
                        System.err.println("Error writing debug phases file for final segment: " + e.getMessage());
                    }
                }
            }
        } else {
            // No prolonged taxi split, write single file
            try (BufferedReader reader = new BufferedReader(new FileReader(outfile + ".tmp"));
                 PrintWriter writer = new PrintWriter(new FileWriter(outfile))) {
                
                // Read and write all header lines
                String line = reader.readLine();
                int rowIndex = 0;
                
                // Write metadata headers (lines starting with '#')
                while (line != null && line.startsWith("#")) {
                    writer.println(line + ",FlightPhase");
                    line = reader.readLine();
                }
                
                // Write column names header
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
            try (PrintWriter writer = new PrintWriter(new FileWriter(debugFile))) {
                // Write header
                writer.println("# Flight " + flight.getId());
                writer.println("# Row_Index\tTimestamp\tAltAGL_ft\tGround_Speed_kts\tRPM\tAirport_Distance_ft\tNearest_Airport\tFlight_Phase");
                writer.println("# ----------------------------------------");
                DoubleTimeSeries altAgl = flight.getDoubleTimeSeries(connection, Parameters.ALT_AGL);
                DoubleTimeSeries groundSpeed = flight.getDoubleTimeSeries(connection, Parameters.GND_SPD);
                DoubleTimeSeries rpm = null;
                try {
                    rpm = flight.getDoubleTimeSeries(connection, Parameters.E1_RPM);
                } catch (Exception e) {
                    // RPM may not be available
                }
                DoubleTimeSeries airportDist = null;
                DoubleTimeSeries nearestAirport = null;
                try {
                    airportDist = flight.getDoubleTimeSeries(connection, Parameters.AIRPORT_DISTANCE);
                } catch (Exception e) {}
                try {
                    nearestAirport = flight.getDoubleTimeSeries(connection, Parameters.NEAREST_AIRPORT);
                } catch (Exception e) {}
                // Get timestamp from StringTimeSeries
                StringTimeSeries timestamps = null;
                try {
                    timestamps = flight.getStringTimeSeries(connection, Parameters.UTC_DATE_TIME);
                } catch (Exception e) {}
                int nRows = altAgl.size();
                for (int i = 0; i < nRows; i++) {
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
    }

    /**
     * Ensures directory structure and JSON record file exist for a maintenance record
     *
     * @param outputDirectory the output directory
     * @param event           the maintenance record
     * @throws IOException if there is an error writing the file
     */
    private static void ensureClusterDirectoryExists(String outputDirectory, MaintenanceRecord event) throws IOException {
        String clusterDir = event.getLabelId();
        String workorderTailDir = event.getWorkorderNumber() + "_" + event.getTailNumber();
        String baseDir = outputDirectory + "/" + clusterDir + "/" + workorderTailDir;
        
        // Create base directory
        File directory = new File(baseDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        
        // Generate JSON record file
        File jsonFile = new File(baseDir + "/" + workorderTailDir + "_record.json");
        if (!jsonFile.exists()) {
            try (FileWriter jsonWriter = new FileWriter(jsonFile)) {
                jsonWriter.write(event.toJSON());
            }
        }
    }

    /**
     * Export the files for the given connection, timeline, target cluster and output directory
     *
     * @param connection      the database connection
     * @param timeline        the list of aircraft timelines
     * @param targetCluster   the target cluster
     * @param outputDirectory the output directory
     * @throws SQLException if there is an error with the SQL query
     * @throws IOException  if there is an error writing the file
     */
    /**
     * Exports flights to CSV files. Returns the number of flights actually extracted (written).
     */
    private static int exportFiles(Connection connection, List<AircraftTimeline> timeline, String targetCluster,
                                    String outputDirectory, boolean debugPhases) throws SQLException, IOException {
        int extractedCount = 0;
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
                
                // Validate flight, detect prolonged taxi for file splitting, touch-and-go for phase marking
                FlightPhaseProcessor.FlightValidationResult validation = null;
                List<Integer> fileSplitIndices = new ArrayList<>();
                try {
                    double[] altAGLValues = FlightPhaseProcessor.getAltAGLValues(connection, flight.getId(), Integer.MAX_VALUE);
                    validation = FlightPhaseProcessor.validateAndDetectTouchAndGo(altAGLValues);
                    
                    // Skip invalid flights (never left ground)
                    if (!validation.isValid) {
                        continue;
                    }
                    
                    // File splitting: only for prolonged taxi (30+ sec in middle)
                    fileSplitIndices = FlightPhaseProcessor.detectProlongedTaxiSplits(connection, flight.getId());
                } catch (Exception e) {
                    System.err.println("Warning: Could not validate flight " + flight.getId() + ": " + e.getMessage());
                    // Continue with extraction even if validation fails
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

                String eventCluster = LABEL_TO_CLUSTER.get(event.getLabel());
                if (eventCluster == null || !eventCluster.equals(targetCluster)) {
                    continue;
                }

                extractedCount++;
                writeFiles(connection, outputDirectory, event, flight, when, validation, fileSplitIndices, debugPhases);
            }
        }
        return extractedCount;
    }

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
            
            // Generate workorders array
            json.append("  \"workorders\": [\n");
            
            int recordIndex = 0;
            for (MaintenanceRecord record : ALL_RECORDS) {
                json.append("    {\n");
                json.append("      \"workorder\": ").append(record.getWorkorderNumber()).append(",\n");
                json.append("      \"label_id\": \"").append(record.getLabelId()).append("\",\n");
                json.append("      \"label\": \"").append(escapeJson(record.getLabel())).append("\",\n");
                json.append("      \"tail_number\": \"").append(record.getTailNumber()).append("\",\n");
                json.append("      \"airframe\": \"").append(record.getAirframe()).append("\",\n");
                json.append("      \"open_date\": \"").append(record.getOpenDate().toString()).append("\",\n");
                json.append("      \"close_date\": \"").append(record.getCloseDate().toString()).append("\",\n");
                json.append("      \"original_action\": \"").append(escapeJson(record.getOriginalAction())).append("\",\n");
                
                // Get flight paths for this workorder
                String labelId = record.getLabelId();
                String workorderTail = record.getWorkorderNumber() + "_" + record.getTailNumber();
                File workorderDir = new File(outputDirectory, labelId + "/" + workorderTail);
                
                json.append("      \"flights\": {\n");
                for (String phase : new String[]{"before", "during", "after"}) {
                    json.append("        \"").append(phase).append("\": [\n");
                    
                    File phaseDir = new File(workorderDir, phase);
                    if (phaseDir.exists() && phaseDir.isDirectory()) {
                        File[] csvFiles = phaseDir.listFiles((dir, name) -> name.endsWith(".csv"));
                        if (csvFiles != null) {
                            // Sort by base number, then by suffix (e.g., 1389.csv, 1389-1.csv, 1389-2.csv)
                            java.util.Arrays.sort(csvFiles, (f1, f2) -> {
                                String n1 = f1.getName();
                                String n2 = f2.getName();
                                // Match: 1389.csv, 1389-1.csv, 1389-2_phases.csv, etc.
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
                            for (int i = 0; i < csvFiles.length; i++) {
                                String fileName = csvFiles[i].getName();
                                String basePattern = "^(\\d+)(\\.csv)$";
                                String derivedPattern = "^(\\d+)-(\\d+)(\\.csv)$";
                                java.util.regex.Pattern baseRegex = java.util.regex.Pattern.compile(basePattern);
                                java.util.regex.Pattern derivedRegex = java.util.regex.Pattern.compile(derivedPattern);
                                java.util.regex.Matcher mBase = baseRegex.matcher(fileName);
                                String baseNum = null;
                                boolean hasDerived = false;
                                if (mBase.matches()) {
                                    baseNum = mBase.group(1);
                                    // Check for any derived file with same base
                                    for (int j = 0; j < csvFiles.length; j++) {
                                        if (j == i) continue;
                                        java.util.regex.Matcher mDerived = derivedRegex.matcher(csvFiles[j].getName());
                                        if (mDerived.matches() && mDerived.group(1).equals(baseNum)) {
                                            hasDerived = true;
                                            break;
                                        }
                                    }
                                }
                                String relativePath;
                                if (mBase.matches() && hasDerived) {
                                    // Output base as -1.csv
                                    relativePath = labelId + "/" + workorderTail + "/" + phase + "/" + baseNum + "-1.csv";
                                } else {
                                    relativePath = labelId + "/" + workorderTail + "/" + phase + "/" + fileName;
                                }
                                json.append("          \"").append(relativePath).append("\"");
                                if (i < csvFiles.length - 1) {
                                    json.append(",");
                                }
                                json.append("\n");
                            }
                        }
                    }
                    
                    json.append("        ]");
                    if (!phase.equals("after")) {
                        json.append(",");
                    }
                    json.append("\n");
                }
                json.append("      },\n");
                
                // Add record JSON path
                String recordJsonPath = labelId + "/" + workorderTail + "/" + workorderTail + "_record.json";
                json.append("      \"record_json\": \"").append(recordJsonPath).append("\"\n");
                
                json.append("    }");
                if (recordIndex < ALL_RECORDS.size() - 1) {
                    json.append(",");
                }
                json.append("\n");
                recordIndex++;
            }
            
            json.append("  ]\n");
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
        String outputDirectory = arguments[0];
        String targetCluster = arguments[1];

        // Check for debug argument
        int argOffset = 2;
        if (arguments.length > 2 && arguments[2].equals("--phases")) {
            debugPhases = true;
            argOffset++;
        }

        String[] allClusters = new String[arguments.length - argOffset];

        System.out.println("allClusters are:");
        for (int i = 0; i < allClusters.length; i++) {
            allClusters[i] = arguments[i + argOffset];
            System.out.println("\t'" + allClusters[i] + "'");
        }

        readClusterFiles(allClusters);

        System.out.println("cluster to label:");
        for (Map.Entry<String, String> kvPair : CLUSTER_TO_LABEL.entrySet()) {
            System.out.println("\t'" + kvPair.getKey() + "' -> '" + kvPair.getValue() + "'");
        }

        System.out.println("unique airframes: ");
        System.out.println("\t" + AIRFRAMES);

        // Process all clusters found in the file(s)
        for (String actualCluster : CLUSTER_TO_LABEL.keySet()) {
            System.out.println("\n\n=== Processing cluster: " + actualCluster + " ===");
            
            String targetLabel = CLUSTER_TO_LABEL.get(actualCluster);
            ArrayList<MaintenanceRecord> targetRecords = RECORDS_BY_LABEL.get(targetLabel);
            
            if (targetRecords == null || targetRecords.isEmpty()) {
                System.err.println("No records found for cluster: " + actualCluster);
                continue;
            }

            try {
                // for each tail
                // 1. get all flights between start and end date
                ArrayList<String> tailsWithoutFlights = new ArrayList<>();
                HashMap<String, Integer> tailFlightCounts = new HashMap<>();
                HashMap<String, Integer> tailSystemIdCounts = new HashMap<>();

                LocalDate startDate = targetRecords.get(0).getOpenDate().minusDays(10);
                LocalDate endDate = targetRecords.get(targetRecords.size() - 1).getCloseDate().plusDays(10);
                Set<String> targetTails = getTailNumbersForRecords(targetRecords);

                System.out.println(actualCluster + " | " + startDate + " to " + endDate + " | " + targetTails.size() + " tail(s)");

            for (String tailNumber : targetTails) {
                ArrayList<MaintenanceRecord> allTailRecords = RECORDS_BY_TAIL_NUMBER.get(tailNumber);
                
                // Filter to only records in this cluster for timeline building
                // This prevents cross-cluster event assignment issues
                ArrayList<MaintenanceRecord> tailRecords = new ArrayList<>();
                for (MaintenanceRecord record : allTailRecords) {
                    String recordCluster = LABEL_TO_CLUSTER.get(record.getLabel());
                    if (recordCluster != null && recordCluster.equals(actualCluster)) {
                        // Create directory structure upfront before filtering
                        ensureClusterDirectoryExists(outputDirectory, record);
                        tailRecords.add(record);
                    }
                }
                Collections.sort(tailRecords);

                PreparedStatement tailStmt =
                        connection.prepareStatement("SELECT system_id FROM tails WHERE tail = ? and fleet_id = ?");
                tailStmt.setString(1, tailNumber);
                int fleetId = 1; // UND
                tailStmt.setInt(2, fleetId);

                ResultSet tailSet = tailStmt.executeQuery();
                List<AircraftTimeline> timeline = buildTimeline(connection, tailSet, startDate, endDate);


                Collections.sort(timeline);

                // Assign each flight to phase (before/during/after) based on open/close:
                // - before: [openDate-10, openDate), during: [openDate, closeDate], after: (closeDate, closeDate+10]
                assignFlightsToPhases(timeline, tailRecords);

                // setting flights to next/flights since prev
                setFlightsToNextFlights(connection, timeline);

                // Write the files
                int extractedThisTail = exportFiles(connection, timeline, actualCluster, outputDirectory, debugPhases);
                extractedFlightsTotal += extractedThisTail;

                tailSystemIdCounts.put(tailNumber, systemIdCount);
                tailFlightCounts.put(tailNumber, extractedThisTail);

                tailSet.close();
                tailStmt.close();
                System.out.println("  " + tailNumber + ": " + tailRecords.size() + " event(s) -> " + extractedThisTail + " flights");
                if (extractedThisTail == 0) {
                    tailsWithoutFlights.add(tailNumber);
                }
            }
            System.out.println("  Total: " + extractedFlightsTotal + " flights extracted");
            if (!tailsWithoutFlights.isEmpty()) {
                System.out.println("  Tails with no flights: " + tailsWithoutFlights);
            }

            } catch (SQLException e) {
                System.err.println("SQLException: " + e);
                e.printStackTrace();
            } catch (IOException e) {
                System.err.println("IOException: " + e);
                e.printStackTrace();
            } catch (TimeUtils.UnrecognizedDateTimeFormatException e) {
                throw new RuntimeException(e);
            }
        } // End for loop over clusters
        
        // Generate manifest file after all clusters are processed
        try {
            generateManifest(outputDirectory);
        } catch (Exception e) {
            System.err.println("Error during manifest generation: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
