/**
 * This abstract class defines the process of calcuating new {@link DoubleTimeSeries} that require more 
 * complex analysis, such as for Stall Probaility and Loss of Control Probability
 *
 * @author <a href = mailto:apl1341@cs.rit.edu>Aidan LaBella @ RIT Computer Science</a>
 */

package org.ngafid.flights;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.ngafid.Database;
import org.ngafid.filters.Filter;

public abstract class Calculation {
    private boolean notCalcuatable;
    private String [] parameterNameSet;

    protected Flight flight;
    protected static Connection connection = Database.getConnection();
    protected Map<String, DoubleTimeSeries> parameters;

    /**
     * Initializes the set of parameters
     */
    public Calculation(Flight flight, String [] parameterNameSet, Map<String, DoubleTimeSeries> parameters) {
        this.flight = flight;
        this.parameterNameSet = parameterNameSet;
        this.parameters = parameters;

        this.getParameters(this.parameters);
    }


    /**
     * Gets references to {@link DoubleTimeSeries} objects and places them in a Map
     *
     * @param parameters map with the {@link DoubleTimeSeries} references
     */
    private final void getParameters(Map<String, DoubleTimeSeries> parameters) {
        try{
            for (String param : this.parameterNameSet) {
                DoubleTimeSeries series = DoubleTimeSeries.getDoubleTimeSeries(connection, this.flight.getId(), param);
                if(series == null){
                    System.err.println("WARNING: " + param + " data was not defined for flight #" + this.flight.getId());
                    this.notCalcuatable = true;
                    return;
                } else if (!parameters.keySet().contains(param)) {
                    parameters.put(param, series);
                }
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns whether this calculation is theoretically possible
     */
    public boolean isNotCalculatable() {
        return this.notCalcuatable;
    }

    /**
     * Runs the calculation
     */
    public void runCalculation() {
        int flightId = this.flight.getId();
        if (!this.isNotCalculatable()) {
            System.out.println("Performing " + getCalculationName() + " calculation on flight #" + flightId);
            this.calculate();
        } else {
            System.err.println("WARNING: flight #" + flightId + " is not calculatable for " + getCalculationName() + "!");
            //this.flight.updateLOCIProcessed(connection, this.dbType);
            return;
        }

        this.updateDatabase();
    }

    /**
     * Performs the calculation and returns the original set of parameters plus those added with the
     * new analysis
     *
     * @return a {@link Map} with the newly calculated {@link DoubleTimeSeries}
     */
    protected abstract void calculate(); 

    /**
     * Updates the database with the new data
     */
    public abstract void updateDatabase();

    /**
     * Gets the name of this calculation
     */
    public abstract String getCalculationName();

    /**
     * Gets an {@link Iterator} of flight ids that have not been calculated yet
     *
     * @return an {@link Iterator} with the flight ids that need to be calculated
     */
    public static Iterator<Integer> getUncalculatedFlightIds() {
        String sqlQuery = "SELECT id FROM flights WHERE id NOT IN (SELECT flight_id FROM calculations) " +
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
     * Writes usage information to stderr
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
     * Processes argumrnts from the command line
     * 
     * @param args the command line arguments
     * @param path the instance of the {@link Optional} where the path of the file out will reside
     * @param flightNums the instance of the {@link Optional} where the set of flight numbers will reside
     */
    public static Optional<Iterator<Integer>> processArgs(String [] args, Optional<Path> path, Optional<Iterator<Integer>> flightNums) {
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
               
                System.out.println(Arrays.toString(nums));
                Iterator<Integer> it = Arrays.stream(nums).iterator();
                flightNums = Optional.of(it);
            }
        }
        return flightNums;
    }

    /**
     * Calculates both {@link StallCalculation} and {@link LossOfControlCalculation} for a given set of {@link Flight} instances
     *
     * @param it the iterator that contains all the flight numbers to calculate for
     * @param path the {@link Optional} path of the file output
     */
    public static void calculateAll(Iterator<Integer> it, Optional<Path> path) {
        Instant start = Instant.now();

        while (it.hasNext()) {
            try {
                Flight flight = Flight.getFlight(connection, it.next());    
                Map<String, DoubleTimeSeries> params = new HashMap<>();

                new VSPDCalculation(flight, params).runCalculation();

                Calculation sc = new StallCalculation(flight, params);
                sc.runCalculation();

                if(flight.getAirframeId() == 1 && !sc.isNotCalculatable()) { //cessnas only!
                    Calculation loc = path.isPresent() ?
                        new LossOfControlCalculation(flight, params, path.get()) : new LossOfControlCalculation(flight, params);
                    loc.runCalculation();
                }

            } catch (SQLException se) {
                se.printStackTrace();
            }
        }

        Instant end = Instant.now();
        long elapsed_millis = Duration.between(start, end).toMillis();
        double elapsed_seconds = ((double) elapsed_millis) / 1000;
        System.out.println("calculations took: " + elapsed_seconds);
    }

    /**
     * Main method for running calculations
     *
     * @param args args from the command line, with the first being a filename for output
     */
    public static void main(String [] args) {
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
