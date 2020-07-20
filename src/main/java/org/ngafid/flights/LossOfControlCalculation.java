/**
 * Loss of control calculator for calculating exceedences
 * @author <a href = mailto:apl1341@cs.rit.edu>Aidan LaBella @ RIT CS</a>
 */

package org.ngafid.flights;

import java.util.*;

import java.io.PrintWriter;
import java.io.File;
import java.io.IOException;

import java.lang.Math;

import java.sql.Connection;
import java.sql.SQLException;

import org.ngafid.Database;
import org.ngafid.flights.DoubleTimeSeries;

public class LossOfControlCalculation{
	static Connection connection = Database.getConnection();
	private int flightId;
	private PrintWriter pw;
	private Map<String, DoubleTimeSeries> parameters;

	public LossOfControlCalculation(int flightId){
		this.flightId = flightId;
		this.parameters = getParameters(flightId);
		this.pw = null;
	}

	public void printToFile(File file){
		try{
			this.pw = new PrintWriter(file);
			this.pw.println("TIME\tLOC-I PROBABILITY");
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
	}

	static Map<String, DoubleTimeSeries> getParameters(int flightId){
		Map<String, DoubleTimeSeries> params = new HashMap<>();
		try{
			params.put("Heading", DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "HDG"));
			params.put("IAS", DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "IAS"));
			params.put("VSPD", DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "VSpd"));	
			params.put("OAT", DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "OAT"));	
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

	private double getOATAt(int index){
		DoubleTimeSeries oat = this.parameters.get("OAT");
		return oat.get(index);
	}

	/**
	 * Calculate the Angle of Attack
	 * @param index the {@link DoubleTimeSeries} index for which instant the LOC probability is calculated
	 *
	 * @return a double representing Angle of Attack
	 */
	private double calculateAOA(int index){
		double b = (this.getVSpdAt(index) * this.beta(0,0)) / (this.getIASAt(index) * this.beta(0,0) * 101.267); //TODO: figure out what these constants mean?
		b = Math.asin(b);

		//TODO: implement the phi / cos gamma here
		
		return 0.0;
	}

	private double delta(int sub){
		return 0.0;
	}

	/**
	 * Beta is the part of the equation with the inverted radical
	 * TODO: figure out what the deltas represnt and other constants to make this code more readable
	 */
	private double beta(int index, int z){
		double n = (1 - (1 - this.delta(1) / this.delta(0)) + (1 - ((-2.94 * Math.pow(10, -5) * z) + .986)));
		double d = (273 + this.getOATAt(index)) / 288;

		return Math.pow((n/d), -2);
	}

	private double calculateProbability(int i){
		return i+1.0;
	}


	/**
	 * Calculates the loss of control probability
	 * @return a floating-point percentage of the probability of loss of control
	 */
	public void calculate(){
		System.out.println("Calculating Loss of Control probability for: flight "+flightId);
		DoubleTimeSeries heading = this.parameters.get("Heading");
		for(int i = 0; i<heading.size(); i++){
			this.pw.println(i+"\t\t"+heading.get(i));
		}
		//TODO: implement the caluclation logic here and put parts of the calc. in helper methods 
		pw.close();
	}
	
	/**
	 * Main method for testing
	 * @param args args from the command line, with the first being a filename for output
	 */
	public static void main(String [] args){
		System.out.println("Loss of control calculator");
		LossOfControlCalculation loc = new LossOfControlCalculation(1);
		if(args.length > 0){
			File file = new File(args[0]);
			System.out.println("Will log to file: "+file.toString());
			loc.printToFile(file);
		}
	}
}
