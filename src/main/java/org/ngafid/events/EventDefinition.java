package org.ngafid.events;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.ngafid.filters.Filter;
import org.ngafid.flights.Airframes;


public class EventDefinition {
    private static final Logger LOG = Logger.getLogger(EventDefinition.class.getName());
    public static final Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();

    private int id = -1;
    private int fleetId;
    private String name;
    private int startBuffer;
    private int stopBuffer;
    private int airframeId;
    private Filter filter;
    private HashSet<String> columnNames;

    /**
     * Creates an event definition.
     *
     * @param fleetId the id of the fleet this event applies to. 0 is generic and will apply to all fleets
     * @param name is the name of the event
     * @param startBuffer is how many seconds this event needs to occur in before it will be tracked
     * @param stopBuffer is how many seconds must go by witihout the event occuring before it stops being tracked
     * @param filter is the event condition filter
     */
    public EventDefinition(int fleetId, String name, int startBuffer, int stopBuffer, int airframeId, Filter filter) {
        this.fleetId = fleetId;
        this.startBuffer = startBuffer;
        this.stopBuffer = stopBuffer;
        this.airframeId = airframeId;
        this.filter = filter;
        this.columnNames = filter.getColumnNames();
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
        this.airframeId = resultSet.getInt(6);
        this.filter = gson.fromJson(resultSet.getString(7), Filter.class);
        this.columnNames = gson.fromJson(resultSet.getString(8), new TypeToken<HashSet<String>>(){}.getType());
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
    public int getAirframeId() {
        return airframeId;
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
    public HashSet<String> getColumnNames() {
        return columnNames;
    }

    /**
     * Inserts this event definition into the database.
     *
     * @param connection is the connection to the database.
     */
    public static void insert(Connection connection, int fleetId, String name, int startBuffer, int stopBuffer, String airframe, String filterJson) throws SQLException {
        Filter filter = gson.fromJson(filterJson, Filter.class);
        HashSet<String> columnNames = filter.getColumnNames();

        if (airframe.equals("All Airframes (Generic)")) {
            String query = "INSERT INTO event_definitions SET fleet_id = ?, name = ?, start_buffer = ?, stop_buffer = ?, airframe_id = ?, condition_json = ?, column_names = ?";

            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, fleetId);
            preparedStatement.setString(2, name);
            preparedStatement.setInt(3, startBuffer);
            preparedStatement.setInt(4, stopBuffer);
            preparedStatement.setInt(5, 0);
            preparedStatement.setString(6, filterJson);
            preparedStatement.setString(7, gson.toJson(columnNames));

            LOG.info(preparedStatement.toString());
            preparedStatement.executeUpdate();
        } else {
            int airframeId = Airframes.getId(connection, airframe);
            String query = "INSERT INTO event_definitions SET fleet_id = ?, name = ?, start_buffer = ?, stop_buffer = ?, airframe_id = ?, condition_json = ?, column_names = ?";

            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, fleetId);
            preparedStatement.setString(2, name);
            preparedStatement.setInt(3, startBuffer);
            preparedStatement.setInt(4, stopBuffer);
            preparedStatement.setInt(5, airframeId);
            preparedStatement.setString(6, filterJson);
            preparedStatement.setString(7, gson.toJson(columnNames));

            LOG.info(preparedStatement.toString());
            preparedStatement.executeUpdate();
        }
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
        String query = "SELECT id, fleet_id, name, start_buffer, stop_buffer, airframe_id, condition_json, column_names FROM event_definitions WHERE " + extraQuery;

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
        String query = "SELECT id, fleet_id, name, start_buffer, stop_buffer, airframe_id, condition_json, column_names FROM event_definitions";

        PreparedStatement preparedStatement = connection.prepareStatement(query);
        LOG.info(preparedStatement.toString());
        ResultSet resultSet = preparedStatement.executeQuery();

        ArrayList<EventDefinition> allEvents = new ArrayList<EventDefinition>();
        while (resultSet.next()) {
            allEvents.add(new EventDefinition(resultSet));
        }

        return allEvents;
    }

    /**
     * Gets a list of all the event definition names.
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

        //add all the event names with the airframe they are for
        query = "SELECT name, airframe FROM event_definitions, airframes WHERE (event_definitions.fleet_id = 0 OR event_definitions.fleet_id = ?) AND airframes.id = event_definitions.airframe_id";

        preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, fleetId);

        LOG.info(preparedStatement.toString());
        resultSet = preparedStatement.executeQuery();

        while (resultSet.next()) {
            allNames.add(resultSet.getString(1) + " - " + resultSet.getString(2));
        }

        return allNames;
    }

    /**
     * Presents a human-readable description of this event definition.
     *
     * @return a string of a human readable description of this event definition.
     */
    public String toHumanReadable() {
        String text = "";
        if (startBuffer == 1) {
            text = "A " + name + " event occurs when " + filter.toHumanReadable() + " is triggered at least " + startBuffer + " time within " + stopBuffer + " seconds, and ends when no trigger occurs for " + stopBuffer + " seconds.";
        } else {
            text = "A " + name + " event occurs when " + filter.toHumanReadable() + " is triggered at least " + startBuffer + " times within " + stopBuffer + " seconds, and ends when no trigger occurs for " + stopBuffer + " seconds.";
        }
        return text;
    }

    /**
     * Returns a string representation of this event definition
     *
     * @return a string representation of this event definition
     */
    public String toString() {
        return "[id: " + id + ", name: '" + name + "', startBuffer: " + startBuffer + ", stopBuffer: " + stopBuffer + ", airframeId: " + airframeId +
            ", condition: " + gson.toJson(filter) + ", column_names : " + gson.toJson(columnNames) + "]";
    }

}
