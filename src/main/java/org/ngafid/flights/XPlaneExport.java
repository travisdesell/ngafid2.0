package org.ngafid.flights;

import org.ngafid.Database;
import org.ngafid.flights.DoubleTimeSeries;

import java.sql.Connection;
import java.sql.SQLException;

import java.util.Map;
import java.util.HashMap;
import java.io.StringWriter;

import static org.ngafid.flights.XPlaneParameters.*;

/**
 * A Class that creates X-Plane FDR files for X-Plane
 * @author <a href = apl1341@cs.rit.edu>Aidan LaBella @ RIT CS</a>
 */

public abstract class XPlaneExport{
	protected static Connection connection = Database.getConnection();
	protected StringWriter dataOut;
	protected Flight flight;
	protected Map<String, DoubleTimeSeries> parameters;

	public XPlaneExport(int flightId){
		try{
			this.flight = Flight.getFlight(connection, flightId);
			this.parameters = getSeriesData(connection, flightId);
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
	 * Creates an export as a StringWriter
	 * @return the text within an X-Plane export as a {@link StringBuffer}
	 */
	protected abstract StringWriter export();

	public String toFdrFile(){
		return dataOut.toString();
	}
}

	
