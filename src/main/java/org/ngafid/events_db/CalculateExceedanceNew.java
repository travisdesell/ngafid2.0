package org.ngafid.events_db;

import org.ngafid.Database;
import org.ngafid.events.Event;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.Flight;
import org.ngafid.flights.StringTimeSeries;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Scanner;
import org.ngafid.events_db.EvalExCondition;

import com.udojava.evalex.Expression;

public class CalculateExceedanceNew {


    public enum EVENT_TYPE { Pitch, Roll, LatAc, NormAc }

    //    int eventTypeId = 1;
    static String timeSeriesName = "Lcl Time";
    static String dateSeriesName = "Lcl Date";

    double minValue = -1;
    double maxValue = -1;
    int bufferTime = -1;
    EVENT_TYPE eventType = null;

    public CalculateExceedanceNew( double minValue, double maxValue, int bufferTime, EVENT_TYPE eventType ){
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.bufferTime = bufferTime;
        this.eventType = eventType;
    }

    static int getEventTypeId( EVENT_TYPE eventType ){
        switch (eventType){
            case Pitch:
                return 1;
            case Roll:
                return 2;
            case LatAc:
                return 3;
            case NormAc:
                return 4;
        }
        return -1;
    }
    static String getEventTypeName( String eventType){
        return eventType;

    }

    public void processFlight(int flightId) {
        Connection connection = Database.getConnection();
        //long startMillis = System.currentTimeMillis();

        try {
            Flight flight = Flight.getFlight(connection, flightId);
            System.out.println("flight id: " + flight.getId());
            ///System.out.println("date: " + flight.getDate());
            System.out.println("flight filename: " + flight.getFilename());

            DoubleTimeSeries eventSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, eventType.name());

            StringTimeSeries timeSeries = StringTimeSeries.getStringTimeSeries(connection, flightId, timeSeriesName);
            StringTimeSeries dateSeries = StringTimeSeries.getStringTimeSeries(connection, flightId, dateSeriesName);

            if (eventSeries == null) {
                //INSERT INTO flight_warnings SET flight_id = ?, message = ?, stack_trace = ''
                //message = "Couldn't calculate Pitch exceedence because flight didn't have pitch data."
                // String message = "Couldn't calculate Pitch exceedence because flight didn't have pitch data";
                // //INSERT INTO flights_processed SET flight_id = ?, event_type_id = ?
                // PreparedStatement stmt = connection.prepareStatement("INSERT INTO flight_warnings SET flight_id = ?, message = ?, stack_trace = ''");
                // stmt.setInt(1, flightId);
                // stmt.setString(2, message);
                // StringWriter sw = new StringWriter();
                // PrintWriter pw = new PrintWriter(sw);
                // exception.printStackTrace(pw);
                // String sStackTrace = sw.toString(); // stack trace as a string
                // exceptionPreparedStatement.setString(3, sStackTrace);
                // System.out.println(stmt.toString());
                // stmt.executeUpdate();

                return;
            } 

            if (timeSeries == null || dateSeries == null) {
                //INSERT INTO flight_warnings SET flight_id = ?, message = ?, stack_trace = ''
                //message = "Couldn't calculate Pitch exceedence because flight didn't have time or date data."
                //INSERT INTO flights_processed SET flight_id = ?, event_type_id = ?

                return;
            }

            // for (int k = 0; k < timeSeries.size(); k++) {
            //     String dateTime = timeSeries.get(k) + " " + dateSeries.get(k);
            //     System.out.println(dateTime);
            // }

            //Step 1: Calculate all the pitch events and put them in this pitchEvents ArrayList
            int startLineNo = -1;
            String startTime = null;
            int endLine = -1;
            String endTime = null;
            int count =0;

            ArrayList<Event> eventList = new ArrayList<>();
            int lineNumber = 0;
            double current;
            for (int i = 0; i < eventSeries.size(); i++) {
                //generate the pitch events here
                lineNumber = i;
                //current = eventSeries.get(i);

                //System.out.println("pitch[" + i + "]: " + current);
                if (eventSeries.get(i) < minValue || eventSeries.get(i) > maxValue){
                    //System.out.println("I am here");
                    if (startTime == null) {
                        startTime = dateSeries.get(i) + " " + timeSeries.get(i);
                        //System.out.println("time: " + timeSeries.get(i));
                        System.out.println("date==========time: " + startTime);
                        startLineNo = lineNumber;
                        //System.out.println("line number: "+startLineNo);
                    }
                    endLine = lineNumber;
                    //System.out.println("pitch in line: " + "[" + lineNumber + "]" + " with Value: " + "[" + current + "]" + " ended at line: " + "[" + endLine + "]");
                    endTime = dateSeries.get(i) + " " + timeSeries.get(i);
                    count =0;

                } else {
                    if (startTime !=null)
                        count ++;
                    else
                        count =0;
                    if (startTime != null)
                        System.out.println("count: " + count + " with value: " + "[" + eventSeries.get(i) + "]" + " in line: " + lineNumber );
                    if (count == bufferTime){
                        System.err.println("Exceed the bufer range and New event created!!");
                    }

                    if (startTime !=null && count == bufferTime){
                        Event event = new Event (startTime, endTime, startLineNo, endLine, 0){};
                        eventList.add(event);
                        startTime = null;
                        startLineNo = -1;
                        endLine = -1;
                        endTime = null; 
                    }
                }
            }

            if (startTime != null) {
                Event event = new Event(startTime, endTime, startLineNo, endLine, 0){};
                eventList.add( event );
            }
            System.out.println("");

            for( int i = 0; i < eventList.size(); i++ ){
                Event event = eventList.get(i);
                System.out.println( "Event : [line:" + event.getStartLine() + " to " + event.getEndLine() + ", time: " + event.getStartTime() + " to " + event.getEndTime() + "]" );
            }
            //Step 2: export the pitch events to the database
            for (int i = 0; i < eventList.size(); i++) {
                Event event = eventList.get(i);
                event.updateDatabase(connection, flightId, getEventTypeId( eventType ));
            }

            // Expression expression = new Expression("pitch <= -30.0 && pitch >= -30.0");
            // EventType eventTypeObj = new EventType(eventType, bufferTime, expression);
            // eventTypeObj.updateDatabase(connection);           

            /*
            event.updateEventTable(connection, eventType, bufferTime, eventType, expression);

            for (int j = 0; j < eventList.size(); j++) {
                Event event = eventList.get(j);
                event.updateEventTable(connection, eventType, bufferTime, expression);
            }
            */

            PreparedStatement stmt = connection.prepareStatement("INSERT INTO flight_processed SET flight_id = ?, event_type_id = ?");
            stmt.setInt(1, flightId);
            stmt.setInt(2, getEventTypeId(eventType));
            System.out.println(stmt.toString());
            stmt.executeUpdate();       

        } catch(SQLException e) {
            System.err.println(e);
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] arguments) {

        Connection connection = Database.getConnection();

        try {
            System.err.println("before!");
            //System.out.println(EvalExCondition.condition);

            Scanner user_input = new Scanner( System.in );
            String thresholdName;
            System.out.print("Please Enter your threshold name inirial Capitial word (ex. Pitch): ");
            thresholdName = user_input.next( );
    
            String operator;
            System.out.print("Please Enter your operator (eaither || or &&): ");
            operator = user_input.next();
    

            EVENT_TYPE event_type = EVENT_TYPE.Pitch;
            //EVENT_TYPE event_type = EVENT_TYPE.thresholdName;

            PreparedStatement stmt = connection.prepareStatement("SELECT id FROM flights WHERE NOT EXISTS(SELECT flight_id FROM flight_processed WHERE event_type_id = ? AND flight_processed.flight_id = flights.id)");
            stmt.setInt(1, getEventTypeId( event_type ));
            ResultSet rs = stmt.executeQuery();
            CalculateExceedanceNew pitchCalculator = new CalculateExceedanceNew( -4, 4, 5,  event_type);

            String conditionsCode;
            // System.out.print("Please Enter your condition: ");
            //conditionsCode = thresholdName + " < " + pitchCalculator.minValue + operator + " " +thresholdName + " > " + pitchCalculator.minValue;
            conditionsCode = pitchCalculator.eventType.toString() + " < " + pitchCalculator.minValue  + " " + operator + " " +pitchCalculator.eventType.toString() + " > " + pitchCalculator.maxValue;

            System.out.println("condition Entered as: " + " [" + conditionsCode + "] " +"\n");

            // Expression expression = new Expression("pitch <= -30.0 && pitch >= -30.0");
            Expression expression = new Expression(conditionsCode);          
          
            EventType eventTypeObj = new EventType();
            eventTypeObj.updateEventTable(connection,pitchCalculator.eventType.toString(),pitchCalculator.bufferTime,expression);

            while (rs.next()) {
                int id = rs.getInt("id");
                System.out.println("=======Going to process flight with number: " + id );

                pitchCalculator.processFlight(id);
                System.out.println("-------------------------\n");
            }
            System.err.println("after!");
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        } 

        //connection.close();
        System.err.println("finished!");
        System.exit(1);
    }
}
