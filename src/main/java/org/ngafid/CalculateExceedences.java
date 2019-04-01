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
import java.util.HashSet;

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

    double maxArray(double[] array) {
        double max = -Double.MAX_VALUE;

        for (int i = 0; i < array.length; i++) {
            if (Double.isNaN(array[i])) continue;
            max = Math.max(array[i], max);
        }
        return max;
    }

    double minArray(double[] array) {
        double min = Double.MAX_VALUE;

        for (int i = 0; i < array.length; i++) {
            if (Double.isNaN(array[i])) continue;
            min = Math.min(array[i], min);
        }
        return min;
    }


    private double getSeverity(int eventId, DoubleTimeSeries[] doubleSeries, int i) {
        double value;

        switch (eventId) {
            case 1: /*low pitch*/
            case 2: /*high pitch*/
            case 3: /*low lateral acceleration*/
            case 4: /*high lateral acceleration*/
            case 5: /*low vertical acceleration*/
            case 6: /*high vertical acceleration*/
                return doubleSeries[0].get(i);

            case 7: /*roll*/
                return Math.abs(doubleSeries[0].get(i));

            case 8: /*VSI on final*/
                return doubleSeries[0].get(i);

            case 9: /*airspeed C172*/
            case 11: /*airspeed PA-28*/
            case 12: /*airspeed PA-44*/
            case 13: /*airspeed SR20*/
                return doubleSeries[0].get(i);

            case 14: /*altitude C172*/
            case 15: /*altitude SR20*/
            case 16: /*altitude PA-28*/
            case 17: /*altitude PA-44*/
                return doubleSeries[0].get(i);

            case 21: /*CHT C172*/
                //CHTs 1-4
                return maxArray(new double[]{doubleSeries[0].get(i), doubleSeries[1].get(i), doubleSeries[2].get(i), doubleSeries[3].get(i)});
            case 22: /*CHT SR20*/
                //CHTs 1-6
                return maxArray(new double[]{doubleSeries[0].get(i), doubleSeries[1].get(i), doubleSeries[2].get(i), doubleSeries[3].get(i), doubleSeries[4].get(i), doubleSeries[5].get(i)});
            case 23: /*CHT PA-28*/
                //CHTs 1-2
                return Math.max(doubleSeries[0].get(i), doubleSeries[1].get(i));

            case 24: /*low oil pressure C172*/
            case 25: /*low oil pressure SR20*/
            case 26: /*low oil pressure PA-28*/
                return doubleSeries[0].get(i);
            case 27: /*low oil pressure PA-44*/
                return Math.min(doubleSeries[0].get(i), doubleSeries[2].get(i));

            case 28: /*low fuel C172*/
            case 29: /*low fuel PA-28*/
            case 30: /*low fuel PA-44*/
                return doubleSeries[1].get(i); //total fuel series

            case 31: /*low airspeed on approach C172*/
            case 32: /*low airspeed on approach PA28*/
            case 33: /*low airspeed on approach PA44*/
            case 34: /*low airspeed on approach SR20*/
                return doubleSeries[1].get(i); //IAS series

            case 35: /*low airspeed on climbout C172*/
            case 36: /*low airspeed on climbout PA28*/
            case 37: /*low airspeed on climbout PA44*/
            case 38: /*low airspeed on climbout SR20*/
                return doubleSeries[1].get(i); //IAS series

            case 39: /*CHT variance C172*/
            case 40: /*CHT variance PA28*/
            case 41: /*CHT variance SR20*/
                return doubleSeries[0].get(i);

            case 42: /*EGT variance C172*/
            case 43: /*EGT variance PA28*/
            case 44: /*EGT variance SR20*/
                return doubleSeries[0].get(i);

            case 45: /*EGT variance PA44*/
                return maxArray(new double[]{doubleSeries[0].get(i), doubleSeries[1].get(i)}); //max variance between engines

            case 46: /*Engine Shutdown C172*/
            case 47: /*Engine Shutdown PA28*/
            case 48: /*Engine Shutdown SR20*/
                return doubleSeries[0].get(i);

            case 49: /*Engine Shutdown PA44*/
                return minArray(new double[]{doubleSeries[0].get(i), doubleSeries[1].get(i)}); //min RPM between engines


            default:
                System.err.println("Could not get severity for unknown event id: " + eventId);
                System.exit(1);
        }

        return 0;
    }

    private double updateSeverity(double currentSeverity, int eventId, DoubleTimeSeries[] doubleSeries, int i) {
        switch (eventId) {
            case 1: /*low pitch*/
            case 3: /*low lateral acceleration*/
            case 5: /*low vertical acceleration*/
                return Math.min(currentSeverity, getSeverity(eventId, doubleSeries, i));

            case 2: /*high pitch*/
            case 4: /*high lateral acceleration*/
            case 6: /*high vertical acceleration*/
                return Math.max(currentSeverity, getSeverity(eventId, doubleSeries, i));

            case 7: /*roll*/
                return Math.max(currentSeverity, getSeverity(eventId, doubleSeries, i));

            case 8: /*VSI on final*/
                return Math.min(currentSeverity, getSeverity(eventId, doubleSeries, i));

            case 9: /*airspeed C172*/
            case 11: /*airspeed PA-28*/
            case 12: /*airspeed PA-44*/
            case 13: /*airspeed SR20*/
                return Math.max(currentSeverity, getSeverity(eventId, doubleSeries, i));

            case 14: /*altitude C172*/
            case 15: /*altitude SR20*/
            case 16: /*altitude PA-28*/
            case 17: /*altitude PA-44*/
                return Math.max(currentSeverity, getSeverity(eventId, doubleSeries, i));

            case 21: /*CHT C172*/
            case 22: /*CHT SR20*/
            case 23: /*CHT PA-28*/
                return Math.max(currentSeverity, getSeverity(eventId, doubleSeries, i));

            case 24: /*low oil pressure C172*/
            case 25: /*low oil pressure SR20*/
            case 26: /*low oil pressure PA-28*/
            case 27: /*low oil pressure PA-44*/
                return Math.min(currentSeverity, getSeverity(eventId, doubleSeries, i));

            case 28: /*low fuel C172*/
            case 29: /*low fuel PA-28*/
            case 30: /*low fuel PA-44*/
                return Math.min(currentSeverity, getSeverity(eventId, doubleSeries, i));

            case 31: /*low airspeed on approach C172*/
            case 32: /*low airspeed on approach PA28*/
            case 33: /*low airspeed on approach PA44*/
            case 34: /*low airspeed on approach SR20*/
                return Math.min(currentSeverity, getSeverity(eventId, doubleSeries, i));

            case 35: /*low airspeed on climbout C172*/
            case 36: /*low airspeed on climbout PA28*/
            case 37: /*low airspeed on climbout PA44*/
            case 38: /*low airspeed on climbout SR20*/
                return Math.min(currentSeverity, getSeverity(eventId, doubleSeries, i));

            case 39: /*CHT variance C172*/
            case 40: /*CHT variance PA28*/
            case 41: /*CHT variance SR20*/
                return Math.max(currentSeverity, getSeverity(eventId, doubleSeries, i));

            case 42: /*EGT variance C172*/
            case 43: /*EGT variance PA28*/
            case 44: /*EGT variance SR20*/
                return Math.max(currentSeverity, getSeverity(eventId, doubleSeries, i));

            case 45: /*EGT variance PA44*/
                return Math.max(currentSeverity, getSeverity(eventId, doubleSeries, i));

            case 46: /*Engine Shutdown C172*/
            case 47: /*Engine Shutdown PA28*/
            case 48: /*Engine Shutdown SR20*/
                return Math.min(currentSeverity, getSeverity(eventId, doubleSeries, i));

            case 49: /*Engine Shutdown PA44*/
                return Math.min(currentSeverity, getSeverity(eventId, doubleSeries, i));

            default:
                System.err.println("Could not update severity for unknown event id: " + eventId);
                System.exit(1);
        }

        return 0;
    }

    public void processFlight(Connection connection, Flight flight, int eventId) {
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
                return;
            }

            HashSet<String> columnNames = eventDefinition.getColumnNames();
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

                EventStatistics.updateFlightsWithoutEvent(connection, fleetId, eventId, flight.getStartDateTime());
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

            for (i = 0; i < doubleSeries[0].size(); i++) {
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
                        severity = getSeverity(eventId, doubleSeries, i);

                        System.out.println("start date time: " + startTime + ", start line number: " + startLine);
                    }
                    endLine = lineNumber;
                    endTime = dateSeries.get(i) + " " + timeSeries.get(i);
                    severity = updateSeverity(severity, eventId, doubleSeries, i);

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
                EventStatistics.updateEventStatistics(connection, fleetId, eventId, event.getStartTime(), event.getSeverity(), event.getDuration());

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

                EventStatistics.updateFlightsWithEvent(connection, fleetId, eventId, flight.getStartDateTime());

            } else {
                PreparedStatement stmt = connection.prepareStatement("INSERT INTO flight_processed SET fleet_id = ?, flight_id = ?, event_definition_id = ?, count = 0, had_error = 0");
                stmt.setInt(1, fleetId);
                stmt.setInt(2, flightId);
                stmt.setInt(3, eventDefinition.getId());
                System.out.println(stmt.toString());
                stmt.executeUpdate();

                EventStatistics.updateFlightsWithoutEvent(connection, fleetId, eventId, flight.getStartDateTime());
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

                        currentCalculator.processFlight(connection, flights.get(j), currentDefinition.getId());
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
