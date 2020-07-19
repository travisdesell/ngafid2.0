/**
 * Loss of control calculator for calculating exceedences
 * @author <a href = mailto:apl1341@cs.rit.edu>Aidan LaBella @ RIT CS</a>
 */

package org.ngafid.flights;

import java.util.*;

import java.lang.Math;

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

	private double getVSpdAt(int index){
		DoubleTimeSeries vspd = this.parameters.get("VSPD");
		return vspd.get(index);
	}

	private double getIASAt(int index){
		DoubleTimeSeries ias = this.parameters.get("IAS");
		return ias.get(index);
	}

	/**
	 * Calculate the Angle of Attack
	 * @param index the {@link DoubleTimeSeries} index for which instant the LOC probability is calculated
	 *
	 * @return a double representing Angle of Attack
	 */
	private double calculateAOA(int index){
		double b = (this.getVSpdAt(index) * this.beta()) / (this.getIASAt(index) * this.beta() * 101.267); //TODO: figure out what these constants mean?
		b = Math.asin(b);

		//TODO: implement the phi / cos gamma here
		
		return 0.0;
	}

	/**
	 * Beta is the part of the equation with the inverted radical
	 * TODO: figure out what the deltas represnt and other constants to make this code more readable
	 */
	private double beta(){
		return 0.0;
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
