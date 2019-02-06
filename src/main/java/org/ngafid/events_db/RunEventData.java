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

import org.ngafid.events_db.CalculatePitch;



public abstract class RunEventData {
    public static double minValue = -4.0;
    public static double maxValue = 4.0;
    // public static double minValue;
    // public static double maxValue;

    public static int eventTypeId = 1;

    public static int bufferTime = 5;
    // public static int bufferTime;

    public static String seriesName = "Pitch";
    //public static String seriesName2 = "Roll";

    public static String timeSeriesName = "Lcl Time";
    public static String dateSeriesName = "Lcl Date";

    public static String pitchSeries;


    public void applyEvent() {

    
     if (eventTypeId == 1){
          minValue = -4.0;
          maxValue = 4.0;
          bufferTime = 5;
        } else {
        System.err.println("Event! Type ID matches the requirements!");
    }

}

    public static void infoFlight(int flightId) {


        Connection connection = Database.getConnection();
        //long startMillis = System.currentTimeMillis();

        try {
            Flight flight = Flight.getFlight(connection, flightId);
            System.out.println("flight id: " + flight.getId());
            ///System.out.println("date: " + flight.getDate());
            System.out.println("flight filename: " + flight.getFilename());

            //SeriesName seriesName = Pitch.seriesName;
            DoubleTimeSeries pitchSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, seriesName);
            //DoubleTimeSeries rollSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, RunEventData.seriesName);
            //DoubleTimeSeries pitchSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, RunEventData.seriesName);

            StringTimeSeries timeSeries = StringTimeSeries.getStringTimeSeries(connection, flightId, timeSeriesName);
            StringTimeSeries dateSeries = StringTimeSeries.getStringTimeSeries(connection, flightId, dateSeriesName);
            

            /*
             * TODO: insert into flights_processed table this flight ID and event id
             */

        } catch(SQLException e) {
            System.err.println(e);
            e.printStackTrace();
            System.exit(1);
        }
    }



}
