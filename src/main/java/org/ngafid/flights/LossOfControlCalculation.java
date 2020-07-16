/**
 * Loss of control calculator for calculating exceedences
 * @author <a href = mailto:apl1341@cs.rit.edu>Aidan LaBella @ RIT CS</a>
 */

package org.ngafid.flights;

import java.util.*;

import java.sql.Connection;
import java.sql.SQLException;

import org.ngafid.Database;
import org.ngafid.flights.DoubleTimeSeries;

public class LossOfControlCalculation{
	static Connection connection = Database.getConnection();
	private int flightId;
	private Map<String, DoubleTimeSeries> parameters;

	public LossOfControlCalculation(int flightId){
		this.flightId = flightId;
		this.parameters = getParameters(flightId);
	}

	static Map<String, DoubleTimeSeries> getParameters(int flightId){
		Map<String, DoubleTimeSeries> params = new HashMap<>();
		try{
			params.put("Heading", DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "HDG"));
			params.put("IAS", DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "IAS"));
			params.put("VSPD", DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "VSpd"));	
		}catch(SQLException e){
			e.printStackTrace();
		}
		return params;		
	}

	/**
	 * Calculates the loss of control probability
	 * @return a floating-point percentage of the probability of loss of control
	 */
	public double calculate(){
		System.out.println("Calculating Loss of Control probability for: flight "+flightId);
		//TODO: implement the caluclation logic here and put parts of the calc. in helper methods 
		return 0.0;
	}
	
	/**
	 * Main method for testing
	 * @param args args from the command line
	 */
	public static void main(String [] args){
		System.out.println("Loss of control calculator");
		LossOfControlCalculation loc = new LossOfControlCalculation(1);
		System.out.println(loc.calculate());
	}
}
