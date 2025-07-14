package org.ngafid.core.event;

import org.ngafid.core.flights.Airframes;
import org.ngafid.core.flights.Flight;
import org.ngafid.core.util.TimeUtils;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public class Event {
    private static final Logger LOG = Logger.getLogger(Event.class.getName());
    private double severity;
    private int id;
    private int fleetId;
    private int flightId;
    private int eventDefinitionId;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private int startLine;
    private int endLine;
    private Integer otherFlightId = null;

    private String systemId;

    private String tail;

    private String tagName;

    private RateOfClosure rateOfClosure;

    private List<EventMetaData> metaDataList;
    // Event's bounding box (where the event is located in space)
    private Double minLatitude, maxLatitude, minLongitude, maxLongitude;


    /**
     * Start and end time must be formatted according to TimeUtils.ISO_8601. Dates in the derived column Parameters.UTC_DATE_TIME
     * are formatted in this way.
     *
     * @param startTime a date time formatted according to TimeUtils.ISO_8601
     * @param endTime   a date time formatted according to TimeUtils.ISO_8601
     * @param startLine
     * @param endLine
     * @param defId
     * @param severity
     */
    public Event(String startTime, String endTime, int startLine, int endLine, int defId, double severity) {
        this(OffsetDateTime.parse(startTime, TimeUtils.ISO_8601_FORMAT), OffsetDateTime.parse(endTime, TimeUtils.ISO_8601_FORMAT), startLine, endLine, defId, severity);
    }

    public Event(OffsetDateTime startTime, OffsetDateTime endTime, int startLine, int endLine, int defId, double severity) {
        this(startTime, endTime, startLine, endLine, defId, severity, 0, null);
    }

    public Event(OffsetDateTime startTime, OffsetDateTime endTime,
                 int startLine, int endLine, int defId, double severity, Integer otherFlightId) {
        this(startTime, endTime, startLine, endLine, defId, severity, -1, otherFlightId);
    }

    public Event(OffsetDateTime startTime, OffsetDateTime endTime, int startLine, int endLine, int defId, double severity, int flightId, Integer otherFlightId) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.startLine = startLine;
        this.endLine = endLine;
        this.severity = severity;
        this.flightId = flightId;
        this.otherFlightId = otherFlightId;
        this.eventDefinitionId = defId;
        this.metaDataList = new ArrayList<>();
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
        this.startTime = TimeUtils.SQLtoOffsetDateTime(resultSet.getString(7));
        this.endTime = TimeUtils.SQLtoOffsetDateTime(resultSet.getString(8));
        this.severity = resultSet.getDouble(9);
        this.otherFlightId = resultSet.getInt(10);
        if (resultSet.wasNull()) {
            this.otherFlightId = null;
        }
    }

    /**
     * Creates an event directly from all its fields
     *
     * @param id            is the id of the event in the database
     * @param fleetId       is the id of the fleet
     * @param flightId      is the id of the flight of this event
     * @param eventDefId    is the id of the event definition of this event
     * @param startLine     is the line the event starts in the data file of the flight
     * @param endLine       is the line the event ends in the data file of the flight
     * @param startTime     is the time the event starts in the data file of the flight
     * @param endTime       is the time the event ends in the data file of the flight
     * @param severity      is the severity rating for the event
     * @param otherFlightId is the other flight id (for proximity events)
     */
    public Event(int id, int fleetId, int flightId, int eventDefId, int startLine, int endLine,
                 String startTime, String endTime, double severity, Integer otherFlightId) {
        this.id = id;
        this.fleetId = fleetId;
        this.flightId = flightId;
        this.eventDefinitionId = eventDefId;
        this.startLine = startLine;
        this.endLine = endLine;
        this.startTime = OffsetDateTime.parse(startTime, TimeUtils.ISO_8601_FORMAT);
        this.endTime = OffsetDateTime.parse(endTime, TimeUtils.ISO_8601_FORMAT);
        this.severity = severity;
        this.otherFlightId = otherFlightId;
    }

    public Event(int id, int fleetId, int flightId, int eventDefinitionId, int startLine, int endLine,
                 String startTime, String endTime, double severity, Integer otherFlightId, String systemId,
                 String tail, String tagName) {
        this(id, fleetId, flightId, eventDefinitionId, startLine, endLine, OffsetDateTime.parse(startTime, TimeUtils.ISO_8601_FORMAT), OffsetDateTime.parse(endTime, TimeUtils.ISO_8601_FORMAT), severity, otherFlightId, systemId, tail, tagName);
    }

    public Event(int id, int fleetId, int flightId, int eventDefinitionId, int startLine, int endLine, OffsetDateTime startTime, OffsetDateTime endTime, double severity, Integer otherFlightId, String systemId, String tail, String tagName) {
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
        this.systemId = systemId;
        this.tail = tail;
        this.tagName = tagName;
    }

    /**
     * Gets all of the event from the database for a given flight.
     *
     * @param connection is the connection to the database.
     * @param flightId   the id of the flight for the event list.
     * @return an array list of all events in the database for the given flight id.
     */
    public static ArrayList<Event> getAll(Connection connection, int flightId) throws SQLException {
        String query = "SELECT id, fleet_id, flight_id, event_definition_id, start_line, end_line, start_time, " +
                "end_time, severity, other_flight_id FROM events WHERE flight_id = " + flightId + " ORDER BY " +
                "start_time";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query); ResultSet resultSet =
                preparedStatement.executeQuery()) {
            ArrayList<Event> allEvents = new ArrayList<Event>();
            while (resultSet.next()) allEvents.add(new Event(resultSet));

            return allEvents;
        }
    }

    /**
     * Gets all events from the database for a given flight.
     *
     * @param connection is the connection to the database.
     * @param fleetId    the fleet to get events for
     * @param eventName  the name of the event to get events for
     * @param startTime  is the earliest time to start getting events (it will get events from the beginning of time if
     *                   it is null)
     * @param endTime    is the latest time to getting events (it will get events until the current date if it is null)
     * @param tagName    is the name of the tag to get events for
     * @return a hashmap where every entry relates to an airframe name for this fleet, containing a vector of all
     * specified events for that airframe between the specified start and end dates (if provided)
     */
    public static HashMap<String, ArrayList<Event>> getEvents(Connection connection, int fleetId,
                                                              String eventName, LocalDate startTime, LocalDate endTime, String tagName)
            throws SQLException {
        // get list of airframes for this fleet so we can set up the hashmap of arraylists for events by airframe
        ArrayList<String> fleetAirframes = Airframes.getAll(connection, fleetId);
        HashMap<Integer, String> airframeIds = new HashMap<Integer, String>();

        // create the hashmap to be returned by this method
        HashMap<String, ArrayList<Event>> eventsByAirframe = new HashMap<String, ArrayList<Event>>();

        // get a map of the airframe ids to airframe names
        for (String airframe : fleetAirframes) {
            airframeIds.put(Airframes.Airframe.getAirframeByName(connection, airframe).getId(), airframe);

            eventsByAirframe.put(airframe, new ArrayList<Event>());
        }

        String query;
        PreparedStatement preparedStatement;

        if (eventName.equals("ANY Event")) {
            query = "SELECT id FROM event_definitions WHERE fleet_id = 0 OR fleet_id = ? ORDER BY name";
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, fleetId);

        } else {
            query = "SELECT id FROM event_definitions WHERE (fleet_id = 0 OR fleet_id = ?) AND name LIKE ? ORDER BY name";
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, fleetId);
            preparedStatement.setString(2, eventName);
        }

        try (PreparedStatement ps = preparedStatement; ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                int definitionId = resultSet.getInt(1);

                // could use this but it won't grab the airframeId because it's not in the events table so
                // doing it the longer way below is quicker
                // ArrayList<Event> eventList = getAll(connection, fleetIdToGetEventsFor, definitionId, startTime, endTime);

                String eventsQuery = "";
                if (Objects.equals(tagName, "All Tags")) {
                    eventsQuery = "SELECT events.id, events.flight_id, events.start_line, events.end_line, events" +
                            ".start_time, events.end_time, events.severity, events.other_flight_id, flights.airframe_id, " +
                            "flights.system_id, tails.tail FROM events, flights, tails WHERE events.flight_id = flights" +
                            ".id AND flights.system_id = tails.system_id  AND events.event_definition_id = ? AND events" +
                            ".fleet_id = ?";
                } else if (!Objects.equals(tagName, "All Tags")) {
                    eventsQuery = "SELECT events.id, events.flight_id, events.start_line, events.end_line, events" +
                            ".start_time, events.end_time, events.severity, events.other_flight_id, flights.airframe_id, " +
                            "flights.system_id, tails.tail, flight_tags.name FROM events, flights, tails, flight_tag_map," +
                            " flight_tags WHERE events.flight_id = flights.id AND flights.system_id = tails.system_id  " +
                            "AND flights.id = flight_tag_map.flight_id AND events.fleet_id = flight_tags.fleet_id AND " +
                            "flight_tag_map.tag_id = flight_tags.id AND events.event_definition_id = ? AND events" +
                            ".fleet_id = ?";
                }
                if (startTime != null) {
                    eventsQuery += " AND events.end_time >= ?";
                }

                if (endTime != null) {
                    eventsQuery += " AND events.end_time <= ?";
                }

                if (!Objects.equals(tagName, "All Tags")) {
                    eventsQuery += " AND flight_tags.name = ?";
                }

                eventsQuery += " ORDER BY events.start_time";

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

                if (!Objects.equals(tagName, "All Tags")) {
                    eventsStatement.setString(current, tagName);
                    current++;
                }

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
                    String systemId = eventSet.getString(10);
                    String tail = eventSet.getString(11);
                    String tag = "";

                    if (!Objects.equals(tagName, "All Tags")) {
                        tag = eventSet.getString(12);
                    }

                    if (eventSet.wasNull()) {
                        otherFlightId = null;
                    }

                    Event event = new Event(eventId, fleetId, flightId, definitionId, startLine,
                            endLine, TimeUtils.SQLtoOffsetDateTime(eventStartTime), TimeUtils.SQLtoOffsetDateTime(eventEndTime), severity, otherFlightId, systemId, tail, tag);

                    int airframeId = eventSet.getInt(9);
                    String airframe = airframeIds.get(airframeId);

                    // add the airframe to
                    eventsByAirframe.get(airframe).add(event);
                }

                eventSet.close();
                eventsStatement.close();
            }
        }

        return eventsByAirframe;
    }

    public static void deleteEvents(Connection connection, int flightId, int eventDefinitionId) throws SQLException {
        String query = "DELETE FROM events WHERE event_definition_id = ? AND (flight_id = ? OR other_flight_id = ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, eventDefinitionId);
            preparedStatement.setInt(2, flightId);
            preparedStatement.setInt(3, flightId);
            preparedStatement.executeUpdate();
        }
    }

    public void updateEnd(String newEndTime, int newEndLine) {
        updateEnd(OffsetDateTime.parse(newEndTime, TimeUtils.ISO_8601_FORMAT), newEndLine);
    }

    public void updateEnd(OffsetDateTime newEndDate, int newEndLine) {
        endTime = newEndDate;
        endLine = newEndLine;
    }

    public int getFlightId() {
        return flightId;
    }

    public void setEventDefinitionId(int eventDefinitionId) {
        this.eventDefinitionId = eventDefinitionId;
    }

    public int getEventDefinitionId() {
        return eventDefinitionId;
    }

    public int getEndLine() {
        return endLine;
    }

    public void setEndLine(int endLine) {
        this.endLine = endLine;
    }

    public int getOtherFlightId() {
        return otherFlightId;
    }

    public String toString() {
        String readable = "[line " + startLine + " to " + endLine + ", time " + startTime + " to " + endTime + ", " +
                "severity: " + severity;
        if (otherFlightId != null) readable += ", other flight: " + otherFlightId;
        readable += "]";

        return readable;
    }

    public int getStartLine() {
        return startLine;
    }

    public void setStartLine(int startLine) {
        this.startLine = startLine;
    }

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public OffsetDateTime getEndTime() {
        return endTime;
    }

    public double getSeverity() {
        return severity;
    }

    public int getDuration() {
        return (endLine - startLine) + 1;
    }

    public String getSystemID() {
        return systemId;
    }

    public String getTail() {
        return tail;
    }

    public void addMetaData(EventMetaData metaData) {
        this.metaDataList.add(metaData);
    }
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public void setMinLatitude(Double minLatitude) { this.minLatitude = minLatitude; }
    public void setMaxLatitude(Double maxLatitude) { this.maxLatitude = maxLatitude; }
    public void setMinLongitude(Double minLongitude) { this.minLongitude = minLongitude; }
    public void setMaxLongitude(Double maxLongitude) { this.maxLongitude = maxLongitude; }

    public Double getMinLatitude() { return minLatitude; }
    public Double getMaxLatitude() { return maxLatitude; }
    public Double getMinLongitude() { return minLongitude; }
    public Double getMaxLongitude() { return maxLongitude; }


    public static PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
        return connection.prepareStatement(
            "INSERT INTO events (fleet_id, flight_id, event_definition_id, start_line, end_line, start_time, end_time, severity, other_flight_id, min_latitude, max_latitude, min_longitude, max_longitude) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            Statement.RETURN_GENERATED_KEYS
        );
    }

    public static void batchInsertion(Connection connection, Flight flight, List<Event> events) throws SQLException, IOException {
        try (PreparedStatement preparedStatement = createPreparedStatement(connection)) {
            for (Event event : events) {
                if (event.getFlightId() == event.getOtherFlightId()) {
                    LOG.warning("Insertion skipped. Self-proximity event detected before DB insert: flight ID = " + event.getFlightId() +
                            ", otherFlightId = " + event.getOtherFlightId());
                }else{
                    event.addBatch(preparedStatement, flight.getFleetId(), event.flightId, event.eventDefinitionId);
                }
            }

            preparedStatement.executeBatch();

            try (ResultSet resultSet = preparedStatement.getGeneratedKeys()) {
                int i = 0;
                while (resultSet.next()) {
                    int generatedId = resultSet.getInt(1);
                    events.get(i).setId(generatedId);
                    events.get(i).updateDatabaseMetadata(connection, generatedId);
                    i += 1;
                }
            }
        }
    }

    public void addBatch(PreparedStatement preparedStatement, int fleetId, int flightId, int eventDefinitionId) throws SQLException {
        this.eventDefinitionId = eventDefinitionId;
        this.flightId = flightId;
        this.fleetId = fleetId;

        preparedStatement.setInt(1, fleetId);
        preparedStatement.setInt(2, flightId);
        preparedStatement.setInt(3, eventDefinitionId);
        preparedStatement.setInt(4, startLine);
        preparedStatement.setInt(5, endLine);

        preparedStatement.setString(6, TimeUtils.UTCtoSQL(startTime));

        preparedStatement.setString(7, TimeUtils.UTCtoSQL(endTime));

        preparedStatement.setDouble(8, severity);

        if (otherFlightId == null) {
            preparedStatement.setNull(9, java.sql.Types.INTEGER);
        } else {
            preparedStatement.setInt(9, otherFlightId);
        }

        if (eventDefinitionId == -1) {
            // Set bounding box values for proximity events
            preparedStatement.setDouble(10, minLatitude);
            preparedStatement.setDouble(11, maxLatitude);
            preparedStatement.setDouble(12, minLongitude);
            preparedStatement.setDouble(13, maxLongitude);
        } else {
            preparedStatement.setNull(10, java.sql.Types.DOUBLE);
            preparedStatement.setNull(11, java.sql.Types.DOUBLE);
            preparedStatement.setNull(12, java.sql.Types.DOUBLE);
            preparedStatement.setNull(13, java.sql.Types.DOUBLE);
        }


        preparedStatement.addBatch();
    }

    public void updateDatabase(Connection connection, int fleetIdUpdated, int flightIdUpdated, int eventDefId)
            throws IOException, SQLException {
        this.flightId = flightIdUpdated;
        this.eventDefinitionId = eventDefId;

        try (PreparedStatement preparedStatement = createPreparedStatement(connection)) {
            addBatch(preparedStatement, fleetIdUpdated, flightIdUpdated, eventDefId);

            preparedStatement.executeBatch();

            try (ResultSet resultSet = preparedStatement.getGeneratedKeys()) {
                if (resultSet.next()) {
                    int eventId = resultSet.getInt(1);
                    updateDatabaseMetadata(connection, eventId);
                }
            }
        }
    }

    private void updateDatabaseMetadata(Connection connection, int eventId) throws SQLException, IOException {
        if (this.rateOfClosure != null) {
            this.rateOfClosure.updateDatabase(connection, eventId);
        }

        if (this.metaDataList != null && !this.metaDataList.isEmpty()) {
            for (EventMetaData metaData : this.metaDataList) {
                metaData.updateDatabase(connection, eventId);
            }
        }
    }

    public void setRateOfClosure(RateOfClosure rateOfClosure) {
        this.rateOfClosure = rateOfClosure;
    }
}
