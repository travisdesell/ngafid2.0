package org.ngafid;

import org.ngafid.Database;
import org.ngafid.events.Event;
import org.ngafid.flights.Airframes;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.Flight;
import org.ngafid.flights.StringTimeSeries;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.util.TreeSet;

import org.ngafid.events.EventDefinition;
import org.ngafid.events.EventStatistics;

import org.ngafid.filters.Conditional;
import org.ngafid.filters.Filter;
import org.ngafid.filters.Pair;

public class CalculateExceedences {

    static String timeSeriesName = "Lcl Time";
    static String dateSeriesName = "Lcl Date";

    private EventDefinition eventDefinition;
    private Filter filter;
    private Conditional conditional;
    private int startBuffer;
    private int stopBuffer;

    public CalculateExceedences(EventDefinition eventDefinition) {
        this.eventDefinition = eventDefinition;
        this.filter = eventDefinition.getFilter();
        this.conditional = new Conditional(filter);
        this.startBuffer = eventDefinition.getStartBuffer();
        this.stopBuffer = eventDefinition.getStopBuffer();
    }

    public void processFlight(Connection connection, Flight flight, EventDefinition eventDefinition, UploadProcessedEmail uploadProcessedEmail) {
        int fleetId = flight.getFleetId();
        int flightId = flight.getId();
        int airframeNameId = flight.getAirframeNameId();
        String flightFilename = flight.getFilename();

        System.out.println("Processing flight: " + flightId + ", " + flightFilename);

        try {
            System.out.println("Event is: '" + eventDefinition.getName() + "'");

            //first check and see if this was actually a flight (RPM > 800)
            Pair<Double,Double> minMaxRPM1 = DoubleTimeSeries.getMinMax(connection, flightId, "E1 RPM");
            Pair<Double,Double> minMaxRPM2 = DoubleTimeSeries.getMinMax(connection, flightId, "E2 RPM");

            System.out.println("minMaxRPM1: " + minMaxRPM1);
            System.out.println("minMaxRPM2: " + minMaxRPM2);

            if ((minMaxRPM1 == null && minMaxRPM2 == null)  //both RPM values are null, can't calculate exceedence
                    || (minMaxRPM2 == null && minMaxRPM1 != null && minMaxRPM1.second() < 800) //RPM2 is null, RPM1 is < 800
                    || (minMaxRPM1 == null && minMaxRPM2 != null && minMaxRPM2.second() < 800) //RPM1 is null, RPM2 is < 800
                    || (minMaxRPM1 != null && minMaxRPM1.second() < 800) && (minMaxRPM2 != null && minMaxRPM2.second() < 800)) { //RPM1 and RPM2 < 800
                //couldn't calculate exceedences for this flight because the engines never kicked on (it didn't fly)
                System.out.println("engines never turned on, setting flight_processed.had_error = 1");

                if (uploadProcessedEmail != null) uploadProcessedEmail.addExceedenceError(flightFilename, "could not calculate exceedences for flight " + flightId + ", '" + flightFilename + "' - engines never turned on");

                PreparedStatement stmt = connection.prepareStatement("INSERT INTO flight_processed SET fleet_id = ?, flight_id = ?, event_definition_id = ?, count = 0, had_error = 1");
                stmt.setInt(1, fleetId);
                stmt.setInt(2, flightId);
                stmt.setInt(3, eventDefinition.getId());
                System.out.println(stmt.toString());
                stmt.executeUpdate();
                stmt.close();
                return;
            }

            TreeSet<String> columnNames = eventDefinition.getColumnNames();
            System.out.println("Number of Column Name(s): [ " + columnNames.size() + " ]");

            //first test and see if min/max values can violate exceedence, otherwise we can skip
            conditional.reset();
            for (String columnName : columnNames) {
                Pair<Double,Double> minMax = DoubleTimeSeries.getMinMax(connection, flightId, columnName);

                if (minMax == null) {
                    System.out.println("minMax for '" + columnName + "' was null, setting flight_processed.had_error = 1");
                    //couldn't calculate this exceedence because at least one of the columns was missing
                    if (uploadProcessedEmail != null) uploadProcessedEmail.addExceedenceError(flightFilename, "could not calculate '" + eventDefinition.getName() + "' for flight " + flightId + ", '" + flightFilename + "' - " + columnName + " was missing");

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

                EventStatistics.updateFlightsWithoutEvent(connection, fleetId, airframeNameId, eventDefinition.getId(), flight.getStartDateTime());
                return;
            }

            StringTimeSeries timeSeries = StringTimeSeries.getStringTimeSeries(connection, flightId, timeSeriesName);
            StringTimeSeries dateSeries = StringTimeSeries.getStringTimeSeries(connection, flightId, dateSeriesName);

            if (timeSeries == null || dateSeries == null) {
                //couldn't calculate this exceedence because the date or time column was missing
                System.out.println("time series or date series was missing, setting flight_processed.had_error = 1");
                if (uploadProcessedEmail != null) uploadProcessedEmail.addExceedenceError(flightFilename, "could not calculate exceedences for flight " + flightId + ", '" + flightFilename + "' - date or time was missing");

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
                                Event event = new Event(startTime, endTime, startLine, endLine, severity);
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
                if (uploadProcessedEmail != null) uploadProcessedEmail.addExceedence(flightFilename, "flight " + flightId + ", '" + flightFilename + "' - '" + eventDefinition.getName() + "' from " + event.getStartTime() + " to " + event.getEndTime());
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
                event.updateStatistics(connection, fleetId, airframeNameId, eventDefinition.getId());

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

                EventStatistics.updateFlightsWithEvent(connection, fleetId, airframeNameId, eventDefinition.getId(), flight.getStartDateTime());

            } else {
                PreparedStatement stmt = connection.prepareStatement("INSERT INTO flight_processed SET fleet_id = ?, flight_id = ?, event_definition_id = ?, count = 0, had_error = 0");
                stmt.setInt(1, fleetId);
                stmt.setInt(2, flightId);
                stmt.setInt(3, eventDefinition.getId());
                System.out.println(stmt.toString());
                stmt.executeUpdate();
                stmt.close();

                EventStatistics.updateFlightsWithoutEvent(connection, fleetId, airframeNameId, eventDefinition.getId(), flight.getStartDateTime());
            }

        } catch(SQLException e) {
            System.err.println(e);
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static ArrayList<EventDefinition> allEvents = null;

    public static void calculateExceedences(Connection connection, int uploadId, UploadProcessedEmail uploadProcessedEmail) throws SQLException {
        Instant start = Instant.now();
        if (allEvents == null) {
            allEvents = EventDefinition.getAll(connection, "id > ?", new Object[]{0});
        }
        System.out.println("n events = " + allEvents.size());

        int airframeTypeId = Airframes.getTypeId(connection, "Fixed Wing");

        for (int i = 0; i < allEvents.size(); i++) {
            //process events for this event type
            EventDefinition currentDefinition = allEvents.get(i);
            System.out.println("\t" + currentDefinition.toString());

            CalculateExceedences currentCalculator = new CalculateExceedences(currentDefinition);

            ArrayList<Flight> flights = null;

            if (currentDefinition.getAirframeNameId() == 0) {
                flights = Flight.getFlights(connection, "airframe_type_id = " + airframeTypeId + " AND upload_id = " + uploadId + " AND NOT EXISTS (SELECT flight_id FROM flight_processed WHERE event_definition_id = " + currentDefinition.getId() + " AND flight_processed.flight_id = flights.id)");
            } else {
                flights = Flight.getFlights(connection, "flights.airframe_id = " + currentDefinition.getAirframeNameId() + " AND upload_id = " + uploadId + " AND airframe_type_id = " + airframeTypeId + " AND NOT EXISTS (SELECT flight_id FROM flight_processed WHERE event_definition_id = " + currentDefinition.getId() + " AND flight_processed.flight_id = flights.id)");
            }

            for (int j = 0; j < flights.size(); j++) {
                if (!flights.get(j).insertCompleted()) {
                    //this flight is currently being inserted to
                    //the database by ProcessFlights
                    continue;
                }

                currentCalculator.processFlight(connection, flights.get(j), currentDefinition, uploadProcessedEmail);
            }
        }

        Instant end = Instant.now();
        long elapsed_millis = Duration.between(start, end).toMillis();
        double elapsed_seconds = ((double) elapsed_millis) / 1000;
        System.out.println("finished in " + elapsed_seconds);

        if (uploadProcessedEmail != null) uploadProcessedEmail.setExceedencesElapsedTime(elapsed_seconds);
    }

    public static void main(String[] arguments) {
        try {
            Connection connection = Database.resetConnection();

            //for now only calculate exceedences for fixed wing aircraft
            int airframeTypeId = Airframes.getTypeId(connection, "Fixed Wing");

            while (true) {
                connection = Database.resetConnection();
                Instant start = Instant.now();
                //ArrayList<EventDefinition> allEvents = EventDefinition.getAll(connection, "id > ?", new Object[]{0});
                //
                ArrayList<EventDefinition> allEvents = EventDefinition.getAll(connection, "id IN (?,?,?,?,?,?,?,?,?,?)", new Object[]{39, 40, 41, 42, 43, 44, 45, 65, 67, 72});

                System.out.println("n events = " + allEvents.size());
                for (int i = 0; i < allEvents.size(); i++) {
                    //process events for this event type
                    EventDefinition currentDefinition = allEvents.get(i);
                    System.out.println("\t" + currentDefinition.toString());

                    CalculateExceedences currentCalculator = new CalculateExceedences(currentDefinition);

                    ArrayList<Flight> flights = null;
                    
                    if (currentDefinition.getAirframeNameId() == 0) {
                        flights = Flight.getFlights(connection, "airframe_type_id = " + airframeTypeId + " AND NOT EXISTS (SELECT flight_id FROM flight_processed WHERE event_definition_id = " + currentDefinition.getId() + " AND flight_processed.flight_id = flights.id)", 100);
                    } else {
                        flights = Flight.getFlights(connection, "flights.airframe_id = " + currentDefinition.getAirframeNameId() + " AND airframe_type_id = " + airframeTypeId + " AND NOT EXISTS (SELECT flight_id FROM flight_processed WHERE event_definition_id = " + currentDefinition.getId() + " AND flight_processed.flight_id = flights.id)", 100);
                    }

                    for (int j = 0; j < flights.size(); j++) {
                        if (!flights.get(j).insertCompleted()) {
                            //this flight is currently being inserted to
                            //the database by ProcessFlights
                            continue;
                        }

                        currentCalculator.processFlight(connection, flights.get(j), currentDefinition, null);
                    }
                }

                Instant end = Instant.now();
                long elapsed_millis = Duration.between(start, end).toMillis();
                double elapsed_seconds = ((double) elapsed_millis) / 1000;
                System.out.println("finished in " + elapsed_seconds);

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
