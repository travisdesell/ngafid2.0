package org.ngafid.core.accounts;

import java.util.List;

import static org.ngafid.core.flights.Parameters.DEFAULT_METRICS;

public class UserPreferences {
    private final List<String> flightMetrics;
    private final int userId;
    private int decimalPrecision;

    /**
     * Constructor
     *
     * @param userId           the users id
     * @param decimalPrecision the precision to display for all metrics in the UI
     * @param flightMetrics    a comma separated list of parameters the user wishes
     *                         to see when they analyze flight data UI-side
     */
    public UserPreferences(int userId, int decimalPrecision, List<String> flightMetrics) {
        this.userId = userId;
        this.decimalPrecision = decimalPrecision;
        this.flightMetrics = flightMetrics;
    }

    public UserPreferences(int userId, int decimalPrecision, String[] metrics) {
        this.userId = userId;
        this.decimalPrecision = decimalPrecision;
        this.flightMetrics = List.of(metrics);
    }


    public static UserPreferences defaultPreferences(int userId) {
        return new UserPreferences(userId, 1, DEFAULT_METRICS);
    }

    public int getDecimalPrecision() {
        return this.decimalPrecision;
    }

    public List<String> getFlightMetrics() {
        return this.flightMetrics;
    }

    public boolean update(int newDecimalPrecision) {
        boolean wasUpdated = false;

        if (newDecimalPrecision != this.decimalPrecision) {
            this.decimalPrecision = newDecimalPrecision;
            wasUpdated = true;
        }

        return wasUpdated;
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
