package org.ngafid.accounts;

import java.util.List;

import com.google.gson.Gson;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.ngafid.flights.CalculationParameters.*;

public class UserPreferences {
    private int userId, decimalPrecision;
    private List<String> flightMetrics;
    private Gson gson;

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
        this.gson = new Gson();

        //we must convert from json since the metrics will be stored in the db as such
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            this.flightMetrics = objectMapper.readValue(metrics, List.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public UserPreferences(int userId, int decimalPrecision, String [] metrics) {
        this.userId = userId;
        this.decimalPrecision = decimalPrecision;
        this.gson = new Gson();

        this.flightMetrics = List.of(metrics);
    }


    public static UserPreferences defaultPreferences(int userId) {
        return new UserPreferences(userId, 1, defaultMetrics);
    }

    public int getDecimalPrecision() {
        return this.decimalPrecision;
    }

    public String getFlightMetrics() {
        return this.gson.toJson(this.flightMetrics);
    }

    public List<String> getFlightMetricsAsList() {
        return this.flightMetrics;
    }
    
    public boolean update(int decimalPrecision, List<String> metrics) {
        boolean wasUpdated = false;

        if (decimalPrecision != this.decimalPrecision) {
            this.decimalPrecision = decimalPrecision;
            wasUpdated = true;
        }

        List<String> newMetrics;
        if (!(newMetrics = metrics).equals(this.flightMetrics)) {
            this.flightMetrics = newMetrics;
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
