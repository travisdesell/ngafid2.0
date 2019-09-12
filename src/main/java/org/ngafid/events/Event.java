package org.ngafid.events;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

import java.util.logging.Logger;

import java.util.ArrayList;

public class Event {
    private static final Logger LOG = Logger.getLogger(Event.class.getName());

    private int id;
    private int flightId;
    private int eventDefinitionId;

    private String startTime;
    private String endTime;

    private int startLine;
    private int endLine;

    public double severity;

    public Event(String startTime, String endTime, int startLine, int endLine, double severity) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.startLine = startLine;
        this.endLine = endLine;
        this.severity = severity;
    }

    /**
     * Creates an event from a mysql query result
     *
     * @param resultSet is the row selected from the database
     */
    public Event(ResultSet resultSet) throws SQLException {
        this.id = resultSet.getInt(1);
        this.flightId = resultSet.getInt(2);
        this.eventDefinitionId = resultSet.getInt(3);
        this.startLine = resultSet.getInt(4);
        this.endLine = resultSet.getInt(5);
        this.startTime = resultSet.getString(6);
        this.endTime = resultSet.getString(7);
        this.severity = resultSet.getDouble(8);
    }


    public void updateEnd(String newEndTime, int newEndLine) {
        endTime = newEndTime;
        endLine = newEndLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public String toString() {
        return  "[line " + startLine + " to " + endLine + ", time " + startTime + " to " + endTime + ", severity: " + severity + "]";
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

    public void updateDatabase(Connection connection, int flightId, int eventDefinitionId) {
        this.flightId = flightId;
        this.eventDefinitionId = eventDefinitionId;

        try {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO events (flight_id, event_definition_id, start_line, end_line, start_time, end_time, severity) VALUES (?, ?, ?, ?, ?, ?, ?)");
            preparedStatement.setInt(1, flightId);
            preparedStatement.setInt(2, eventDefinitionId);
            preparedStatement.setInt(3, startLine);
            preparedStatement.setInt(4, endLine);

            if (startTime.equals(" ")) {
                preparedStatement.setString(5, null);
            } else {
                preparedStatement.setString(5, startTime);
            }

            if (endTime.equals(" ")) {
                preparedStatement.setString(6, null);
            } else {
                preparedStatement.setString(6, endTime);
            }

            preparedStatement.setDouble(7, severity);

            System.err.println(preparedStatement);

            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            System.err.println("ERROR commiting event do database.");
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

    /**
     * Gets all of the event from the database for a given flight.
     *
     * @param connection is the connection to the database.
     * @param flightId the id of the flight for the event list.
     *
     * @return an array list of all events in the database for the given flight id.
     */
    public static ArrayList<Event> getAll(Connection connection, int flightId) throws SQLException {
        String query = "SELECT id, flight_id, event_definition_id, start_line, end_line, start_time, end_time, severity FROM events WHERE flight_id = ? ORDER BY start_time";

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

}

