package org.ngafid.events.proximity;

import org.ngafid.common.TimeUtils;
import org.ngafid.common.filters.Pair;
import org.ngafid.events.Event;
import org.ngafid.events.EventStatistics;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.StringTimeSeries;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class FlightTimeLocation {
    //CHECKSTYLE:OFF
    // set to true if the flight has the required time series values and a start and
    // end date time
    boolean valid = false;

    // set to true when the double and string time series data has been
    // read from the database, and the epochTime array has been calculated
    boolean hasSeriesData = false;

    int fleetId;
    int flightId;
    int airframeNameId;

    String startDateTime;
    String endDateTime;

    double minLatitude;
    double maxLatitude;
    double minLongitude;
    double maxLongitude;

    double minAltMSL;
    double maxAltMSL;

    long[] epochTime;
    double[] altitudeMSL;
    double[] altitudeAGL;
    double[] latitude;
    double[] longitude;
    double[] indicatedAirspeed;

    StringTimeSeries dateSeries;
    StringTimeSeries timeSeries;
    //CHECKSTYLE:ON


    public FlightTimeLocation(Connection connection, int fleetId, int flightId, int airframeNameId,
                              String startDateTime, String endDateTime) throws SQLException {
        this.fleetId = fleetId;
        this.flightId = flightId;
        this.airframeNameId = airframeNameId;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;

        // first check and see if the flight had a start and end time, if not we cannot process it
        // System.out.println("Getting info for flight with start date time: " + startDateTime + " and end date time: "
        // + endDateTime);

        if (startDateTime == null || endDateTime == null) {
            // flight didnt have a start or end time
            valid = false;
            return;
        }

        // then check and see if this was actually a flight (RPM > 800)
        Pair<Double, Double> minMaxRPM1 = DoubleTimeSeries.getMinMax(connection, flightId, "E1 RPM");
        Pair<Double, Double> minMaxRPM2 = DoubleTimeSeries.getMinMax(connection, flightId, "E2 RPM");

        if ((minMaxRPM1 == null && minMaxRPM2 == null) // both RPM values are null, can't calculate exceedence
                || (minMaxRPM2 == null && minMaxRPM1.second() < 800) // RPM2 is null, RPM1 is < 800 (RPM1 would not be
                // null as well)
                || (minMaxRPM1 == null && minMaxRPM2.second() < 800) // RPM1 is null, RPM2 is < 800 (RPM2 would not be
                // null as well)
                || ((minMaxRPM1 != null && minMaxRPM2 != null && minMaxRPM1.second() < 800
                && minMaxRPM2.second() < 800))) { // RPM1 and RPM2 < 800
            // couldn't calculate exceedences for this flight because the engines never kicked on (it didn't fly)
            valid = false;
            return;
        }

        // then check and see if this flight had a latitude and longitude, if not we cannot calculate adjacency
        Pair<Double, Double> minMaxLatitude = DoubleTimeSeries.getMinMax(connection, flightId, "Latitude");
        Pair<Double, Double> minMaxLongitude = DoubleTimeSeries.getMinMax(connection, flightId, "Longitude");

        // if (minMaxLatitude != null) System.out.println("min max latitude: " + minMaxLatitude.first() + ", " +
        // minMaxLatitude.second());
        // if (minMaxLongitude != null) System.out.println("min max longitude: " + minMaxLongitude.first() + ", " +
        // minMaxLongitude.second());

        if (minMaxLatitude == null || minMaxLongitude == null) {
            // flight didn't have latitude or longitude
            valid = false;
            return;
        }

        minLatitude = minMaxLatitude.first();
        maxLatitude = minMaxLatitude.second();
        minLongitude = minMaxLongitude.first();
        maxLongitude = minMaxLongitude.second();

        // then check and see if this flight had alt MSL, if not we cannot calculate adjacency
        Pair<Double, Double> minMaxAltMSL = DoubleTimeSeries.getMinMax(connection, flightId, "AltMSL");

        if (minMaxAltMSL == null) {
            // flight didn't have alt MSL
            valid = false;
            return;
        }

        minAltMSL = minMaxAltMSL.first();
        maxAltMSL = minMaxAltMSL.second();

        // this flight had the necessary values and time series to calculate adjacency
        valid = true;
    }

    /**
     * Get the time series data for altitude, latitude, longitude, and indicated airspeed
     *
     * @param connection the connection to the database
     * @return true if the time series data was successfully retrieved
     * @throws IOException  io exception
     * @throws SQLException sql exception
     */
    public boolean getSeriesData(Connection connection) throws IOException, SQLException {
        // get the time series data for altitude, latitude and longitude
        DoubleTimeSeries altMSLSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "AltMSL");
        DoubleTimeSeries altAGLSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "AltAGL");
        DoubleTimeSeries latitudeSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "Latitude");
        DoubleTimeSeries longitudeSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "Longitude");
        DoubleTimeSeries indicatedAirspeedSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "IAS");

        // check to see if we could get these columns
        if (altMSLSeries == null || altAGLSeries == null || latitudeSeries == null || longitudeSeries == null
                || indicatedAirspeedSeries == null)
            return false;

        altitudeMSL = altMSLSeries.innerArray();
        altitudeAGL = altAGLSeries.innerArray();
        latitude = latitudeSeries.innerArray();
        longitude = longitudeSeries.innerArray();
        indicatedAirspeed = indicatedAirspeedSeries.innerArray();

        // calculate the epoch time for each row as longs so they can most be quickly compared
        // we need to keep track of the date and time series for inserting in the event info
        dateSeries = StringTimeSeries.getStringTimeSeries(connection, flightId, "Lcl Date");
        timeSeries = StringTimeSeries.getStringTimeSeries(connection, flightId, "Lcl Time");
        StringTimeSeries utcOffsetSeries = StringTimeSeries.getStringTimeSeries(connection, flightId, "UTCOfst");

        // check to see if we could get these columns
        if (dateSeries == null || timeSeries == null || utcOffsetSeries == null)
            return false;

        // System.out.println("date length: " + dateSeries.size() + ", time length: " + timeSeries.size() + ", utc
        // length: " + utcOffsetSeries.size());
        int length = dateSeries.size();

        epochTime = new long[length];
        for (int i = 0; i < length; i++) {
            if (dateSeries.emptyAt(i)
                    || timeSeries.emptyAt(i)
                    || utcOffsetSeries.emptyAt(i)) {
                epochTime[i] = 0;
                continue;
            }

            epochTime[i] = TimeUtils.toEpochSecond(dateSeries.get(i), timeSeries.get(i), utcOffsetSeries.get(i));
        }

        hasSeriesData = true;

        return true;
    }

    public boolean hasRegionOverlap(FlightTimeLocation other) {
        return other.maxLatitude >= this.minLatitude && other.minLatitude <= this.maxLatitude
                && other.maxLongitude >= this.minLongitude && other.minLongitude <= this.maxLongitude;
    }

    public boolean isValid() {
        return valid;
    }

    public boolean hasSeriesData() {
        return hasSeriesData;
    }

    public boolean alreadyProcessed(Connection connection) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT flight_id FROM flight_processed WHERE fleet_id = ? AND " +
                        "flight_id = ? AND event_definition_id = ?")) {
            stmt.setInt(1, fleetId);
            stmt.setInt(2, flightId);
            stmt.setInt(3, CalculateProximity.ADJACENCY_EVENT_DEFINITION_ID);

            // if there was a flight processed entry for this flight it was already processed
            try (ResultSet resultSet = stmt.executeQuery()) {
                return resultSet.next();
            }
        }

    }

    public static boolean proximityAlreadyCalculated(Connection connection, FlightTimeLocation first,
                                                     FlightTimeLocation second) throws SQLException {
        try (PreparedStatement stmt = connection
                .prepareStatement("SELECT flight_id FROM events WHERE flight_id = ? AND other_flight_id = ?")) {
            stmt.setInt(1, first.flightId);
            stmt.setInt(2, second.flightId);

            // if there was a flight processed entry for this flight it was already processed
            try (ResultSet resultSet = stmt.executeQuery()) {
                if (resultSet.next()) {
                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    public void updateWithEvent(Connection connection, Event event, String eventStartDateTime)
            throws IOException, SQLException {
        event.updateDatabase(connection, fleetId, flightId, CalculateProximity.ADJACENCY_EVENT_DEFINITION_ID);
        event.updateStatistics(connection, fleetId, airframeNameId, CalculateProximity.ADJACENCY_EVENT_DEFINITION_ID);

        double severity = event.getSeverity();
        double duration = event.getDuration();

        try (PreparedStatement stmt = connection.prepareStatement(
                "UPDATE flight_processed SET count = count + 1, sum_duration = sum_duration + ?, " +
                        "min_duration = LEAST(min_duration, ?), max_duration = GREATEST(max_duration, ?), " +
                        "sum_severity = sum_severity + ?, min_severity = LEAST(min_severity, ?), " +
                        "max_severity = GREATEST(max_severity, ?) " +
                        "WHERE fleet_id = ? AND flight_id = ? AND event_definition_id = ?")) {
            stmt.setInt(1, fleetId);
            stmt.setInt(2, flightId);
            stmt.setInt(3, CalculateProximity.ADJACENCY_EVENT_DEFINITION_ID);
            stmt.setDouble(4, duration);
            stmt.setDouble(5, duration);
            stmt.setDouble(6, duration);
            stmt.setDouble(7, severity);
            stmt.setDouble(8, severity);
            stmt.setDouble(9, severity);
            stmt.executeUpdate();
        }

        EventStatistics.updateFlightsWithEvent(connection, fleetId, airframeNameId,
                CalculateProximity.ADJACENCY_EVENT_DEFINITION_ID, eventStartDateTime);
    }
}
