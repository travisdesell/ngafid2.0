package org.ngafid.core.bin;

import org.ngafid.core.Database;
import org.ngafid.core.flights.DoubleTimeSeries;
import org.ngafid.core.flights.Flight;
import org.ngafid.core.flights.Parameters;
import org.ngafid.core.flights.export.CSVWriter;
import org.ngafid.core.flights.export.CachedCSVWriter;
import org.ngafid.core.flights.maintenance.AircraftTimeline;
import org.ngafid.core.flights.maintenance.MaintenanceRecord;
import org.ngafid.core.util.TimeUtils;

import java.io.*;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private static final HashMap<String, Integer> CLUSTER_COUNTS = new HashMap<>();
    private static int systemIdCount = 0;
    private static int count = 0;

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
                    lineCount++;

                    MaintenanceRecord record = new MaintenanceRecord(line);
                    
                    // Each record's originalAction becomes its own cluster name
                    String clusterName = record.getOriginalAction();
                    
                    // Add cluster mapping for this record
                    if (!CLUSTER_TO_LABEL.containsKey(clusterName)) {
                        CLUSTER_TO_LABEL.put(clusterName, record.getLabel());
                        LABEL_TO_CLUSTER.put(record.getLabel(), clusterName);
                        CLUSTER_COUNTS.put(clusterName, 0);
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

                        // add it to the list of reords by labels
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
        System.out.println("earliest date: " + ALL_RECORDS.first().getOpenDate());
        System.out.println("latest date: " + ALL_RECORDS.last().getCloseDate());
        System.out.println("unique tails: ");
        System.out.println("\t" + TAIL_NUMBERS);
    }

    /**
     * Get the records for the target cluster and label
     *
     * @param targetCluster the target cluster
     * @param targetLabel   the target label
     * @param targetRecords the list of all records
     * @return the set of tails for the target cluster and label
     */
    private static Set<String> getRecordForClusterAndLabel(String targetCluster, String targetLabel,
                                                           List<MaintenanceRecord> targetRecords) {
        System.out.println("\n\n\n");
        System.out.println("Getting records for cluster: '" + targetCluster + "' and label: '" + targetLabel + "'");

        HashSet<String> targetTails = new HashSet<>();
        for (MaintenanceRecord record : targetRecords) {
            targetTails.add(record.getTailNumber());
        }

        return targetTails;
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
            System.out.println("\tsystem id '" + systemId + "' flights:");

            PreparedStatement stmt = connection.prepareStatement(
                    "SELECT id, start_time, end_time, airframe_id FROM flights " +
                            "WHERE system_id = ? AND start_time > ? AND end_time < ? ORDER BY start_time");
            stmt.setString(1, systemId);
            stmt.setString(2, startDate.toString());
            stmt.setString(3, endDate.toString());

            System.out.println(stmt);

            // if there was a flight processed entry for this flight it was already processed
            ResultSet resultSet = stmt.executeQuery();
            while (resultSet.next()) {
                int flightId = resultSet.getInt(1);
                String flightStartTime = resultSet.getString(2);
                String flightEndTime = resultSet.getString(3);

                // convert the start time (which is in GMT) to CST
                flightStartTime = TimeUtils.toString(TimeUtils.convertToOffset(flightStartTime, "+00:00", "-06:00"));
                flightEndTime = TimeUtils.toString(TimeUtils.convertToOffset(flightEndTime, "+00:00", "-06:00"));

                timeline.add(new AircraftTimeline(flightId, flightStartTime, flightEndTime));
                count++;
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
            int count = rs.getInt(1);
            System.out.println("Flight " + flightId + " has " + count + " takeoff/landing itinerary entries");
            tookOff = count > 0;
        }
        rs.close();
        stmt.close();
        System.out.println("Flight " + flightId + " took off: " + tookOff);
        return tookOff;
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
            // 1. Flight is on maintenance day (daysToNext==0 or daysSincePrevious==0)
            // 2. OR when transitioning between maintenance events (nextEvent changes from previous flight)
            boolean isOnMaintenanceDay = ac.getDaysToNext() == 0 || ac.getDaysSincePrevious() == 0;
            boolean isAfterMaintenanceGap = false;
            
            if (!isOnMaintenanceDay && currentAircraft > 0 && ac.getNextEvent() != null) {
                AircraftTimeline prev = timeline.get(currentAircraft - 1);
                // Check if this is the first flight after a maintenance event (next event changed)
                isAfterMaintenanceGap = prev.getNextEvent() != ac.getNextEvent();
                if (isAfterMaintenanceGap) {
                    System.out.println("DEBUG: Detected maintenance gap at flight " + ac.getFlightId() + 
                                     " - prevNext=" + (prev.getNextEvent() != null ? prev.getNextEvent().getLabel() : "null") +
                                     ", curNext=" + (ac.getNextEvent() != null ? ac.getNextEvent().getLabel() : "null"));
                }
            }
            
            if (isOnMaintenanceDay || isAfterMaintenanceGap) {
                int i = 1;
                int flightCount = 0;
                while ((currentAircraft - i) >= 0 && flightCount < numberOfExtractions) {
                    AircraftTimeline a = timeline.get(currentAircraft - i);
                    if (a.getDaysToNext() == 0) {
                        a.setFlightsToNext(-1); // -1 means the flight occurred day of the event, or outside of the range
                        i++;
                        continue;
                    }

                    // Check if this is a ground run - if so, skip it
                    if (!flightTookOff(connection, a.getFlightId())) {
                        System.out.println("Skipping ground-run flight (before): " + a.getFlightId());
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
                        a.setFlightsSincePrevious(-1); // -1 means the flight occurred day of the event, or
                        // outside of the range
                        i++;
                        continue;
                    }

                    // Check if this is a ground run - if so, skip it
                    if (!flightTookOff(connection, a.getFlightId())) {
                        System.out.println("Skipping ground-run flight (after): " + a.getFlightId());
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
                            System.out.println("Skipping ground-run flight (before, second pass): " + a.getFlightId());
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
                            System.out.println("Skipping ground-run flight (after, second pass): " + a.getFlightId());
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
     * @param eventCluster    the event cluster
     * @param event           the maintenance record
     * @param flight          the flight
     * @param when            the when string
     * @param validation      the flight validation result (for touch-and-go detection)
     * @throws IOException  if there is an error writing the file
     * @throws SQLException if there is an error with the SQL query
     */
    private static void writeFiles(Connection connection, String outputDirectory, String eventCluster, 
                                   MaintenanceRecord event, Flight flight, String when,
                                   FlightPhaseProcessor.FlightValidationResult validation) 
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

        // Create directory structure: <cluster_id>/<workorder>_<tail>/phase/
        String clusterDir = event.getClusterId();
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
        System.out.println(outfile);

        String zipRoot = NGAFID_ARCHIVE_DIR + "/" + flight.getFleetId() + "/" + flight.getUploaderId() + "/";

        // First, write the original CSV
        CSVWriter csvWriter = new CachedCSVWriter(zipRoot, flight, Optional.of(new File(outfile + ".tmp")), false);
        csvWriter.writeToFile();

        // Compute complete flight phases (includes touch-and-go/go-around marking and altitude smoothing)
        FlightPhaseProcessor.FlightPhaseData phaseData = null;
        try {
            phaseData = FlightPhaseProcessor.computeCompleteFlightPhases(connection, flight, validation);
            
            System.out.println("Computed phases for flight " + flight.getId() + ": " + 
                             FlightPhaseProcessor.getPhaseSummary(phaseData));
        } catch (Exception e) {
            System.err.println("Warning: Could not compute flight phases for flight " + flight.getId() + ": " + e.getMessage());
        }

        // Read the temporary CSV and write with FlightPhase column
        // Handle touch-and-go splitting if needed
        if (validation != null && validation.hasTouchAndGo()) {
            // Split flight into multiple CSV files
            System.out.println("Splitting flight " + flight.getId() + " into " + 
                             (validation.splitIndices.size() + 1) + " segments");
            
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
                
                for (int splitIndex : validation.splitIndices) {
                    // Determine output file name
                    String segmentFile;
                    if (segmentNumber == 1) {
                        segmentFile = outfile;  // First segment keeps original name
                    } else {
                        segmentFile = fullDir + "/" + flight.getId() + "-" + segmentNumber + ".csv";
                    }
                    
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
                    
                    System.out.println("  Created segment " + segmentNumber + ": " + 
                                     new File(segmentFile).getName());
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
                
                System.out.println("  Created segment " + segmentNumber + ": " + 
                                 new File(finalFile).getName());
            }
        } else {
            // No touch-and-go, write single file
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
    }

    /**
     * Sanitize directory name by removing/replacing invalid characters
     *
     * @param name the directory name to sanitize
     * @return sanitized directory name
     */
    private static String sanitizeDirectoryName(String name) {
        // Replace invalid characters with underscores, limit length
        return name.replaceAll("[^a-zA-Z0-9_\\-\\.\\s]", "_")
                   .replaceAll("\\s+", "_")
                   .substring(0, Math.min(name.length(), 100));
    }

    /**
     * Ensures directory structure and JSON record file exist for a maintenance record
     *
     * @param outputDirectory the output directory
     * @param eventCluster    the event cluster name
     * @param event           the maintenance record
     * @throws IOException if there is an error writing the file
     */
    private static void ensureClusterDirectoryExists(String outputDirectory, String eventCluster, MaintenanceRecord event) throws IOException {
        String clusterDir = event.getClusterId();
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
    private static void exportFiles(Connection connection, List<AircraftTimeline> timeline, String targetCluster,
                                    String outputDirectory) throws SQLException, IOException {
        System.err.println("\n\nflightsToNext, flightsSincePrev set, now exporting files:");
        for (int currentAircraft = 0; currentAircraft < timeline.size(); currentAircraft++) {
            AircraftTimeline ac = timeline.get(currentAircraft);

            // Extract flights if:
            // 1. Day of maintenance (before or after)
            // 2. Between open and close dates (daysSincePrevious >= 0 AND daysToNext >= 0)
            // 3. One of 3 flights before (flightsToNext 0-2)
            // 4. One of 3 flights after (flightsSincePrevious 0-2), but only if no nearby maintenance
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
                
                // Validate flight and check for touch-and-gos BEFORE extraction
                FlightPhaseProcessor.FlightValidationResult validation = null;
                try {
                    double[] altAGLValues = FlightPhaseProcessor.getAltAGLValues(connection, flight.getId(), Integer.MAX_VALUE);
                    validation = FlightPhaseProcessor.validateAndDetectTouchAndGo(altAGLValues);
                    
                    // Skip invalid flights (never left ground)
                    if (!validation.isValid) {
                        System.out.println("Skipping flight " + flight.getId() + " - never exceeded 10ft AGL (max: " + 
                                         String.format("%.1f", validation.maxAltAGL) + "ft)");
                        continue;
                    }
                    
                    // Handle touch-and-gos by splitting CSV files
                    if (validation.hasTouchAndGo()) {
                        System.out.println("Flight " + flight.getId() + " has " + validation.getTouchAndGoCount() + 
                                         " touch-and-go(s) detected");
                    }
                } catch (Exception e) {
                    System.err.println("Warning: Could not validate flight " + flight.getId() + ": " + e.getMessage());
                    // Continue with extraction even if validation fails
                }

                String when = null;

                MaintenanceRecord event = null;
                if (isDuringMaintenance) {
                    // Flight is between open and close dates
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

                writeFiles(connection, outputDirectory, eventCluster, event, flight, when, validation);
            }
        }
    }

    /**
     * Generates a manifest JSON file containing information about all extracted flights
     *
     * @param outputDirectory the base directory where extracted flights are stored
     */
    private static void generateManifest(String outputDirectory) {
        try {
            System.out.println("\nGenerating manifest file...");
            
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"generated_at\": \"").append(LocalDateTime.now().toString()).append("\",\n");
            
            // Calculate statistics
            int totalFlights = 0;
            HashMap<String, Integer> clusterFlightCounts = new HashMap<>();
            HashMap<String, String> clusterNames = new HashMap<>();
            
            // Build cluster name map from records
            for (MaintenanceRecord record : ALL_RECORDS) {
                if (record.getClusterId() != null && record.getClusterName() != null) {
                    clusterNames.put(record.getClusterId(), record.getClusterName());
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
                json.append("      \"cluster_id\": \"").append(record.getClusterId()).append("\",\n");
                json.append("      \"cluster_name\": \"").append(escapeJson(record.getClusterName())).append("\",\n");
                json.append("      \"tail_number\": \"").append(record.getTailNumber()).append("\",\n");
                json.append("      \"airframe\": \"").append(record.getAirframe()).append("\",\n");
                json.append("      \"open_date\": \"").append(record.getOpenDate().toString()).append("\",\n");
                json.append("      \"close_date\": \"").append(record.getCloseDate().toString()).append("\",\n");
                json.append("      \"original_action\": \"").append(escapeJson(record.getOriginalAction())).append("\",\n");
                
                // Get flight paths for this workorder
                String clusterId = record.getClusterId();
                String workorderTail = record.getWorkorderNumber() + "_" + record.getTailNumber();
                File workorderDir = new File(outputDirectory, clusterId + "/" + workorderTail);
                
                json.append("      \"flights\": {\n");
                for (String phase : new String[]{"before", "during", "after"}) {
                    json.append("        \"").append(phase).append("\": [\n");
                    
                    File phaseDir = new File(workorderDir, phase);
                    if (phaseDir.exists() && phaseDir.isDirectory()) {
                        File[] csvFiles = phaseDir.listFiles((dir, name) -> name.endsWith(".csv"));
                        if (csvFiles != null) {
                            Arrays.sort(csvFiles);
                            for (int i = 0; i < csvFiles.length; i++) {
                                String relativePath = clusterId + "/" + workorderTail + "/" + phase + "/" + csvFiles[i].getName();
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
                String recordJsonPath = clusterId + "/" + workorderTail + "/" + workorderTail + "_record.json";
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
            
            System.out.println("Manifest file generated: " + manifestFile.getAbsolutePath());
            System.out.println("Total workorders: " + RECORDS_BY_WORKORDER.size());
            System.out.println("Total flights: " + totalFlights);
            
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

        String outputDirectory = arguments[0];
        String targetCluster = arguments[1];

        String[] allClusters = new String[arguments.length - 2];

        System.out.println("allClusters are:");
        for (int i = 0; i < allClusters.length; i++) {
            allClusters[i] = arguments[i + 2];
            System.out.println("\t'" + allClusters[i] + "'");
        }

        readClusterFiles(allClusters);

        System.out.println("cluster to label:");
        for (Map.Entry<String, String> kvPair : CLUSTER_TO_LABEL.entrySet()) {
            System.out.println("\t'" + kvPair.getKey() + "' -> '" + kvPair.getValue()
                    + "' -- count: " + CLUSTER_COUNTS.get(kvPair.getKey()));
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
                ArrayList<String> tailsWithoutFlights = new ArrayList<String>();
                HashMap<String, Integer> tailFlightCounts = new HashMap<>();
                HashMap<String, Integer> tailSystemIdCounts = new HashMap<>();

                LocalDate startDate = targetRecords.get(0).getOpenDate().minusDays(10);
                LocalDate endDate = targetRecords.get(targetRecords.size() - 1).getCloseDate().plusDays(10);
                Set<String> targetTails = getRecordForClusterAndLabel(actualCluster, targetLabel, targetRecords);

            System.out.println("earliest date for label: " + startDate);
            System.out.println("latest date for label: " + endDate);
            System.out.println("label present for tails: " + targetTails);

            for (String tailNumber : targetTails) {
                System.out.println("\n\nNEW TAIL: '" + tailNumber + "'");
                System.out.println("records for tail: '" + tailNumber + "'");
                ArrayList<MaintenanceRecord> allTailRecords = RECORDS_BY_TAIL_NUMBER.get(tailNumber);
                
                // Filter to only records in this cluster for timeline building
                // This prevents cross-cluster event assignment issues
                ArrayList<MaintenanceRecord> tailRecords = new ArrayList<>();
                for (MaintenanceRecord record : allTailRecords) {
                    String recordCluster = LABEL_TO_CLUSTER.get(record.getLabel());
                    if (recordCluster != null && recordCluster.equals(actualCluster)) {
                        // Create directory structure upfront before filtering
                        ensureClusterDirectoryExists(outputDirectory, actualCluster, record);
                        tailRecords.add(record);
                    }
                }
                Collections.sort(tailRecords);

                for (MaintenanceRecord record : tailRecords) {
                    System.out.println("\t" + record.getOpenDate() + " to " + record.getCloseDate() +
                            " for " + record.getLabel() + ", airframe: " + record.getAirframe());
                }
                System.out.println("tail: '" + tailNumber + "' had " + tailRecords.size() + " events in this cluster.");
                System.out.println();

                PreparedStatement tailStmt =
                        connection.prepareStatement("SELECT system_id FROM tails WHERE tail = ? and fleet_id = ?");
                tailStmt.setString(1, tailNumber);
                int fleetId = 1; // UND
                tailStmt.setInt(2, fleetId);

                System.out.println("tail: '" + tailNumber + "' database flights:");

                ResultSet tailSet = tailStmt.executeQuery();
                List<AircraftTimeline> timeline = buildTimeline(connection, tailSet, startDate, endDate);


                int currentRecord = 0;
                Collections.sort(timeline);
                MaintenanceRecord previousRecord = null;
                MaintenanceRecord record = tailRecords.get(currentRecord);
                for (int currentAircraft = 0; currentAircraft < timeline.size(); currentAircraft++) {
                    AircraftTimeline ac = timeline.get(currentAircraft);

                    while (record != null && ac.getEndTime().isAfter(record.getOpenDate())) {
                        previousRecord = record;
                        currentRecord++;
                        if (currentRecord >= tailRecords.size()) {
                            record = null;
                            break;
                        }
                        record = tailRecords.get(currentRecord);
                    }

                    while (record == null || !ac.getEndTime().isAfter(record.getCloseDate())) {
                        long daysToNext = -1;
                        if (record != null)
                            daysToNext = Math.max(0, ChronoUnit.DAYS.between(ac.getEndTime(), record.getOpenDate()));

                        if (daysToNext == 0) {
                            // this is the day an event occurred so the previous is the current record as
                            // well
                            previousRecord = record;
                        }

                        long daysSincePrev = -1;
                        if (previousRecord != null)
                            daysSincePrev = Math.max(0,
                                    ChronoUnit.DAYS.between(previousRecord.getCloseDate(), ac.getStartTime()));

                        if (previousRecord != null) {
                            ac.setPreviousEvent(previousRecord, daysSincePrev);
                        } else {
                            ac.setPreviousEvent(null, -1);
                        }

                        if (record != null) {
                            ac.setNextEvent(record, daysToNext);
                        } else {
                            ac.setNextEvent(null, -1);
                        }

                        currentAircraft++;
                        if (currentAircraft >= timeline.size()) {
                            break;
                        }
                        ac = timeline.get(currentAircraft);
                    }

                    if (record != null) {
                        // step back so the last AC can be processed
                        currentAircraft--;
                    }
                }

                // setting flights to next/flights since prev
                setFlightsToNextFlights(connection, timeline);

                // Write the files
                exportFiles(connection, timeline, actualCluster, outputDirectory);

                tailSystemIdCounts.put(tailNumber, systemIdCount);
                tailFlightCounts.put(tailNumber, count);

                tailSet.close();
                tailStmt.close();
                System.out.println(count + " total flights.");
                if (count == 0) {
                    tailsWithoutFlights.add(tailNumber);
                }
            }
            System.out.println("all tail numbers (" + TAIL_NUMBERS.size() + "): " + TAIL_NUMBERS);
            System.out.println("tails without flights (" + tailsWithoutFlights.size() + "): " + tailsWithoutFlights);

            System.out.println("flight counts per tail:");
            for (Map.Entry<String, Integer> kvPair : tailFlightCounts.entrySet()) {
                System.out.println("\t'" + kvPair.getKey() + "' -> " + kvPair.getValue()
                        + " (" + tailSystemIdCounts.get(kvPair.getKey()) + ")");
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
        generateManifest(outputDirectory);
    }
}
