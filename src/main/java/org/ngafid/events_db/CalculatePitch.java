package org.ngafid.events_db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;
import org.ngafid.events.Event;

import java.nio.ByteBuffer;

import org.ngafid.Database;
import org.ngafid.flights.Flight;
import org.ngafid.flights.DoubleTimeSeries;

import org.ngafid.flights.StringTimeSeries;


public class CalculatePitch {
    static double minValue = -4.0;
    static double maxValue = 4.0;

    public static void processFlight(int flightId) {
        int eventType = 1;
        String seriesName = "Pitch";
        String timeSeriesName = "Lcl Time";
        String dateSeriesName = "Lcl Date";

        Connection connection = Database.getConnection();
        //long startMillis = System.currentTimeMillis();

        try {
            Flight flight = Flight.getFlight(connection, flightId);
            System.out.println("flight id: " + flight.getId());
            ///System.out.println("date: " + flight.getDate());
            System.out.println("flight filename: " + flight.getFilename());

            DoubleTimeSeries pitchSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, seriesName);

            StringTimeSeries timeSeries = StringTimeSeries.getStringTimeSeries(connection, flightId, timeSeriesName);
            StringTimeSeries dateSeries = StringTimeSeries.getStringTimeSeries(connection, flightId, dateSeriesName);

            /*
            for (int k = 0; k < timeSeries.size(); k++) {
                String dateTime = timeSeries.get(k) + " " + dateSeries.get(k);

                System.out.println(dateTime);
            }
            */

            //Step 1: Calculate all the pitch events and put them in this pitchEvents ArrayList
            //ArrayList<Event> pitchEvents = ...;

            int startLineNo = -1;
            String startTime = null;
            int endLine = -1;
            String endTime = null;
            int count =0;

            ArrayList<Event> pitchEventList = new ArrayList<>();
            int lineNumber = 0;
            int bufferTime = 5;

            double current;
            for (int i = 0; i < pitchSeries.size(); i++) {
                //generate the pitch events here
                lineNumber = i;
                current = pitchSeries.get(i);
                //System.out.println("pitch[" + i + "]: " + current);
                if (current < minValue || current > maxValue){
                    //System.out.println("I am here");
                    if (startTime == null) {
                        startTime = timeSeries.get(i) + " " + dateSeries.get(i);
                        //System.out.println("time: " + timeSeries.get(i));
                        startLineNo = lineNumber;
                        //System.out.println("line number: "+startLineNo);

                    }
                    endLine = lineNumber;
                    //System.out.println("pitch in line: " + "[" + lineNumber + "]" + " with Value: " + "[" + current + "]" + " ended at line: " + "[" + endLine + "]");
                    endTime = timeSeries.get(i) + " " + dateSeries.get(i);
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
                        pitchEventList.add(event);
                        startTime = null;
                        startLineNo = -1;
                        endLine = -1;
                        endTime = null; 
                    }
                }
            }

            if (startTime != null) {
                Event event = new Event(startTime, endTime, startLineNo, endLine, 0){};
                pitchEventList.add( event );
            }
            System.out.println("");

            for( int i = 0; i < pitchEventList.size(); i++ ){
                Event event = pitchEventList.get(i);
                System.out.println( "Event : [line:" + event.getStartLine() + " to " + event.getEndLine() + ", time: " + event.getStartTime() + " to " + event.getEndTime() + "]" );
            }
            //Step 2: export the pitch events to the database

            for (int i = 0; i < pitchEventList.size(); i++) {
                Event event = pitchEventList.get(i);
                
                //make sure you know the flightID and the eventType number
                event.updateDatabase(connection, flightId, eventType, bufferTime);
                System.out.println( "startDateTime : [line:" + event.getMyStartDate() + " to " + event.getMyEndDate() + "]" );
                System.out.println( "bufferTime added to database: [" + bufferTime+ "]" );
            }

            /*
             * TODO: insert into flights_processed table this flight ID and event id
             */

        } catch(SQLException e) {
            System.err.println(e);
            e.printStackTrace();
            System.exit(1);
        }
        System.err.println("finished!");
        //long endMillis = System.currentTimeMillis();
        //System.out.println("It took " + (endMillis - startMillis) + " ms to run the code");
        System.exit(1);
    }

    public static void main(String[] arguments) {
        ArrayList<Integer> flightIds = new ArrayList<Integer>();

        flightIds.add(672);
        flightIds.add(677);
        flightIds.add(679);
        flightIds.add(713);

        /*
         * TODO:
         * instead of hardcoded flights, get flights from database:
         *
         * pitch_id = 1
         * pitch_low_threshold = -10
         * pitch_high_threshold = 10
         *
         * SELECT id FROM flights WHERE NOT EXISTS(SELECT flight_id FROM flights_processed WHERE event_type_id = pitch_id AND flights_processed.flight_id = flights.id) AND NOT EXISTS (SELECT id FROM double_series WHERE name = 'Pitch' AND double_series.flight_id = flights.id AND (min < pitch_low_threshold OR max > pitch_high_threshold))
         *
         */

        for (int i = 0; i < flightIds.size(); i++) {
            processFlight(flightIds.get(i));
        }
    }
}
