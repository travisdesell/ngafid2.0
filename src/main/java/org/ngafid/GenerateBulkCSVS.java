package org.ngafid;

import org.ngafid.common.CSVWriter;
import org.ngafid.flights.Flight;

import java.io.FileWriter;
import java.io.File;

import java.sql.Connection;
import java.sql.SQLException;

public class GenerateBulkCSVS {
	private String outDirectoryRoot, uploadDirectoryRoot;
	private int flightLower, flightUpper;

	static final Connection connection = Database.getConnection();

	/**
	 * Constructor
	 *
	 * @param outDirectoryRoot the root directory of the output file(s)
	 * @param useZip indicated if all files will be exported in a zip file
	 * @param flightLower the lower bound of the flightid 
	 * @param flightUpper the upper bound of the flightid
	 */
	public GenerateBulkCSVS(String outDirectoryRoot, boolean useZip, int flightLower, int flightUpper) {
		this.outDirectoryRoot = outDirectoryRoot;
		this.flightLower = flightLower;
		this.flightUpper = flightUpper;
		this.displayInfo();
	}

	/**
	 * Dispays info to stdout about the csv generation
	 */
	private void displayInfo() {
		System.out.println("Generating bulk csvs info:");
		System.out.println("Flight range: " + this.flightLower + " to " + this.flightUpper);
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
		for (int i = flightLower; i <= flightUpper; i++) {
			try{
				Flight flight = Flight.getFlight(connection, i);
				int uploaderId = flight.getUploaderId(); 
				this.uploadDirectoryRoot = WebServer.NGAFID_ARCHIVE_DIR + "/" + flight.getFleetId() + "/" +
					uploaderId + "/";

				CSVWriter csvWriter = new CSVWriter(this.uploadDirectoryRoot, flight);
				File file = new File(this.outDirectoryRoot+"flight_"+i+".csv");
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
		return "CSV Generator for flights: " + flightLower + " to " + flightUpper;
	}

	/**
	 * Main method
	 *
	 * @param args cmdline args
	 */
	public static void main(String[] args) {
		if (args.length < 5 || args.length > 6) {
			usage();
			System.exit(1);
		}
		String dir = null;
		int lwr = -1, upr = -1;
		boolean zip = false;
		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
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
				case "-z":
					zip = true;
					break;
				default:
					break;
			}
		}

		assert dir != null && upr != -1 && lwr != -1;

		GenerateBulkCSVS gb = new GenerateBulkCSVS(dir, zip, lwr, upr);
		gb.generate();
		System.out.println("done!");
		System.exit(0);
	}
}
