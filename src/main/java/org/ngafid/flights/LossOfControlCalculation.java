/**
 * Loss of control calculator for calculating exceedences
 * @author <a href = mailto:apl1341@cs.rit.edu>Aidan LaBella @ RIT CS</a>
 */

package org.ngafid.flights;

import java.util.*;

import java.io.PrintWriter;
import java.io.File;
import java.io.FileNotFoundException;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Files;

import java.lang.Math;

import java.sql.Connection;
import java.sql.SQLException;

import org.ngafid.Database;

public class LossOfControlCalculation{
	static Connection connection = Database.getConnection();

	//Standard atmospheric pressure in in. mercury
	static final double STD_PRESS_INHG = 29.92;
	static final double COMP_CONV = Math.PI / 180; 
	static final double AOACrit = 15;
	static final double proSpinLim = 4;

	private int flightId;
	private File file;
	private PrintWriter pw;
	private Map<String, DoubleTimeSeries> parameters;

	public LossOfControlCalculation(int flightId){ 
		this.flightId = flightId;
		this.parameters = getParameters(flightId);
	}

	public LossOfControlCalculation(int flightId, Path path){ 
		this(flightId);
		this.createFileOut(path);
	}

	private void createFileOut(Path path){
		String filename = "/flight_"+flightId+".out";

		file = new File(path.toString()+filename);
		System.out.println("LOCI_CALCULATOR: printing to file "+file.toString()+" for flight #"+this.flightId);

		try{
			this.pw = new PrintWriter(file);
		}catch(FileNotFoundException e) {
			System.err.println("File not writable!");
			System.exit(1);
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
		if(index < series.size() - 1){
			return currIndex - series.get(index + 1);
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
		double yawRate = 180 - Math.abs(180 - Math.abs(lag(hdg, index)) % 360);
		//double yawRate = lag(hdg, index);
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

	private double calculateProbability(int i){
		double prob = (this.calculateStallProbability(i) * this.getProSpin(i)) / 100;
		return prob;
	}


	/**
	 * Calculates the loss of control probability
	 * @return a floating-point percentage of the probability of loss of control
	 */
	public void calculate(){
		System.out.println("LOCI_CALCULATOR: Now calculating Loss of Control probability for: flight "+flightId);
		this.printDetails();

		DoubleTimeSeries loci = new DoubleTimeSeries("LOCI", "double");
		DoubleTimeSeries stallProbability = new DoubleTimeSeries("StallProbability", "double");
		DoubleTimeSeries heading = this.parameters.get("Heading");

		for(int i = 0; i<heading.size(); i++){
			stallProbability.add(this.calculateStallProbability(i));
			loci.add(this.calculateProbability(i));
		}

		loci.updateDatabase(connection, this.flightId);  
		stallProbability.updateDatabase(connection, this.flightId);

		if(this.pw != null){
			this.writeFile(loci, stallProbability);
		}
	}

	public void writeFile(DoubleTimeSeries loci, DoubleTimeSeries sProb){
		try{
			pw.println("Index:\t\t\tStall Probability:\t\t\t\tLOC-I Probability:");
			for(int i = 0; i<loci.size(); i++){
				pw.println(i+"\t\t\t"+sProb.get(i)+"\t\t\t\t"+loci.get(i));
			}
			pw.println("\n\nMaximum Values: ");
			pw.println("Stall Probability: "+sProb.getMax()+" LOC-I: "+loci.getMax());

			pw.println("Average Values: ");
			pw.println("Stall Probability: "+sProb.getAvg()+" LOC-I: "+loci.getAvg());
		}catch (Exception e) { 
			e.printStackTrace();
		}finally{
			pw.close();
		}
	}


	public static void displayHelp(){
		System.err.println("USAGE: loci-calculator [fleet id] [OPTION]");
		System.err.println("fleet_id: the id of the fleet to calculate flights for");
		System.err.println("Options: ");
		System.err.println("-f [directory root]");
		System.err.println("\tPrint calculations to file(s) where the argument is the root directory in which the files will be created in.\n" +
				"\tFilenames will be in the format: flight_N.out, where N is the flight number");
		System.err.println("-n [flight number(s)]");
		System.err.println("\tOnly calculate LOC-I for the flight numbers specified. If specifiying more than one flight, delimit flight numbers by commas with no spaces\n"+
				"\ti.e 10,5,3,65,2");
		System.exit(0);
	}

	public void printDetails(){
		System.err.println("\n\n");
		System.err.println("+------------ LOCI CALCULATION INFO ------------+");
		System.err.println("| flight_id: "+flightId+"\t\t\t\t\t\t\t\t|");
		System.err.println("| logfile: "+(file != null ? file.toString() : "None specified.")+"\t\t\t\t\t\t|");
		System.err.println("+-----------------------------------------------+");
		System.err.println("\n\n");
	}
	
	/**
	 * Main method for running calculations
	 * @param args args from the command line, with the first being a filename for output
	 */
	public static void main(String [] args){
		System.out.println("Loss of control calculator");

		Optional<Path> path = Optional.empty();	
		Optional<int[]> flightNums = Optional.empty();

		int fleetId = 1;

		if(args.length < 1){
			displayHelp();
		}else{
			fleetId = Integer.parseInt(args[0]);
		}

		for(int i = 1; i < args.length; i++) {
			if(args[i].equals("-h") || args[i].equals("--help") || args[i].equals("-help")){
				displayHelp();
				System.exit(0);
			}else if(args[i].equals("-f")) {
				if(i == args.length - 1) {
					System.err.println("No arguments specified for -f option! Exiting!");
					System.exit(1);
				}
				path = Optional.of(FileSystems.getDefault().getPath(args[i+1]));
				if(!Files.exists(path.get())) {
					System.err.println("Non-existent filepath: "+path.get().toString()+", exiting!");
					System.exit(1);
				}else if(!new File(path.get().toUri()).isDirectory()){
					System.err.println("Filepath: "+path.get().toString()+" is not a directory, exiting!");
					System.exit(1);
				}
			}else if(args[i].equals("-n")) {
				if(i == args.length - 1) {
					System.err.println("No arguments specified for -n option! Exiting!");
					System.exit(1);
				}
				String numbers = args[i+1];

				String [] numsAsStrings = numbers.split(",");
				int [] nums = new int[numsAsStrings.length];

				for(int j = 0; j < nums.length; j++){
					nums[j] = Integer.parseInt(numsAsStrings[j]);
				}

				flightNums = Optional.of(nums);
			}
		}

		if(flightNums.isPresent()) {
			int [] nums = flightNums.get();

			for(int i = 0; i < nums.length; i++){
				LossOfControlCalculation loc = path.isPresent() ?
					new LossOfControlCalculation(nums[i], path.get()) : new LossOfControlCalculation(nums[i]);
				loc.calculate();
			}
		} else {
			try{
				int [] nums = Flight.getFlightNumbers(Database.getConnection(), fleetId);
				for(int i = 0; i < nums.length; i++){
					//LossOfControlCalculation loc = path.isPresent() ?
						//new LossOfControlCalculation(nums[i], path.get()) : new LossOfControlCalculation(nums[i]);
					//loc.calculate();
					System.out.println(nums[i]);
				}
				//here assume we will calcaulate for all flights for the given fleet
				//
			}catch (SQLException e) {
				e.printStackTrace();
			}

		}
	}
}
