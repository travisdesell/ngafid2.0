package org.ngafid.events_db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;
import org.ngafid.events.Event;

import org.ngafid.Database;
import org.ngafid.flights.Flight;
import org.ngafid.flights.DoubleTimeSeries;

import org.ngafid.flights.StringTimeSeries;


public class CalculatePitch {
    static double minValue = -4.0;
    static double maxValue = 4.0;
    public static void main(String[] arguments) {
        int flightId = 819;
        String seriesName = "Pitch";
        String timeSeriesName = "Lcl Time";

        Connection connection = Database.getConnection();
        //long startMillis = System.currentTimeMillis();

        try {
            Flight flight = Flight.getFlight(connection, flightId);
            System.out.println("flight id: " + flight.getId());
            System.out.println("flight filename: " + flight.getFilename());

            DoubleTimeSeries pitchSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, seriesName);
            StringTimeSeries timeSeries = StringTimeSeries.getStringTimeSeries(connection, flightId, timeSeriesName);

            //Step 1: Calculate all the pitch events and put them in this pitchEvents ArrayList
            //ArrayList<Event> pitchEvents = ...;

            int startLineNo = -1;
            String startTime = null;
            int endLine= -1;
            String endTime = null;
            int count =0;

            ArrayList<Event> PitchEventList = new ArrayList<>();
            int lineNumber = 0;
            int bufferTime = 5;

            double current;
            for (int i = 0; i < pitchSeries.size(); i++) {
                //generate the pitch events here
                lineNumber = i;
                current = pitchSeries.get(i);
                System.out.println("pitch[" + i + "]: " + current);
                if (current < minValue || current > maxValue){
                    //System.out.println("I am here");
                    if (startTime == null) {
                        startTime = timeSeries.get(i);
                        //System.out.println("time: " + timeSeries.get(i));
                        startLineNo = lineNumber;
                        //System.out.println("line number: "+startLineNo);
                    }
                    endLine = lineNumber;
                    System.out.println("pitch in line: " + "[" + lineNumber + "]" + " with Value: " + "[" + current + "]" + " ended at line: " + "[" + endLine + "]");
                    endTime = timeSeries.get(i);
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
                        PitchEventList.add(event);
                        startTime = null;
                        startLineNo = -1;
                        endLine = -1;
                        endTime = null; 
                    }
                }
            }

            if (startTime != null) {
                Event event = new Event(startTime, endTime , startLineNo, endLine, 0){};
                PitchEventList.add( event );
            }
            System.out.println("");           
            for( int i = 0; i < PitchEventList.size(); i++ ){
                Event event = PitchEventList.get(i);
                System.out.println( "Event : [line:" + event.getStartLine() + " to " + event.getEndLine() + ", time: " + event.getStartTime() + " to " + event.getEntTime() + "]" );
            }

            //Step 2: export the pitch events to the database
            //for (int i = 0; i < pitchEvents.size(); i++) {
            //    pitchEvents.get(i).updateDatabase(connection);
            //}

            /*
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO double_series (flight_id, name, data_type, length, valid_length, min, avg, max, data, ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
            preparedStatement.setInt(1, flightId);
            preparedStatement.setString(2, name);
            preparedStatement.setString(3, dataType);
            preparedStatement.setInt(4, timeSeries.size());
            preparedStatement.setInt(5, validCount);

            if (Double.isNaN(min)) {
                preparedStatement.setNull(6, java.sql.Types.DOUBLE);
            } else {
                preparedStatement.setDouble(6, min);
            }

            if (Double.isNaN(avg)) {
                preparedStatement.setNull(7, java.sql.Types.DOUBLE);
            } else {
                preparedStatement.setDouble(7, avg);
            }

            if (Double.isNaN(max)) {
                preparedStatement.setNull(8, java.sql.Types.DOUBLE);
            } else {
                preparedStatement.setDouble(8, max);
            }

            ByteBuffer byteBuffer = ByteBuffer.allocate(timeSeries.size() * 8);
            for (int i = 0; i < timeSeries.size(); i++) {
                byteBuffer.putDouble(timeSeries.get(i));
            }
            byte[] byteArray = byteBuffer.array();
            System.err.println(preparedStatement);
            Blob seriesBlob = new SerialBlob(byteArray);
            preparedStatement.setBlob(9, seriesBlob);
            preparedStatement.executeUpdate();
            preparedStatement.close();
            */


        } catch(SQLException e) {
            System.err.println(e);
            e.printStackTrace();
            System.exit(1);
        }
        //System.err.println("finished!");
        //long endMillis = System.currentTimeMillis();
        //System.out.println("It took " + (endMillis - startMillis) + " ms to run the code");
        System.exit(1);
    }

}
