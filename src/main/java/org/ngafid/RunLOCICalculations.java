/**
 * This file is uses for running standalone LOCI calculations in the NGAFID
 *
 * @author <a href = mailto:apl1341@cs.rit.edu>Aidan LaBella @ RIT Computer Science</a>
 */

package org.ngafid;

import org.ngafid.filters.Filter;
import org.ngafid.flights.Flight;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public final class RunLOCICalculations {
    private static Connection connection = null;

    private RunLOCICalculations() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Gets an {@link Iterator} of flight ids that have not been calculated yet
     *
     * @return an {@link Iterator} with the flight ids that need to be calculated
     */
    public static Iterator<Integer> getUncalculatedFlightIds() {
        String sqlQuery = "SELECT id FROM flights WHERE id NOT IN (SELECT flight_id FROM calculations) " + " AND " +
                "fleet_id = (SELECT id FROM fleet WHERE EXISTS (SELECT id FROM uploads WHERE fleet.id = uploads" +
                ".fleet_id AND uploads.status = 'IMPORTED'))";
        List<Integer> nums = null;

        try (PreparedStatement preparedStatement = connection.prepareStatement(sqlQuery); ResultSet resultSet =
                preparedStatement.executeQuery()) {

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
     * Writes usage information to stderr
     */
    public static void displayHelp() {
        System.err.println("USAGE: loci-calculator [fleet id] [OPTION]");
        System.err.println("fleet_id: the id of the fleet to calculate flights for");
        System.err.println("Options: ");
        System.err.println("-f [directory root]");
        System.err.println("\tPrint calculations to file(s) where the argument is the root directory in which the " +
                "files will be created in.\n" + "\tFilenames will be in the format: flight_N.out, where N is the " +
                "flight number");
        System.err.println("-n [flight number(s)]");
        System.err.println("\tOnly calculate LOC-I for the flight numbers specified. If specifiying more than one " +
                "flight, delimit flight numbers by commas with no spaces\n" + "\ti.e 10,5,3,65,2");
        System.exit(0);
    }

    /**
     * Processes argumrnts from the command line
     *
     * @param args       the command line arguments
     * @param path       the instance of the {@link Optional} where the path of the
     *                   file out will reside
     * @param flightNums the instance of the {@link Optional} where the set of
     *                   flight numbers will reside
     * @return the {@link Optional} instance of the {@link Iterator} of flight
     */
    public static Optional<Iterator<Integer>> processArgs(String[] args, Optional<Path> path,
                                                          Optional<Iterator<Integer>> flightNums) {
        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "-h", "--help", "-help" -> {
                    displayHelp();
                    System.exit(0);
                }
                case "-f" -> {
                    if (i == args.length - 1) {
                        System.err.println("No arguments specified for -f option! Exiting!");
                        System.exit(1);
                    }
                    path = Optional.of(FileSystems.getDefault().getPath(args[i + 1]));
                    if (!Files.exists(path.get())) {
                        System.err.println("Non-existent filepath: " + path.get() + ", exiting!");
                        System.exit(1);
                    } else if (!new File(path.get().toUri()).isDirectory()) {
                        System.err.println("Filepath: " + path.get() + " is not a directory, exiting!");
                        System.exit(1);
                    }
                }
                case "-n" -> {
                    if (i == args.length - 1) {
                        System.err.println("No arguments specified for -n option! Exiting!");
                        System.exit(1);
                    }
                    String numbers = args[i + 1];

                    String[] numsAsStrings = numbers.split(",");
                    int[] nums = new int[numsAsStrings.length];

                    for (int j = 0; j < nums.length; j++) {
                        nums[j] = Integer.parseInt(numsAsStrings[j]);
                    }

                    System.out.println(Arrays.toString(nums));
                    Iterator<Integer> it = Arrays.stream(nums).iterator();
                    flightNums = Optional.of(it);
                }
                default -> {
                    System.err.println("Invalid option: " + args[i]);
                    displayHelp();
                    System.exit(1);
                }
            }
        }
        return flightNums;
    }

    /**
     * Calculates both {@link StallCalculation} and {@link LossOfControlCalculation}
     * for a given set of {@link Flight} instances
     *
     * @param it   the iterator that contains all the flight numbers to calculate
     *             for
     * @param path the {@link Optional} path of the file output
     */
    public static void calculateAll(Iterator<Integer> it, Optional<Path> path) {
        Instant start = Instant.now();

        while (it.hasNext()) {
            try {
                Flight flight = Flight.getFlight(connection, it.next());
                flight.runLOCICalculations(connection);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Instant end = Instant.now();
        long elapsedMillis = Duration.between(start, end).toMillis();
        double elapsedSeconds = ((double) elapsedMillis) / 1000;
        System.out.println("calculations took: " + elapsedSeconds);
    }

    /**
     * Main method for running calculations
     *
     * @param args args from the command line, with the first being a filename for
     *             output
     */
    public static void main(String[] args) throws SQLException {
        connection = Database.getConnection();
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
                            System.err.println("No flights found waiting for a LOCI calculation, sleeping 3s");
                            try {
                                Thread.sleep(3000);
                            } catch (InterruptedException ie) {
                                ie.printStackTrace();
                            }
                        }
                    }
                } else {
                    flightNums = processArgs(args, path, flightNums);
                }

                fleetId = Integer.parseInt(first);
            } catch (NumberFormatException e) {
                System.err.println("FATAL ERROR: Make sure your first argument is the fleet id!");
                System.exit(1);
            }
        }

        if (flightNums.isPresent()) {
            calculateAll(flightNums.get(), path);
        } else {
            try {
                // Find the C172 flights only!
                ArrayList<String> inputs = new ArrayList<>();
                inputs.add("Airframe");
                inputs.add("is");
                inputs.add("Cessna 172S");

                int[] nums = Flight.getFlightNumbers(connection, fleetId, new Filter(inputs));
                calculateAll(Arrays.stream(nums).iterator(), path);
                System.exit(0);
                // here assume we will calcaulate for all flights for the given fleet
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
