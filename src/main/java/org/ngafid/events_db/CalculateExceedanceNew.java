package org.ngafid.events_db;

import org.ngafid.Database;
import org.ngafid.events.Event;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.Flight;
import org.ngafid.flights.StringTimeSeries;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import com.udojava.evalex.Expression;

import org.ngafid.events_db.EvalExCondition;
import org.ngafid.events_db.EventType;

public class CalculateExceedanceNew {

    static String timeSeriesName = "Lcl Time";
    static String dateSeriesName = "Lcl Date";

    private EventType eventType;
    private static Expression expression;

    int bufferTime = -1;
    int startLineNo = -1;
    String startTime = null;
    int endLine = -1;
    String endTime = null;
    int count =0;

    public CalculateExceedanceNew(EventType eventType) {
        this.eventType = eventType;
        this.expression = new Expression(eventType.getConditionText());
        this.bufferTime = eventType.getBufferTime();
    }

    //static Expression expression;
    public static void test(double pitch) {
        BigDecimal result = expression.with("pitch", Double.toString(pitch)).eval();
        System.out.println("result for pitch = " + pitch + ": " + result);
    }

    public void processFlight(int flightId) {
        Connection connection = Database.getConnection();
        //long startMillis = System.currentTimeMillis();

        try {
            System.out.println();
            System.out.println();
            System.out.println("Processing flight id: " + flightId);
            System.out.println("For EventType: " + eventType);

            Expression expression = new Expression(eventType.getConditionText());

            Flight flight = Flight.getFlight(connection, flightId);
            System.out.println("flight id is: [ " + flight.getId() + " ]");
            ///System.out.println("date: " + flight.getDate());
            System.out.println("flight filename is: [ " + flight.getFilename() + " ]");
            System.out.println("Number of Column Name(s): [ " + eventType.getColumnNames().length + " ]");

            DoubleTimeSeries[] eventSeriesArr = new DoubleTimeSeries[ eventType.getColumnNames().length ];
            for( int i = 0; i < eventSeriesArr.length; i++ )
                eventSeriesArr[i] = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, eventType.getColumnNames()[i] );

            System.out.println("Event(s) Name are: [ " + eventType.getName() + " ]");

            StringTimeSeries timeSeries = StringTimeSeries.getStringTimeSeries(connection, flightId, timeSeriesName);
            StringTimeSeries dateSeries = StringTimeSeries.getStringTimeSeries(connection, flightId, dateSeriesName);

            if (eventSeriesArr == null || eventSeriesArr.length == 0 || eventSeriesArr[0] == null) {
                //INSERT INTO flight_warnings SET flight_id = ?, message = ?, stack_trace = ''
                //message = "Couldn't calculate Pitch exceedence because flight didn't have pitch data."
                // String message = "Couldn't calculate Pitch exceedence because flight didn't have pitch data";
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

            ArrayList<Event> eventList = new ArrayList<>();
            int lineNumber = 0;

            for (int i = 0; i < eventSeriesArr[0].size(); i++) {
                //generate the pitch events here
                lineNumber = i;
                //current = eventSeries.get(i);
                //System.out.println("pitch[" + i + "]: " + current);
/*
                double currentValue = eventSeries.get(i);
                System.out.println("iteration: " + i + ", columnName: " + eventType.getColumnNames() + ", currentValue: "+ currentValue + ", expression: " + expression);
                System.out.println("all columns: " + eventType.getColumnNames());
                BigDecimal result = null;
*/
                double currentValue = eventSeriesArr[0].get(i);
                BigDecimal result = null;

               if (!Double.isNaN(currentValue)) {

                   Expression partialExpression = expression.with(eventType.getColumnNames()[0], new BigDecimal(currentValue));
                   for( int j = 1; j < eventType.getColumnNames().length; j++ ){
                       currentValue = eventSeriesArr[j].get(i);
                       partialExpression = partialExpression.with(eventType.getColumnNames()[j], new BigDecimal(currentValue));
                   }
                   result = partialExpression.eval();
                }

                // value = columnEventSeries[0].get(i)
                // Expression partialExpression = expression.with(columnNames[0], value);
                // for j = 1 .. number columns
                //      value = columnEventSeries[j].get(i)
                //      Expression partialExpression = paritalExpression.with(columnNames[j], value);
                // result = partialExpression.eval();

                /*if (!Double.isNaN(currentValue)) {
                    result = expression.with(eventType.getColumnName(), Double.toString(currentValue)).eval();
                }*/

                System.out.println("result: " +  result);

                if (result == null || result.compareTo(BigDecimal.ZERO) == 0) {
                    if (startTime !=null)
                        count ++;
                    else
                        count =0;
                    if (startTime != null)
                        System.out.println("count: " + count + " with value: " + "[" + eventSeriesArr[0].get(i) + "]" + " in line: " + lineNumber );
                    if (count == bufferTime){
                        System.err.println("Exceed the buffer range and New event created!!");
                    }

                    if (startTime !=null && count == bufferTime){
                        Event event = new Event (startTime, endTime, startLineNo, endLine, 0){};
                        eventList.add(event);
                        startTime = null;
                        startLineNo = -1;
                        endLine = -1;
                        endTime = null;
                    }
                } else {
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
                event.updateDatabase(connection, flightId, eventType.getId());
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
            stmt.setInt(2, eventType.getId());
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

            System.out.println("All Event Types:");
            ArrayList<EventType> allEvents = EventType.getAll(connection);
            for (int i = 0; i < allEvents.size(); i++) {
                //process  events for this event type
                EventType currentEventType = allEvents.get(i);

                System.out.println("\t" + currentEventType.toString());

                CalculateExceedanceNew currentCalculator = new CalculateExceedanceNew(allEvents.get(i));

                //TODO: update to check and see if the flight could possibly have the exceedence
                PreparedStatement stmt = connection.prepareStatement("SELECT id FROM flights WHERE NOT EXISTS(SELECT flight_id FROM flight_processed WHERE event_type_id = ? AND flight_processed.flight_id = flights.id)");
                // PreparedStatement stmt = connection.prepareStatement("SELECT id FROM flights WHERE NOT EXISTS(SELECT flight_id FROM flight_processed WHERE event_type_id = ? AND flight_processed.flight_id = flights.id) AND NOT EXISTS (SELECT id FROM double_series WHERE name = ? AND double_series.flight_id = flights.id AND (min < (SELECT min_value FROM event_type WHERE id = ?) AND max > (SELECT max_value FROM event_type WHERE id = ?)))");

                stmt.setInt(1, currentEventType.getId());
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    int id = rs.getInt("id");
                    System.out.println("=======Going to process flight with number: [ " + id + " ] =======" );

                    //select min/max from double_series where name = eventType.getColumnName()
                    //don't process flights whose min and max doesn't violate the exceedence

                    currentCalculator.processFlight(id);
                    System.out.println("-----------------------------------------------------------------\n");
                }

                /*
                   PreparedStatement stmtEventType = connection.prepareStatement("SELECT condition_text FROM event_type ");
                   stmtEventType.setString(1, condition);
                   ResultSet rsEventType = stmtEventType.executeQuery();

                   while (rsEventType.next()) {
                   int id = rsEventType.getInt("id");
                   System.out.println("=======Going to process flight with number: " + id );

                   currentCalculator.processFlight(id);
                   System.out.println("-------------------------\n");
                   }
                   */

            }
            //System.out.println("Finished.");

        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }

        //connection.close();
        System.err.println("finished!");
        System.exit(1);
    }
}
