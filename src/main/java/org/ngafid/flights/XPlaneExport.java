package org.ngafid.flights;

import org.ngafid.Database;
import org.ngafid.flights.DoubleTimeSeries;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import java.sql.Connection;
import java.sql.SQLException;

import java.util.Map;
import java.util.HashMap;

import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.IOException;

import org.ngafid.WebServer;

import static org.ngafid.flights.XPlaneParameters.*;

/**
 * A Class that creates X-Plane FDR files for X-Plane
 * @author <a href = apl1341@cs.rit.edu>Aidan LaBella @ RIT CS</a>
 */

public abstract class XPlaneExport{
	protected static Connection connection = Database.getConnection();
	protected String aircraftPath;
	private String startDateTime;
	protected StringWriter dataOut;
	protected Flight flight;
	protected Map<String, DoubleTimeSeries> parameters;

	/**
	 * Defualt constructor for X-Plane exports
	 * @param flightId the flightId to create the export for
	 */
	public XPlaneExport(int flightId, String aircraftPath){
		try{
			this.aircraftPath = aircraftPath+",";
			this.flight = Flight.getFlight(connection, flightId);
			this.parameters = getSeriesData(connection, flightId);
			this.startDateTime = flight.getStartDateTime();
			this.dataOut = this.export();
		}catch (SQLException e){
			this.dataOut = new StringWriter();
			this.dataOut.write(e.toString());
		}
	}

	/**
	 * Creates a hashmap with references to {@link DoubleTimeSeries} instances
	 * for a given flight parameter
	 * @param connection the connection to the database
	 * @param flightId the flight for which to retrieve data for
	 * @return a Map with the pertinent data
	 */
	public static Map<String, DoubleTimeSeries> getSeriesData(Connection connection, int flightId) throws SQLException{
		Map<String, DoubleTimeSeries> seriesData = new HashMap<>();

		seriesData.put(ALT_MSL, DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "AltMSL"));
		seriesData.put(LATITUDE, DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "Latitude"));
		seriesData.put(LONGITUDE, DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "Longitude"));
		seriesData.put(HEADING, DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "HDG"));
		seriesData.put(PITCH, DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "Pitch"));
		seriesData.put(ROLL, DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "Roll"));
		seriesData.put(IAS, DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "IAS"));
	    seriesData.put(E1_RPM, DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "E1 RPM"));
	    seriesData.put(E1_EGT, DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "E1 EGT2"));

		return seriesData;
	}

	/**
	 * Converts the time in the database to a format X-Plane will recognize
	 * i.e. HH:MM:SS.MS -> HH:MM:SS
	 *
	 * @return a String with the new formatted time
	 */
	private String getTime(){
		System.out.println("FOUND TIME:");
		System.out.println(this.startDateTime);

		String [] timeStrings = this.startDateTime.split(" ");

		String zuluTime = (timeStrings[1].split("\\."))[0];

		System.out.println(zuluTime);
		return zuluTime;
	}

	/**
	 * Converts the date in the database to a date X-Plane will recognize
	 * i.e. YYYY-MM-DD --> MM/DD/YY
	 *
	 * @return a String with the new formatted date
	 */
	private String getDate(){
		String [] timeStrings = this.startDateTime.split(" ");

		String [] dateParts = timeStrings[0].split("-");

		String year = dateParts[0].substring(2,4);
		String month = dateParts[1];
		String day = dateParts[2];

		String date = month+"/"+day+"/"+year;

		return date;

	}

	/**
	 * Creates an export using MustacheFactory
	 * @return an instance of a StringWriter that contains the export in the respective *.fdr format
	 */
	private StringWriter export(){
		HashMap<String, Object> scopes = new HashMap<String, Object>();

		scopes.put(ENDL, POSIX_ENDL);
		scopes.put(TAIL, TAIL.toUpperCase()+","+flight.getTailNumber()+",");
		scopes.put(ACFT, ACFT.toUpperCase()+","+this.aircraftPath);
		scopes.put(TIME, TIME.toUpperCase()+","+ this.getTime()+",");
		scopes.put(DATE, DATE.toUpperCase()+","+ this.getDate()+",");

		StringBuffer sb = new StringBuffer();
		this.writeFlightData(sb, scopes);

		String templateFile = WebServer.MUSTACHE_TEMPLATE_DIR + "template.fdr";

		MustacheFactory mf = new DefaultMustacheFactory();
		Mustache mustache = mf.compile(templateFile);

		scopes.put(DATA, sb.toString());
		scopes.put(COMM, COMM.toUpperCase()+",Flight " + flight.getId() +",");

		StringWriter stringOut = new StringWriter();

		try{
			mustache.execute(new PrintWriter(stringOut), scopes).flush();
		}catch(IOException e){
			stringOut.write(e.toString());
		}

		return stringOut;
	}

    /**
     * Returns a string full of 0's, follwed by a comma, to poplulate the extra as null in the FDR format.
     * If we start tracking other data, we can change this method to include such data
     * @param nZeros the number of 0's
     * @return a string in the format 0(0),0(1),...0(n),
     */
    protected String getZeros(int nZeros){
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i<nZeros; i++){
            sb.append(NULL_DATA);
        }
        return sb.toString();
    }

	/**
	 * Fills the data block with the flight data for the version of X-Plane requested
	 * @param buffer the {@link StringBuffer} to write to 
	 * @param scopes the scopes used by {@link MustacheFactory}
	 */
	protected abstract void writeFlightData(StringBuffer buffer, Map<String, Object> scopes);

	/**
	 * Returns the file as a String
	 * @return a {@link String} instance of the data
	 */
	public String toFdrFile(){
		return dataOut.toString();
	}
}

	
