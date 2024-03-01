package org.ngafid.proximity;

import org.ngafid.Database;
import org.ngafid.UploadProcessedEmail;
import org.ngafid.events.Event;
import org.ngafid.events.RateOfClosure;
import org.ngafid.flights.Flight;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import org.ngafid.events.EventMetaData;
import org.ngafid.events.EventStatistics;
import org.ngafid.airports.Airports;


public class CalculateProximity {

    //Proximity events (and potentially other complicated event calculations) will have negative IDs so they
    //can be excluded from the regular event calculation process
    public static final int adjacencyEventDefinitionId = -1;

    //use this to get a representation of a flight's current time, position and altitude

    public static long timeMatchFlights = 0;
    public static long locMatchFlights = 0;
    public static long eventsFound = 0;

    static String timeSeriesName = "Lcl Time";
    static String dateSeriesName = "Lcl Date";

    public static double calculateDistance(double flightLatitude, double flightLongitude, double otherFlightLatitude,
                                           double otherFlightLongitude, double flightAltitude, double otherFlightAltitude){

        double distanceFt = Airports.calculateDistanceInFeet(flightLatitude, flightLongitude, otherFlightLatitude, otherFlightLongitude);
        double altDiff = Math.abs(flightAltitude - otherFlightAltitude);
        distanceFt = Math.sqrt((distanceFt * distanceFt) + (altDiff * altDiff));
        return distanceFt;
    }

    public static double calculateLateralDistance(double flightLatitude, double flightLongitude, double otherFlightLatitude,
                                           double otherFlightLongitude) {
       
        double lateralDistance = Airports.calculateDistanceInFeet(flightLatitude, flightLongitude, otherFlightLatitude, otherFlightLongitude);
        return lateralDistance;
    }
    
    public static double calculateVerticalDistance(double flightAltitude, double otherFlightAltitude) {
        
        double verticalDistance = Math.abs(flightAltitude - otherFlightAltitude);
        return verticalDistance;
    }

    public static double[] calculateRateOfClosure(FlightTimeLocation flightInfo, FlightTimeLocation otherInfo, int startLine,
                                                        int endLine, int otherStartLine,int otherEndLine ) {

        int shift = 5;

        int newStart1 = (startLine - shift) >= 0 ? (startLine - shift) : 0;
        int newStart2 = (otherStartLine - shift) >= 0 ? (otherStartLine - shift) : 0;

        int startShift1 = startLine - newStart1;
        int startShift2 = otherStartLine - newStart2;

        int startShift = Math.min(startShift1, startShift2);

        System.out.println("original start shift: " + shift + ", new start shift: " + startShift);

        newStart1 = startLine - startShift;
        newStart2 = otherStartLine - startShift;

        System.out.println("start line: " + startLine + ", otherStartLine: " + otherStartLine);
        System.out.println("shifted start line: " + newStart1 + ", otherStartLine: " + newStart2);

        int newEnd1 = (endLine + shift) <= flightInfo.epochTime.length ? (endLine + shift) : flightInfo.epochTime.length;
        int newEnd2 = (otherEndLine + shift) <= otherInfo.epochTime.length ? (otherEndLine + shift) : otherInfo.epochTime.length;

        int endShift1 = newEnd1 - endLine;
        int endShift2 = newEnd2 - otherEndLine;

        int endShift = Math.min(endShift1, endShift2);

        System.out.println("original end shift: " + shift + ", new end shift: " + endShift);

        newEnd1 = endLine + endShift;
        newEnd2 = otherEndLine + endShift;

        System.out.println("end line: " + endLine + ", otherEndLine: " + otherEndLine);
        System.out.println("shifted end line: " + newEnd1 + ", otherEndLine: " + newEnd2);

        startLine = newStart1;
        otherStartLine = newStart2;
        endLine = newEnd1;
        otherEndLine = newEnd2;

        double previousDistance = calculateDistance(flightInfo.latitude[startLine], flightInfo.longitude[startLine],
                    otherInfo.latitude[otherStartLine], otherInfo.longitude[otherStartLine],
                    flightInfo.altitudeMSL[startLine], otherInfo.altitudeMSL[otherStartLine]);

        ArrayList<Double> rateOfClosure = new ArrayList<Double>();
        int i = startLine + 1, j = otherStartLine + 1, index = 0;
        while (i < endLine && j < otherEndLine) {
            // System.out.println("flight1.epochTime[" + i + "]: " + flightInfo.epochTime[i] + ", flight2.epochTime[" + j + "]: " + otherInfo.epochTime[j] + ", previousDistance: " + previousDistance);
            if (flightInfo.epochTime[i] == 0) {
                i++;
                continue;
            }
            if (otherInfo.epochTime[j] == 0) {
                j++;
                continue;
            }
            //make sure both iterators are for the same time
            if (flightInfo.epochTime[i] < otherInfo.epochTime[j]) {
                i++;
                continue;
            }
            if (otherInfo.epochTime[j] < flightInfo.epochTime[i]) {
                j++;
                continue;
            }
            double currentDistance = calculateDistance(flightInfo.latitude[i], flightInfo.longitude[i],
                    otherInfo.latitude[j], otherInfo.longitude[j], flightInfo.altitudeMSL[i], otherInfo.altitudeMSL[j]);

            rateOfClosure.add(previousDistance - currentDistance);
            previousDistance = currentDistance;
            i++;
            j++;
            index++;
        }

        //convert the arraylist to a primitive array
        double[] roc = new double[rateOfClosure.size()];

        System.out.println("rate of closure, length:" + roc.length);
        for (int k = 0; k < roc.length; k++) {
            roc[k] = rateOfClosure.get(k);
            System.out.println("\t" + roc[k]);
        }

        //leave in to verify how things work in these edge cases
        if (startShift < 5 || endShift < 5) {
            System.exit(1);
        }

        return roc;
    }

    public static boolean addProximityIfNotInList(ArrayList<Event> eventList, Event testEvent) {
        for (Event event : eventList) {
            if (event.getFlightId() == testEvent.getFlightId() && event.getOtherFlightId() == testEvent.getOtherFlightId()) {
                return false;
            }
        }
        eventList.add(testEvent);

        return true;
    }
    
    public static void processFlightWithError(Connection connection, int fleetId, int flightId) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("INSERT INTO flight_processed SET fleet_id = ?, flight_id = ?, event_definition_id = ?, count = 0, had_error = 1");
        stmt.setInt(1, fleetId);
        stmt.setInt(2, flightId);
        stmt.setInt(3, adjacencyEventDefinitionId);
        //System.out.println(stmt.toString());
        stmt.executeUpdate();
        stmt.close();
    }

    public static void processFlight(Connection connection, Flight flight, UploadProcessedEmail uploadProcessedEmail) {
        System.out.println("Processing flight: " + flight.getId() + ", " + flight.getFilename());
        int fleetId = flight.getFleetId();
        int flightId = flight.getId();
        int airframeNameId = flight.getAirframeNameId();
        String flightFilename = flight.getFilename();

        try {
            //get enough information about the flight to determine if we can calculate adjacencies with it
            FlightTimeLocation flightInfo = new FlightTimeLocation(connection, flight.getFleetId(), flightId, flight.getAirframeNameId(), flight.getStartDateTime(), flight.getEndDateTime());

            if (!flightInfo.isValid()) {
                uploadProcessedEmail.addProximityError(flightFilename, "could not calculate proximity for flight " + flightId + ", '" + flightFilename + "' - was missing required data columns (date, time, latitude, longitude, altitude and/or indicated airspeed)");
                processFlightWithError(connection, fleetId, flightId);
                return;
            }

            ArrayList<Flight> potentialFlights = Flight.getFlights(connection, "(id != " + flightId + " AND start_timestamp <= UNIX_TIMESTAMP('" + flightInfo.endDateTime + "') AND end_timestamp >= UNIX_TIMESTAMP('" + flightInfo.startDateTime + "'))");

            System.out.println("Found " + potentialFlights.size() + " potential time matched flights.");
            //System.out.println("Flight start time: " + flightInfo.startDateTime + ", end time: " + flightInfo.endDateTime);
            //System.out.println("Flight latitude min: " + flightInfo.minLatitude + ", max: " + flightInfo.maxLatitude);
            //System.out.println("Flight longitude min: " + flightInfo.minLongitude + ", max: " + flightInfo.maxLongitude);

            ArrayList<Event> eventList = new ArrayList<>();
            String startTime = null;
            String endTime = null;
            String otherStartTime = null;
            String otherEndTime = null;

            int startLine = -1;
            int endLine = -1;
            int otherStartLine =-1;
            int otherEndLine = -1;

            int startCount = 0;
            int stopCount = 0;
            double severity = 0;
            double lateralDistance = 0;
            double verticalDistance = 0;

            //TODO: should probably grab these from the database event definition instead of hard
            //coding them; but we don't need to pull the event definition so this is a tad bit faster.
            int startBuffer = 1;
            int stopBuffer = 30;

            for (Flight otherFlight : potentialFlights) {
                //System.out.println("\tmatched to flight with start time: " + otherFlight.getStartDateTime() + ", end time: " + otherFlight.getEndDateTime());
                timeMatchFlights++;

                FlightTimeLocation otherInfo = new FlightTimeLocation(connection, otherFlight.getFleetId(), otherFlight.getId(), otherFlight.getAirframeNameId(), otherFlight.getStartDateTime(), otherFlight.getEndDateTime());
                if (!otherInfo.isValid()) {
                    //matched flight did not have all the information necessary to compute adjacency
                    continue;
                }

                //see if proximity between these two flights was already calculated, if so we can skip
                if (FlightTimeLocation.proximityAlreadyCalculated(connection, otherInfo, flightInfo)) {
                    System.out.println("Not re-performing proximity calculation");
                    continue;
                }

                //System.out.println("\t\tother latitude min: " + otherInfo.minLatitude + ", max: " + otherInfo.maxLatitude);
                //System.out.println("\t\tother longitude min: " + otherInfo.minLongitude + ", max: " + otherInfo.maxLongitude);

                if (flightInfo.hasRegionOverlap(otherInfo)) {
                    //System.out.println("\t\tLatitude/Longitude overlap!");
                    locMatchFlights++;

                    if (!flightInfo.hasSeriesData()) {
                        if (!flightInfo.getSeriesData(connection)) {
                            //could not get the required time series data columns
                            processFlightWithError(connection, fleetId, flightId);
                            return;
                        }
                    }

                    if (!otherInfo.getSeriesData(connection)) {
                        //the other flight didn't have the necesary time series data columns
                        continue;
                    }


                    //skip the first 30 seconds as it is usually the FDR being initialized
                    int i = 30, j = 30;

                    int totalMatches = 0;
                    //System.out.println("\t\tgot series data for both flights, iterate over times");
                    while (i < flightInfo.epochTime.length && j < otherInfo.epochTime.length) {
                        //skip entries where the epoch time was 0 (the date/time was null)
                        if (flightInfo.epochTime[i] == 0) {
                            i++;
                            continue;
                        }

                        if (otherInfo.epochTime[j] == 0) {
                            j++;
                            continue;
                        }

                        //make sure both iterators are for the same time
                        if (flightInfo.epochTime[i] < otherInfo.epochTime[j]) {
                            i++;
                            continue;
                        }

                        if (otherInfo.epochTime[j] < flightInfo.epochTime[i]) {
                            j++;
                            continue;
                        }

                        double distanceFt = calculateDistance(flightInfo.latitude[i], flightInfo.longitude[i], otherInfo.latitude[j],
                                otherInfo.longitude[j], flightInfo.altitudeMSL[i] , otherInfo.altitudeMSL[j]);
                        double lateralDistanceFt = calculateLateralDistance(flightInfo.latitude[i], flightInfo.longitude[i], otherInfo.latitude[j],
                                otherInfo.longitude[j]);
                        double verticalDistanceFt = calculateVerticalDistance(flightInfo.altitudeMSL[i] , otherInfo.altitudeMSL[j]);

                        if (distanceFt < 1000.0 && flightInfo.altitudeAGL[i] >= 50 && otherInfo.altitudeAGL[j] >= 50 && flightInfo.indicatedAirspeed[i] > 20 && otherInfo.indicatedAirspeed[j] > 20) {
                            /*
                            System.out.println("\t\t\tother time[" + j + "]: " + otherInfo.epochTime[j] + " == flight time[" + i + "]: " + flightInfo.epochTime[i]
                                    + ", flight lat/lon: " + flightInfo.latitude[i] + " " + flightInfo.longitude[i] + ", other lat/lon: " + otherInfo.latitude[j] + " " + otherInfo.longitude[j]
                                    + " -- distance: " + distanceFt
                                    );
                            */

                            //System.out.println("\t\t\t\t\tflight alt AGL: " + flightInfo.altitudeAGL[i] + ", other alt AGL: " + otherInfo.altitudeAGL[j] + ", final distance: " + distanceFt);

                            //startTime is null if an exceedence is not being tracked
                            if (startTime == null) {
                                //start tracking a new exceedence
                                startTime = flightInfo.dateSeries.get(i) + " " + flightInfo.timeSeries.get(i);
                                otherStartTime = otherInfo.dateSeries.get(j) + " " + otherInfo.timeSeries.get(j);

                                startLine = i;
                                otherStartLine = j;
                                severity = distanceFt;
                                lateralDistance = lateralDistanceFt;
                                verticalDistance = verticalDistanceFt;

                                //System.out.println("start date time: " + startTime + ", start line number: " + startLine);
                            }
                            endLine = i;
                            otherEndLine = j;
                            endTime = flightInfo.dateSeries.get(i) + " " + flightInfo.timeSeries.get(i);
                            otherEndTime = otherInfo.dateSeries.get(j) + " " + otherInfo.timeSeries.get(j);

                            if (distanceFt < severity) {
                                //this time was even closer than the last closest proximity
                                //for this event, update the severity
                                severity = distanceFt;
                            }
                            lateralDistance = lateralDistanceFt < lateralDistance ? lateralDistanceFt : lateralDistance; 
                            verticalDistance = verticalDistanceFt < verticalDistance ? verticalDistanceFt : verticalDistance; 
                            //increment the startCount, reset the endCount
                            startCount++;
                            stopCount = 0;

                        } else {
                            //this time didn't trigger proximity

                            if (startTime != null) {
                                //we're already tracking a proximity event, so increment
                                //the stop count
                                stopCount++;

                                if (stopCount == stopBuffer) {
                                    //System.err.println("Stop count (" + stopCount + ") reached the stop buffer (" + stopBuffer + "), new event created!");

                                    if (startCount < startBuffer) {
                                        //we didn't have enough triggers to reach the start count so don't create
                                        //the event
                                    } else {
                                        //we had enough triggers to reach the start count so create the event
                                        System.out.println("Creating event for flight : " + flightId );
                                        Event event = new Event (startTime, endTime, startLine, endLine, severity, otherFlight.getId());
                                        Event otherEvent = new Event(otherStartTime, otherEndTime, otherStartLine, otherEndLine, severity, flightId);
                                        EventMetaData lateralDistanceMetaData = new EventMetaData("lateral_distance", lateralDistance); 
                                        EventMetaData verticalDistanceMetaData = new EventMetaData("vertical_distance", verticalDistance);
                                        event.addMetaData(lateralDistanceMetaData);
                                        event.addMetaData(verticalDistanceMetaData);

                                        otherEvent.addMetaData(lateralDistanceMetaData);
                                        otherEvent.addMetaData(verticalDistanceMetaData);
                                        if ( severity > 0) {
                                            double[] rateOfClosureArray = calculateRateOfClosure(flightInfo, otherInfo, startLine, endLine, otherStartLine, otherEndLine);
                                            RateOfClosure rateOfClosure = new RateOfClosure(rateOfClosureArray);
                                            event.setRateOfClosure(rateOfClosure);
                                            otherEvent.setRateOfClosure(rateOfClosure);
                                        }

                                        addProximityIfNotInList(eventList, event);
                                        addProximityIfNotInList(eventList, otherEvent);

                                        //add in an event for the other flight as well so we don't need to recalculate this
                                        //otherInfo.updateWithEvent(connection, otherEvent, otherFlight.getStartDateTime());
                                    }

                                    //reset the event values
                                    startTime = null;
                                    otherStartTime = null;
                                    endTime = null;
                                    otherEndTime = null;
                                    startLine = -1;
                                    otherEndLine = -1;
                                    endLine = -1;
                                    otherEndLine = -1;

                                    //reset the start and stop counts
                                    startCount = 0;
                                    stopCount = 0;
                                }
                            }
                        }

                        //iterate both as they had matching times
                        i++;
                        j++;
                        totalMatches++;
                    }
                    //System.out.println("\t\tseries matched time on " + totalMatches + " rows");

                    //if there was an event still going when one flight ended, create it and add it to the list
                    if (startTime != null) {

                        Event event = new Event(startTime, endTime, startLine, endLine, severity, otherFlight.getId());
                        Event otherEvent = new Event(otherStartTime, otherEndTime, otherStartLine, otherEndLine, severity, flightId);

                        EventMetaData lateralDistanceMetaData = new EventMetaData("lateral_distance", lateralDistance); 
                        EventMetaData verticalDistanceMetaData = new EventMetaData("vertical_distance", verticalDistance);
                        event.addMetaData(lateralDistanceMetaData);
                        event.addMetaData(verticalDistanceMetaData);

                        otherEvent.addMetaData(lateralDistanceMetaData);
                        otherEvent.addMetaData(verticalDistanceMetaData);

                        if ( severity > 0 ) {
                            double[] rateOfClosureArray = calculateRateOfClosure(flightInfo, otherInfo, startLine, endLine, otherStartLine,otherEndLine);
                            RateOfClosure rateOfClosure = new RateOfClosure(rateOfClosureArray);
                            event.setRateOfClosure(rateOfClosure);
                            otherEvent.setRateOfClosure(rateOfClosure);
                        }

                        addProximityIfNotInList(eventList, event);
                        addProximityIfNotInList(eventList, otherEvent);

                        //add in an event for the other flight as well so we don't need to recalculate this
                        //otherInfo.updateWithEvent(connection, otherEvent, otherFlight.getStartDateTime());

                    }
                }
                //end the loop processing a particular flight
            }
            //end the loop processing all flights

            for (Event event : eventList) {
                System.out.println("\t" + event.toString());
                eventsFound++;
                uploadProcessedEmail.addProximity(flightFilename, "flight " + flightId + ", '" + flightFilename + "' - had a proximity event with flight " + event.getOtherFlightId() + " from " + event.getStartTime() + " to " + event.getEndTime());
            }

            System.out.println("\n");


            //Step 2: export the events and their statistics in the database
            double sumDuration = 0.0;
            double sumSeverity = 0.0;
            double minSeverity = Double.MAX_VALUE;
            double maxSeverity = -Double.MAX_VALUE;
            double minDuration = Double.MAX_VALUE;
            double maxDuration = -Double.MAX_VALUE;
            for (int i = 0; i < eventList.size(); i++) {
                Event event = eventList.get(i);
                event.updateDatabase(connection, fleetId, flightId, adjacencyEventDefinitionId);
                if (event.getStartTime() != null) {
                    EventStatistics.updateEventStatistics(connection, fleetId, airframeNameId, adjacencyEventDefinitionId, event.getStartTime(), event.getSeverity(), event.getDuration());
                } else if (event.getEndTime() != null) {
                    EventStatistics.updateEventStatistics(connection, fleetId, airframeNameId, adjacencyEventDefinitionId, event.getEndTime(), event.getSeverity(), event.getDuration());
                } else {
                    System.out.println("WARNING: could not update event statistics for event: " + event);
                    System.out.println("WARNING: event start and end time were both null.");
                }

                double currentSeverity = eventList.get(i).getSeverity();
                double currentDuration = eventList.get(i).getDuration();
                sumDuration += currentDuration;
                sumSeverity += currentSeverity;

                if (currentSeverity > maxSeverity) maxSeverity = currentSeverity;
                if (currentSeverity < minSeverity) minSeverity = currentSeverity;
                if (currentDuration > maxDuration) maxDuration = currentDuration;
                if (currentDuration < minDuration) minDuration = currentDuration;
            }

            if (eventList.size() > 0) {
                PreparedStatement stmt = connection.prepareStatement("INSERT INTO flight_processed SET fleet_id = ?, flight_id = ?, event_definition_id = ?, count = ?, sum_duration = ?, min_duration = ?, max_duration = ?, sum_severity = ?, min_severity = ?, max_severity = ?, had_error = 0");
                stmt.setInt(1, fleetId);
                stmt.setInt(2, flightId);
                stmt.setInt(3, adjacencyEventDefinitionId);
                stmt.setInt(4, eventList.size());
                stmt.setDouble(5, sumDuration);
                stmt.setDouble(6, minDuration);
                stmt.setDouble(7, maxDuration);
                stmt.setDouble(8, sumSeverity);
                stmt.setDouble(9, minSeverity);
                stmt.setDouble(10, maxSeverity);
                //System.out.println(stmt.toString());
                stmt.executeUpdate();
                stmt.close();

                EventStatistics.updateFlightsWithEvent(connection, fleetId, airframeNameId, adjacencyEventDefinitionId, flight.getStartDateTime());

            } else {
                PreparedStatement stmt = connection.prepareStatement("INSERT INTO flight_processed SET fleet_id = ?, flight_id = ?, event_definition_id = ?, count = 0, had_error = 0");
                stmt.setInt(1, fleetId);
                stmt.setInt(2, flightId);
                stmt.setInt(3, adjacencyEventDefinitionId);
                //System.out.println(stmt.toString());
                stmt.executeUpdate();
                stmt.close();

                EventStatistics.updateFlightsWithoutEvent(connection, fleetId, airframeNameId, adjacencyEventDefinitionId, flight.getStartDateTime());
            }

        } catch(SQLException e) {
            System.err.println(e);
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void calculateProximity(Connection connection, int uploadId, UploadProcessedEmail uploadProcessedEmail) throws SQLException {
        Instant start = Instant.now();

        ArrayList<Flight> flights = Flight.getFlights(connection, "upload_id = " + uploadId + " AND NOT EXISTS (SELECT flight_id FROM flight_processed WHERE event_definition_id = " + adjacencyEventDefinitionId + " AND flight_processed.flight_id = flights.id)");

        int count = 0;
        for (int j = 0; j < flights.size(); j++) {
            if (!flights.get(j).insertCompleted()) {
                //this flight is currently being inserted to
                //the database by ProcessFlights
                continue;
            }

            processFlight(connection, flights.get(j), uploadProcessedEmail);
            count++;
        }

        Instant end = Instant.now();
        double elapsed_seconds = (double)Duration.between(start, end).toMillis() / 1000.0;
        double average_seconds = ((double) elapsed_seconds) / (double)count;
        double avgTimeMatchedFlights = ((double)timeMatchFlights / (double) count);
        double avgLocationMatchedFlights = ((double)locMatchFlights / (double)count);

        System.out.println("calculated " + count + " proximity evaluations in " + elapsed_seconds + " seconds, averaged: " + average_seconds + " seconds per flight");
        System.out.println("avg time matched flights: " + avgTimeMatchedFlights + ", avg loc matched flights: " + avgLocationMatchedFlights);
        System.out.println("proximity events found:"  + eventsFound);

        uploadProcessedEmail.setProximityElapsedTime(elapsed_seconds, average_seconds, avgTimeMatchedFlights, avgLocationMatchedFlights);
    }


    public static void main(String[] arguments) {
        try {

            int flightsPerQuery = 5000;
            while (true) {
                Connection connection = Database.resetConnection();
                Instant start = Instant.now();

                ArrayList<Flight> flights = Flight.getFlights(connection, "NOT EXISTS (SELECT flight_id FROM flight_processed WHERE event_definition_id = " + adjacencyEventDefinitionId + " AND flight_processed.flight_id = flights.id)", flightsPerQuery);

                int count = 0;
                for (int j = 0; j < flights.size(); j++) {
                    if (!flights.get(j).insertCompleted()) {
                        //this flight is currently being inserted to
                        //the database by ProcessFlights
                        continue;
                    }

                    processFlight(connection, flights.get(j), null);
                    count++;
                }

                Instant end = Instant.now();
                double elapsed_seconds = (double)Duration.between(start, end).toMillis() / 1000.0;
                double average_seconds = ((double) elapsed_seconds) / (double)count;
                System.out.println("calculated " + count + " adjacency evaluations in " + elapsed_seconds + " seconds, averaged: " + average_seconds + " seconds per flight");
                System.out.println("avg time matched flights: " + ((double)timeMatchFlights / (double) count) + ", avg loc matched flights: " + ((double)locMatchFlights / (double)count));
                System.out.println("evnets found:"  + eventsFound);
                //System.exit(1);

                try {
                    Thread.sleep(3000);
                } catch (Exception e) {
                    System.err.println(e);
                    e.printStackTrace();
                }
            }

            //connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.err.println("finished!");
        System.exit(1);
    }
}
