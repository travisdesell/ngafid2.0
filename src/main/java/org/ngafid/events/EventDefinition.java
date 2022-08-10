package org.ngafid.events;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.TreeSet;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.ngafid.filters.Filter;

import org.ngafid.flights.Airframes;
import org.ngafid.flights.DoubleTimeSeries;


public class EventDefinition {
    private static final Logger LOG = Logger.getLogger(EventDefinition.class.getName());
    public static final Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();

    public static final int MIN_SEVERITY = 1;
    public static final int MAX_SEVERITY = 2;
    public static final int MIN_ABS_SEVERITY = 3;
    public static final int MAX_ABS_SEVERITY = 4;

    private int id = 0;
    private int fleetId;
    private String name;
    private int startBuffer;
    private int stopBuffer;
    private int airframeNameId;
    private Filter filter;
    private TreeSet<String> columnNames;
    private TreeSet<String> severityColumnNames;
    private String severityType;
    private int[] severityColumnIds;
    private int severityTypeId;

    /**
     *  Initializes the severity column ids and severity type id so that the severity values can
     *  be calculated quickly.
     */
    void initializeSeverity() {
        if (severityType.equals("min")) {
            severityTypeId = MIN_SEVERITY;
        } else if (severityType.equals("max")) {
            severityTypeId = MAX_SEVERITY;
        } else if (severityType.equals("min abs")) {
            severityTypeId = MIN_ABS_SEVERITY;
        } else if (severityType.equals("max abs")) {
            severityTypeId = MAX_ABS_SEVERITY;
        } else {
            LOG.severe("Unknown severity type: '" + severityType + " for EventDefinition: '" + name + "'");
            System.exit(1);
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
            //there was a column in the severity column names that wasn't in the column names
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

            System.exit(1);
        }
    }

    /**
     * Creates an event definition.
     *
     * @param fleetId the id of the fleet this event applies to. 0 is generic and will apply to all fleets
     * @param name is the name of the event
     * @param startBuffer is how many seconds this event needs to occur in before it will be tracked
     * @param stopBuffer is how many seconds must go by witihout the event occuring before it stops being tracked
     * @param filter is the event condition filter
     * @param severityColumnNames a list of column names (unique) which are used to calculate the severity
     * @param severityType a string representation of the severity type, can be 'min', 'abs' or 'max'
     */
    public EventDefinition(int fleetId, String name, int startBuffer, int stopBuffer, int airframeNameId, Filter filter, TreeSet<String> severityColumnNames, String severityType) {
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
                this.filter = gson.fromJson(resultSet.getString(7), Filter.class);
            } catch (Exception e) {
                System.err.println("Error with filter: " + e);
                System.err.println(resultSet.getString(7));

                e.printStackTrace();
                System.exit(1);
            }
            this.columnNames = gson.fromJson(resultSet.getString(8), new TypeToken<TreeSet<String>>(){}.getType());
            this.severityColumnNames = gson.fromJson(resultSet.getString(9), new TypeToken<TreeSet<String>>(){}.getType());
        } else {
            try {
                this.filter = gson.fromJson(resultSet.getString(7), Filter.class);
            } catch (NullPointerException e) {
                this.filter = null;
            }

            this.columnNames = new TreeSet<String>();
            this.severityColumnNames = new TreeSet<String>();
        }

        this.severityType = resultSet.getString(10);

        initializeSeverity();
    }

    public static EventDefinition getEventDefinition(Connection connection, String eventName) {
        EventDefinition eventDef = null;

        eventName = "name = '" + eventName + "'";
        String query = "SELECT id, fleet_id, name, start_buffer, stop_buffer, airframe_id, condition_json, column_names, severity_column_names, severity_type FROM event_definitions WHERE " + eventName;

        try {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            System.out.println(preparedStatement.toString());

            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                eventDef = new EventDefinition(resultSet);
            }

            preparedStatement.close();
            resultSet.close();
        } catch (SQLException se) {
            se.printStackTrace();
        }

        return eventDef;
    }

    public static EventDefinition getEventDefinition(Connection connection, int eventID) {
        EventDefinition eventDef = null;

        String eventIDStr = "id = '" + eventID + "'";
        String query = "SELECT id, fleet_id, name, start_buffer, stop_buffer, airframe_id, condition_json, column_names, severity_column_names, severity_type FROM event_definitions WHERE " + eventIDStr;

        try {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            System.out.println(preparedStatement.toString());

            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                eventDef = new EventDefinition(resultSet);
            }

            preparedStatement.close();
            resultSet.close();
        } catch (SQLException se) {
            se.printStackTrace();
        }

        return eventDef;
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
     * @param time is the time step in the series
     */

    double maxArray(DoubleTimeSeries[] columns, int time) {
        double max = -Double.MAX_VALUE;

        for (int i = 0; i < severityColumnIds.length; i++) {
            double value = columns[severityColumnIds[i]].get(time);
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
     * @param time is the time step in the series
     */

    double maxAbsArray(DoubleTimeSeries[] columns, int time) {
        double max = -Double.MAX_VALUE;

        for (int i = 0; i < severityColumnIds.length; i++) {
            double value = Math.abs(columns[severityColumnIds[i]].get(time));
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
     * @param time is the time step in the series
     */

    double minArray(DoubleTimeSeries[] columns, int time) {
        double min = Double.MAX_VALUE;

        for (int i = 0; i < severityColumnIds.length; i++) {
            double value = columns[severityColumnIds[i]].get(time);
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
     * @param time is the time step in the series
     */

    double minAbsArray(DoubleTimeSeries[] columns, int time) {
        double min = Double.MAX_VALUE;

        for (int i = 0; i < severityColumnIds.length; i++) {
            double value = Math.abs(columns[severityColumnIds[i]].get(time));
            if (Double.isNaN(value)) continue;
            min = Math.min(value, min);
        }
        return min;
    }


    /**
     * Gets the severity value for this event definition at time 
     *
     * @param columns is an array of DoubleTimeSeries for each column of data used to calculate this event
     * @param i is the time step the severity is being calculated for
     *
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

        //should never get here
        return 0.0;
    }


    /**
     * @param columns is an array of DoubleTimeSeries for each column of data used to calculate this event
     * @param i is the time step the severity is being calculated for
     *
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
                System.err.println("Error getting severity for event:  " + toString());
                System.err.println("Could not update severity for unknown event severity type: '" + severityType + "', severityTypeId: '" + severityTypeId + "'");
                System.exit(1);
        }

        //should never get here
        return 0;
    }


    /**
     * Updates an existing event definition into the database.
     *
     * @param connection is the connection to the database.
     */
    public static void update(Connection connection, int fleetId, int eventId, String name, int startBuffer, int stopBuffer, String airframe, String filterJson, String severityColumnNamesJson,  String severityType) throws SQLException {
        Filter filter = gson.fromJson(filterJson, Filter.class);
        TreeSet<String> columnNames = filter.getColumnNames();

        if (airframe.equals("All Airframes")) {
            String query = "UPDATE event_definitions SET fleet_id = ?, name = ?, start_buffer = ?, stop_buffer = ?, airframe_id = ?, condition_json = ?, column_names = ?, severity_column_names = ?, severity_type = ? WHERE id = ?";

            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, fleetId);
            preparedStatement.setString(2, name);
            preparedStatement.setInt(3, startBuffer);
            preparedStatement.setInt(4, stopBuffer);
            preparedStatement.setInt(5, 0);
            preparedStatement.setString(6, filterJson);
            preparedStatement.setString(7, gson.toJson(columnNames));
            preparedStatement.setString(8, severityColumnNamesJson);
            preparedStatement.setString(9, severityType);
            preparedStatement.setInt(10, eventId);

            LOG.info(preparedStatement.toString());
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } else {
            int airframeNameId = Airframes.getNameId(connection, airframe);
            String query = "UPDATE event_definitions SET fleet_id = ?, name = ?, start_buffer = ?, stop_buffer = ?, airframe_id = ?, condition_json = ?, column_names = ?, severity_column_names = ?, severity_type = ? WHERE id = ?";

            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, fleetId);
            preparedStatement.setString(2, name);
            preparedStatement.setInt(3, startBuffer);
            preparedStatement.setInt(4, stopBuffer);
            preparedStatement.setInt(5, airframeNameId);
            preparedStatement.setString(6, filterJson);
            preparedStatement.setString(7, gson.toJson(columnNames));
            preparedStatement.setString(8, severityColumnNamesJson);
            preparedStatement.setString(9, severityType);
            preparedStatement.setInt(10, eventId);

            LOG.info(preparedStatement.toString());
            preparedStatement.executeUpdate();
            preparedStatement.close();
        }
    }


    /**
     * Inserts this event definition into the database.
     *
     * @param connection is the connection to the database.
     */
    public static void insert(Connection connection, int fleetId, String name, int startBuffer, int stopBuffer, String airframe, String filterJson, String severityColumnNamesJson,  String severityType) throws SQLException {
        Filter filter = gson.fromJson(filterJson, Filter.class);
        TreeSet<String> columnNames = filter.getColumnNames();

        if (airframe.equals("All Airframes")) {
            String query = "INSERT INTO event_definitions SET fleet_id = ?, name = ?, start_buffer = ?, stop_buffer = ?, airframe_id = ?, condition_json = ?, column_names = ?, severity_column_names = ?, severity_type = ?";

            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, fleetId);
            preparedStatement.setString(2, name);
            preparedStatement.setInt(3, startBuffer);
            preparedStatement.setInt(4, stopBuffer);
            preparedStatement.setInt(5, 0);
            preparedStatement.setString(6, filterJson);
            preparedStatement.setString(7, gson.toJson(columnNames));
            preparedStatement.setString(8, severityColumnNamesJson);
            preparedStatement.setString(9, severityType);

            LOG.info(preparedStatement.toString());
            preparedStatement.executeUpdate();
            preparedStatement.close();
        } else {
            int airframeNameId = Airframes.getNameId(connection, airframe);
            String query = "INSERT INTO event_definitions SET fleet_id = ?, name = ?, start_buffer = ?, stop_buffer = ?, airframe_id = ?, condition_json = ?, column_names = ?, severity_column_names = ?, severity_type = ?";

            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, fleetId);
            preparedStatement.setString(2, name);
            preparedStatement.setInt(3, startBuffer);
            preparedStatement.setInt(4, stopBuffer);
            preparedStatement.setInt(5, airframeNameId);
            preparedStatement.setString(6, filterJson);
            preparedStatement.setString(7, gson.toJson(columnNames));
            preparedStatement.setString(8, severityColumnNamesJson);
            preparedStatement.setString(9, severityType);

            LOG.info(preparedStatement.toString());
            preparedStatement.executeUpdate();
            preparedStatement.close();
        }
    }

    /**
     * Inserts this event definition into the database.
     * This is meant for special events (those that have a negative ID).
     *
     * @param connection is the connection to the database.
     */
    public static void insert(Connection connection, int id, String name, int startBuffer, int stopBuffer, int airframeId) throws SQLException {
        if (id > 0) {
            LOG.info("Passed a positive ID to special event insertion.");
            System.exit(0);
        }

        String query = "INSERT INTO event_definitions SET fleet_id = ?, flight_id = 0, name = ?, start_buffer = ?, stop_buffer = ?, airframe_id = ?, condition_json = ?, column_names = ?, severity_column_names = ?, severity_type = ?, id = ?";

        PreparedStatement preparedStatement = connection.prepareStatement(query);
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
        preparedStatement.close();
    }


    /**
     * Gets all  event definitions from the database with a query.
     *
     * @param connection is the connection to the database.
     * @param extraQuery is a string of extra SQL conditions
     * @param extraParameters are the parameters to that query
     *
     * @return the event definitions from the database.
     */
    public static ArrayList<EventDefinition> getAll(Connection connection, String extraQuery, Object[] extraParameters) throws SQLException {
        String query = "SELECT id, fleet_id, name, start_buffer, stop_buffer, airframe_id, condition_json, column_names, severity_column_names, severity_type FROM event_definitions WHERE " + extraQuery;

        PreparedStatement preparedStatement = connection.prepareStatement(query);
        for (int i = 0; i < extraParameters.length; i++) {
            if (extraParameters[i] instanceof String) {
                preparedStatement.setString(i + 1, (String)extraParameters[i]);
            } else if (extraParameters[i] instanceof Integer) {
                preparedStatement.setInt(i + 1, (Integer)extraParameters[i]);
            } else if (extraParameters[i] instanceof Double) {
                preparedStatement.setDouble(i + 1, (Double)extraParameters[i]);
            } else {
                LOG.severe("unknown parameter to event definition query: " + extraParameters[i]);
                System.exit(1);
            }
        }

        LOG.info(preparedStatement.toString());
        ResultSet resultSet = preparedStatement.executeQuery();

        ArrayList<EventDefinition> allEvents = new ArrayList<EventDefinition>();
        while (resultSet.next()) {
            allEvents.add(new EventDefinition(resultSet));
        }
        resultSet.close();
        preparedStatement.close();


        return allEvents;
    }


    /**
     * Gets all of the event definitions from the database.
     *
     * @param connection is the connection to the database.
     *
     * @return an array list of all event definitions in the database.
     */
    public static ArrayList<EventDefinition> getAll(Connection connection) throws SQLException {
        String query = "SELECT id, fleet_id, name, start_buffer, stop_buffer, airframe_id, condition_json, column_names, severity_column_names, severity_type FROM event_definitions";

        PreparedStatement preparedStatement = connection.prepareStatement(query);
        LOG.info(preparedStatement.toString());
        ResultSet resultSet = preparedStatement.executeQuery();

        ArrayList<EventDefinition> allEvents = new ArrayList<EventDefinition>();
        while (resultSet.next()) {
            allEvents.add(new EventDefinition(resultSet));
        }
        resultSet.close();
        preparedStatement.close();

        return allEvents;
    }

    /**
     * Gets a list of all the event definition names, will only have one name for non-generic events.
     *
     * @param connection is the connection to the database.
     * @param fleetId is the fleet id for the event definitions
     *
     * @return an array list of all event names in the database for this fleet
     */
    public static ArrayList<String> getUniqueNames(Connection connection, int fleetId) throws SQLException {
        //add all the generic event names
        String query = "SELECT DISTINCT(name) FROM event_definitions WHERE (event_definitions.fleet_id = 0 OR event_definitions.fleet_id = ?)";

        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, fleetId);

        LOG.info(preparedStatement.toString());
        ResultSet resultSet = preparedStatement.executeQuery();

        ArrayList<String > uniqueNames = new ArrayList<>();
        while (resultSet.next()) {
            uniqueNames.add(resultSet.getString(1));
        }
        resultSet.close();
        preparedStatement.close();

        return uniqueNames;
    }

    /**
     * Gets a list of all the event definition names. Will have "- airframe" appended to it for non-generic events.
     *
     * @param connection is the connection to the database.
     * @param fleetId is the fleet id for the event definitions
     *
     * @return an array list of all event names in the database for this fleet
     */
    public static ArrayList<String> getAllNames(Connection connection, int fleetId) throws SQLException {
        //add all the generic event names
        String query = "SELECT name FROM event_definitions WHERE (event_definitions.fleet_id = 0 OR event_definitions.fleet_id = ?) AND event_definitions.airframe_id = 0";

        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, fleetId);

        LOG.info(preparedStatement.toString());
        ResultSet resultSet = preparedStatement.executeQuery();

        ArrayList<String > allNames = new ArrayList<>();
        while (resultSet.next()) {
            allNames.add(resultSet.getString(1));
        }
        resultSet.close();
        preparedStatement.close();

        //add all the event names with the airframe they are for
        query = "SELECT name, airframe FROM event_definitions, airframes WHERE (event_definitions.fleet_id = 0 OR event_definitions.fleet_id = ?) AND airframes.id = event_definitions.airframe_id";

        preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, fleetId);

        LOG.info(preparedStatement.toString());
        resultSet = preparedStatement.executeQuery();

        while (resultSet.next()) {
            allNames.add(resultSet.getString(1) + " - " + resultSet.getString(2));
        }
        resultSet.close();
        preparedStatement.close();

        return allNames;
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
        return "[id: " + id + ", name: '" + name + "', startBuffer: " + startBuffer + ", stopBuffer: " + stopBuffer + ", airframeNameId: " + airframeNameId +
            ", condition: " + gson.toJson(filter) + ", column_names : " + gson.toJson(columnNames) + ", severity_column_names: " + gson.toJson(severityColumnNames) + ", severity_type: " + severityType + "]";
    }

}
