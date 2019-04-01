package org.ngafid.events;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;


public class Event {
    private String startTime;
    private String endTime;

    // Added to get the "calculateStartEndTime"
    public String myStartDateTime;
    public String myEndDateTime;

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

            if (startTime.equals(" ")) {
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
}

