package org.ngafid;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;


import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.ngafid.flights.FlightAlreadyExistsException;
import org.ngafid.flights.FatalFlightFileException;

import org.ngafid.flights.Flight;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.StringTimeSeries;

import org.ngafid.events.Event;
import org.ngafid.events2.PitchEvent;

public class ProcessFile {
    static double minValue = -2.0;
    static double maxValue = 2.0;
    public static void main(String[] arguments) {
        //String filename = "/Users/fa3019/Code/ngafid2.0/example_data/C172/log_110812_095915_KCKN.csv";
        String filename = "./example_data/C172/log_110812_095915_KCKN.csv";

        try {
            Flight flight = new Flight(filename, null);

            DoubleTimeSeries pitchSeries = flight.getDoubleTimeSeries("Pitch");
            StringTimeSeries timeSeries = flight.getStringTimeSeries("Lcl Time");
            //System.out.println();
            int startLineNo = -1;
            String startTime = null;
            List<Event> eventList = new ArrayList<>();
            int lineNumber = 0;

            int bufferTime = 5;

            long startMillis = System.currentTimeMillis();

            for (int i = 0; i < pitchSeries.size(); i++) {
                lineNumber = i + 4;
                double current = pitchSeries.get(i);
                //String time = timeSeries.get(i);
                //System.out.println("Line: " + lineNumber + " Pitch Event: " +pitchSeries.get(i));
                System.out.println( "Line: " + lineNumber + ", Pitch: " + current);

                if (current < minValue || current > maxValue) {
                    if (startTime == null) {
                        startTime = timeSeries.get(i);
                        startLineNo = lineNumber;
                    }

                } else {
                    if (startTime != null) {
                        Event event = new Event(startTime, timeSeries.get(i-1), startLineNo, lineNumber-1, 0){};
                        eventList.add( event );
                        startTime = null;
                        startLineNo = -1;
                    }
                }
            }

            if (startTime != null) {
                Event event = new Event(startTime, timeSeries.get( timeSeries.size()-1 ) , startLineNo, lineNumber, 0){};
                eventList.add( event );
            }

            //Update end time and line
            int listSize = eventList.size();
            for( int i = 0; i < listSize; i++ ){
                if(i > 0 && ( eventList.get(i).getStartLine() - eventList.get(i-1).getEndLine() ) < bufferTime ){
                    System.out.print( "Going to update event [" +  eventList.get(i-1).getStartLine() + "," + eventList.get(i-1).getStartTime() + " , " + eventList.get(i-1).getEndLine() + "," + eventList.get(i-1).getEntTime() + "] -> " );
                    eventList.get(i-1).updateEnd(eventList.get(i).getEntTime() , eventList.get(i).getEndLine());

                    System.out.println( " [" +  eventList.get(i-1).getStartLine() + "," + eventList.get(i-1).getStartTime() + " , " + eventList.get(i-1).getEndLine() + "," + eventList.get(i-1).getEntTime() + "] " );
                    eventList.remove(i);
                    listSize--;
                    i--;
                }
            }
            for( int i = 0; i < eventList.size(); i++ ){
                Event event = eventList.get(i);
                System.out.println( "Event : [line:" + event.getStartLine() + " to " + event.getEndLine() + ", time: " + event.getStartTime() + " to " + event.getEntTime() + "]" );
            }
            System.out.println("I am here");

            long endMillis = System.currentTimeMillis();

            System.out.println("It took " + (endMillis - startMillis) + " ms to run the code");

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.err.println("finished!");
    }

}
