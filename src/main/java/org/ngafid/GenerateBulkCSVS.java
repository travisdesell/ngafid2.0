package org.ngafid;

import org.ngafid.common.CSVWriter;
import org.ngafid.flights.Flight;
import org.ngafid.filters.Filter;

import java.io.FileWriter;
import java.io.File;

import java.sql.Connection;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.Optional;
import java.util.List;

public class GenerateBulkCSVS {
	private String outDirectoryRoot, uploadDirectoryRoot;
	private int fleetId;
	private List<Flight> flights;

	static final Connection connection = Database.getConnection();

	/**
	 * Constructor
	 *
	 * @param outDirectoryRoot the root directory of the output file(s)
	 * @param useZip indicated if all files will be exported in a zip file
	 * @param flightLower the lower bound of the flightid 
	 * @param flightUpper the upper bound of the flightid
	 */
	public GenerateBulkCSVS(String outDirectoryRoot, int fleetId, boolean useZip, int flightLower, int flightUpper) {
		this.outDirectoryRoot = outDirectoryRoot;
		this.fleetId = fleetId;
		try{
			this.flights = Flight.getFlightsByRange(connection, fleetId, flightLower, flightUpper);
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(1);
		}
		this.displayInfo();
	}

	public GenerateBulkCSVS(String outDirectoryRoot, int fleetId, boolean useZip, String startDate, String endDate) {
		this.outDirectoryRoot = outDirectoryRoot;
		this.fleetId = fleetId;
		this.getIdsByDate(startDate, endDate);
		this.displayInfo();
	}

	private void getIdsByDate(String startDate, String endDate) {
		//"inputs":["Start Date",">=","2020-09-07"]},{"type":"RULE","inputs":["End Date","<=","2020-09-28"]
		ArrayList<String> startInputs = new ArrayList<>();
		startInputs.add("Start Date");
		startInputs.add(">=");
		startInputs.add(startDate);

		ArrayList<String> endInputs = new ArrayList<>();
		endInputs.add("End Date");
		endInputs.add("<=");
		endInputs.add(endDate);

		Filter root = new Filter("AND");
		root.addFilter(new Filter(startInputs));
		root.addFilter(new Filter(endInputs));

		try{
			this.flights = Flight.getFlights(connection, fleetId, root);
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}


	/**
	 * Dispays info to stdout about the csv generation
	 */
	private void displayInfo() {
		System.out.println("Generating bulk csvs info:");
		System.out.println("Output Directory: " + this.outDirectoryRoot);
	}

	/**
	 * Info function for command line usage
	 */
	public static void usage() {
		System.err.println("Generate Bulk CSVS");
	}
   
	/**
	 * Generates the csvs
	 */
	public void generate() {
		for (Flight flight : flights) {
			try{
				int uploaderId = flight.getUploaderId(); 
				this.uploadDirectoryRoot = WebServer.NGAFID_ARCHIVE_DIR + "/" + flight.getFleetId() + "/" +
					uploaderId + "/";

				CSVWriter csvWriter = new CSVWriter(this.uploadDirectoryRoot, flight);
				File file = new File(this.outDirectoryRoot+"flight_"+flight.getId()+".csv");
				FileWriter fw = new FileWriter(file);
				
				fw.write(csvWriter.write());
				fw.close();
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return "CSV Generator for flights: " + this.flights.toString();
	}

	/**
	 * Main method
	 *
	 * @param args cmdline args
	 */
	public static void main(String[] args) {
		String dir = null;
		int lwr = -1, upr = -1;
		int fleetId = -1;
		Optional<String> lDate = Optional.empty(),
						 uDate = Optional.empty();

		boolean zip = false;
		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
				case "-f":
					if (i == args.length - 1) {
						System.err.println("Error: no fleetId specified!");
						break;
					}
					fleetId = Integer.parseInt(args[i + 1]);
					break;

				case "-o":
					if (i == args.length - 1) {
						System.err.println("Error: no output directory specified!");
						break;
					}
					dir = args[i + 1];
					break;

				case "-r":
					if (i > args.length - 2) {
						System.err.println("Error: flight id range not valid!");
						break;
					}
					lwr = Integer.parseInt(args[i + 1]);
					upr = Integer.parseInt(args[i + 2]);
					break;

				case "-z":
					zip = true;
					break;
					
				case "-d":
					if (i > args.length - 2) {
						System.err.println("Error: date range not valid!");
						break;
					}
					lDate = Optional.of(args[i + 1]);
					uDate = Optional.of(args[i + 2]);
					break;

				default:
					break;
			}
		}

		if (!dir.substring(dir.length() - 1, dir.length()).equals("/")) {
			dir += "/";
		}


		GenerateBulkCSVS gb;
		if(lDate.isPresent() && uDate.isPresent()){
			gb = new GenerateBulkCSVS(dir, fleetId, zip, lDate.get(), uDate.get());
		} else {
			gb = new GenerateBulkCSVS(dir, fleetId, zip, lwr, upr);
		}

		gb.generate();
		System.out.println("done!");
		System.exit(0);
	}
}
