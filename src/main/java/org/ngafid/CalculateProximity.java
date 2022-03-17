package org.ngafid;

import org.ngafid.Database;
import org.ngafid.events.Event;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.Flight;
import org.ngafid.flights.StringTimeSeries;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.time.Duration;
import java.time.Instant;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashSet;
//import java.util.logging.Level;
//import java.util.logging.Logger;
import java.util.TreeSet;

import org.ngafid.common.TimeUtils;

import org.ngafid.events.EventDefinition;
import org.ngafid.events.EventStatistics;

import org.ngafid.filters.Conditional;
import org.ngafid.filters.Filter;
import org.ngafid.filters.Pair;

import org.ngafid.airports.Airports;

public class CalculateProximity {

    //Proximity events (and potentially other complicated event calculations) will have negative IDs so they
    //can be excluded from the regular event calculation process
    public static final int adjacencyEventDefinitionId = -1;

    //use this to get a representation of a flight's current time, position and altitude

    public static long timeMatchFlights = 0;
    public static long locMatchFlights = 0;
    public static long eventsFound = 0;

    protected static class FlightTimeLocation {
        //set to true if the flight has the required time series values and a start and 
        //end date time
        boolean valid = false;

        //set to true when the double and string time series data has been
        //read from the database, and the epochTime array has been calculated
        boolean hasSeriesData = false;

        int fleetId;
        int flightId;
        int airframeNameId;

        String startDateTime;
        String endDateTime;

        double minLatitude;
        double maxLatitude;
        double minLongitude;
        double maxLongitude;

        double minAltMSL;
        double maxAltMSL;

        long[] epochTime;
        double[] altitudeMSL;
        double[] altitudeAGL;
        double[] latitude;
        double[] longitude;
        double[] indicatedAirspeed;

        StringTimeSeries dateSeries;
        StringTimeSeries timeSeries;

        public FlightTimeLocation(Connection connection, int fleetId, int flightId, int airframeNameId, String startDateTime, String endDateTime) throws SQLException {
            this.fleetId = fleetId;
            this.flightId = flightId;
            this.airframeNameId = airframeNameId;
            this.startDateTime = startDateTime;
            this.endDateTime = endDateTime;

            //first check and see if the flight had a start and end time, if not we cannot process it
            //System.out.println("Getting info for flight with start date time: " + startDateTime + " and end date time: " + endDateTime);

            if (startDateTime == null || endDateTime == null) {
                //flight didnt have a start or end time
                valid = false;
                return;
            }

            //then check and see if this was actually a flight (RPM > 800)
            Pair<Double,Double> minMaxRPM1 = DoubleTimeSeries.getMinMax(connection, flightId, "E1 RPM");
            Pair<Double,Double> minMaxRPM2 = DoubleTimeSeries.getMinMax(connection, flightId, "E2 RPM");

            //if (minMaxRPM1 != null) System.out.println("min max E1 RPM: " + minMaxRPM1.first() + ", " + minMaxRPM1.second());
            //if (minMaxRPM2 != null) System.out.println("min max E2 RPM: " + minMaxRPM2.first() + ", " + minMaxRPM2.second());
            
            if ((minMaxRPM1 == null && minMaxRPM2 == null)  //both RPM values are null, can't calculate exceedence
                    || (minMaxRPM2 == null && (minMaxRPM1 != null && minMaxRPM1.second() < 800)) //RPM2 is null, RPM1 is < 800
                    || (minMaxRPM1 == null && (minMaxRPM2 != null && minMaxRPM2.second() < 800)) //RPM1 is null, RPM2 is < 800
                    || ((minMaxRPM1.second() < 800) && (minMaxRPM2.second() < 800))) { //RPM1 and RPM2 < 800
                //couldn't calculate exceedences for this flight because the engines never kicked on (it didn't fly)
                valid = false;
                return;
            }

            //then check and see if this flight had a latitude and longitude, if not we cannot calculate adjacency
            Pair<Double,Double> minMaxLatitude = DoubleTimeSeries.getMinMax(connection, flightId, "Latitude");
            Pair<Double,Double> minMaxLongitude = DoubleTimeSeries.getMinMax(connection, flightId, "Longitude");

            //if (minMaxLatitude != null) System.out.println("min max latitude: " + minMaxLatitude.first() + ", " + minMaxLatitude.second());
            //if (minMaxLongitude != null) System.out.println("min max longitude: " + minMaxLongitude.first() + ", " + minMaxLongitude.second());

            if (minMaxLatitude == null || minMaxLongitude == null) {
                //flight didn't have latitude or longitude
                valid = false;
                return;
            }

            minLatitude = minMaxLatitude.first();
            maxLatitude = minMaxLatitude.second();
            minLongitude = minMaxLongitude.first();
            maxLongitude = minMaxLongitude.second();

            //then check and see if this flight had alt MSL, if not we cannot calculate adjacency
            Pair<Double,Double> minMaxAltMSL = DoubleTimeSeries.getMinMax(connection, flightId, "AltMSL");

            //if (minMaxAltMSL != null) System.out.println("min max alt MSL: " + minMaxAltMSL.first() + ", " + minMaxAltMSL.second());

            if (minMaxAltMSL == null) {
                //flight didn't have alt MSL
                valid = false;
                return;
            }

            minAltMSL = minMaxAltMSL.first();
            maxAltMSL = minMaxAltMSL.second();

            //this flight had the necessary values and time series to calculate adjacency
            valid = true;
        }

        public boolean getSeriesData(Connection connection) throws SQLException {
            //get the time series data for altitude, latitude and longitude
            DoubleTimeSeries altMSLSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "AltMSL");
            DoubleTimeSeries altAGLSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "AltAGL");
            DoubleTimeSeries latitudeSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "Latitude");
            DoubleTimeSeries longitudeSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "Longitude");
            DoubleTimeSeries indicatedAirspeedSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "IAS");

            //check to see if we could get these columns
            if (altMSLSeries == null || altAGLSeries == null || latitudeSeries == null || longitudeSeries == null || indicatedAirspeedSeries == null) return false;

            altitudeMSL = altMSLSeries.innerArray();
            altitudeAGL = altAGLSeries.innerArray();
            latitude = latitudeSeries.innerArray();
            longitude = longitudeSeries.innerArray();
            indicatedAirspeed = indicatedAirspeedSeries.innerArray();

            //calculate the epoch time for each row as longs so they can most be quickly compared
            //we need to keep track of the date and time series for inserting in the event info
            dateSeries = StringTimeSeries.getStringTimeSeries(connection, flightId, "Lcl Date");
            timeSeries = StringTimeSeries.getStringTimeSeries(connection, flightId, "Lcl Time");
            StringTimeSeries utcOffsetSeries = StringTimeSeries.getStringTimeSeries(connection, flightId, "UTCOfst");

            //check to see if we could get these columns
            if (dateSeries == null || timeSeries == null || utcOffsetSeries == null) return false;

            //System.out.println("date length: " + dateSeries.size() + ", time length: " + timeSeries.size() + ", utc length: " + utcOffsetSeries.size());
            int length = dateSeries.size();

            epochTime = new long[length];
            for (int i = 0; i < length; i++) {
                if (dateSeries.get(i) == null || dateSeries.get(i).equals("")
                        || timeSeries.get(i) == null || timeSeries.get(i).equals("")
                        || utcOffsetSeries.get(i) == null || utcOffsetSeries.get(i).equals("")) {
                    epochTime[i] = 0;
                    continue;
                }

                epochTime[i] = TimeUtils.toEpochSecond(dateSeries.get(i), timeSeries.get(i), utcOffsetSeries.get(i));
            }

            hasSeriesData = true;

            return true;
        }

        public boolean hasRegionOverlap(FlightTimeLocation other) {
            return other.maxLatitude >= this.minLatitude && other.minLatitude <= this.maxLatitude 
                        && other.maxLongitude >= this.minLongitude && other.minLongitude <= this.maxLongitude;
        }

        public boolean isValid() {
            return valid;
        }

        public boolean hasSeriesData() {
            return hasSeriesData;
        }

        public boolean alreadyProcessed(Connection connection) throws SQLException {
            PreparedStatement stmt = connection.prepareStatement("SELECT flight_id FROM flight_processed WHERE fleet_id = ? AND flight_id = ? AND event_definition_id = ?");
            stmt.setInt(1, fleetId);
            stmt.setInt(2, flightId);
            stmt.setInt(3, adjacencyEventDefinitionId);

            System.out.println(stmt.toString());

            //if there was a flight processed entry for this flight it was already processed
            ResultSet resultSet = stmt.executeQuery();
            if (resultSet.next()) {
                System.out.println("already processed!");
                resultSet.close();
                stmt.close();
                return true;
            } else {
                System.out.println("not already processed!");
                resultSet.close();
                stmt.close();
                return false;
            }
        }

        public static boolean proximityAlreadyCalculated(Connection connection, FlightTimeLocation first, FlightTimeLocation second) throws SQLException {
            PreparedStatement stmt = connection.prepareStatement("SELECT flight_id FROM events WHERE flight_id = ? AND other_flight_id = ?");
            stmt.setInt(1, first.flightId);
            stmt.setInt(2, second.flightId);

            System.out.println(stmt.toString());

            //if there was a flight processed entry for this flight it was already processed
            ResultSet resultSet = stmt.executeQuery();
            if (resultSet.next()) {
                System.out.println("proximity event already exists!");
                resultSet.close();
                stmt.close();
                return true;
            } else {
                System.out.println("proximity does not already exist!");
                resultSet.close();
                stmt.close();
                return false;
            }
        }

        public void updateWithEvent(Connection connection, Event event, String startDateTime) throws SQLException {

            event.updateDatabase(connection, fleetId, flightId, adjacencyEventDefinitionId);
            if (event.getStartTime() != null) {
                EventStatistics.updateEventStatistics(connection, fleetId, airframeNameId, adjacencyEventDefinitionId, event.getStartTime(), event.getSeverity(), event.getDuration());
            } else if (event.getEndTime() != null) {
                EventStatistics.updateEventStatistics(connection, fleetId, airframeNameId, adjacencyEventDefinitionId, event.getEndTime(), event.getSeverity(), event.getDuration());
            } else {
                System.out.println("WARNING: could not update event statistics for event: " + event);
                System.out.println("WARNING: event start and end time were both null.");
            }
            double severity = event.getSeverity();
            double duration = event.getDuration();

            PreparedStatement stmt = connection.prepareStatement("UPDATE flight_processed SET count = count + 1, sum_duration = sum_duration + ?, min_duration = LEAST(min_duration, ?), max_duration = GREATEST(max_duration, ?), sum_severity = sum_severity + ?, min_severity = LEAST(min_severity, ?), max_severity = GREATEST(max_severity, ?) WHERE fleet_id = ? AND flight_id = ? AND event_definition_id = ?");
            stmt.setInt(1, fleetId);
            stmt.setInt(2, flightId);
            stmt.setInt(3, adjacencyEventDefinitionId);
            stmt.setDouble(4, duration);
            stmt.setDouble(5, duration);
            stmt.setDouble(6, duration);
            stmt.setDouble(7, severity);
            stmt.setDouble(8, severity);
            stmt.setDouble(9, severity);
            System.out.println(stmt.toString());
            stmt.executeUpdate();
            stmt.close();

            EventStatistics.updateFlightsWithEvent(connection, fleetId, airframeNameId, adjacencyEventDefinitionId, startDateTime);

        }
    }

    static String timeSeriesName = "Lcl Time";
    static String dateSeriesName = "Lcl Date";
    
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


                        double distanceFt = Airports.calculateDistanceInFeet(flightInfo.latitude[i], flightInfo.longitude[i], otherInfo.latitude[j], otherInfo.longitude[j]);
                        double altDiff = Math.abs(flightInfo.altitudeMSL[i] - otherInfo.altitudeMSL[j]);
                        distanceFt = Math.sqrt((distanceFt * distanceFt) + (altDiff * altDiff));

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
                                        Event event = new Event (startTime, endTime, startLine, endLine, severity, otherFlight.getId());
                                        eventList.add(event);

                                        //add in an event for the other flight as well so we don't need to recalculate this
                                        otherInfo.updateWithEvent(connection, new Event(otherStartTime, otherEndTime, otherStartLine, otherEndLine, severity, flightId), otherFlight.getStartDateTime());
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
                        eventList.add( event );

                        //add in an event for the other flight as well so we don't need to recalculate this
                        otherInfo.updateWithEvent(connection, new Event(otherStartTime, otherEndTime, otherStartLine, otherEndLine, severity, flightId), otherFlight.getStartDateTime());
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
