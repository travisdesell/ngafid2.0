package org.ngafid.events;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.io.FileNotFoundException;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;

import org.ngafid.flights.StringTimeSeries;

import java.security.MessageDigest;

import java.sql.Statement;

import javax.xml.bind.DatatypeConverter;

import com.udojava.evalex.Expression;


public abstract class Event {
    private String startTime;
    private String endTime;

    // Added to get the "calculateStartEndTime"
    public String myStartDateTime;
    public String myEndDateTime;

    private int startLine;
    private int endLine;

    private int bufferTime;
    // Added to get the "calculateStartEndTime"

    public Event(String startTime, String endTime, int startLine, int endLine, int bufferTime) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.startLine = startLine;
        this.endLine = endLine;
        this.bufferTime = bufferTime;
    }

    public void updateEnd(String newEndTime, int newEndLine) {
        endTime = newEndTime;
        endLine = newEndLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public String toString() {
        return  "[line " + startLine + " to " + endLine + ", time " + startTime + " to " + endTime + "]";
    }

    public boolean isFinished(int currentLine, ArrayList<String> lineValues) {
        if ((currentLine - endLine) >= bufferTime) {
            return true;
        } else {
            return false;
        }
    }
    public void setStartTime( String startTime ){
        this.startTime = startTime;
    }

    public void setEndTime( String endTime ){
        this.endTime = endTime;
    }

    public void setStartLine( int startLine ){
        this.startLine = startLine;
    }

    public void setEndLine( int endLine ){
        this.endLine = endLine;
    }

    public void setBufferTime( int bufferTime ){
        this.bufferTime = bufferTime;
    }

    public int getStartLine(){
        return startLine;
    }

    public String getStartTime(){
        return startTime;
    }

    public String getEndTime(){
        return endTime;
    }

    public int getBufferTime(){
        return bufferTime;
    }

    /*
     *  TODO: javadocs for everything!
     */


    public void updateDatabase(Connection connection, int flightId, int eventType) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO events (flight_id, event_type, start_line, end_line, start_time, end_time) VALUES (?, ?, ?, ?, ?, ?)");
            preparedStatement.setInt(1, flightId);
            preparedStatement.setInt(2, eventType);
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

            System.err.println(preparedStatement);

            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}

