package org.ngafid.accounts;


import java.util.List;

import static org.ngafid.flights.CalculationParameters.*;


public class UserPreferences {


    private int userId, decimalPrecision;
    private boolean emailOptOut, emailUploadProcessing, emailUploadStatus, emailCriticalEvents, emailUploadError;
    private EmailFrequency emailFrequency;
    private List<String> flightMetrics;

    /**
     *
     * @param userId the users id
     * @param decimalPrecision the precision to display for all metrics in the UI
     * @param emailOptOut whether the user wants to receive emails
     * @param emailUploadProcessing whether the user wants to receive emails when a flight is being processed
     * @param emailUploadStatus whether the user wants to receive emails when a flight is finished processing
     * @param emailCriticalEvents whether the user wants to receive emails when a critical event is detected
     * @param emailUploadError whether the user wants to receive emails when a flight fails to process
     * @param emailFrequency the frequency of emails the user wants to receive
     * @param flightMetrics a comma separated list of parameters the user wishes to see when they analyze flight data UI-side
     */
    public UserPreferences(int userId, int decimalPrecision, List<String> flightMetrics, boolean emailOptOut, boolean emailUploadProcessing, boolean emailUploadStatus, boolean emailCriticalEvents, boolean emailUploadError, EmailFrequency emailFrequency) {
        this.userId = userId;
        this.decimalPrecision = decimalPrecision;
        this.flightMetrics = flightMetrics;
        this.emailOptOut = emailOptOut;
        this.emailUploadProcessing = emailUploadProcessing;
        this.emailUploadStatus = emailUploadStatus;
        this.emailCriticalEvents = emailCriticalEvents;
        this.emailUploadError = emailUploadError;
        this.emailFrequency = emailFrequency;
    }

    public UserPreferences(int userId, int decimalPrecision, String[] flightMetrics, boolean emailOptOut, boolean emailUploadProcessing, boolean emailUploadStatus, boolean emailCriticalEvents, boolean emailUploadError, EmailFrequency emailFrequency) {
        this.userId = userId;
        this.decimalPrecision = decimalPrecision;
        this.flightMetrics = List.of(flightMetrics);
        this.emailOptOut = emailOptOut;
        this.emailUploadProcessing = emailUploadProcessing;
        this.emailUploadStatus = emailUploadStatus;
        this.emailCriticalEvents = emailCriticalEvents;
        this.emailUploadError = emailUploadError;
        this.emailFrequency = emailFrequency;
    }
    public static UserPreferences defaultPreferences(int userId) {
        return new UserPreferences(userId, 1, defaultMetrics, false, false, false, false, false, EmailFrequency.NEVER);
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
