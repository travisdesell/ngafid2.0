package org.ngafid;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;


import java.time.Duration;
import java.time.LocalDate;
import java.time.Instant;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.time.temporal.TemporalAmount;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeSet;


import org.ngafid.WebServer;

import org.ngafid.common.TimeUtils;

import org.ngafid.flights.CSVWriter;
import org.ngafid.flights.FlightAlreadyExistsException;
import org.ngafid.flights.FatalFlightFileException;

import org.ngafid.flights.Flight;
import org.ngafid.flights.FlightError;
import org.ngafid.flights.UploadError;

import org.ngafid.maintenance.MaintenanceRecord;
import org.ngafid.maintenance.AircraftTimeline;

public class ExtractMaintenanceFlights {
    private static Connection connection = Database.getConnection();

    private static HashMap<Integer, MaintenanceRecord> recordsByWorkorder = new HashMap<>();
    private static HashMap<String, ArrayList<MaintenanceRecord>> recordsByLabel = new HashMap<>();
    private static HashMap<String, ArrayList<MaintenanceRecord>> recordsByTailNumber = new HashMap<>();
    private static TreeSet<MaintenanceRecord> allRecords = new TreeSet<>();
    private static TreeSet<String> tailNumbers = new TreeSet<>();
    private static TreeSet<String> airframes = new TreeSet<>();
    private static HashMap<String, String> clusterToLabel = new HashMap<>();
    private static HashMap<String, String> labelToCluster = new HashMap<>();
    private static HashMap<String, Integer> clusterCounts = new HashMap<>();


    public static void main(String[] arguments) {
        String output_directory = arguments[0];
        String targetCluster = arguments[1];

        String[] allClusters = new String[arguments.length - 2];

        System.out.println("allClusters are:");
        for (int i = 0; i < allClusters.length; i++) {
            allClusters[i] = arguments[i+2];
            System.out.println("\t'" + allClusters[i] + "'");
        }

        int lineCount = 0;
        for (int i = 0; i < allClusters.length; i++) {
            BufferedReader reader;

            ArrayList<MaintenanceRecord> clusterRecords = new ArrayList<>();

            try {
                System.out.println("READING FILE: '" + allClusters[i] + "'");
                reader = new BufferedReader(new FileReader(allClusters[i]));
                String header = reader.readLine();

                String line = reader.readLine();
                String clusterName = null;

                boolean first = true;
                MaintenanceRecord previous = null;
                while (line != null) {
                    //System.out.println(line);
                    lineCount++;

                    MaintenanceRecord record = new MaintenanceRecord(line);
                    if (first) {
                        clusterName = allClusters[i].substring(allClusters[i].lastIndexOf("/") + 1, allClusters[i].lastIndexOf("."));
                        clusterToLabel.put(clusterName, record.getLabel());
                        labelToCluster.put(record.getLabel(), clusterName);
                        clusterCounts.put(clusterName, 0);
                        first = false;
                    }

                    MaintenanceRecord existingRecord = recordsByWorkorder.get(record.getWorkorderNumber());
                    if (existingRecord == null) {
                        //this is a record we have not yet seen before
                        recordsByWorkorder.put(record.getWorkorderNumber(), record);

                        if ((record.getAirframe().equals("C172") || record.getAirframe().equals("ARCH") || record.getAirframe().equals("SEMI"))) {
                            //only add tail numbers from C172, PA28 or PA44
                            tailNumbers.add(record.getTailNumber());
                        }

                        airframes.add(record.getAirframe());

                        //add it to the list of records by tails
                        ArrayList<MaintenanceRecord> tailSet = recordsByTailNumber.get(record.getTailNumber());
                        if (tailSet == null) {
                            tailSet = new ArrayList<MaintenanceRecord>();
                            recordsByTailNumber.put(record.getTailNumber(), tailSet);
                        } 
                        tailSet.add(record);

                        //add it to the list of reords by labels
                        ArrayList<MaintenanceRecord> labelList = recordsByLabel.get(record.getLabel());
                        if (labelList == null) {
                            labelList = new ArrayList<MaintenanceRecord>();
                            recordsByLabel.put(record.getLabel(), labelList);
                        } 
                        labelList.add(record);

                        allRecords.add(record);
                    } else {
                        existingRecord.combine(record);
                    }

                    //System.out.println("\t" + record.toString());

                    clusterRecords.add(record);
                    clusterCounts.put(clusterName, clusterCounts.get(clusterName) + 1);

                    // read next line
                    line = reader.readLine();
                }

                reader.close();
            } catch (IOException e) {
                System.err.println("Could not read cluster file: '" + allClusters[i] + "'");
                e.printStackTrace();
                System.exit(1);
            }
        }

        //System.out.println("\n\n\n");
        for (MaintenanceRecord record : allRecords) {
            //System.out.println(record.toString());
        }

        System.out.println("\n\n\n");
        System.out.println("Number of record lines: " + lineCount);
        System.out.println("Number of workorders: " + allRecords.size());
        System.out.println("earliest date: " + allRecords.first().getOpenDate());
        System.out.println("latest date: " + allRecords.last().getCloseDate());
        System.out.println("unique tails: ");
        System.out.println("\t" + tailNumbers);

        System.out.println("cluster to label:");
        for (Map.Entry<String, String> kvPair : clusterToLabel.entrySet()) {
            System.out.println("\t'" + kvPair.getKey() + "' -> '" + kvPair.getValue() + "' -- count: " + clusterCounts.get(kvPair.getKey()));
        }

        //System.out.println("\t" + clusterToLabel);

        System.out.println("unique airframes: ");
        System.out.println("\t" + airframes);

        String targetLabel = clusterToLabel.get(targetCluster);
        ArrayList<MaintenanceRecord> targetRecords = recordsByLabel.get(targetLabel);

        System.out.println("\n\n\n");
        System.out.println("Getting records for cluster: '" + targetCluster + "' and label: '" + targetLabel + "'");

        HashSet<String> targetTails = new HashSet<>();
        for (MaintenanceRecord record : targetRecords) {
            //C172 is Cessna 172
            //ARCH is PA28 (Piper Archer)
            //SEMI IS PA44 (Piper Seminole) 
            if (!(record.getAirframe().equals("C172") || record.getAirframe().equals("ARCH") || record.getAirframe().equals("SEMI"))) {
                //skip the others
                continue;
            }

            //System.out.println(record.toString());
            targetTails.add(record.getTailNumber());
        }
        LocalDate startDate = targetRecords.get(0).getOpenDate().minusDays(10);
        LocalDate endDate = targetRecords.get(targetRecords.size() -1 ).getCloseDate().plusDays(10);
        System.out.println("earliest date for label: " + startDate);
        System.out.println("latest date for label: " + endDate);
        System.out.println("label present for tails: " + targetTails);

        try {
            //for each tail
            //1. get all flights between start and end date

            ArrayList<String> tailsWithoutFlights = new ArrayList<String>();
            HashMap<String,Integer> tailFlightCounts = new HashMap<>();
            HashMap<String,Integer> tailSystemIdCounts = new HashMap<>();

            for (String tailNumber : targetTails) {
            //for (String tailNumber : tailNumbers) {

                System.out.println("\n\nNEW TAIL: '" + tailNumber + "'");
                System.out.println("records for tail: '" + tailNumber + "'");
                ArrayList<MaintenanceRecord> tailRecords = recordsByTailNumber.get(tailNumber);
                Collections.sort(tailRecords);

                for (MaintenanceRecord record : tailRecords) {
                    System.out.println("\t" + record.getOpenDate() + " to " + record.getCloseDate() + " for " + record.getLabel() + ", airframe: " + record.getAirframe());
                }
                System.out.println("tail: '" + tailNumber + "' had " + tailRecords.size() + " events.");
                System.out.println();


                PreparedStatement tailStmt = connection.prepareStatement("SELECT system_id FROM tails WHERE tail = ? and fleet_id = ?");
                tailStmt.setString(1, tailNumber);
                int fleetId = 1; // UND
                tailStmt.setInt(2, fleetId);

                System.out.println("tail: '" + tailNumber + "' database flights:");

                ArrayList<AircraftTimeline> timeline = new ArrayList<>();

                int systemIdCount = 0;
                int count = 0;
                ResultSet tailSet = tailStmt.executeQuery();
                while (tailSet.next()) {
                    String systemId = tailSet.getString(1);
                    System.out.println("\tsystem id '" + systemId + "' flights:");

                    PreparedStatement stmt = connection.prepareStatement("SELECT id, start_time, end_time, airframe_id FROM flights WHERE system_id = ? AND start_time > ? AND end_time < ? ORDER BY start_time");
                    stmt.setString(1, systemId);
                    stmt.setString(2, startDate.toString());
                    stmt.setString(3, endDate.toString());

                    System.out.println(stmt.toString());

                    //if there was a flight processed entry for this flight it was already processed
                    ResultSet resultSet = stmt.executeQuery();
                    while (resultSet.next()) {
                        int flightId = resultSet.getInt(1);
                        String flightStartTime = resultSet.getString(2);
                        String flightEndTime = resultSet.getString(3);
                        int airframeId = resultSet.getInt(4);

                        //System.out.println("\t\tid: " + flightId + ", " + flightStartTime + " to " + flightEndTime + ", airframe id: " + airframeId);

                        //convert the start time (which is in GMT) to CST
                        flightStartTime = TimeUtils.convertToOffset(flightStartTime, "+00:00", "-06:00");
                        flightEndTime = TimeUtils.convertToOffset(flightEndTime, "+00:00", "-06:00");

                        //System.out.println("\t\tid: " + flightId + ", " + flightStartTime + " to " + flightEndTime + ", airframe id: " + airframeId);
                        timeline.add(new AircraftTimeline(flightId, flightStartTime, flightEndTime));
                        count++;
                    }

                    systemIdCount++;
                    resultSet.close();
                    stmt.close();
                    //break;
                }

                int currentRecord = 0;
                Collections.sort(timeline);
                MaintenanceRecord previousRecord = null;
                MaintenanceRecord record = tailRecords.get(currentRecord);
                for (int currentAircraft = 0; currentAircraft < timeline.size(); currentAircraft++) {
                    AircraftTimeline ac = timeline.get(currentAircraft);

                    //System.err.println("current aircraft: " + ac);
                    //System.err.println("starting with record: " + record);
                    while (record != null && ac.getEndTime().compareTo(record.getOpenDate()) > 0) {
                        previousRecord = record;
                        currentRecord++;
                        if (currentRecord >= tailRecords.size()) {
                            record = null;
                            break;
                        }
                        record = tailRecords.get(currentRecord);
                        //System.out.println("\t\tmoved to record: " + record);
                    }
                    //System.out.println("moved to record: " + record);

                    while (record == null || ac.getEndTime().compareTo(record.getCloseDate()) <= 0) {
                        long daysToNext = -1;
                        if (record != null) daysToNext = Math.max(0, ChronoUnit.DAYS.between(ac.getEndTime(), record.getOpenDate()));

                        if (daysToNext == 0) {
                            //this is the day an event occurred so the previous is the current record as well
                            previousRecord = record;
                        }

                        long daysSincePrev = -1;
                        if (previousRecord != null) daysSincePrev = Math.max(0, ChronoUnit.DAYS.between(previousRecord.getCloseDate(), ac.getStartTime()));

                        if (previousRecord != null) {
                            //System.out.print("previous record close " + previousRecord.getCloseDate() + " (" + daysSincePrev + ") ");
                            ac.setPreviousEvent(previousRecord, daysSincePrev);
                        } else {
                            ac.setPreviousEvent(null, -1);
                        }

                        if (record != null) {
                            //System.out.print(" next record open " + record.getOpenDate() + " (" + daysToNext + ") ");
                            ac.setNextEvent(record, daysToNext);
                        } else {
                            ac.setNextEvent(null, -1);
                        }
                        //System.out.print(" " + ac.toString());
                        //System.out.println();

                        currentAircraft++;
                        if (currentAircraft >= timeline.size()) break;
                        ac = timeline.get(currentAircraft);
                    }

                    if (record != null) {
                        //step back so the last AC can be processed
                        currentAircraft--;
                    }

                    /*
                    if (record != null) {
                        long daysToNext = ChronoUnit.DAYS.between(ac.getEndTime(), record.getOpenDate());
                        if (daysToNext == 0) {
                            //this is the day an event occurred so the previous is the current record as well
                            previousRecord = record;
                        }

                        long daysSincePrev = -1;
                        if (previousRecord != null) daysSincePrev = Math.max(0, ChronoUnit.DAYS.between(previousRecord.getCloseDate(), ac.getStartTime()));

                        ac.setPreviousEvent(previousRecord, daysSincePrev);
                        ac.setNextEvent(record, daysToNext);
                        System.out.println("\t\t" + ac.toString() + ", previous record close " + previousRecord.getCloseDate() + " (" + daysSincePrev + "), next record open " + record.getOpenDate() + " (" + daysToNext + ")");

                    } else {
                        long daysSincePrev = ChronoUnit.DAYS.between(previousRecord.getCloseDate(), ac.getStartTime());
                        System.out.println("\t\t" + ac.toString() + ", previous record close " + previousRecord.getCloseDate() + " (" + daysSincePrev + "), no next record");
                        ac.setPreviousEvent(previousRecord, daysSincePrev);
                        ac.setNextEvent(null, -1);
                    }
                    */
                }

                //setting flights to next/flights since prev
                int NUMBER_EXTRACTIONS = 5;
                for (int currentAircraft = 0; currentAircraft < timeline.size(); currentAircraft++) {
                    AircraftTimeline ac = timeline.get(currentAircraft);

                    if (ac.getDaysToNext() == 0 || ac.getDaysSincePrevious() == 0) {
                        int i = 1;
                        int flightCount = 0;
                        while ((currentAircraft - i) >= 0 && (i - 1) < NUMBER_EXTRACTIONS) {
                            AircraftTimeline a = timeline.get(currentAircraft - i);
                            if (a.getDaysToNext() == 0) {
                                //System.out.println("setting timeline[" + (currentAircraft - i) + " flightsToNext to: 0 because days since previous is: " + a.getDaysToNext());
                                a.setFlightsToNext(-1); //-1 means the flight occurred day of the event, or outside of the range
                                i++;
                                continue;
                            }

                            //System.out.println("setting timeline[" + (currentAircraft - i) + " flightsToNext to: " + flightCount + " because days since previous is: " + a.getDaysToNext());
                            a.setFlightsToNext(flightCount);
                            i++;
                            flightCount++;
                        }

                        i = 1;
                        flightCount = 0;

                        while ((currentAircraft + i) < timeline.size() && (i - 1) < NUMBER_EXTRACTIONS) {
                            AircraftTimeline a = timeline.get(currentAircraft + i);
                            if (a.getDaysSincePrevious() == 0) {
                                //System.out.println("setting timeline[" + (currentAircraft + i) + " flightsSincePrevious to: 0 because days since previous is: " + a.getDaysSincePrevious());
                                a.setFlightsSincePrevious(-1); //-1 means the flight occurred day of the event, or outside of the range
                                i++;
                                continue;
                            }

                            //System.out.println("setting timeline[" + (currentAircraft + i) + " flightsSincePrevious to: " + flightCount + " because days since previous is: " + a.getDaysSincePrevious());
                            a.setFlightsSincePrevious(flightCount);
                            i++;
                            flightCount++;
                        }
                    }
                }

                System.err.println("\n\nflightsToNext, flightsSincePrev set, now exporting files:");
                for (int currentAircraft = 0; currentAircraft < timeline.size(); currentAircraft++) {
                    AircraftTimeline ac = timeline.get(currentAircraft);
                    //System.out.println(ac + " -- NEXT " + ac.getNextEvent() + " -- PREV " + ac.getPreviousEvent());

                    if (ac.getDaysSincePrevious() == 0 || ac.getDaysToNext() == 0 || ac.getFlightsSincePrevious() != -1 || ac.getFlightsToNext() != -1) {

                        Flight flight = Flight.getFlight(connection, ac.getFlightId());

                        String when = null;

                        MaintenanceRecord event = null;
                        if (ac.getDaysSincePrevious() == 0) {
                            event = ac.getPreviousEvent();
                            when =  "_day_of";
                        } else if (ac.getFlightsSincePrevious() != -1) {
                            event = ac.getPreviousEvent();
                            when = "_after_" + ac.getFlightsSincePrevious();
                        } else if (ac.getDaysToNext() == 0) {
                            event = ac.getNextEvent();
                            when = "_day_of";
                        } else {
                            event = ac.getNextEvent();
                            when = "_before_" + ac.getFlightsToNext();
                        }

                        if (event == null) {
                            System.err.println("ERROR: event is null! currentAircraft: " + currentAircraft + ", timeline.size(): " + timeline.size());
                        }
                        if (labelToCluster == null) System.err.println("ERROR: labelToCluster is null!");

                        String eventCluster = labelToCluster.get(event.getLabel());

                        if (!eventCluster.equals(targetCluster)) continue;
                        //System.out.println(ac.toString());

                        //System.out.println(" -- Event: '" + event.getLabel() + "', open '" + event.getOpenDate() + ", close '" + event.getCloseDate() + "'");

                        String airframeType = flight.getAirframeType();
                        if (airframeType.equals("Cessna 172S") || airframeType.equals("Cessna 172R") || airframeType.equals("Cessna 172T")) {
                            airframeType = "C172";
                        } else if (airframeType.equals("PA-28-181")) {
                            airframeType = "PA28";
                        } else if (airframeType.equals("PA-44-180")) {
                            airframeType = "PA44";
                        }


                        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy_MM_dd");

                        String outfile = output_directory + "/" + eventCluster;

                        File directory = new File(outfile);
                        if (!directory.exists()) {
                            directory.mkdir();
                        }

                        String recordFile = output_directory + "/" + eventCluster + "/open_" + event.getOpenDate().format(fmt) + "_close_" + event.getCloseDate().format(fmt) + "_record.txt";
                        BufferedWriter bw = new BufferedWriter(new FileWriter(new File(recordFile).getAbsoluteFile()));
                        bw.write(event.toJSON());
                        bw.close();

                        outfile += "/open_" + event.getOpenDate().format(fmt) + "_close_" + event.getCloseDate().format(fmt) + "_flight_" + airframeType + "_" + flight.getTailNumber() + when + "_" + flight.getId() + ".csv";
                        System.out.println(outfile);

                        String zipRoot = WebServer.NGAFID_ARCHIVE_DIR + "/" + fleetId + "/" + flight.getUploaderId() + "/";

                        CSVWriter csvWriter = new CSVWriter(zipRoot, flight);
                        String contents = csvWriter.write();
                        //System.out.println(contents);

                        bw = new BufferedWriter(new FileWriter(new File(outfile).getAbsoluteFile()));
                        bw.write(contents);
                        bw.close();

                        if (ac.getFlightsSincePrevious() == 4) System.out.println();
                    }
                }
 
                tailSystemIdCounts.put(tailNumber, systemIdCount);
                tailFlightCounts.put(tailNumber, count);

                tailSet.close();
                tailStmt.close();
                System.out.println(count + " total flights.");
                if (count == 0) {
                    tailsWithoutFlights.add(tailNumber);
                }
                //return;
            }
            System.out.println("all tail numbers (" + tailNumbers.size() + "): " + tailNumbers);
            System.out.println("tails without flights (" + tailsWithoutFlights.size() + "): " + tailsWithoutFlights);

            System.out.println("flight counts per tail:");
            for (Map.Entry<String, Integer> kvPair : tailFlightCounts.entrySet()) {
                System.out.println("\t'" + kvPair.getKey() + "' -> " + kvPair.getValue() + " (" + tailSystemIdCounts.get(kvPair.getKey()) + ")");
            }


        } catch (SQLException e) {
            System.err.println("SQLException: " + e);
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("IOException: " + e);
            e.printStackTrace();
        }
    }
}
