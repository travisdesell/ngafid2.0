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
	static final double STD_PRESS_INHG = 29.92;
	static final double COMP_CONV = Math.PI / 180; 
	static final double AOACrit = 15;
	static final double proSpinLim = 4;

	private int flightId, precision;
	private PrintWriter pw;
	private Map<String, DoubleTimeSeries> parameters;

	public LossOfControlCalculation(int flightId, int precision){
		this.flightId = flightId;
		this.precision = precision;
		this.parameters = getParameters(flightId);
		this.pw = null;
	}

	public void printToFile(File file){
		try{
			this.pw = new PrintWriter(file);
			this.pw.println("TIME\tSTALL PROBABILITY\tLOC-I PROBABILITY");
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
			params.put("Roll", DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "Roll"));
		}catch(SQLException e){
			e.printStackTrace();
		}
		return params;		
	}

	private double lag(DoubleTimeSeries series, int index){
		double currIndex = series.get(index);
		if(index > 1){
			return currIndex - series.get(index -1);
		}
		return currIndex;
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
		double yawRate = 180 - Math.abs(180 - Math.abs(lag(hdg, index) % 360));
		return yawRate;
	}

	private double getRollComp(int index){
		DoubleTimeSeries roll = this.parameters.get("Roll");
		return roll.get(index) * COMP_CONV;
	}

	private double getYawComp(int index){
		return this.getYawRate(index) * COMP_CONV;
	}
	
	private double getVRComp(int index){
		return ((this.getTrueAirspeedFtMin(index) / 60) * this.getYawComp(index));
	}

	private double getCTComp(int index){
		return Math.sin(this.getRollComp(index)) * 32.2;
	}

	private double getCordComp(int index){
	  	return Math.abs(this.getCTComp(index) - this.getVRComp(index)) * 100;
	}

	private double getProSpin(int index){
	  	return Math.min((this.getCordComp(index) / proSpinLim), 100);
	}

	private double calculateStallProbability(int index){
	    double prob = Math.min(((Math.abs(this.getAOASimple(index) / AOACrit)) * 100), 100);
	    return prob;
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
		double prob = (this.calculateStallProbability(i) * this.getProSpin(i)) / 100;
		return prob;
	}


	/**
	 * Calculates the loss of control probability
	 * @return a floating-point percentage of the probability of loss of control
	 */
	public void calculate(){
		System.out.println("Calculating Loss of Control probability for: flight "+flightId);
		DoubleTimeSeries heading = this.parameters.get("Heading");
		double maxsProb = Double.MIN_VALUE;
		double maxlProb = Double.MIN_VALUE;
		for(int i = 0; i<heading.size(); i++){
			double sprob = this.calculateStallProbability(i);
			double lprob = this.calculateProbability(i);
			if(sprob > maxsProb) {
				maxsProb = sprob;
			}
			if(lprob > maxlProb) {
				maxlProb = lprob;
			}
			this.pw.printf(i+"\t\t %."+this.precision+"f"+"\t\t\t\t %."+this.precision+"f\n", sprob, lprob);
		}
		this.pw.println("\n\n MAX STALL PROBABILITY: "+maxsProb);
		this.pw.println("\n\n MAX LOC-I PROBABILITY: "+maxlProb);
		//TODO: implement the caluclation logic here and put parts of the calc. in helper methods 
		pw.close();
	}

	public static void displayHelp(){
		System.err.println("Usage: loci [flight number] [logfile path] [precision]");
		System.exit(0);
	}
	
	/**
	 * Main method for testing
	 * @param args args from the command line, with the first being a filename for output
	 */
	public static void main(String [] args){
		System.out.println("Loss of control calculator");
		if(args.length > 2 && args.length <= 3){
			int flightId = Integer.parseInt(args[0]);
			int precision = Integer.parseInt(args[2]);
			File file = new File(args[1]);
			System.err.println("\n\n");
			System.err.println("+----------- LOCI CALCULATION INFO -----------+");
			System.err.println("| flight_id: "+flightId+"\t\t\t\t|");
			System.err.println("| logfile: "+file.toString()+"\t\t\t|");
			System.err.println("| precision: "+precision+"\t\t\t\t|");
			System.err.println("+---------------------------------------------+");
			System.err.println("\n\n");
			LossOfControlCalculation loc = new LossOfControlCalculation(flightId, precision);
			loc.printToFile(file);
			loc.calculate();
		}else{
			displayHelp();
		}

	}
}
