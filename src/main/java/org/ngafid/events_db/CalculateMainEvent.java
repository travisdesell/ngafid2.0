package org.ngafid.events_db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.ngafid.events.Event;

import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.FileNotFoundException;

import java.nio.ByteBuffer;

import org.ngafid.Database;
import org.ngafid.flights.Flight;
import org.ngafid.flights.DoubleTimeSeries;

import org.ngafid.flights.StringTimeSeries;

import org.ngafid.events_db.RunEventData;

public class CalculateMainEvent {

    public static void processFlight(int flightId) {

        Connection connection = Database.getConnection();
        //long startMillis = System.currentTimeMillis();

        try {
            Flight flight = Flight.getFlight(connection, flightId);
            System.out.println("flight id: " + flight.getId());
            System.out.println("flight filename: " + flight.getFilename());

            DoubleTimeSeries pitchSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, RunEventData.seriesName);

            StringTimeSeries timeSeries = StringTimeSeries.getStringTimeSeries(connection, flightId, RunEventData.timeSeriesName);
            StringTimeSeries dateSeries = StringTimeSeries.getStringTimeSeries(connection, flightId, RunEventData.dateSeriesName);

            if (pitchSeries == null) {

                return;
            } 


            if (timeSeries == null || dateSeries == null) {
                return;
            }

            // for (int k = 0; k < timeSeries.size(); k++) {
            //     String dateTime = timeSeries.get(k) + " " + dateSeries.get(k);
            //     System.out.println(dateTime);
            // }

            //Step 1: Calculate all the pitch events and put them in this pitchEvents ArrayList
            //ArrayList<Event> pitchEvents = ...;

            int startLineNo = -1;
            String startTime = null;
            int endLine = -1;
            String endTime = null;
            int count =0;

            ArrayList<Event> mainEventList = new ArrayList<>();
            int lineNumber = 0;
            // int bufferTime = 5;

            double current;
            //double mainSeries;

            // if (RunEventData.eventTypeId == 1)
            //     for (int k = 0; k < pitchSeries.size(); k++) {
            //      double mainSeries = pitchSeries.get(k);
            //      System.out.println(mainSeries);
            // }

            if (RunEventData.eventTypeId == 1){
                for (int i = 0; i < pitchSeries.size(); i++) {
                    //generate the pitch events here
                    lineNumber = i;
                    current = pitchSeries.get(i);
                    //System.out.println("pitch[" + i + "]: " + current);
                    if (current < RunEventData.minValue || current > RunEventData.maxValue){
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
                        if (count == RunEventData.bufferTime){
                            System.err.println("Exceed the bufer range and New event created!!");
                        }

                        if (startTime !=null && count == RunEventData.bufferTime){
                            Event event = new Event (startTime, endTime, startLineNo, endLine, 0){};
                            mainEventList.add(event);
                            startTime = null;
                            startLineNo = -1;
                            endLine = -1;
                            endTime = null; 
                        }
                    }
                }
            }

            if (startTime != null) {
                Event event = new Event(startTime, endTime, startLineNo, endLine, 0){};
                mainEventList.add( event );
            }
            System.out.println("");

            for( int i = 0; i < mainEventList.size(); i++ ){
                Event event = mainEventList.get(i);
                System.out.println( "Event : [line:" + event.getStartLine() + " to " + event.getEndLine() + ", time: " + event.getStartTime() + " to " + event.getEndTime() + "]" );
            }
            //Step 2: export the pitch events to the database

            for (int i = 0; i < mainEventList.size(); i++) {
                Event event = mainEventList.get(i);

                event.updateDatabase(connection, flightId, RunEventData.eventTypeId);
                //System.out.println("=============this is event type ID: " + (RunEventData.eventTypeId)); 
                //System.out.println("=============this is buffer time: " + (RunEventData.bufferTime));
            }

            /*
             * TODO:
             * update flight_processed table
             * INSERT INTO flight_processed SET flight_id = ? AND event_type_id = ?
             */

            PreparedStatement stmt = connection.prepareStatement("INSERT INTO flight_processed SET flight_id = ?, event_type_id = ?");
            stmt.setInt(1, flightId);
            stmt.setInt(2, RunEventData.eventTypeId);

            System.out.println(stmt.toString());
            stmt.executeUpdate();


            /*
             * TODO: insert into flights_processed table this flight ID and event id
             */

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

            //int pitchId = 1;
            //int eventTypeId = pitchId;

            PreparedStatement stmt = connection.prepareStatement("SELECT id FROM flights WHERE NOT EXISTS(SELECT flight_id FROM flight_processed WHERE event_type_id = ? AND flight_processed.flight_id = flights.id)");
            stmt.setInt(1, RunEventData.eventTypeId);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("id");
                System.out.println("=======Going to process flight with number: " + id );

                processFlight(id);
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
