package org.ngafid.events_db;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;

import java.util.ArrayList;
import java.util.logging.Logger;


import org.ngafid.Database;
import org.ngafid.events.Event;

public class EventType {
    private static final Logger LOG = Logger.getLogger(EventType.class.getName());

    private int id;
    private String name;
    private int bufferTime;
    private String conditionText;
    private String columnNames;

    public EventType(String name, int bufferTime, String columnNames, String conditionText) {
        this.id = -1;
        this.name = name;
        this.bufferTime = bufferTime;
        this.columnNames = columnNames;
        this.conditionText = conditionText;
    }

    /**
     * Creates a EventType object from a result set
     *
     * @param A ResultSet object from a database query
     */
    public EventType(ResultSet resultSet) throws SQLException {
        this.id = resultSet.getInt(1);
        this.name = resultSet.getString(2);
        this.bufferTime = resultSet.getInt(3);
        this.columnNames = resultSet.getString(4);
        this.conditionText = resultSet.getString(5);
    }

    /** 
     * Gets all event types in the database.
     *
     * @param connection The database connection.
     *
     * @exception SQLException If there was a query/database problem.
     *
     * @return An array list of all event type entries in the database. Can be empty if there are none.
     */

    public static ArrayList<EventType> getAll(Connection connection) throws SQLException {
        PreparedStatement query = connection.prepareStatement("SELECT id, name, buffer_time, column_names, condition_text FROM event_type");

        LOG.info(query.toString());
        ResultSet resultSet = query.executeQuery();

        ArrayList<EventType> allEvents = new ArrayList<EventType>();

        while (resultSet.next()) {
            allEvents.add(new EventType(resultSet));
        }   

        return allEvents;
    }   

    /**
     * Returns a string representation of this event type.
     *
     * @return A string represntation of this event type.
     */
    public String toString() {
        return "[EVENT TYPE: '" + name + "', bufferTime: " + bufferTime + ", columnNames: '" + columnNames + "', conditionText: '" + conditionText + "']";
    }

    public void setBufferTime( int bufferTime ){
        this.bufferTime = bufferTime;
    }

    public int getBufferTime(){
        return bufferTime;
    }

    //this should be in its own file because we only need to insert the event types to 
    //the database one time
    // Expression expression = new Expression("pitch <= -30.0 && pitch >= -30.0");
    // EventType eventTypeObj = new EventType(eventType, bufferTime, expression);
    // eventTypeObj.updateEventTable(connection);

    // Expression expression = new Expression("roll <= -22.0 && roll >= 15.0");
    // EventType eventTypeObj = new EventType(eventType, bufferTime, expression);
    // eventTypeObj.updateDatabase(connection);

    public void updateDatabase(Connection connection) {
        try {
            // Expression expression = new Expression("pitch <= -30.0 && pitch >= -30.0");
            // EventType eventTypeObj = new EventType(eventType, bufferTime, expression);
            // eventTypeObj.updateEventTable(connection);

            String query = "INSERT INTO event_type (name, buffer_time, column_names, condition_text) VALUES (?, ?, ?, ?)";

            // create the mysql insert preparedstatement
            PreparedStatement preparedStmt = connection.prepareStatement(query);
            preparedStmt.setString (1, name);
            preparedStmt.setInt (2, bufferTime);
            preparedStmt.setString (3, columnNames);
            preparedStmt.setString (4, conditionText);

            LOG.info(preparedStmt.toString());

            // execute the preparedstatement
            preparedStmt.execute();

            // gets the id of what we just inserted into the database
            ResultSet resultSet = preparedStmt.getGeneratedKeys();
            if (resultSet.next()) {
               this.id = resultSet.getInt(1);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

    public static void main(String[] arguments) {

        //1. get name of new event type
        //2. get condition text of new event type
        //3. get column names

        String name = "LatAc"; //get from user
        int bufferTime = 5; //get from user
        String columnNames = "LatAc"; //get from user
        String conditionText = "LatAc <= -15.0 && LatAc >= 15.0"; //get from user

        EventType eventType = new EventType(name, bufferTime, columnNames, conditionText);

        Connection connection = Database.getConnection();
        eventType.updateDatabase(connection);
    }
}

