package org.ngafid;

public class GenerateBulkCSVS {
	private String directoryRoot;
	private int flightLower, flightUpper;

	public GenerateBulkCSVS(String directoryRoot, int flightLower, int flightUpper) {
		this.directoryRoot = directoryRoot;
		this.flightLower = flightLower;
		this.flightUpper = flightUpper;
		this.displayInfo();
	}

	private void displayInfo() {
		System.out.println("Generating bulk csvs info:");
		System.out.println("Flight range: " + this.flightLower + " to " + this.flightUpper);
		System.out.println("Output Directory: " + this.directoryRoot);
	}

	public static void usage() {
		System.err.println("Generate Bulk CSVS");
	}

	public static void main(String[] args) {
		if (args.length != 5) {
			usage();
			System.exit(1);
		}
		String dir = null;
		int lwr = -1, upr = -1;
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
				default:
					break;
			}
		}

		assert dir != null && upr != -1 && lwr != -1;

		GenerateBulkCSVS gb = new GenerateBulkCSVS(dir, lwr, upr);
	}
}
