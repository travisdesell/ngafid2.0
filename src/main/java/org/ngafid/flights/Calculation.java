/**
 * This abstract class defines the process of calcuating new {@link DoubleTimeSeries} that require more 
 * complex analysis, such as for Stall Probaility and Loss of Control Probability
 *
 * @author <a href = mailto:apl1341@cs.rit.edu>Aidan LaBella @ RIT Computer Science</a>
 */

package org.ngafid.flights;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.ngafid.Database;

public abstract class Calculation {
    private boolean notCalcuatable;
    private String [] parameterNameSet;
    protected Flight flight;
    protected static Connection connection = Database.getConnection();
    protected Map<String, DoubleTimeSeries> parameters;

    /**
     * Indicates the type of calculation to identify as in the processed table of the database
     */
    protected String dbType;

    /**
     * Initializes the set of parameters
     */
    public Calculation(Flight flight, String [] parameterNameSet, Map<String, DoubleTimeSeries> parameters, String dbType) {
        this.flight = flight;
        this.parameterNameSet = parameterNameSet;
        this.parameters = parameters;
        this.dbType = dbType;
        this.getParameters(this.parameters);
    }

    /**
     * Determines if a calculation has already been performed
     *
     * @return true if there has been a calculation with the same parameters performed prior, false otherwise
     */
    public boolean alreadyCalculated() {
        String sqlQuery = "SELECT EXISTS(SELECT * FROM loci_processed WHERE type = '" + this.dbType + "' AND flight_id = ?)";

        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sqlQuery);
            preparedStatement.setInt(1, this.flight.getId());
            ResultSet resultSet = preparedStatement.executeQuery();
    
            if (resultSet.next()) {
                return resultSet.getBoolean(1);
            }

        } catch (SQLException se) {
            se.printStackTrace();
        }

        return false;
    }

    /**
     * Gets references to {@link DoubleTimeSeries} objects and places them in a Map
     *
     * @param parameters map with the {@link DoubleTimeSeries} references
     */
    private void getParameters(Map<String, DoubleTimeSeries> parameters) {
        try{
            for(String param : this.parameterNameSet) {
                DoubleTimeSeries series = DoubleTimeSeries.getDoubleTimeSeries(connection, this.flight.getId(), param);
                if(series == null){
                    System.err.println("WARNING: " + series + " data was not defined for flight #" + this.flight.getId());
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
     * Performs the calculation and returns the original set of parameters plus those added with the
     * new analysis
     *
     * @return a {@link Map} with the newly calculated {@link DoubleTimeSeries}
     */
    protected abstract void calculate(); 

    /**
     * Runs the calculation
     *
     * @return a {@link Map} of parameters plus those just calculated
     */
    public final Map<String, DoubleTimeSeries> runCalculation() {
        int flightId = this.flight.getId();
        if (!this.isNotCalculatable()) {
            if (!this.alreadyCalculated()) {
                System.out.println("Performing " + this.dbType + " calculation on flight #" + flightId);
                this.calculate();
            } else {
                System.err.println("flight #" + flightId + " already calculated, ignoring");
                return this.parameters;
            }
        } else {
            System.err.println("WARNING: flight #" + flightId + " is not calculatable for " + this.dbType + "!");
            return this.parameters;
        }

        this.flight.updateLOCIProcessed(connection, this.dbType);
        this.updateDatabase();

        return this.parameters;
    }

    /**
     * Updates the database with the new data
     */
    public abstract void updateDatabase();
}
