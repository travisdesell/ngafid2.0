package org.ngafid.events;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import java.util.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.ngafid.flights.Airframes;

public class Event {
    private static final Logger LOG = Logger.getLogger(Event.class.getName());

    private int id;
    private int fleetId;
    private int flightId;
    private int eventDefinitionId;

    private String startTime;
    private String endTime;

    private int startLine;
    private int endLine;

    public double severity;

    private Integer otherFlightId = null;

    public Event(String startTime, String endTime, int startLine, int endLine, double severity) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.startLine = startLine;
        this.endLine = endLine;
        this.severity = severity;
        this.otherFlightId = null;
    }

    public Event(String startTime, String endTime, int startLine, int endLine, double severity, Integer otherFlightId) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.startLine = startLine;
        this.endLine = endLine;
        this.severity = severity;
        this.otherFlightId = otherFlightId;
    }


    /**
     * Creates an event from a mysql query result
     *
     * @param resultSet is the row selected from the database
     */
    public Event(ResultSet resultSet) throws SQLException {
        this.id = resultSet.getInt(1);
        this.fleetId = resultSet.getInt(1);
        this.flightId = resultSet.getInt(3);
        this.eventDefinitionId = resultSet.getInt(4);
        this.startLine = resultSet.getInt(5);
        this.endLine = resultSet.getInt(6);
        this.startTime = resultSet.getString(7);
        this.endTime = resultSet.getString(8);
        this.severity = resultSet.getDouble(9);
        this.otherFlightId = resultSet.getInt(10);
        if (resultSet.wasNull()) {
            this.otherFlightId = null;
        }
    }

    /**
     * Creates an event directly from all its fields
     *
     * @param id is the id of the event in the database
     * @param fleetId is the id of the fleet
     * @param flightId is the id of the flight of this event
     * @param startLine is the line the event starts in the data file of the flight
     * @param endLine is the line the event ends in the data file of the flight
     * @param startTime is the time the event starts in the data file of the flight
     * @param endTime is the time the event ends in the data file of the flight
     * @param severity is the severity rating for the event
     * @param otherFlightId is the other flight id (for proximity events)
     */
    public Event(int id, int fleetId, int flightId, int eventDefinitionId, int startLine, int endLine, String startTime, String endTime, double severity, Integer otherFlightId) {
        this.id = id;
        this.fleetId = fleetId;
        this.flightId = flightId;
        this.eventDefinitionId = eventDefinitionId;
        this.startLine = startLine;
        this.endLine = endLine;
        this.startTime = startTime;
        this.endTime = endTime;
        this.severity = severity;
        this.otherFlightId = otherFlightId;
    }

    public void updateEnd(String newEndTime, int newEndLine) {
        endTime = newEndTime;
        endLine = newEndLine;
    }

    public int getFlightId() {
        return flightId;
    }

    public int getEndLine() {
        return endLine;
    }

    public int getOtherFlightId() {
        return otherFlightId;
    }

    public String toString() {
        String readable =  "[line " + startLine + " to " + endLine + ", time " + startTime + " to " + endTime + ", severity: " + severity;
        if (otherFlightId != null) readable += ", other flight: " + otherFlightId;
        readable += "]";

        return readable;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public void setStartLine(int startLine) {
        this.startLine = startLine;
    }

    public void setEndLine(int endLine) {
        this.endLine = endLine;
    }

    public int getStartLine() {
        return startLine;
    }

    public int getId() {
        return id;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public double getSeverity() {
        return severity;
    }

    public int getDuration() {
        return (endLine - startLine) + 1;
    }

    public int getEventDefinitionId() {
        return this.eventDefinitionId;
    }

    public void updateStatistics(Connection connection, int fleetId, int airframeNameId, int eventDefinitionId) throws SQLException {
        if (this.getStartTime() != null) {
            EventStatistics.updateEventStatistics(connection, fleetId, airframeNameId, eventDefinitionId, this.getStartTime(), this.getSeverity(), this.getDuration());
        } else if (this.getEndTime() != null) {
            EventStatistics.updateEventStatistics(connection, fleetId, airframeNameId, eventDefinitionId, this.getEndTime(), this.getSeverity(), this.getDuration());
        } else {
            System.out.println("WARNING: could not update event statistics for event: " + this);
            System.out.println("WARNING: event start and end time were both null.");
        }
    }

    public void updateDatabase(Connection connection, int fleetId, int flightId, int eventDefinitionId) {
        this.flightId = flightId;
        this.eventDefinitionId = eventDefinitionId;

        try {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO events (fleet_id, flight_id, event_definition_id, start_line, end_line, start_time, end_time, severity, other_flight_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
            preparedStatement.setInt(1, fleetId);
            preparedStatement.setInt(2, flightId);
            preparedStatement.setInt(3, eventDefinitionId);
            preparedStatement.setInt(4, startLine);
            preparedStatement.setInt(5, endLine);

            if (startTime.equals(" ")) {
                preparedStatement.setString(6, null);
            } else {
                preparedStatement.setString(6, startTime);
            }

            if (endTime.equals(" ")) {
                preparedStatement.setString(7, null);
            } else {
                preparedStatement.setString(7, endTime);
            }

            preparedStatement.setDouble(8, severity);

            if (otherFlightId == null) {
                preparedStatement.setNull(9, java.sql.Types.INTEGER);
            } else {
                preparedStatement.setInt(9, otherFlightId);
            }

            System.err.println(preparedStatement);

            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            System.err.println("ERROR commiting event do database.");
            System.err.println("fleetId: " + fleetId);
            System.err.println("flightId: " + flightId);
            System.err.println("eventDefinitionId: " + eventDefinitionId);
            System.err.println("startLine: " + startLine);
            System.err.println("endLine: " + endLine);
            System.err.println("startTime: " + startTime);
            System.err.println("endTime: " + endTime);
            System.err.println("severity: " + severity);

            e.printStackTrace();
            System.exit(1);
        }
    }

    public static Event getEvent(Connection connection, int eventId) throws SQLException {
        String sql = "SELECT id, fleet_id, flight_id, event_definition_id, start_line, end_line, start_time, end_time, severity, other_flight_id FROM events WHERE id = ?";

        PreparedStatement query = connection.prepareStatement(sql);
        query.setInt(1, eventId);

        ResultSet resultSet = query.executeQuery();

        Event event = null;

        if (resultSet.next()) {
            event = new Event(resultSet);
        }

        return event;
    }

    /**
     * Gets all of the event from the database for a given flight.
     *
     * @param connection is the connection to the database.
     * @param flightId the id of the flight for the event list.
     *
     * @return an array list of all events in the database for the given flight id.
     */
    public static ArrayList<Event> getAll(Connection connection, int flightId) throws SQLException {
        String query = "SELECT id, fleet_id, flight_id, event_definition_id, start_line, end_line, start_time, end_time, severity, other_flight_id FROM events WHERE flight_id = ? ORDER BY start_time";

        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, flightId);
        LOG.info(preparedStatement.toString());
        ResultSet resultSet = preparedStatement.executeQuery();

        ArrayList<Event> allEvents = new ArrayList<Event>();
        while (resultSet.next()) {
            allEvents.add(new Event(resultSet));
        }
        resultSet.close();
        preparedStatement.close();

        return allEvents;
    }

    /**
     * Gets all of the event from the database for a given fleet and {@link EventDefinition}.
     *
     * @param connection is the connection to the database.
     * @param fleetId the id of the fleet for the event list.
     * @param eventDefinitionId the event def id
     *
     * @return an array list of all events in the database for the given flight id.
     */
    public static List<Event> getAll(Connection connection, int fleetId, int eventDefinitionId) throws SQLException {
        String query = "SELECT id, fleet_id, flight_id, event_definition_id, start_line, end_line, start_time, end_time, severity, other_flight_id FROM events WHERE fleet_id = ? AND event_definition_id = ? ORDER BY start_time";

        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, fleetId);
        preparedStatement.setInt(2, eventDefinitionId);

        LOG.info(preparedStatement.toString());
        ResultSet resultSet = preparedStatement.executeQuery();

        List<Event> allEvents = new ArrayList<Event>();
        while (resultSet.next()) {
            allEvents.add(new Event(resultSet));
        }
        resultSet.close();
        preparedStatement.close();

        return allEvents;
    }

    /**
     * Gets all of the event from the database for a given flight, fleet and {@link EventDefinition}.
     *
     * @param connection is the connection to the database.
     * @param fleetId the id of the fleet for the event list.
     * @param flightId the id of the flight for the event list.
     * @param eventDefinitionId the event def id
     *
     * @return an array list of all events in the database for the given flight id.
     */
    public static List<Event> getAll(Connection connection, int fleetId, int flightId, int eventDefinitionId) throws SQLException {
        String query = "SELECT id, fleet_id, flight_id, event_definition_id, start_line, end_line, start_time, end_time, severity, other_flight_id FROM events WHERE fleet_id = ? AND flight_id = ? AND event_definition_id = ? ORDER BY start_time";

        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, fleetId);
        preparedStatement.setInt(2, flightId);
        preparedStatement.setInt(3, eventDefinitionId);

        LOG.info(preparedStatement.toString());
        ResultSet resultSet = preparedStatement.executeQuery();

        List<Event> allEvents = new ArrayList<Event>();
        while (resultSet.next()) {
            allEvents.add(new Event(resultSet));
        }
        resultSet.close();
        preparedStatement.close();

        return allEvents;
    }

    /**
     * Gets all of the event from the database for a given flight.
     *
     * @param connection is the connection to the database.
     * @param fleetId the fleet to get events for
     * @param eventName the name of the event to get events for
     * @param startTime is the earliest time to start getting events (it will get events from the beginning of time if it is null)
     * @param endTime is the latest time to getting events (it will get events until the current date if it is null)
     *
     * @return a hashmap where every entry relates to an airframe name for this fleet, containing a vector of all specified events for that airframe between the specified start and end dates (if provided)
     */
    public static HashMap<String, ArrayList<Event>> getEvents(Connection connection, int fleetId, String eventName, LocalDate startTime, LocalDate endTime) throws SQLException {
        //get list of airframes for this fleet so we can set up the hashmap of arraylists for events by airframe
        ArrayList<String> fleetAirframes = Airframes.getAll(connection, fleetId);
        HashMap<Integer, String> airframeIds = new HashMap<Integer, String>();

        //create the hashmap to be returned by this method
        HashMap<String, ArrayList<Event>> eventsByAirframe = new HashMap<String, ArrayList<Event>>();

        //get a map of the airframe ids to airframe names
        for (String airframe : fleetAirframes) {
            airframeIds.put(Airframes.getNameId(connection, airframe), airframe);

            eventsByAirframe.put(airframe, new ArrayList<Event>());
        }

        String query = "SELECT id FROM event_definitions WHERE (fleet_id = 0 OR fleet_id = ?) AND name LIKE ? ORDER BY name";

        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, fleetId);
        preparedStatement.setString(2, eventName);

        LOG.info(preparedStatement.toString());
        ResultSet resultSet = preparedStatement.executeQuery();

        while (resultSet.next()) {
            int definitionId = resultSet.getInt(1);
            LOG.info("getting events for definition id: " + definitionId);

            //could use this but it won't grab the airframeId because it's not in the events table so
            //doing it the longer way below is quicker
            //ArrayList<Event> eventList = getAll(connection, fleetId, definitionId, startTime, endTime);

            String eventsQuery = "SELECT events.id, events.flight_id, events.start_line, events.end_line, events.start_time, events.end_time, events.severity, events.other_flight_id, flights.airframe_id FROM events, flights WHERE events.flight_id = flights.id AND events.event_definition_id = ? AND events.fleet_id = ?";

            if (startTime != null) {
                eventsQuery += " AND events.end_time >= ?";
            }

            if (endTime != null) {
                eventsQuery += " AND events.end_time <= ?";
            }

            eventsQuery += " ORDER BY events.start_time";

            LOG.info(eventsQuery);
            LOG.info("startTime: '" + startTime + "', endTime: '" + endTime + "'");

            PreparedStatement eventsStatement = connection.prepareStatement(eventsQuery);
            eventsStatement.setInt(1, definitionId);
            eventsStatement.setInt(2, fleetId);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            int current = 3;
            if (startTime != null) {
                eventsStatement.setString(current, startTime.format(formatter));
                current++;
            }

            if (endTime != null) {
                eventsStatement.setString(current, endTime.format(formatter));
                current++;
            }

            LOG.info(eventsStatement.toString());
            ResultSet eventSet = eventsStatement.executeQuery();

            while (eventSet.next()) {
                int eventId = eventSet.getInt(1);
                int flightId = eventSet.getInt(2);
                int startLine = eventSet.getInt(3);
                int endLine = eventSet.getInt(4);
                String eventStartTime = eventSet.getString(5);
                String eventEndTime = eventSet.getString(6);
                double severity = eventSet.getDouble(7);
                Integer otherFlightId = eventSet.getInt(8);
                if (eventSet.wasNull()) {
                    otherFlightId = null;
                }

                Event event = new Event(eventId, fleetId, flightId, definitionId, startLine, endLine, eventStartTime, eventEndTime, severity, otherFlightId);
                System.out.println("event: " + event.toString());

                int airframeId = eventSet.getInt(9);
                String airframe = airframeIds.get(airframeId);

                //add the airframe to 
                eventsByAirframe.get(airframe).add(event);
            }

            eventSet.close();
            eventsStatement.close();
        }

        preparedStatement.close();
        resultSet.close();

        return eventsByAirframe;
    }
}

