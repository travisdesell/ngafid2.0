package org.ngafid.events;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ngafid.filters.Filter;
import org.ngafid.flights.Airframes;
import org.ngafid.flights.DoubleTimeSeries;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

public class EventDefinition {
    public static final int MIN_SEVERITY = 1;
    public static final int MAX_SEVERITY = 2;
    public static final int MIN_ABS_SEVERITY = 3;
    public static final int MAX_ABS_SEVERITY = 4;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Logger LOG = Logger.getLogger(EventDefinition.class.getName());
    private static final String SQL_FIELDS = "id, fleet_id, name, start_buffer, stop_buffer, airframe_id, " + "condition_json, " +
            "column_names, severity_column_names, severity_type";
    /*
     * Caches event definitions by name. Events are rarely added anyway...
     */
    private static final Map<String, List<EventDefinition>> NAME_TO_EVENT_DEFINITIONS = new HashMap<>();
    private static Map<Integer, String> EVENT_DEFINITION_ID_TO_NAME = null;
    @JsonProperty
    private final int fleetId;
    @JsonProperty
    private final int startBuffer;
    @JsonProperty
    private final int stopBuffer;
    @JsonProperty
    private final int airframeNameId;
    @JsonProperty
    private final TreeSet<String> severityColumnNames;
    @JsonProperty
    private final String severityType;
    @JsonProperty
    private int id = 0;
    @JsonProperty
    private String name;
    @JsonProperty
    private Filter filter;
    @JsonProperty
    private TreeSet<String> columnNames;
    @JsonProperty
    private int[] severityColumnIds;
    @JsonProperty
    private int severityTypeId;

    /**
     * Creates an event definition.
     *
     * @param fleetId             the id of the fleet this event applies to. 0 is generic and will apply to all fleets
     * @param name                is the name of the event
     * @param startBuffer         is how many seconds this event needs to occur in before it will be tracked
     * @param stopBuffer          is how many seconds must go by without the event occuring before it stops being
     *                            tracked
     * @param airframeNameId      ID of the airframe name
     * @param filter              is the event condition filter
     * @param severityColumnNames a list of column names (unique) which are used to calculate the severity
     * @param severityType        a string representation of the severity type, can be 'min', 'abs' or 'max'
     */
    public EventDefinition(int fleetId, String name, int startBuffer, int stopBuffer, int airframeNameId, Filter filter,
                           TreeSet<String> severityColumnNames, String severityType) {
        this.fleetId = fleetId;
        this.startBuffer = startBuffer;
        this.stopBuffer = stopBuffer;
        this.airframeNameId = airframeNameId;
        this.filter = filter;
        this.columnNames = filter.getColumnNames();
        this.severityColumnNames = severityColumnNames;
        this.severityType = severityType;

        initializeSeverity();
    }

    /**
     * Creates an event definition from a mysql query
     *
     * @param resultSet is the row selected from the database
     */
    public EventDefinition(ResultSet resultSet) throws SQLException, JsonProcessingException {
        this.id = resultSet.getInt(1);
        this.fleetId = resultSet.getInt(2);
        this.name = resultSet.getString(3);
        this.startBuffer = resultSet.getInt(4);
        this.stopBuffer = resultSet.getInt(5);
        this.airframeNameId = resultSet.getInt(6);
        if (id >= 0) {
            try {
                this.filter = OBJECT_MAPPER.readValue(resultSet.getString(7), Filter.class);
            } catch (Exception e) {
                System.err.println("Error with filter: " + e);
                System.err.println(resultSet.getString(7));

                e.printStackTrace();
                System.exit(1);
            }
        } else {
            try {
                this.filter = OBJECT_MAPPER.readValue(resultSet.getString(7), Filter.class);
            } catch (NullPointerException | JsonProcessingException e) {
                this.filter = null;
            }
        }

        this.columnNames = OBJECT_MAPPER.readValue(resultSet.getString(8), new TypeReference<>() {
        });
        this.severityColumnNames = OBJECT_MAPPER.readValue(resultSet.getString(9), new TypeReference<>() {
        });
        this.severityType = resultSet.getString(10);

        initializeSeverity();
    }

    /**
     * Get an event definition by specifying the event name
     *
     * @param connection is the DB session
     * @param eventName  is the event name being retrieved
     * @return Event Definition matching the name passed in
     */
    public static EventDefinition getEventDefinition(Connection connection, String eventName) throws IOException, SQLException {
        eventName = "name = '" + eventName + "'";
        String query = "SELECT " + SQL_FIELDS + " FROM event_definitions WHERE " + eventName;

        return getEventDefinitionFromDB(connection, query);
    }

    /**
     * Get an event definition by specifying the name and airframe ID it is for
     * Useful for events that vary based on airframe ID
     *
     * @param connection is the DB session
     * @param eventName  is the event name being retrieved
     * @param airframeID is the airframe ID that goes with the event
     * @return Event Definition matching name and ID passed in
     */
    public static EventDefinition getEventDefinition(Connection connection, String eventName, int airframeID) throws IOException,
            SQLException {
        String airframeIDStr = "airframe_id = " + airframeID;

        eventName = "name = '" + eventName + "'";
        String query = "SELECT " + SQL_FIELDS + " FROM event_definitions WHERE " + eventName + " AND " + airframeIDStr;

        return getEventDefinitionFromDB(connection, query);
    }

    public static List<EventDefinition> getEventDefs(Connection connection, String eventName)
            throws SQLException, JsonProcessingException {
        if (NAME_TO_EVENT_DEFINITIONS.containsKey(eventName)) return NAME_TO_EVENT_DEFINITIONS.get(eventName);

        List<EventDefinition> definitions = new ArrayList<>();

        String query = "SELECT " + SQL_FIELDS + " FROM event_definitions WHERE name = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, eventName);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    EventDefinition ed = new EventDefinition(resultSet);
                    definitions.add(ed);
                }
            }
        }

        NAME_TO_EVENT_DEFINITIONS.put(eventName, definitions);

        return definitions;
    }

    public static Map<Integer, String> getEventDefinitionIdToNameMap(Connection connection) throws SQLException {
        if (EVENT_DEFINITION_ID_TO_NAME == null) {
            String query = "SELECT id, name FROM event_definitions";
            try (PreparedStatement ps = connection.prepareStatement(query); ResultSet resultSet = ps.executeQuery()) {

                EVENT_DEFINITION_ID_TO_NAME = new HashMap<>();

                while (resultSet.next()) {
                    int id = resultSet.getInt("id");
                    String name = resultSet.getString("name");
                    EVENT_DEFINITION_ID_TO_NAME.put(id, name);
                }
            }
        }

        return EVENT_DEFINITION_ID_TO_NAME;
    }

    /**
     * Helper method for retrieving the EventDefinition from server
     *
     * @param connection is the DB session
     * @param query      is the query to execute to the DB
     * @return Event Definition result from DB
     */
    private static EventDefinition getEventDefinitionFromDB(Connection connection, String query) throws SQLException,
            JsonProcessingException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(query); ResultSet resultSet =
                preparedStatement.executeQuery()) {
            LOG.info(preparedStatement.toString());

            if (resultSet.next()) {
                return new EventDefinition(resultSet);
            } else {
                return null;
            }
        }
    }

    /**
     * Updates an existing event definition into the database.
     *
     * @param connection              is the connection to the database.
     * @param fleetId                 the fleet id
     * @param eventId                 the event id
     * @param name                    the name
     * @param startBuffer             the start buffer
     * @param stopBuffer              the stop buffer
     * @param airframe                the airframe
     * @param filterJson              the filter json
     * @param severityColumnNamesJson the severity column names json of the event definition
     * @param severityType            the severity type
     * @throws SQLException if there is an error with the SQL query
     */
    public static void update(Connection connection, int fleetId, int eventId, String name, int startBuffer, int stopBuffer,
                              String airframe, String filterJson, String severityColumnNamesJson, String severityType)
            throws SQLException, JsonProcessingException {
        int airframeNameID = 0;

        if (!airframe.equals("All Airframes")) {
            airframeNameID = new Airframes.Airframe(connection, airframe).getId();
        }

        update(connection, fleetId, eventId, name, startBuffer, stopBuffer, airframeNameID, filterJson, severityColumnNamesJson,
                severityType);
    }

    /**
     * Updates an existing event definition into the database.
     *
     * @param connection              is the connection to the database.
     * @param fleetId                 the fleet id
     * @param eventId                 the event id
     * @param name                    the name
     * @param startBuffer             the start buffer
     * @param stopBuffer              the stop buffer
     * @param airframeNameID          the airframe id
     * @param filterJson              the filter json
     * @param severityColumnNamesJson the severity column names json
     * @param severityType            the severity type
     * @throws SQLException if there is an error with the SQL query
     */
    public static void update(Connection connection, int fleetId, int eventId, String name, int startBuffer, int stopBuffer,
                              int airframeNameID, String filterJson, String severityColumnNamesJson, String severityType)
            throws SQLException, JsonProcessingException {
        Filter filter = OBJECT_MAPPER.readValue(filterJson, Filter.class);
        String columnNamesJson;

        if (eventId > 0) {
            columnNamesJson = OBJECT_MAPPER.writeValueAsString(filter.getColumnNames());
        } else {
            columnNamesJson = severityColumnNamesJson;
        }

        String query = "UPDATE event_definitions SET fleet_id = ?, name = ?, start_buffer = ?, stop_buffer = ?, " + "airframe_id = ?, " +
                "condition_json = ?, column_names = ?, severity_column_names = ?, severity_type = ? " + "WHERE id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, fleetId);
            preparedStatement.setString(2, name);
            preparedStatement.setInt(3, startBuffer);
            preparedStatement.setInt(4, stopBuffer);
            preparedStatement.setInt(5, airframeNameID);
            preparedStatement.setString(6, filterJson);
            preparedStatement.setString(7, columnNamesJson);
            preparedStatement.setString(8, severityColumnNamesJson);
            preparedStatement.setString(9, severityType);
            preparedStatement.setInt(10, eventId);

            LOG.info(preparedStatement.toString());
            preparedStatement.executeUpdate();
        }
    }

    /**
     * Inserts this event definition into the database.
     *
     * @param connection              is the connection to the database.
     * @param fleetId                 is the fleet id for the event definitions
     * @param name                    is the name of the event definition
     * @param startBuffer             is the start buffer
     * @param stopBuffer              is the stop buffer
     * @param airframe                is the airframe
     * @param filterJson              is the filter json
     * @param severityColumnNamesJson is the severity column names json
     * @param severityType            is the severity type
     * @throws SQLException if there is an error with the SQL query
     */
    public static void insert(Connection connection, int fleetId, String name, int startBuffer, int stopBuffer, String airframe,
                              String filterJson, String severityColumnNamesJson, String severityType) throws SQLException,
            JsonProcessingException {
        Filter filter = OBJECT_MAPPER.readValue(filterJson, Filter.class);
        TreeSet<String> columnNames = filter.getColumnNames();

        if (airframe.equals("All Airframes")) {
            String query = "INSERT INTO event_definitions SET fleet_id = ?, name = ?, start_buffer = ?, " + "stop_buffer = ?, airframe_id" +
                    " = ?, condition_json = ?, column_names = ?, severity_column_names = " + "?," + " severity_type = ?";

            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setInt(1, fleetId);
                preparedStatement.setString(2, name);
                preparedStatement.setInt(3, startBuffer);
                preparedStatement.setInt(4, stopBuffer);
                preparedStatement.setInt(5, 0);
                preparedStatement.setString(6, filterJson);
                preparedStatement.setString(7, OBJECT_MAPPER.writeValueAsString(columnNames));
                preparedStatement.setString(8, severityColumnNamesJson);
                preparedStatement.setString(9, severityType);

                LOG.info(preparedStatement.toString());
                preparedStatement.executeUpdate();
            }
        } else {
            int airframeNameId = new Airframes.Airframe(connection, airframe).getId();
            String query = "INSERT INTO event_definitions SET fleet_id = ?, name = ?, start_buffer = ?, " + "stop_buffer = ?, airframe_id" +
                    " = ?, condition_json = ?, column_names = ?, severity_column_names = " + "?," + " severity_type = ?";

            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setInt(1, fleetId);
                preparedStatement.setString(2, name);
                preparedStatement.setInt(3, startBuffer);
                preparedStatement.setInt(4, stopBuffer);
                preparedStatement.setInt(5, airframeNameId);
                preparedStatement.setString(6, filterJson);
                preparedStatement.setString(7, OBJECT_MAPPER.writeValueAsString(columnNames));
                preparedStatement.setString(8, severityColumnNamesJson);
                preparedStatement.setString(9, severityType);

                LOG.info(preparedStatement.toString());
                preparedStatement.executeUpdate();
            }
        }
    }

    /**
     * Inserts this event definition into the database.
     * This is meant for special events (those that have a negative ID).
     *
     * @param connection  is the connection to the database.
     * @param id          the id of the event definition
     * @param name        the name of the event definition
     * @param startBuffer the start buffer
     * @param stopBuffer  the stop buffer
     * @param airframeId  the airframe id
     * @throws SQLException if there is an error with the SQL query
     */
    public static void insert(Connection connection, int id, String name, int startBuffer, int stopBuffer, int airframeId) throws SQLException {
        if (id > 0) {
            LOG.info("Passed a positive ID to special event insertion.");
            System.exit(0);
        }

        String query = "INSERT INTO event_definitions SET fleet_id = ?, name = ?, " + "start_buffer = " + "?, stop_buffer " +
                "= ?, airframe_id = ?, condition_json = ?, column_names = ?," + "severity_column_names" + " = ?, severity_type = ?, id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, 0);
            preparedStatement.setString(2, name);
            preparedStatement.setInt(3, startBuffer);
            preparedStatement.setInt(4, stopBuffer);
            preparedStatement.setInt(5, airframeId);
            preparedStatement.setString(6, "{}");
            preparedStatement.setString(7, "{}");
            preparedStatement.setString(8, "{}");
            preparedStatement.setString(9, "max");
            preparedStatement.setInt(10, id);

            LOG.info(preparedStatement.toString());
            preparedStatement.executeUpdate();
        }
    }

    /**
     * Gets all event definitions from the database with a query.
     *
     * @param connection      is the connection to the database.
     * @param extraQuery      is a string of extra SQL conditions
     * @param extraParameters are the parameters to that query
     * @return the event definitions from the database.
     */
    public static ArrayList<EventDefinition> getAll(Connection connection, String extraQuery, Object[] extraParameters)
                                                                            throws SQLException, JsonProcessingException {
        String query = "SELECT id, fleet_id, name, start_buffer, stop_buffer, airframe_id, condition_json," + " " + "column_names, " +
                "severity_column_names, severity_type FROM event_definitions WHERE ?" + extraQuery;

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            for (int i = 0; i < extraParameters.length; i++) {
                if (extraParameters[i] instanceof String) {
                    preparedStatement.setString(i + 1, (String) extraParameters[i]);
                } else if (extraParameters[i] instanceof Integer) {
                    preparedStatement.setInt(i + 1, (Integer) extraParameters[i]);
                } else if (extraParameters[i] instanceof Double) {
                    preparedStatement.setDouble(i + 1, (Double) extraParameters[i]);
                } else {
                    LOG.severe("unknown parameter to event definition query: " + extraParameters[i]);
                    System.exit(1);
                }
            }

            LOG.info(preparedStatement.toString());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                ArrayList<EventDefinition> allEvents = new ArrayList<EventDefinition>();
                while (resultSet.next()) {
                    allEvents.add(new EventDefinition(resultSet));
                }

                return allEvents;
            }
        }
    }

    /**
     * Gets all the event definitions from the database.
     *
     * @param connection is the connection to the database.
     * @return an array list of all event definitions in the database.
     */
    public static List<EventDefinition> getAll(Connection connection) throws SQLException, JsonProcessingException {
        String query = "SELECT id, fleet_id, name, start_buffer, stop_buffer, airframe_id, condition_json, " + "column_names, " +
                "severity_column_names, severity_type FROM event_definitions";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query); ResultSet resultSet =
                preparedStatement.executeQuery()) {
            LOG.info(preparedStatement.toString());

            List<EventDefinition> allEvents = new ArrayList<>();
            while (resultSet.next()) {
                allEvents.add(new EventDefinition(resultSet));
            }

            return allEvents;
        }
    }

    /**
     * Gets a list of all the event definition names, will only have one name for non-generic events.
     *
     * @param connection is the connection to the database.
     * @param fleetId    is the fleet id for the event definitions
     * @return an array list of all event names in the database for this fleet
     */
    public static ArrayList<String> getUniqueNames(Connection connection, int fleetId) throws SQLException {
        // add all the generic event names
        String query = "SELECT DISTINCT(name) FROM event_definitions WHERE (event_definitions.fleet_id = 0 " + "OR " + "event_definitions" +
                ".fleet_id = ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, fleetId);

            LOG.info(preparedStatement.toString());

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                ArrayList<String> uniqueNames = new ArrayList<>();
                while (resultSet.next()) {
                    uniqueNames.add(resultSet.getString(1));
                }
                return uniqueNames;
            }
        }
    }

    /**
     * Gets a list of all the event definition names, will only have one name for non-generic events.
     *
     * @param connection is the connection to the database.
     * @return an array list of all event names in the database for this fleet
     */
    public static ArrayList<String> getUniqueNames(Connection connection) throws SQLException {
        // add all the generic event names
        String query = "SELECT DISTINCT(name) FROM event_definitions";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query); ResultSet resultSet =
                preparedStatement.executeQuery()) {
            LOG.info(preparedStatement.toString());

            ArrayList<String> uniqueNames = new ArrayList<>();
            while (resultSet.next()) {
                uniqueNames.add(resultSet.getString(1));
            }

            return uniqueNames;
        }
    }

    /**
     * Gets a list of all the event definition names. Will have "- airframe" appended to it for non-generic events.
     *
     * @param connection is the connection to the database.
     * @param fleetId    is the fleet id for the event definitions
     * @return an array list of all event names in the database for this fleet
     */
    public static ArrayList<String> getAllNames(Connection connection, int fleetId) throws SQLException {
        // add all the generic event names
        ArrayList<String> allNames = new ArrayList<>();

        String query = "SELECT name FROM event_definitions WHERE (event_definitions.fleet_id = 0 OR " + "event_definitions.fleet_id = ?) "
                + "AND event_definitions.airframe_id = 0";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, fleetId);

            LOG.info(preparedStatement.toString());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    allNames.add(resultSet.getString(1));
                }
            }
        }

        // add all the event names with the airframe they are for
        query = "SELECT name, airframe FROM event_definitions, airframes " + "WHERE (event_definitions.fleet_id = 0 " + "OR " +
                "event_definitions.fleet_id = ?) " + "AND airframes.id = event_definitions.airframe_id";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, fleetId);

            LOG.info(preparedStatement.toString());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    allNames.add(resultSet.getString(1) + " - " + resultSet.getString(2));
                }
            }
        }

        return allNames;
    }

    /**
     * Initializes the severity column ids and severity type id so that the severity values can
     * be calculated quickly.
     */
    void initializeSeverity() {
        switch (severityType) {
            case "min" -> severityTypeId = MIN_SEVERITY;
            case "max" -> severityTypeId = MAX_SEVERITY;
            case "min abs" -> severityTypeId = MIN_ABS_SEVERITY;
            case "max abs" -> severityTypeId = MAX_ABS_SEVERITY;
            default -> {
                LOG.severe("Unknown severity type: '" + severityType + " for EventDefinition: '" + name + "'");
                System.exit(1);
            }
        }

        severityColumnIds = new int[severityColumnNames.size()];

        int current = 0;
        int columnId = 0;
        for (String columnName : columnNames) {
            if (severityColumnNames.contains(columnName)) {
                severityColumnIds[current] = columnId;
                current++;
            }
            columnId++;
        }

        if (current != severityColumnNames.size()) {
            // there was a column in the severity column names that wasn't in the column names
            LOG.severe("ERROR initializing EventDefinition: '" + this.name + "'");
            LOG.severe("current: " + current + ", severityColumnIds.length: " + severityColumnIds.length);
            LOG.severe("severityColumnNames did not match columnNames");
            LOG.severe("severityColumnNames:");
            for (String columnName : severityColumnNames) {
                LOG.severe("\t'" + columnName + "'");
            }

            LOG.severe("columnNames:");
            for (String columnName : columnNames) {
                LOG.severe("\t'" + columnName + "'");
            }

            throw new RuntimeException("severityColumnNames did not match columnNames");
        }
    }

    /**
     * @return the id (from the database) of the event definition
     */
    public int getId() {
        return id;
    }

    /**
     * @return the fleet id (from the database) of the event definition
     */
    public int getFleetId() {
        return fleetId;
    }

    /**
     * @return the name of the event definition
     */
    public String getName() {
        return name;
    }

    /**
     * @return the start buffer time (in seconds0 of the event definition
     */
    public int getStartBuffer() {
        return startBuffer;
    }

    /**
     * @return the stop buffer time (in seconds0 of the event definition
     */
    public int getStopBuffer() {
        return stopBuffer;
    }

    /**
     * @return the airframe id of the event definition
     */
    public int getAirframeNameId() {
        return airframeNameId;
    }

    /**
     * @return the airframe id of the event definition
     */
    public Filter getFilter() {
        return filter;
    }

    /**
     * returns the columnNames hashmap
     *
     * @return the columnNames hashmap
     */
    public TreeSet<String> getColumnNames() {
        return columnNames;
    }

    /**
     * Calculates the maximum value in an array of DoubleTimeSeries at a particular time.
     * It ignores NaNs and returns -Double.MAX_VALUE if all values are NaN
     *
     * @param columns are the DoubleTimeSeries
     * @param time    is the time step in the series
     * @return the maximum value from the given columns at time
     */

    double maxArray(DoubleTimeSeries[] columns, int time) {
        double max = -Double.MAX_VALUE;

        for (int severityColumnId : severityColumnIds) {
            double value = columns[severityColumnId].get(time);
            if (Double.isNaN(value)) continue;
            max = Math.max(value, max);
        }
        return max;
    }

    /**
     * Calculates the maximum absolute value in an array of DoubleTimeSeries at a particular time.
     * It ignores NaNs and returns -Double.MAX_VALUE if all values are NaN
     *
     * @param columns are the DoubleTimeSeries
     * @param time    is the time step in the series
     * @return the maximum absolute value from the given columns at time
     */

    double maxAbsArray(DoubleTimeSeries[] columns, int time) {
        double max = -Double.MAX_VALUE;

        for (int severityColumnId : severityColumnIds) {
            double value = Math.abs(columns[severityColumnId].get(time));
            if (Double.isNaN(value)) continue;
            max = Math.max(value, max);
        }
        return max;
    }

    /**
     * Calculates the minimum value in an array of DoubleTimeSeries at a particular time.
     * It ignores NaNs and returns Double.MAX_VALUE if all values are NaN
     *
     * @param columns are the DoubleTimeSeries
     * @param time    is the time step in the series
     * @return the minimum value from the given columns at time
     */

    double minArray(DoubleTimeSeries[] columns, int time) {
        double min = Double.MAX_VALUE;

        for (int severityColumnId : severityColumnIds) {
            double value = columns[severityColumnId].get(time);
            if (Double.isNaN(value)) continue;
            min = Math.min(value, min);
        }
        return min;
    }

    /**
     * Calculates the minimum absolute value in an array of DoubleTimeSeries at a particular time.
     * It ignores NaNs and returns +Double.MAX_VALUE if all values are NaN
     *
     * @param columns are the DoubleTimeSeries
     * @param time    is the time step in the series
     * @return the minimum absolute value from the given columns at time
     */

    double minAbsArray(DoubleTimeSeries[] columns, int time) {
        double min = Double.MAX_VALUE;

        for (int severityColumnId : severityColumnIds) {
            double value = Math.abs(columns[severityColumnId].get(time));
            if (Double.isNaN(value)) continue;
            min = Math.min(value, min);
        }
        return min;
    }

    /**
     * Gets the severity value for this event definition at time
     *
     * @param columns is an array of DoubleTimeSeries for each column of data used to calculate this event
     * @param time    is the time step the severity is being calculated for
     * @return the severity value from the given columns at time
     */

    public double getSeverity(DoubleTimeSeries[] columns, int time) {
        if (columns.length == 1) {
            switch (severityTypeId) {
                case MIN_ABS_SEVERITY:
                case MAX_ABS_SEVERITY:
                    return Math.abs(columns[0].get(time));

                case MIN_SEVERITY:
                case MAX_SEVERITY:
                    return columns[0].get(time);

                default:
                    LOG.severe("Unknown severity type: '" + severityType + " for EventDefinition: '" + name + "'");
                    System.exit(1);
            }

        } else {
            switch (severityTypeId) {
                case MIN_SEVERITY:
                    return minArray(columns, time);

                case MAX_SEVERITY:
                    return maxArray(columns, time);

                case MIN_ABS_SEVERITY:
                    return minAbsArray(columns, time);

                case MAX_ABS_SEVERITY:
                    return maxAbsArray(columns, time);

                default:
                    LOG.severe("Unknown severity type: '" + severityType + " for EventDefinition: '" + name + "'");
                    System.exit(1);
            }
        }

        // should never get here
        return 0.0;
    }

    /**
     * @param currentSeverity is the current severity value
     * @param doubleSeries    is an array of DoubleTimeSeries for each column of data used to calculate this event
     * @param time            is the time step the severity is being calculated for
     * @return the severity value from the given columns at time i
     */

    public double updateSeverity(double currentSeverity, DoubleTimeSeries[] doubleSeries, int time) {
        switch (severityTypeId) {
            case MAX_ABS_SEVERITY:
            case MAX_SEVERITY:
                return Math.max(currentSeverity, getSeverity(doubleSeries, time));

            case MIN_ABS_SEVERITY:
            case MIN_SEVERITY:
                return Math.min(currentSeverity, getSeverity(doubleSeries, time));

            default:
                System.err.println("Error getting severity for event:  " + this);
                System.err.println("Could not update severity for unknown event severity type: '" + severityType + "', severityTypeId: '" + severityTypeId + "'");
                System.exit(1);
        }

        // should never get here
        return 0;
    }

    /**
     * Have an event definition update itself in the database.
     *
     * @param connection is the connection to the database.
     * @throws SQLException if there is an error with the SQL query
     */
    public void updateSelf(Connection connection) throws SQLException, JsonProcessingException {
        this.columnNames = new TreeSet<>(this.severityColumnNames);
        update(connection, fleetId, id, name, startBuffer, stopBuffer, airframeNameId, OBJECT_MAPPER.writeValueAsString(filter),
                OBJECT_MAPPER.writeValueAsString(severityColumnNames), severityType);
    }

    /**
     * Presents a human-readable description of this event definition.
     *
     * @return a string of a human-readable description of this event definition.
     */
    public String toHumanReadable() {
        if (this.id < 0) {
            String humanReadableStr = filter.toHumanReadable();

            return (humanReadableStr.matches("^[AEIOU].*") ? "An " : "A ") + humanReadableStr;
        }

        String text = (name.matches("^[AEIOU].*") ? "An " : "A ");
        if (startBuffer == 1) {
            text += name + " event occurs when " + filter.toHumanReadable() + " is triggered at least " + startBuffer + " time within " + stopBuffer + " seconds, and ends when no trigger occurs for " + stopBuffer + " seconds.";
        } else {
            text += name + " event occurs when " + filter.toHumanReadable() + " is triggered at least " + startBuffer + " times within " + stopBuffer + " seconds, and ends when no trigger occurs for " + stopBuffer + " seconds.";
        }

        return text;
    }

    /**
     * Returns a string representation of this event definition
     *
     * @return a string representation of this event definition
     */
    public String toString() {
        try {
            return "[id: " + id + ", name: '" + name + "', startBuffer: " + startBuffer + ", stopBuffer: " + stopBuffer + ", " +
                    "airframeNameId: " + airframeNameId + ", condition: " + OBJECT_MAPPER.writeValueAsString(filter) + ", column_names : "
                    + OBJECT_MAPPER.writeValueAsString(columnNames) + ", severity_column_names: " + OBJECT_MAPPER.writeValueAsString(severityColumnNames) + ", severity_type: " + severityType + "]";
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
