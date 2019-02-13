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
                current = eventSeries.get(i);

                //System.out.println("pitch[" + i + "]: " + current);
                if (current < minValue || current > maxValue){
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
                        System.out.println("count: " + count + " with value: " + "[" + current + "]" + " in line: " + lineNumber );
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

            // for (int j = 0; j < eventList.size(); j++) {
            //     Event event = eventList.get(j);
            //     event.updateEventTable(connection, eventType, bufferTime, eventType, EvalExCondition.condition);
            // }

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
        //ArrayList<Integer> flightIds = new ArrayList<Integer>();
        /*
           flightIds.add(672);
           flightIds.add(677);
           flightIds.add(679);
           flightIds.add(713);
           */

        /*
         * TODO:
         * instead of hardcoded flights, get flights from database:
         *
         * pitch_id = 1
         * pitch_low_threshold = -10
         * pitch_high_threshold = 10
         *
         * easy first version:
         * SELECT id FROM flights WHERE NOT EXISTS(SELECT flight_id FROM flights_processed WHERE event_type_id = pitch_id AND flights_processed.flight_id = flights.id) 
         *
         * harder second version:
         * SELECT id FROM flights WHERE NOT EXISTS(SELECT flight_id FROM flights_processed WHERE event_type_id = pitch_id AND flights_processed.flight_id = flights.id) AND NOT EXISTS (SELECT id FROM double_series WHERE name = 'Pitch' AND double_series.flight_id = flights.id AND (min < pitch_low_threshold OR max > pitch_high_threshold))
         *
         */

        try {
            System.err.println("before!");
            System.out.println(EvalExCondition.condition);

            EVENT_TYPE event_type = EVENT_TYPE.Pitch;
            PreparedStatement stmt = connection.prepareStatement("SELECT id FROM flights WHERE NOT EXISTS(SELECT flight_id FROM flight_processed WHERE event_type_id = ? AND flight_processed.flight_id = flights.id)");
            stmt.setInt(1, getEventTypeId( event_type ));
            ResultSet rs = stmt.executeQuery();

            CalculateExceedanceNew pitchCalculator = new CalculateExceedanceNew( -4, 4, 5,  event_type);

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
        /*
           for (int i = 0; i < flightIds.size(); i++) {
           System.err.println(i);
           processFlight(flightIds.get(i));
           }
           */
        //connection.close();
        System.err.println("finished!");
        System.exit(1);
    }
}
