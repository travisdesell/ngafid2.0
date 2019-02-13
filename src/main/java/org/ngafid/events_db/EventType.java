package org.ngafid.events_db;

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
import org.ngafid.events_db.CalculateExceedanceNew;


public class EventType {
    // Added to get the "calculateStartEndTime"
    private int bufferTime;
    // Added to get the "calculateStartEndTime"

    // public EventType(EVENT_TYPE eventType, int bufferTime, Expresion expression){

    // }
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

    public void updateEventTable(Connection connection, String eventType, int bufferTime, Expression expression){
        try {
            // Expression expression = new Expression("pitch <= -30.0 && pitch >= -30.0");
            // EventType eventTypeObj = new EventType(eventType, bufferTime, expression);
            // eventTypeObj.updateEventTable(connection);

            String query = "INSERT INTO event_type (name, buffer_time, column_name, condition_text) VALUES (?, ?, ?, ?)";

            // create the mysql insert preparedstatement
            PreparedStatement preparedStmt = connection.prepareStatement(query);
            preparedStmt.setString (1, eventType);
            preparedStmt.setInt (2, bufferTime);
            preparedStmt.setString (3, eventType);
            preparedStmt.setString (4, expression.toString());

            // execute the preparedstatement
            preparedStmt.execute();

        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

}

