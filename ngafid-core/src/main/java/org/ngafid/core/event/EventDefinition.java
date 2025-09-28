package org.ngafid.core.event;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.ngafid.core.Database;
import org.ngafid.core.flights.Airframes;
import org.ngafid.core.flights.DoubleTimeSeries;
import org.ngafid.core.util.filters.Filter;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class EventDefinition {
    // TODO: Replace with Jackson
    public static final Gson GSON = new GsonBuilder().serializeSpecialFloatingPointValues().create();

    private static final Logger LOG = Logger.getLogger(EventDefinition.class.getName());
    private static final String SQL_FIELDS = "id, fleet_id, name, start_buffer, stop_buffer, airframe_id, " +
            "condition_json, column_names, severity_column_names, severity_type";

    /*
     * Caches event definitions by name. Events are rarely added anyways...
     */
    private static final Map<Integer, String> EVENT_DEFINITION_ID_TO_NAME = new ConcurrentHashMap<>();

    static {
        String query = "SELECT id, name FROM event_definitions";
        try (Connection connection = Database.getConnection(); PreparedStatement ps = connection.prepareStatement(query); ResultSet resultSet = ps.executeQuery()) {
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String name = resultSet.getString("name");
                EVENT_DEFINITION_ID_TO_NAME.put(id, name);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public enum SeverityType {
        MIN,
        MAX,
        MIN_ABS,
        MAX_ABS;

        public double apply(double current, double value) {
            return switch (this) {
                case MIN -> Math.min(current, value);
                case MIN_ABS -> Math.min(current, Math.abs(value));
                case MAX -> Math.max(current, value);
                case MAX_ABS -> Math.max(current, Math.abs(value));
            };
        }

        public double defaultValue() {
            return switch (this) {
                case MIN, MIN_ABS -> Double.MAX_VALUE;
                case MAX, MAX_ABS -> Double.MIN_VALUE;
            };
        }
    }

    private int id = 0;
    private final int fleetId;
    private String name;
    private final int startBuffer;
    private final int stopBuffer;
    private final int airframeNameId;
    private Filter filter;
    private TreeSet<String> columnNames;
    private final TreeSet<String> severityColumnNames;
    private final SeverityType severityType;

    /**
     * Creates an event definition.
     *
     * @param fleetId             the id of the fleet this event applies to. 0 is generic and will apply to all fleets
     * @param name                is the name of the event
     * @param startBuffer         is how many seconds this event needs to occur in before it will be tracked
     * @param stopBuffer          is how many seconds must go by witihout the event occuring before it stops being
     *                            tracked
     * @param airframeNameId      ID of the airframe name
     * @param filter              is the event condition filter
     * @param severityColumnNames a list of column names (unique) which are used to calculate the severity
     * @param severityType        a string representation of the severity type, can be 'min', 'abs' or 'max'
     */
    public EventDefinition(int fleetId, String name, int startBuffer, int stopBuffer, int airframeNameId,
                           Filter filter, TreeSet<String> severityColumnNames, SeverityType severityType) {
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
    public EventDefinition(ResultSet resultSet) throws SQLException {
        this.id = resultSet.getInt(1);
        this.fleetId = resultSet.getInt(2);
        this.name = resultSet.getString(3);
        this.startBuffer = resultSet.getInt(4);
        this.stopBuffer = resultSet.getInt(5);
        this.airframeNameId = resultSet.getInt(6);
        if (id >= 0) {
            try {
                this.filter = GSON.fromJson(resultSet.getString(7), Filter.class);
            } catch (Exception e) {
                System.err.println("Error with filter: " + e);
                System.err.println(resultSet.getString(7));

                e.printStackTrace();
                System.exit(1);
            }
        } else {
            try {
                this.filter = GSON.fromJson(resultSet.getString(7), Filter.class);
            } catch (NullPointerException e) {
                e.printStackTrace();
                this.filter = null;
            }
        }

        this.columnNames = GSON.fromJson(resultSet.getString(8), new TypeToken<TreeSet<String>>() {
        }.getType());
        this.severityColumnNames = GSON.fromJson(resultSet.getString(9), new TypeToken<TreeSet<String>>() {
        }.getType());

        String severityTypeStr = resultSet.getString(10);
        severityTypeStr = severityTypeStr.toUpperCase();
        severityTypeStr = severityTypeStr.replaceAll(" ", "_");
        this.severityType = SeverityType.valueOf(severityTypeStr);

        initializeSeverity();
    }

    /**
     * Get an event definition by specifying the event name
     *
     * @param connection is the DB session
     * @param eventName  is the event name being retrieved
     * @return Event Definition matching the name passed in
     */
    public static EventDefinition getEventDefinition(Connection connection, String eventName) throws SQLException {
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
    public static EventDefinition getEventDefinition(Connection connection, String eventName, int airframeID)
            throws IOException, SQLException {
        String airframeIDStr = "airframe_id = " + airframeID;

        eventName = "name = '" + eventName + "'";
        String query = "SELECT " + SQL_FIELDS + " FROM event_definitions WHERE " + eventName + " AND " + airframeIDStr;

        return getEventDefinitionFromDB(connection, query);
    }

    public static EventDefinition getEventDefinition(Connection connection, int eventID) throws SQLException {
        String eventIDStr = "id = '" + eventID + "'";
        String query = "SELECT " + SQL_FIELDS + " FROM event_definitions WHERE " + eventIDStr;

        return getEventDefinitionFromDB(connection, query);
    }

    public static Map<Integer, String> getEventDefinitionIdToNameMap(Connection connection) throws SQLException {
        return EVENT_DEFINITION_ID_TO_NAME;
    }

    /**
     * Helper method for retrieving the EventDefinition from server
     *
     * @param connection is the DB session
     * @param query      is the query to execute to the DB
     * @return Event Definition result from DB
     */
    private static EventDefinition getEventDefinitionFromDB(Connection connection, String query) throws SQLException {
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
    public static void update(Connection connection, int fleetId, int eventId, String name, int startBuffer,
                              int stopBuffer, String airframe, String filterJson, String severityColumnNamesJson,
                              SeverityType severityType) throws SQLException {
        int airframeNameID = 0;

        if (!airframe.equals("All Airframes")) {
            airframeNameID = new Airframes.Airframe(connection, airframe, null).getId();
        }

        update(connection, fleetId, eventId, name, startBuffer, stopBuffer, airframeNameID, filterJson,
                severityColumnNamesJson, severityType.toString());
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
    public static void update(Connection connection, int fleetId, int eventId, String name, int startBuffer,
                              int stopBuffer, int airframeNameID, String filterJson, String severityColumnNamesJson,
                              String severityType) throws SQLException {
        Filter filter = GSON.fromJson(filterJson, Filter.class);
        String columnNamesJson;

        if (eventId > 0) {
            columnNamesJson = GSON.toJson(filter.getColumnNames());
        } else {
            columnNamesJson = severityColumnNamesJson;
        }

        String query = "UPDATE event_definitions SET fleet_id = ?, name = ?, start_buffer = ?, stop_buffer = ?, " +
                "airframe_id = ?, condition_json = ?, column_names = ?, severity_column_names = ?, severity_type = ? "
                + "WHERE id = ?";

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
    public static void insert(Connection connection, int fleetId, String name, int startBuffer, int stopBuffer,
                              String airframe, String filterJson, String severityColumnNamesJson,
                              String severityType) throws SQLException {
        Filter filter = GSON.fromJson(filterJson, Filter.class);
        TreeSet<String> columnNames = filter.getColumnNames();

        if (airframe.equals("All Airframes")) {
            String query = "INSERT INTO event_definitions SET fleet_id = ?, name = ?, start_buffer = ?, " +
                    "stop_buffer = ?, airframe_id = ?, condition_json = ?, column_names = ?, severity_column_names = "
                    + "?," + " severity_type = ?";

            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setInt(1, fleetId);
                preparedStatement.setString(2, name);
                preparedStatement.setInt(3, startBuffer);
                preparedStatement.setInt(4, stopBuffer);
                preparedStatement.setInt(5, 0);
                preparedStatement.setString(6, filterJson);
                preparedStatement.setString(7, GSON.toJson(columnNames));
                preparedStatement.setString(8, severityColumnNamesJson);
                preparedStatement.setString(9, severityType);

                LOG.info(preparedStatement.toString());
                preparedStatement.executeUpdate();
            }
        } else {
            int airframeNameId = new Airframes.Airframe(connection, airframe, null).getId();
            String query = "INSERT INTO event_definitions SET fleet_id = ?, name = ?, start_buffer = ?, " +
                    "stop_buffer = ?, airframe_id = ?, condition_json = ?, column_names = ?, severity_column_names = "
                    + "?," + " severity_type = ?";

            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setInt(1, fleetId);
                preparedStatement.setString(2, name);
                preparedStatement.setInt(3, startBuffer);
                preparedStatement.setInt(4, stopBuffer);
                preparedStatement.setInt(5, airframeNameId);
                preparedStatement.setString(6, filterJson);
                preparedStatement.setString(7, GSON.toJson(columnNames));
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
    public static void insert(Connection connection, int id, String name, int startBuffer, int stopBuffer,
                              int airframeId) throws SQLException {
        if (id > 0) {
            LOG.info("Passed a positive ID to special event insertion.");
            System.exit(0);
        }

        String query = "INSERT INTO event_definitions SET fleet_id = ?, flight_id = 0, name = ?, " + "start_buffer = "
                + "?, stop_buffer = ?, airframe_id = ?, condition_json = ?, column_names = ?,"
                + "severity_column_names" + " = ?, severity_type = ?, id = ?";

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
    public static ArrayList<EventDefinition> getAll(Connection connection, String extraQuery,
                                                    Object[] extraParameters) throws SQLException {
        String query = "SELECT id, fleet_id, name, start_buffer, stop_buffer, airframe_id, condition_json," + " " +
                "column_names, severity_column_names, severity_type FROM event_definitions WHERE " + extraQuery;

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
     * Gets all of the event definitions from the database.
     *
     * @param connection is the connection to the database.
     * @return an array list of all event definitions in the database.
     */
    public static ArrayList<EventDefinition> getAll(Connection connection) throws SQLException {
        String query = "SELECT id, fleet_id, name, start_buffer, stop_buffer, airframe_id, condition_json, " +
                "column_names, severity_column_names, severity_type FROM event_definitions";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query); ResultSet resultSet =
                preparedStatement.executeQuery()) {
            LOG.info(preparedStatement.toString());

            ArrayList<EventDefinition> allEvents = new ArrayList<EventDefinition>();
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
        String query = "SELECT DISTINCT(name) FROM event_definitions WHERE (event_definitions.fleet_id = 0 " + "OR " +
                "event_definitions.fleet_id = ?)";

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

        String query = "SELECT name FROM event_definitions WHERE (event_definitions.fleet_id = 0 OR " +
                "event_definitions.fleet_id = ?) AND event_definitions.airframe_id = 0";
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
        query = "SELECT name, airframe FROM event_definitions, airframes " +
                "WHERE (event_definitions.fleet_id = 0 OR " + "event_definitions.fleet_id = ?) " +
                "AND airframes.id = event_definitions.airframe_id";

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
        if (!columnNames.containsAll(severityColumnNames)) {
            // there was a column in the severity column names that wasn't in the column names
            LOG.severe("ERROR initializing EventDefinition: '" + this.name + "'");
            LOG.severe("severityColumnNames did not match columnNames");
            LOG.severe("severityColumnNames:");
            for (String columnName : severityColumnNames) {
                LOG.severe("\t'" + columnName + "'");
            }

            LOG.severe("columnNames:");
            for (String columnName : columnNames) {
                LOG.severe("\t'" + columnName + "'");
            }

            LOG.severe("This means that this event definition is no good, id = " + id);

            System.exit(1);
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
     * Gets the severity value for this event definition at time
     *
     * @param columns is an array of DoubleTimeSeries for each column of data used to calculate this event
     * @param time    is the time step the severity is being calculated for
     * @return the severity value from the given columns at time
     */

    public double getSeverity(Map<String, DoubleTimeSeries> columns, int time) {
        return getSeverity(columns, severityType.defaultValue(), time);
    }

    public double getSeverity(Map<String, DoubleTimeSeries> columns, double severity, int time) {
        for (String columnName : severityColumnNames) {
            double value = columns.get(columnName).get(time);
            if (Double.isNaN(value)) continue;
            severity = severityType.apply(severity, value);
        }

        return severity;
    }

    /**
     * Have an event definition update itself in the database.
     *
     * @param connection is the connection to the database.
     * @throws SQLException if there is an error with the SQL query
     */
    public void updateSelf(Connection connection) throws SQLException {
        this.columnNames = new TreeSet<>(this.severityColumnNames);
        update(connection, fleetId, id, name, startBuffer, stopBuffer, airframeNameId, GSON.toJson(filter),
                GSON.toJson(severityColumnNames), severityType.toString());
    }

    /**
     * Deletes an event definition from database
     */
    public void delete(Connection connection) throws SQLException {
        String query = "DELETE FROM event_definitions WHERE id=?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    /**
     * Presents a human-readable description of this event definition.
     *
     * @return a string of a human readable description of this event definition.
     */
    public String toHumanReadable() {
        if (this.id < 0) {
            String humanReadableStr = filter.toHumanReadable();

            return (humanReadableStr.matches("^[AEIOU].*") ? "An " : "A ") + humanReadableStr;
        }

        String text = (name.matches("^[AEIOU].*") ? "An " : "A ");
        if (startBuffer == 1) {
            text += name + " event occurs when " + filter.toHumanReadable() + " is triggered at least " + startBuffer
                    + " time within " + stopBuffer + " seconds, and ends when no trigger occurs for " + stopBuffer
                    + " seconds.";
        } else {
            text += name + " event occurs when " + filter.toHumanReadable() + " is triggered at least " + startBuffer
                    + " times within " + stopBuffer + " seconds, and ends when no trigger occurs for " + stopBuffer
                    + " seconds.";
        }

        return text;
    }

    /**
     * Returns a string representation of this event definition
     *
     * @return a string representation of this event definition
     */
    public String toString() {
        return "[id: " + id + ", name: '" + name + "', startBuffer: " + startBuffer + ", stopBuffer: " + stopBuffer
                + ", airframeNameId: " + airframeNameId +
                ", condition: " + GSON.toJson(filter) + ", column_names : " + GSON.toJson(columnNames)
                + ", severity_column_names: " + GSON.toJson(severityColumnNames) + ", severity_type: " + severityType
                + "]";
    }

}
