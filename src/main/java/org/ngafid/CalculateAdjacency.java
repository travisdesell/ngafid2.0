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
import java.util.TreeSet;

import org.ngafid.events.EventDefinition;
import org.ngafid.events.EventStatistics;

import org.ngafid.filters.Conditional;
import org.ngafid.filters.Filter;
import org.ngafid.filters.Pair;

import org.ngafid.airports.Airports;

public class CalculateAdjacency {

    //Adjacency events (and potentially other complicated event calculations) will have negative IDs so they
    //can be excluded from the regular event calculation process
    public static final int adjacencyEventDefinitionId = -1;

    //use this to get a representation of a flight's current time, position and altitude

    protected static class FlightTimeLocation {
        //set to true if the flight has the required time series values and a start and 
        //end date time
        boolean valid = false;

        //set to true when the double and string time series data has been
        //read from the database, and the epochTime array has been calculated
        boolean hasSeriesData = false;

        int flightId;

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
        double[] latitude;
        double[] longitude;

        public FlightTimeLocation(Connection connection, int flightId, String startDateTime, String endDateTime) throws SQLException {
            this.flightId = flightId;
            this.startDateTime = startDateTime;
            this.endDateTime = endDateTime;

            //first check and see if the flight had a start and end time, if not we cannot process it
            System.out.println("Getting info for flight with start date time: " + startDateTime + " and end date time: " + endDateTime);

            if (startDateTime == null || endDateTime == null) {
                //flight didnt have a start or end time
                valid = false;
                return;
            }

            //then check and see if this was actually a flight (RPM > 800)
            Pair<Double,Double> minMaxRPM1 = DoubleTimeSeries.getMinMax(connection, flightId, "E1 RPM");
            Pair<Double,Double> minMaxRPM2 = DoubleTimeSeries.getMinMax(connection, flightId, "E2 RPM");

            if (minMaxRPM1 != null) System.out.println("min max E1 RPM: " + minMaxRPM1.first() + ", " + minMaxRPM1.second());
            if (minMaxRPM2 != null) System.out.println("min max E2 RPM: " + minMaxRPM2.first() + ", " + minMaxRPM2.second());
            
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

            if (minMaxLatitude != null) System.out.println("min max latitude: " + minMaxLatitude.first() + ", " + minMaxLatitude.second());
            if (minMaxLongitude != null) System.out.println("min max longitude: " + minMaxLongitude.first() + ", " + minMaxLongitude.second());

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

            if (minMaxAltMSL != null) System.out.println("min max alt MSL: " + minMaxAltMSL.first() + ", " + minMaxAltMSL.second());

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

        public void getSeriesData(Connection connection) throws SQLException {
            //get the time series data for altitude, latitude and longitude
            DoubleTimeSeries altMSLSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "AltMSL");
            DoubleTimeSeries latitudeSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "Latitude");
            DoubleTimeSeries longitudeSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "Longitude");

            altitudeMSL = altMSLSeries.innerArray();
            latitude = latitudeSeries.innerArray();
            longitude = longitudeSeries.innerArray();

            //calculate the epoch time for each row as longs so they can most be quickly compared
            StringTimeSeries dateSeries = StringTimeSeries.getStringTimeSeries(connection, flightId, "Lcl Date");
            StringTimeSeries timeSeries = StringTimeSeries.getStringTimeSeries(connection, flightId, "Lcl Time");
            StringTimeSeries utcOffsetSeries = StringTimeSeries.getStringTimeSeries(connection, flightId, "UTCOfst");

            System.out.println("date length: " + dateSeries.size() + ", time length: " + timeSeries.size() + ", utc length: " + utcOffsetSeries.size());
            int length = dateSeries.size();

            epochTime = new long[length];
            for (int i = 0; i < length; i++) {
                if (dateSeries.get(i) == null || dateSeries.get(i).equals("")
                        || timeSeries.get(i) == null || timeSeries.get(i).equals("")
                        || utcOffsetSeries.get(i) == null || utcOffsetSeries.get(i).equals("")) {
                    epochTime[i] = 0;
                    continue;
                }

                System.out.print("row " + i  + ": " + dateSeries.get(i) + " " + timeSeries.get(i) + " " + utcOffsetSeries.get(i));

                // create a LocalDateTime using the date time passed as parameter
                LocalDateTime ldt = LocalDateTime.parse(dateSeries.get(i) + " " + timeSeries.get(i), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                // parse the offset
                ZoneOffset zoneOffset = ZoneOffset.of(utcOffsetSeries.get(i));

                // create an OffsetDateTime using the parsed offset
                OffsetDateTime odt = OffsetDateTime.of(ldt, zoneOffset);

                // print the date time with the parsed offset
                //System.out.println(zoneOffset.toString() + ":\t" + odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

                // create a ZonedDateTime from the OffsetDateTime and use UTC as time zone
                ZonedDateTime utcZdt = odt.atZoneSameInstant(ZoneOffset.UTC);

                // print the date time in UTC using the ISO ZONED DATE TIME format
                System.out.print(" -- UTC (zoned):\t" + utcZdt.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));

                // and then print it again using your desired format
                System.out.println(" -- UTC:\t" + utcZdt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " -- epoch second: " + utcZdt.toEpochSecond());

                epochTime[i] = utcZdt.toEpochSecond();
            }
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
    }

    static String timeSeriesName = "Lcl Time";
    static String dateSeriesName = "Lcl Date";
    
    public static void processFlightWithError(Connection connection, int fleetId, int flightId) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement("INSERT INTO flight_processed SET fleet_id = ?, flight_id = ?, event_definition_id = ?, count = 0, had_error = 1");
        stmt.setInt(1, fleetId);
        stmt.setInt(2, flightId);
        stmt.setInt(3, adjacencyEventDefinitionId);
        System.out.println(stmt.toString());
        stmt.executeUpdate();
        stmt.close();
    }

    public static void processFlight(Connection connection, Flight flight) {
        System.out.println("Processing flight: " + flight.getId() + ", " + flight.getFilename());


        int fleetId = flight.getFleetId();
        int flightId = flight.getId();
        int airframeId = flight.getAirframeId();

        try {
            //get enough information about the flight to determine if we can calculate adjacencies with it
            FlightTimeLocation flightInfo = new FlightTimeLocation(connection, flightId, flight.getStartDateTime(), flight.getEndDateTime());

            if (!flightInfo.isValid()) {
                System.exit(1);
                processFlightWithError(connection, fleetId, flightId);
                return;
            }

            ArrayList<Flight> potentialFlights = Flight.getFlights(connection, "(id != " + flightId + " AND start_time <= '" + flightInfo.endDateTime + "' AND end_time >= '" + flightInfo.startDateTime + "')");

            System.out.println("Found " + potentialFlights.size() + " potential time matched flights.");
            System.out.println("Flight start time: " + flightInfo.startDateTime + ", end time: " + flightInfo.endDateTime);
            System.out.println("Flight latitude min: " + flightInfo.minLatitude + ", max: " + flightInfo.maxLatitude);
            System.out.println("Flight longitude min: " + flightInfo.minLongitude + ", max: " + flightInfo.maxLongitude);

            for (Flight otherFlight : potentialFlights) {
                System.out.println("\tmatched to flight with start time: " + otherFlight.getStartDateTime() + ", end time: " + otherFlight.getEndDateTime());

                FlightTimeLocation otherInfo = new FlightTimeLocation(connection, otherFlight.getId(), otherFlight.getStartDateTime(), otherFlight.getEndDateTime());
                if (!otherInfo.isValid()) {
                    //matched flight did not have all the information necessary to compute adjacency
                    continue;
                }

                System.out.println("\t\tother latitude min: " + otherInfo.minLatitude + ", max: " + otherInfo.maxLatitude);
                System.out.println("\t\tother longitude min: " + otherInfo.minLongitude + ", max: " + otherInfo.maxLongitude);

                if (flightInfo.hasRegionOverlap(otherInfo)) {
                    System.out.println("\t\tLatitude/Longitude overlap!");

                    if (!flightInfo.hasSeriesData()) {
                        flightInfo.getSeriesData(connection);
                    }

                    otherInfo.getSeriesData(connection);


                    int i = 0, j = 0;

                    int totalMatches = 0;
                    System.out.println("\t\tgot series data for both flights, iterate over times");
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
                        if (distanceFt < 1000.0) {
                            System.out.println("\t\t\tother time[" + j + "]: " + otherInfo.epochTime[j] + " == flight time[" + i + "]: " + flightInfo.epochTime[i]
                                    + ", flight lat/lon: " + flightInfo.latitude[i] + " " + flightInfo.longitude[i] + ", other lat/lon: " + otherInfo.latitude[j] + " " + otherInfo.longitude[j]
                                    + " -- distance: " + distanceFt
                                    );

                            double altDiff = Math.abs(flightInfo.altitudeMSL[i] - otherInfo.altitudeMSL[j]);

                            distanceFt = Math.sqrt((distanceFt * distanceFt) + (altDiff * altDiff));
                            System.out.println("\t\t\t\t\tflight alt msl: " + flightInfo.altitudeMSL[i] + ", other alt msl: " + otherInfo.altitudeMSL[j] + ", final distance: " + distanceFt);
                        }

                        //iterate both as they had matching times
                        i++;
                        j++;
                        totalMatches++;
                    }
                    System.out.println("\t\tseries matched time on " + totalMatches + " rows");
                }
            }

            System.exit(1);

            /*


            TreeSet<String> columnNames = eventDefinition.getColumnNames();
            System.out.println("Number of Column Name(s): [ " + columnNames.size() + " ]");

            //first test and see if min/max values can violate exceedence, otherwise we can skip
            conditional.reset();
            for (String columnName : columnNames) {
                Pair<Double,Double> minMax = DoubleTimeSeries.getMinMax(connection, flightId, columnName);

                if (minMax == null) {
                    //couldn't calculate this exceedence because at least one of the columns was missing
                    PreparedStatement stmt = connection.prepareStatement("INSERT INTO flight_processed SET fleet_id = ?, flight_id = ?, event_definition_id = ?, count = 0, had_error = 1");
                    stmt.setInt(1, fleetId);
                    stmt.setInt(2, flightId);
                    stmt.setInt(3, eventDefinition.getId());
                    System.out.println(stmt.toString());
                    stmt.executeUpdate();
                    stmt.close();
                    return;
                }

                System.out.println(columnName + ", min: " + minMax.first() + ", max: " + minMax.second());
                conditional.set(columnName, minMax);
            }

            System.out.println("Post-set conditional: " + conditional.toString());
            boolean result = conditional.evaluate();
            System.out.println("overall result: " + result);

            if (!result) {
                //this flight could not have caused one of these events
                PreparedStatement stmt = connection.prepareStatement("INSERT INTO flight_processed SET fleet_id = ?, flight_id = ?, event_definition_id = ?, count = 0, had_error = 0");
                stmt.setInt(1, fleetId);
                stmt.setInt(2, flightId);
                stmt.setInt(3, eventDefinition.getId());
                System.out.println(stmt.toString());
                stmt.executeUpdate();
                stmt.close();

                EventStatistics.updateFlightsWithoutEvent(connection, fleetId, airframeId, eventDefinition.getId(), flight.getStartDateTime());
                return;
            }

            StringTimeSeries timeSeries = StringTimeSeries.getStringTimeSeries(connection, flightId, timeSeriesName);
            StringTimeSeries dateSeries = StringTimeSeries.getStringTimeSeries(connection, flightId, dateSeriesName);

            if (timeSeries == null || dateSeries == null) {
                //couldn't calculate this exceedence because the date or time column was missing
                PreparedStatement stmt = connection.prepareStatement("INSERT INTO flight_processed SET fleet_id = ?, flight_id = ?, event_definition_id = ?, count = 0, had_error = 1");
                stmt.setInt(1, fleetId);
                stmt.setInt(2, flightId);
                stmt.setInt(3, eventDefinition.getId());
                System.out.println(stmt.toString());
                stmt.executeUpdate();
                stmt.close();
                return;
            }

            DoubleTimeSeries[] doubleSeries = new DoubleTimeSeries[ columnNames.size() ];
            int i = 0;
            for (String columnName : columnNames) {
                doubleSeries[i++] = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, columnName);
            }


            //Step 1: Calculate all the pitch events and put them in this pitchEvents ArrayList
            ArrayList<Event> eventList = new ArrayList<>();
            int lineNumber = 0;
            String startTime = null;
            String endTime = null;
            int startLine = -1;
            int endLine = -1;

            int startCount = 0;
            int stopCount = 0;
            double severity = 0;

            //skip the first 30 seconds as that is usually the FDR being initialized
            for (i = 30; i < doubleSeries[0].size(); i++) {
            //for (i = 0; i < doubleSeries[0].size(); i++) {
                lineNumber = i;
                double currentValue = doubleSeries[0].get(i);

                //System.out.println("Pre-set conditional: " + conditional.toString());

                conditional.reset();
                for (DoubleTimeSeries series : doubleSeries) {
                    conditional.set(series.getName(), series.get(i));
                }
                //System.out.println("Post-set conditional: " + conditional.toString());

                result = conditional.evaluate();

                //System.out.println(conditional + ", result: " +  result);

                if (!result) {
                    if (startTime != null) {
                        //we're tracking an event, so increment the stopCount
                        stopCount++;
                        System.out.println("stopCount: " + stopCount + " with on line: " + lineNumber );

                        if (stopCount == stopBuffer) {
                            System.err.println("Stop count (" + stopCount + ") reached the stop buffer (" + stopBuffer + "), new event created!");

                            if (startCount < startBuffer) {
                                //we didn't have enough triggers to reach the start count so don't create
                                //the event
                            } else {
                                //we had enough triggers to reach the start count so create the event
                                Event event = new Event (startTime, endTime, startLine, endLine, severity);
                                eventList.add(event);
                            }

                            //reset the event values
                            startTime = null;
                            endTime = null;
                            startLine = -1;
                            endLine = -1;

                            //reset the start and stop counts
                            startCount = 0;
                            stopCount = 0;
                        }
                    }
                } else {
                    //row triggered exceedence

                    //startTime is null if an exceedence is not being tracked
                    if (startTime == null) {
                        startTime = dateSeries.get(i) + " " + timeSeries.get(i);
                        startLine = lineNumber;
                        severity = eventDefinition.getSeverity(doubleSeries, i);

                        System.out.println("start date time: " + startTime + ", start line number: " + startLine);
                    }
                    endLine = lineNumber;
                    endTime = dateSeries.get(i) + " " + timeSeries.get(i);
                    severity = eventDefinition.updateSeverity(severity, doubleSeries, i);

                    //increment the startCount, reset the endCount
                    startCount++;
                    stopCount = 0;
                }
            }

            if (startTime != null) {
                Event event = new Event(startTime, endTime, startLine, endLine, severity);
                eventList.add( event );
            }
            System.out.println("");

            for (i = 0; i < eventList.size(); i++) {
                Event event = eventList.get(i);
                System.out.println( "Event : [line: " + event.getStartLine() + " to " + event.getEndLine() + ", time: " + event.getStartTime() + " to " + event.getEndTime() + "]" );
            }

            //Step 2: export the pitch events to the database
            double sumDuration = 0.0;
            double sumSeverity = 0.0;
            double minSeverity = Double.MAX_VALUE;
            double maxSeverity = -Double.MAX_VALUE;
            double minDuration = Double.MAX_VALUE;
            double maxDuration = -Double.MAX_VALUE;
            for (i = 0; i < eventList.size(); i++) {
                Event event = eventList.get(i);
                event.updateDatabase(connection, fleetId, flightId, eventDefinition.getId());
                if (event.getStartTime() != null) {
                    EventStatistics.updateEventStatistics(connection, fleetId, airframeId, eventDefinition.getId(), event.getStartTime(), event.getSeverity(), event.getDuration());
                } else if (event.getEndTime() != null) {
                    EventStatistics.updateEventStatistics(connection, fleetId, airframeId, eventDefinition.getId(), event.getEndTime(), event.getSeverity(), event.getDuration());
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
                stmt.setInt(3, eventDefinition.getId());
                stmt.setInt(4, eventList.size());
                stmt.setDouble(5, sumDuration);
                stmt.setDouble(6, minDuration);
                stmt.setDouble(7, maxDuration);
                stmt.setDouble(8, sumSeverity);
                stmt.setDouble(9, minSeverity);
                stmt.setDouble(10, maxSeverity);
                System.out.println(stmt.toString());
                stmt.executeUpdate();
                stmt.close();

                EventStatistics.updateFlightsWithEvent(connection, fleetId, airframeId, eventDefinition.getId(), flight.getStartDateTime());

            } else {
                PreparedStatement stmt = connection.prepareStatement("INSERT INTO flight_processed SET fleet_id = ?, flight_id = ?, event_definition_id = ?, count = 0, had_error = 0");
                stmt.setInt(1, fleetId);
                stmt.setInt(2, flightId);
                stmt.setInt(3, eventDefinition.getId());
                System.out.println(stmt.toString());
                stmt.executeUpdate();
                stmt.close();

                EventStatistics.updateFlightsWithoutEvent(connection, fleetId, airframeId, eventDefinition.getId(), flight.getStartDateTime());
            }
            */

        } catch(SQLException e) {
            System.err.println(e);
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] arguments) {
        try {
            Connection connection = Database.getConnection();

            while (true) {
                Instant start = Instant.now();

                ArrayList<Flight> flights = Flight.getFlights(connection, "NOT EXISTS (SELECT flight_id FROM flight_processed WHERE event_definition_id = " + adjacencyEventDefinitionId + " AND flight_processed.flight_id = flights.id)", 100);

                int count = 0;
                for (int j = 0; j < flights.size(); j++) {
                    if (!flights.get(j).insertCompleted()) {
                        //this flight is currently being inserted to
                        //the database by ProcessFlights
                        continue;
                    }

                    processFlight(connection, flights.get(j));
                    count++;
                }

                Instant end = Instant.now();
                long elapsed_millis = Duration.between(start, end).toMillis();
                double elapsed_seconds = ((double) elapsed_millis) / 1000;
                System.out.println("calculated " + count + " adjacency evaluations in " + elapsed_seconds);

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
