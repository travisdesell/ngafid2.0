/**
 * This abstract class defines the process of calcuating new {@link DoubleTimeSeries} that require more 
 * complex analysis, such as for Stall Probaility and Loss of Control Probability
 *
 * @author <a href = mailto:apl1341@cs.rit.edu>Aidan LaBella @ RIT Computer Science</a>
 */

package org.ngafid.flights;

import java.sql.Connection;
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
	public abstract Map<String, DoubleTimeSeries> calculate();

	/**
	 * Updates the database with the new data
	 */
	public abstract void updateDatabase();
}
