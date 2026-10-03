package org.ngafid.core.accounts;

import static org.ngafid.core.flights.Parameters.DEFAULT_METRICS;

import java.util.List;

/**
 * A user's flight-analysis display preferences: the decimal precision for metrics and the set of flight metrics shown.
 *
 * <p>Provides a default configuration (precision 1 and the system default metrics) for newly created users.
 */
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

    /**
     * Constructs user preferences from a metrics array, wrapping it as an immutable list.
     *
     * @param userId the user's id
     * @param decimalPrecision the precision to display for all metrics in the UI
     * @param metrics the parameters the user wishes to see when analyzing flight data
     */
    public UserPreferences(int userId, int decimalPrecision, String[] metrics) {
        this.userId = userId;
        this.decimalPrecision = decimalPrecision;
        this.flightMetrics = List.of(metrics);
    }

    /**
     * Builds the default preferences for a user: decimal precision of 1 and the default set of flight metrics.
     *
     * @param userId the user's id
     * @return a preferences object populated with default values
     */
    public static UserPreferences defaultPreferences(int userId) {
        return new UserPreferences(userId, 1, DEFAULT_METRICS);
    }

    public int getDecimalPrecision() {
        return this.decimalPrecision;
    }

    public List<String> getFlightMetrics() {
        return this.flightMetrics;
    }

    /**
     * Updates the in-memory decimal precision if it differs from the current value.
     *
     * @param newDecimalPrecision the new decimal precision to apply
     * @return true if the precision changed (and was updated), false if it was already the given value
     */
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
