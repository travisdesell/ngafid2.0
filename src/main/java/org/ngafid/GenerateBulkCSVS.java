/**
 * Generates/Copies Bulk CSV files for flights in the ngafid
 *
 * @author <a href = "mailto:apl1341@cs.rit.edu">Aidan LaBella</a>
 */

package org.ngafid;

import org.ngafid.common.CSVWriter;
import org.ngafid.flights.Flight;
import org.ngafid.filters.Filter;

import java.io.*;

import java.sql.Connection;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.Optional;
import java.util.List;

import java.util.zip.*;

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

	/**
	 * Constructor
	 *
	 * @param outDirectoryRoot the root directory of the output file(s)
	 * @param useZip indicated if all files will be exported in a zip file
	 * @param startDate the start date to use
	 * @param endDate the end tdate to use
	 */
	public GenerateBulkCSVS(String outDirectoryRoot, int fleetId, boolean useZip, String startDate, String endDate) {
		this.outDirectoryRoot = outDirectoryRoot;
		this.fleetId = fleetId;
		this.getIdsByDate(startDate, endDate);
		this.displayInfo();
	}

	/**
	 * Querys the database and gets ids by date
	 *
	 * @param startDate the start date to use
	 * @param endDate the end tdate to use
	 */
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
		System.err.println("Usage: generate_csvs -f (fleet id) -o (output directory root) [-d date range YYYY-MM-DD to YYYY-MM-DD]\n" +
							"[-z (enable zip arciving)] [-r range lwr upr (flight numbers)]");
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
	 * Generates files but places them in a ZIP archive
	 */
	public void generateToZip() {
		File file = new File(this.outDirectoryRoot+"flights_" + flights.get(0).getId()
				+ "_" + flights.get(flights.size() - 1).getId() + ".zip");

		try {
			if(!file.exists()){
				System.out.println("Creating new file: "+file);
				file.createNewFile();
			}

			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
			ZipOutputStream zipOut = new ZipOutputStream(bos);

			for (Flight flight : flights) {
				try{
					int uploaderId = flight.getUploaderId(); 
					this.uploadDirectoryRoot = WebServer.NGAFID_ARCHIVE_DIR + "/" + flight.getFleetId() + "/" +
						uploaderId + "/";

					CSVWriter csvWriter = new CSVWriter(this.uploadDirectoryRoot, flight);
					ZipEntry zipEntry = csvWriter.getFlightEntry();

					zipOut.putNextEntry(zipEntry);
					zipOut.write(csvWriter.toBinaryData());

					zipOut.closeEntry();
					
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(1);
				}
			}
			zipOut.close();
		} catch (IOException e) {
			e.printStackTrace();
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
		if (args[0].equals("-h") || args[0].equals("--help") || args[0].equals("-help") || args[0].equals("--h")) {
			usage();
			System.exit(0);
		}

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

		if(zip){
			gb.generateToZip();
		} else {
			gb.generate();
		}

		System.out.println("done!");
		System.exit(0);
	}
}
