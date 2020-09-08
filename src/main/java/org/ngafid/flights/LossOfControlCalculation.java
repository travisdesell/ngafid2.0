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
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.ngafid.Database;
import org.ngafid.filters.Filter;

import static org.ngafid.flights.LossOfControlParameters.*;

public class LossOfControlCalculation{
	static Connection connection = Database.getConnection();

	//Standard atmospheric pressure in in. mercury
	private int fleetId, flightId;
	private File file;
	private Optional<PrintWriter> pw;
	private Map<String, DoubleTimeSeries> parameters;

	/**
	 * Constructor 
	 *
	 * @param fleetId the id of the fleet being calculated
	 * @param flightID the flightId of the flight being processed
	 */
	public LossOfControlCalculation(int fleetId, int flightId){ 
		this.fleetId = fleetId;
		this.flightId = flightId;
		this.pw = Optional.empty();
	}

	/**
	 * Constructor 
	 *
	 * @param fleetId the id of the fleet being calculated
	 * @param flightID the flightId of the flight being processed
	 * @param path the filepath ROOT directory to print logfiles too
	 * */
	public LossOfControlCalculation(int fleetId, int flightId, Path path){ 
		this(fleetId, flightId);
		this.createFileOut(path);
	}

	/**
	 * Creates an output filestream
	 * @param path the path of the file to write
	 */
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

	/**
	 * Gets references to {@link DoubleTimeSeries} objects and places them in a Map
	 *
	 * @param flightId the flightId to get data for
	 *
	 * @return a map with the {@link DoubleTimeSeries} references
	 */
	static Map<String, DoubleTimeSeries> getParameters(int flightId){
		Map<String, DoubleTimeSeries> params = new HashMap<>();
		try{
			for(String param : dtsParamStrings) {
				DoubleTimeSeries series = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, param);
				if(series == null){
					return null;
				} else {
					params.put(param, series);
				}
			}
		}catch(SQLException e){
			e.printStackTrace();
		}
		return params;		
	}

	/**
	 * Determines if a dataset is calculatable
	 *
	 * @return true if it is calculatable, false otherwise
	 */
	public boolean notCalcuatable(){
		return this.parameters == null;
	}

	/**
	 * Gets the lagged difference of two points in a {@link DoubleTimeSeries}
	 * The difference is between the current index and the one prior
	 * 
	 * @param series the {@link DoubleTimeSeries} to lag
	 * @param index the start index
	 */
	private double lag(DoubleTimeSeries series, int index){
		double currIndex = series.get(index);
		if(index < series.size() - 1){
			return currIndex - series.get(index + 1);
		}
		return currIndex;
	}

	/**
	 * Gets the vertical speed at a given index
	 *
	 * @param index the index of the {@link DoubleTimeSeries} to access
	 *
	 * @return a double with the value at the given index
	 */
	private double getVspd(int index){
		DoubleTimeSeries vspd = this.parameters.get(VSPD);
		return vspd.get(index);
	}

	/**
	 * Gets the indicated airspeed at a given index
	 *
	 * @param index the index of the {@link DoubleTimeSeries} to access
	 *
	 * @return a double with the value at the given index
	 */
	private double getIAS(int index){
		DoubleTimeSeries ias = this.parameters.get(IAS);
		return ias.get(index);
	}

	/**
	 * Gets the outside air temp at a given index
	 *
	 * @param index the index of the {@link DoubleTimeSeries} to access
	 *
	 * @return a double with the value at the given index
	 */
	private double getOAT(int index){
		DoubleTimeSeries oat = this.parameters.get(OAT);
		return oat.get(index);
	}

	/**
	 * Gets the barometric pressure at a given index
	 *
	 * @param index the index of the {@link DoubleTimeSeries} to access
	 *
	 * @return a double with the value at the given index
	 */
	private double getBaroPress(int index){
		DoubleTimeSeries press = this.parameters.get(BARO_A);
		return press.get(index);
	}

	/**
	 * Calculates the temperature ratio at a given index
	 *
	 * @param index the index of the {@link DoubleTimeSeries} to access
	 *
	 * @return a double with the value at the given index
	 */
	private double getTempRatio(int index){
		return (273 + getOAT(index)) / 288;
	}

	/**
	 * Calculates the pressure ratio at a given index
	 *
	 * @param index the index of the {@link DoubleTimeSeries} to access
	 *
	 * @return a double with the value at the given index
	 */
	private double getPressureRatio(int index){
		return this.getBaroPress(index) / STD_PRESS_INHG;
	}

	/**
	 * Calculates the density ratio at a given index
	 *
	 * @param index the index of the {@link DoubleTimeSeries} to access
	 *
	 * @return a double with the value at the given index
	 */
	private double getDensityRatio(int index){
		return this.getPressureRatio(index) / this.getTempRatio(index);
	}

	/**
	 * Calculates the true airspeed at a given index
	 *
	 * @param index the index of the {@link DoubleTimeSeries} to access
	 *
	 * @return a double with the value at the given index
	 */
    private double getTrueAirspeed(int index){
        return this.getIAS(index) * Math.pow(this.getDensityRatio(index), -0.5);
    }

	/**
	 * Calculates the true airspeed (in ft/min) at a given index
	 *
	 * @param index the index of the {@link DoubleTimeSeries} to access
	 *
	 * @return a double with the value at the given index
	 */
	private double getTrueAirspeedFtMin(int index){
		return this.getTrueAirspeed(index) * (6076 / 60);
	}

	/**
	 * Calculates the geometric vertical speed at a given index
	 *
	 * @param index the index of the {@link DoubleTimeSeries} to access
	 *
	 * @return a double with the value at the given index
	 */
	private double getVspdGeometric(int index){
		return this.getVspd(index) * Math.pow(this.getDensityRatio(index), -0.5);
	}

	/**
	 * Calculates the flight path angle at a given index
	 *
	 * @param index the index of the {@link DoubleTimeSeries} to access
	 *
	 * @return a double with the value at the given index
	 */
	private double getFlightPathAngle(int index){
		double fltPthAngle = Math.asin(this.getVspdGeometric(index) / this.getTrueAirspeedFtMin(index));
		fltPthAngle = fltPthAngle * (180 / Math.PI);
		return fltPthAngle;
	}

	/**
	 * Calculates the simple angle of attack at a given index
	 *
	 * @param index the index of the {@link DoubleTimeSeries} to access
	 *
	 * @return a double with the value at the given index
	 */
	private double getAOASimple(int index){
		DoubleTimeSeries pitch = this.parameters.get(PITCH);
		return pitch.get(index) - this.getFlightPathAngle(index);
	}

	private double getYawRate(int index){
		DoubleTimeSeries hdg = this.parameters.get(HDG); 
		double yawRate = 180 - Math.abs(180 - Math.abs(lag(hdg, index)) % 360);
		//double yawRate = lag(hdg, index);
		return yawRate;
	}

	private double getRollComp(int index){
		DoubleTimeSeries roll = this.parameters.get(ROLL);
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

	public boolean alreadyCalculated(){
		String queryString = "SELECT EXISTS(SELECT * FROM loci_processed WHERE fleet_id = ? and flight_id = ?)";
		try{
			PreparedStatement query = connection.prepareStatement(queryString);
			query.setInt(1, this.fleetId);
			query.setInt(2, this.flightId);

			ResultSet resultSet = query.executeQuery();

			if(resultSet.next()){
				return resultSet.getBoolean(1);
			}
		}catch (SQLException se) {
			se.printStackTrace();
		}
		return false;
	}


	private void updateDatabase(){
		String queryString = "INSERT INTO loci_processed (fleet_id, flight_id) VALUES(?,?)";
		try{
			PreparedStatement query = connection.prepareStatement(queryString);
			query.setInt(1, this.fleetId);
			query.setInt(2, this.flightId);

			query.executeUpdate();
		}catch (SQLException se) {
			se.printStackTrace();
		}
	}

	/**
	 * Calculates the loss of control probability
	 * @return a floating-point percentage of the probability of loss of control
	 */
	public void calculate(){
		this.printDetails();
		this.parameters = getParameters(this.flightId);

		DoubleTimeSeries loci = new DoubleTimeSeries("LOCI", "double");
		DoubleTimeSeries stallProbability = new DoubleTimeSeries("StallProbability", "double");
		DoubleTimeSeries altAGL = this.parameters.get(ALT_AGL);

		for(int i = 0; i<altAGL.size(); i++){
			stallProbability.add(this.calculateStallProbability(i));
			loci.add(this.calculateProbability(i));
		}

		loci.updateDatabase(connection, this.flightId);  
		stallProbability.updateDatabase(connection, this.flightId);
		this.updateDatabase();

		if(this.pw.isPresent()){
			this.writeFile(loci, stallProbability);
		}
	}

	public void writeFile(DoubleTimeSeries loci, DoubleTimeSeries sProb){
		PrintWriter pw = this.pw.get();
		try{
			pw.println("Index:\t\t\tStall Probability:\t\t\t\tLOC-I Probability:"+"\t\t\tAirspeed:");
			for(int i = 0; i<loci.size(); i++){
				pw.println(i+"\t\t\t"+sProb.get(i)+"\t\t\t\t"+loci.get(i)+"\t\t\t"+this.getIAS(i));
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
		System.err.println("------------ LOCI/Stall Probability CALCULATION INFO ------------");
		System.err.println("flight_id: "+flightId);
		System.err.println("logfile: "+(file != null ? file.toString() : "None specified."));
		System.err.println("-----------------------------------------------------------------");
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
			try{
				fleetId = Integer.parseInt(args[0]);
			}catch(NumberFormatException e){
				System.err.println("FATAL ERROR: Make sure your first argument is the fleet id!");
				System.exit(1);
			}
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
			long start = System.currentTimeMillis();

			for(int i = 0; i < nums.length; i++){
				LossOfControlCalculation loc = path.isPresent() ?
					new LossOfControlCalculation(fleetId, nums[i], path.get()) : new LossOfControlCalculation(fleetId, nums[i]);
				if(!loc.notCalcuatable() && !loc.alreadyCalculated()){
					loc.calculate();
				}
			}
			long time = System.currentTimeMillis() - start;
			long secondsTime = time / 1000;
			System.out.println("calculations took: "+secondsTime+"s");
			System.exit(0);
		} else {
			try{
				//Find the C172 flights only!
				ArrayList<String> inputs = new ArrayList<>();
				inputs.add("Airframe");
				inputs.add("is");
				inputs.add("Cessna 172S");

				int [] nums = Flight.getFlightNumbers(Database.getConnection(), fleetId, new Filter(inputs));
				long start = System.currentTimeMillis();
				for(int i = 0; i < nums.length; i++){
					LossOfControlCalculation loc = path.isPresent() ?
						new LossOfControlCalculation(fleetId, nums[i], path.get()) : new LossOfControlCalculation(fleetId, nums[i]);
					if(!loc.notCalcuatable() && !loc.alreadyCalculated()) {
						loc.calculate();
					}
				}
				long time = System.currentTimeMillis() - start;
				long secondsTime = time / 1000;
				System.out.println("calculations took: "+secondsTime+"s");
				System.exit(0);
				//here assume we will calcaulate for all flights for the given fleet
			}catch (SQLException e) {
				e.printStackTrace();
			}

		}
	}
}
