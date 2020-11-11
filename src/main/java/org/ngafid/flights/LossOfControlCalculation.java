/**
 * Loss of control calculator for calculating exceedences
 * @author <a href = mailto:apl1341@cs.rit.edu>Aidan LaBella @ RIT CS</a>
 */

package org.ngafid.flights;

import java.io.PrintWriter;
import java.io.File;
import java.io.FileNotFoundException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Files;

import java.lang.Math;

import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.ngafid.Database;
import org.ngafid.filters.Filter;

import static org.ngafid.flights.LossOfControlParameters.*;

public class LossOfControlCalculation extends Calculation {
	private File file;
	private Optional<PrintWriter> pw;

	/**
	 * Constructor 
	 *
	 * @param flight the flight to calculate for
	 * @param cachedParameters a set parameters that includes the already-calculated AOA Simple 
	 * and stall probability
	 */
	public LossOfControlCalculation(Flight flight, Map<String, DoubleTimeSeries> cachedParameters) { 
		super(flight, lociParamStrings, cachedParameters);

		cachedParameters.put(PRO_SPIN_FORCE, new DoubleTimeSeries(PRO_SPIN_FORCE, "double"));
		cachedParameters.put(YAW_RATE, new DoubleTimeSeries(YAW_RATE, "double"));

		this.pw = Optional.empty();
	}

	/**
	 * Constructor 
	 *
	 * @param flight the flight to calculate for
	 * @param cachedParameters a set parameters that includes the already-calculated AOA Simple 
	 * @param path the filepath ROOT directory to print logfiles too
	 * */
	public LossOfControlCalculation(Flight flight, Map<String, DoubleTimeSeries> cachedParameters, Path path) { 
		this(flight, cachedParameters);
		//try to create a file output 
		this.createFileOut(path);
	}

	/**
	 * Creates an output filestream
	 *
	 * @param path the path of the file to write
	 */
	private void createFileOut(Path path) {
		String filename = "/flight_"+ this.flight.getId() +".out";

		file = new File(path.toString()+filename);
		System.out.println("LOCI_CALCULATOR: printing to file " + file.toString() + " for flight #" + this.flight.getId());

		try {
			this.pw = Optional.of(new PrintWriter(file));
		} catch(FileNotFoundException e) {
			System.err.println("File not writable!");
			System.exit(1);
		}
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
		if(index >= 1) {
			return currIndex - series.get(index - 1);
		}
		return currIndex;
	}

	/**
	 * Calculates the yaw rate at a given index
	 *
	 * @param index the index of the {@link DoubleTimeSeries} to access
	 *
	 * @return a double with the calculated value at the given index
	 */
	private double getYawRate(int index){
		DoubleTimeSeries hdg = this.parameters.get(HDG); 
		DoubleTimeSeries yawRate = this.parameters.get(YAW_RATE);
		double value = 180 - Math.abs(180 - Math.abs(lag(hdg, index)) % 360);

		if (yawRate.size() == index) {
			yawRate.add(value);
		}

		return value;
	}

	/**
	 * Gets the roll comp at a given index
	 *
	 * @param index the index of the {@link DoubleTimeSeries} to access
	 *
	 * @return a double with the calculated value at the given index
	 */
	private double getRollComp(int index){
		DoubleTimeSeries roll = this.parameters.get(ROLL);
		return roll.get(index) * COMP_CONV;
	}

	/**
	 * Gets the yaw comp at a given index
	 *
	 * @param index the index of the {@link DoubleTimeSeries} to access
	 *
	 * @return a double with the calculated value at the given index
	 */
	private double getYawComp(int index){
		return this.getYawRate(index) * COMP_CONV;
	}
	
	/**
	 * Gets the VR comp at a given index
	 *
	 * @param index the index of the {@link DoubleTimeSeries} to access
	 *
	 * @return a double with the calculated value at the given index
	 */
	private double getVRComp(int index){
		return ((this.parameters.get(TAS_FTMIN).get(index) / 60) * this.getYawComp(index));
	}

	/**
	 * Gets the CT comp at a given index
	 *
	 * @param index the index of the {@link DoubleTimeSeries} to access
	 *
	 * @return a double with the calculated value at the given index
	 */
	private double getCTComp(int index){
		return Math.sin(this.getRollComp(index)) * 32.2;
	}

	/**
	 * Gets the cord comp at a given index
	 *
	 * @param index the index of the {@link DoubleTimeSeries} to access
	 *
	 * @return a double with the calculated value at the given index
	 */
	private double getCordComp(int index){
	  	return Math.abs(this.getCTComp(index) - this.getVRComp(index)) * 100;
	}

	/**
	 * Gets the pro spin force at a given index
	 *
	 * @param index the index of the {@link DoubleTimeSeries} to access
	 *
	 * @return a double with the calculated value at the given index
	 */
	private double getProSpin(int index){
		DoubleTimeSeries psf = this.parameters.get(PRO_SPIN_FORCE);
		
		double value = Math.min((this.getCordComp(index) / PROSPIN_LIM), 100);

		if (psf.size() == index) {
			psf.add(value);
		}

		return value;
	}

	/**
	 * Calculates the LOC-I probability at a given index
	 *
	 * @param index the index of the {@link DoubleTimeSeries} to access
	 *
	 * @return a double with the calculated value at the given index as a percentage
	 */
	private double calculateProbability(int index) {
		double prob = (this.parameters.get(STALL_PROB).get(index) * this.getProSpin(index));
		return prob;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void updateDatabase(){
		this.parameters.get(LOCI).updateDatabase(connection, super.flight.getId());
		this.parameters.get(PRO_SPIN_FORCE).updateDatabase(connection, super.flight.getId());
		this.parameters.get(YAW_RATE).updateDatabase(connection, super.flight.getId());
	}

	/**
	 * Calculates the loss of control probability
	 *
	 * @return a floating-point percentage of the probability of loss of control
	 */
	public Map<String, DoubleTimeSeries> calculate() {
		System.out.println("calculating");
		this.printDetails();
		
		DoubleTimeSeries loci;
		this.parameters.put(LOCI, 
				(loci = new DoubleTimeSeries(LOCI, "string")));
		DoubleTimeSeries altAGL = this.parameters.get(ALT_AGL);

		for(int i = 0; i < altAGL.size(); i++) {
			loci.add(this.calculateProbability(i) / 100);
		}

		updateDatabase();

		if(this.pw.isPresent()) {
			this.writeFile(loci, this.parameters.get(STALL_PROB));
		}

		return this.parameters;
	}

	/**
	 * Writes the data to a file for analysis purposes
	 *
	 * @pre {@link Optional} pw has a wrapped instance of {@link PrintWriter}
	 *
	 * @param loci the loss of contorl {@link DoubleTimeSeries}
	 * @param sProb the stall probability {@link DoubleTimeSeries}
	 */
	public void writeFile(DoubleTimeSeries loci, DoubleTimeSeries sProb){
		PrintWriter pw = this.pw.get();
		System.out.println("printing to file");
		try{
			pw.println("Index:\t\t\tStall Probability:\t\t\t\tLOC-I Probability:");
			for(int i = 0; i<loci.size(); i++){
				pw.println(i+"\t\t\t"+sProb.get(i)+"\t\t\t\t"+loci.get(i));
			}
			pw.println("\n\nMaximum Values: ");
			pw.println("Stall Probability: "+sProb.getMax()+" LOC-I: "+loci.getMax());

			pw.println("Average Values: ");
			pw.println("Stall Probability: "+sProb.getAvg()+" LOC-I: "+loci.getAvg());
		} catch (Exception e) { 
			e.printStackTrace();
		} finally {
			pw.close();
		}
	}

	/**
	 * Gets an {@link Iterator} of flight ids that have not been calculated yet
	 *
	 * @return an {@link Iterator} with the flight ids that need to be calculated
	 */
	public static Iterator<Integer> getUncalculatedFlightIds() {
		String sqlQuery = "SELECT id FROM flights WHERE id NOT IN (SELECT flight_id FROM loci_processed) " +
			" AND fleet_id = (SELECT id FROM fleet WHERE EXISTS (SELECT id FROM uploads WHERE fleet.id = uploads.fleet_id AND uploads.status = 'IMPORTED'))";
		List<Integer> nums = null;

		try {
			PreparedStatement preparedStatement = connection.prepareStatement(sqlQuery);
			//preparedStatement.setInt(1, C172SP_ID);
			ResultSet resultSet = preparedStatement.executeQuery();
	
			nums = new ArrayList<>();
			while (resultSet.next()) {
				nums.add(resultSet.getInt(1));
			}

		} catch (SQLException se) {
			se.printStackTrace();
		}

		if (nums != null && !nums.isEmpty()) {
			return nums.iterator();
		} else {
			return null;
		}
	}


	/**
	 * Writes usage information to standard error
	 */
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

	/**
	 * Writes details about the calcualtion to standard error
	 */
	public void printDetails(){
		System.err.println("\n\n");
		System.err.println("------------ LOCI/Stall Probability CALCULATION INFO ------------");
		System.err.println("flight_id: "+flight.getId());
		System.err.println("logfile: "+(file != null ? file.toString() : "None specified."));
		System.err.println("-----------------------------------------------------------------");
		System.err.println("\n\n");
	}

	/**
	 * Processes argumrnts from the command line
	 * 
	 * @param args the command line arguments
	 * @param path the instance of the {@link Optional} where the path of the file out will reside
	 * @param flightNums the instance of the {@link Optional} where the set of flight numbers will reside
	 */
	public static void processArgs(String [] args, Optional<Path> path, Optional<Iterator<Integer>> flightNums) {
		for(int i = 1; i < args.length; i++) {
			if(args[i].equals("-h") || args[i].equals("--help") || args[i].equals("-help")){
				displayHelp();
				System.exit(0);
			} else if(args[i].equals("-f")) {
				if(i == args.length - 1) {
					System.err.println("No arguments specified for -f option! Exiting!");
					System.exit(1);
				}
				path = Optional.of(FileSystems.getDefault().getPath(args[i+1]));
				if(!Files.exists(path.get())) {
					System.err.println("Non-existent filepath: "+path.get().toString()+", exiting!");
					System.exit(1);
				} else if(!new File(path.get().toUri()).isDirectory()){
					System.err.println("Filepath: "+path.get().toString()+" is not a directory, exiting!");
					System.exit(1);
				}
			} else if(args[i].equals("-n")) {
				if(i == args.length - 1) {
					System.err.println("No arguments specified for -n option! Exiting!");
					System.exit(1);
				}
				String numbers = args[i+1];

				String [] numsAsStrings = numbers.split(",");
				int [] nums = new int[numsAsStrings.length];

				for(int j = 0; j < nums.length; j++) {
					nums[j] = Integer.parseInt(numsAsStrings[j]);
				}

				Iterator<Integer> it = Arrays.stream(nums).iterator();
				flightNums = Optional.of(it);
			}
		}
	}

	/**
	 * Calculates both {@link StallCalculation} and {@link LossOfControlCalculation} for a given set of {@link Flight} instances
	 *
	 * @param it the iterator that contains all the flight numbers to calculate for
	 * @param path the {@link Optional} path of the file output
	 */
	public static void calculateAll(Iterator<Integer> it, Optional<Path> path) {
		long start = System.currentTimeMillis();

		while (it.hasNext()) {
			try {
				Flight flight = Flight.getFlight(connection, it.next());	
				StallCalculation sc = new StallCalculation(flight);

				if (!sc.isNotCalculatable()) { 
					Map<String, DoubleTimeSeries> parameters = sc.calculate();
					LossOfControlCalculation loc = path.isPresent() ?
						new LossOfControlCalculation(flight, parameters, path.get()) : new LossOfControlCalculation(flight, parameters);
					if(!loc.isNotCalculatable() && flight.getAirframeId() == 1) { //cessnas only!
						loc.calculate();
					}
				}

				flight.updateLOCIProcessed(connection); //update EVEN IF it isnt calculatable

			} catch (SQLException se) {
				se.printStackTrace();
			}
		}

		long time = System.currentTimeMillis() - start;
		long secondsTime = time / 1000;
		System.out.println("calculations took: " + secondsTime + "s");
	}

	/**
	 * Main method for running calculations
	 *
	 * @param args args from the command line, with the first being a filename for output
	 */
	public static void main(String [] args){
		System.out.println("Loss of control calculator");

		Optional<Path> path = Optional.empty();	
		Optional<Iterator<Integer>> flightNums = Optional.empty();

		int fleetId = -1;

		if (args.length < 1) {
			displayHelp();
		} else {
			try {
				String first = args[0];

				if (first.equalsIgnoreCase("-h") || first.equalsIgnoreCase("--help")) {
					displayHelp();
				}

				if (first.equalsIgnoreCase("auto")) {
					while (true) {
						System.out.println("automatically selecting fleets with uncalculated LOCI/SP");
						Iterator<Integer> it = getUncalculatedFlightIds();
						if (it != null) {
							calculateAll(it, path);
						} else {
							System.err.println("No flights found waiting for a LOCI calculation, sleeping 10s");
							try {
								Thread.sleep(10000);
							} catch (InterruptedException ie) {
								ie.printStackTrace();
							}
						}
					}
				} else {
					processArgs(args, path, flightNums);
				}

				fleetId = Integer.parseInt(first);
			} catch(NumberFormatException e) {
				System.err.println("FATAL ERROR: Make sure your first argument is the fleet id!");
				System.exit(1);
			}
		}


		if (flightNums.isPresent()) {
			calculateAll(flightNums.get(), path);
		} else {
			try {
				//Find the C172 flights only!
				ArrayList<String> inputs = new ArrayList<>();
				inputs.add("Airframe");
				inputs.add("is");
				inputs.add("Cessna 172S");

				int [] nums = Flight.getFlightNumbers(Database.getConnection(), fleetId, new Filter(inputs));
				calculateAll(Arrays.stream(nums).iterator(), path);
				System.exit(0);
				//here assume we will calcaulate for all flights for the given fleet
			} catch (SQLException e) {
				e.printStackTrace();
			}

		}
	}
}
