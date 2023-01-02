package org.ngafid.accounts;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.ngafid.flights.CalculationParameters.*;


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
    public UserPreferences(int userId, int decimalPrecision, List<String> flightMetrics) {
        this.userId = userId;
        this.decimalPrecision = decimalPrecision;
        this.flightMetrics = flightMetrics;
    }

    public UserPreferences(int userId, int decimalPrecision, String [] metrics) {
        this.userId = userId;
        this.decimalPrecision = decimalPrecision;
        this.flightMetrics = List.of(metrics);
    }


    public static UserPreferences defaultPreferences(int userId) {
        return new UserPreferences(userId, 1, defaultMetrics);
    }

    public int getDecimalPrecision() {
        return this.decimalPrecision;
    }

    public List<String> getFlightMetrics() {
        return this.flightMetrics;
    }
    
    public boolean update(int decimalPrecision) {
        boolean wasUpdated = false;

        if (decimalPrecision != this.decimalPrecision) {
            this.decimalPrecision = decimalPrecision;
            wasUpdated = true;
        }

        return wasUpdated;
    }

    public Map<String, String> getUserPreferences(Connection connection, int userID) throws SQLException {
        Map<String, String> userPreferences = new HashMap<>();

        PreparedStatement statement = connection.prepareStatement("SELECT * FROM user_preferences WHERE user_id = ?");



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
