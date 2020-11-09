package org.ngafid.accounts;

import java.util.List;
import java.util.ArrayList;

public class UserPreferences {
	private int userId, decimalPrecision;
	private List<String> flightMetrics;

	/**
	 * Constructor
	 *
	 * @param userId the users id
	 * @param decimalPrecision the precision to display for all metrics in the UI
	 * @param metrics a comma separated list of parameters the user wishes to see when they analyze flight data UI-side
	 */
	public UserPreferences(int userId, int decimalPrecision, String metrics) {
		this.userId = userId;
		this.decimalPrecision = decimalPrecision;
		this.flightMetrics = new ArrayList<>();

		for (String metric : metrics.split(",")) {
			flightMetrics.add(metric.trim());
		}
	}

	/**
	 * Delivers a string representation of this class
	 *
	 * @return a {@link String} with the users preferences
	 */
	@Override
	public String toString() {
		return "user_id : " + this.userId + " precision: " + this.decimalPrecision + " metrics " + this.flightMetrics;
	}
}
