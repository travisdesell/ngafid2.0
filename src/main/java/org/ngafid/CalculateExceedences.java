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

import java.util.ArrayList;
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

    public void processFlight(Connection connection, Flight flight, EventDefinition eventDefinition) {
        System.out.println("Processing flight: " + flight.getId() + ", " + flight.getFilename());

        int fleetId = flight.getFleetId();
        int flightId = flight.getId();
        
        try {
            System.out.println("Event is: '" + eventDefinition.getName() + "'");

            //first check and see if this was actually a flight (RPM > 800)
            Pair<Double,Double> minMaxRPM1 = DoubleTimeSeries.getMinMax(connection, flightId, "E1 RPM");
            Pair<Double,Double> minMaxRPM2 = DoubleTimeSeries.getMinMax(connection, flightId, "E2 RPM");

            if ((minMaxRPM1 == null && minMaxRPM2 == null)  //both RPM values are null, can't calculate exceedence
                    || (minMaxRPM2 == null && (minMaxRPM1 != null && minMaxRPM1.second() < 800)) //RPM2 is null, RPM1 is < 800
                    || (minMaxRPM1 == null && (minMaxRPM2 != null && minMaxRPM2.second() < 800)) //RPM1 is null, RPM2 is < 800
                    || ((minMaxRPM1.second() < 800) && (minMaxRPM2.second() < 800))) { //RPM1 and RPM2 < 800
                //couldn't calculate exceedences for this flight because the engines never kicked on (it didn't fly)
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

                EventStatistics.updateFlightsWithoutEvent(connection, fleetId, eventDefinition.getId(), flight.getStartDateTime());
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
                event.updateDatabase(connection, flightId, eventDefinition.getId());
                if (event.getStartTime() != null) {
                    EventStatistics.updateEventStatistics(connection, fleetId, eventDefinition.getId(), event.getStartTime(), event.getSeverity(), event.getDuration());
                } else if (event.getEndTime() != null) {
                    EventStatistics.updateEventStatistics(connection, fleetId, eventDefinition.getId(), event.getEndTime(), event.getSeverity(), event.getDuration());
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

                EventStatistics.updateFlightsWithEvent(connection, fleetId, eventDefinition.getId(), flight.getStartDateTime());

            } else {
                PreparedStatement stmt = connection.prepareStatement("INSERT INTO flight_processed SET fleet_id = ?, flight_id = ?, event_definition_id = ?, count = 0, had_error = 0");
                stmt.setInt(1, fleetId);
                stmt.setInt(2, flightId);
                stmt.setInt(3, eventDefinition.getId());
                System.out.println(stmt.toString());
                stmt.executeUpdate();
                stmt.close();

                EventStatistics.updateFlightsWithoutEvent(connection, fleetId, eventDefinition.getId(), flight.getStartDateTime());
            }

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
                ArrayList<EventDefinition> allEvents = EventDefinition.getAll(connection);

                for (int i = 0; i < allEvents.size(); i++) {
                    //process events for this event type
                    EventDefinition currentDefinition = allEvents.get(i);
                    System.out.println("\t" + currentDefinition.toString());

                    CalculateExceedences currentCalculator = new CalculateExceedences(currentDefinition);

                    ArrayList<Flight> flights = null;
                    
                    if (currentDefinition.getAirframeId() == 0) {
                        flights = Flight.getFlights(connection, "NOT EXISTS (SELECT flight_id FROM flight_processed WHERE event_definition_id = " + currentDefinition.getId() + " AND flight_processed.flight_id = flights.id)", 100);
                    } else {
                        flights = Flight.getFlights(connection, "flights.airframe_id = " + currentDefinition.getAirframeId() + " AND NOT EXISTS (SELECT flight_id FROM flight_processed WHERE event_definition_id = " + currentDefinition.getId() + " AND flight_processed.flight_id = flights.id)", 100);
                    }

                    for (int j = 0; j < flights.size(); j++) {
                        if (!flights.get(j).insertCompleted()) {
                            //this flight is currently being inserted to
                            //the database by ProcessFlights
                            continue;
                        }

                        currentCalculator.processFlight(connection, flights.get(j), currentDefinition);
                    }
                }

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
