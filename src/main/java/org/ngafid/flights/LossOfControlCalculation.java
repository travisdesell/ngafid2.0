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

	//Standard atmospheric pressure in in. mercury
	static double STD_PRESS_INHG = 29.92;

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
			params.put("BaroA", DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "BaroA"));
			params.put("Pitch", DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "Pitch"));
		}catch(SQLException e){
			e.printStackTrace();
		}
		return params;		
	}

	private double lag(DoubleTimeSeries series){
		//TODO: implement some sort of lag function that replicates R's implementation
		// see: https://math.stackexchange.com/questions/2548314/what-is-lag-in-a-time-series
		return 0.0;
	}

	private double getVspd(int index){
		DoubleTimeSeries vspd = this.parameters.get("VSPD");
		return vspd.get(index);
	}

	private double getIAS(int index){
		DoubleTimeSeries ias = this.parameters.get("IAS");
		return ias.get(index);
	}

	private double getOAT(int index){
		DoubleTimeSeries oat = this.parameters.get("OAT");
		return oat.get(index);
	}

	private double getBaroPress(int index){
		DoubleTimeSeries press = this.parameters.get("BaroA");
		return press.get(index);
	}

	private double getTempRatio(int index){
		return (273 + getOAT(index)) / 288;
	}

	private double getPressureRatio(int index){
		return this.getBaroPress(index) / STD_PRESS_INHG;
	}

	private double getDensityRatio(int index){
		return this.getPressureRatio(index) / this.getTempRatio(index);
	}

    private double getTrueAirspeed(int index){
        return this.getIAS(index) * Math.pow(this.getDensityRatio(index), -0.5);
    }

	private double getTrueAirspeedFtMin(int index){
		return this.getTrueAirspeed(index) * (6076 / 60);
	}

	private double getVspdGeometric(int index){
		return this.getVspd(index) * Math.pow(this.getDensityRatio(index), -0.5);
	}

	private double getFlightPathAngle(int index){
		double fltPthAngle = Math.asin(this.getVspdGeometric(index) / this.getTrueAirspeedFtMin(index));
		fltPthAngle = fltPthAngle * (180 / Math.PI);
		return fltPthAngle;
	}

	private double getAOASimple(int index){
		DoubleTimeSeries pitch = this.parameters.get("Pitch");
		return pitch.get(index) - this.getFlightPathAngle(index);
	}

	private double getYawRate(int index){
		DoubleTimeSeries hdg = this.parameters.get("Heading"); 
		//double yawRate =  
		//Flight_Data$Yaw_Rate <- (Flight_Data$HDG - lag(Flight_Data$HDG))
		//Flight_Data$Yaw_Rate <-
		// 180 - abs(180 - abs(Flight_Data$HDG - lag(Flight_Data$HDG)) %% 360)
		// TODO: these rates are calculated and stored as a constant for the entire timeseries, I believe
		return 0.0;
	}


	/**
	 * Calculate the Angle of Attack
	 * @param index the {@link DoubleTimeSeries} index for which instant the LOC probability is calculated
	 *
	 * @return a double representing Angle of Attack
	 */
	private double calculateAOA(int index){
		double b = (this.getVspd(index) * this.beta(0,0)) / (this.getIAS(index) * this.beta(0,0) * 101.267); //TODO: figure out what these constants mean?
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
		double d = (273 + this.getOAT(index)) / 288;

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
			this.pw.println(i+"\t\t"+getDensityRatio(i));
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
			loc.calculate();
		}
	}
}
