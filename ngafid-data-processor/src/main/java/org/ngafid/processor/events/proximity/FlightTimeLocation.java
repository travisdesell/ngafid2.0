package org.ngafid.processor.events.proximity;

import org.ngafid.core.flights.DoubleTimeSeries;
import org.ngafid.core.flights.Flight;
import org.ngafid.core.flights.Parameters;
import org.ngafid.core.flights.StringTimeSeries;
import org.ngafid.core.util.filters.Pair;

import java.io.IOException;
import java.sql.Connection;
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

    double[] altitudeMSL;
    double[] altitudeAGL;
    double[] latitude;
    double[] longitude;
    double[] indicatedAirspeed;

    StringTimeSeries utc;
    DoubleTimeSeries epochTime;
    //CHECKSTYLE:ON


    public FlightTimeLocation(Connection connection, Flight flight) throws SQLException {
        this.fleetId = flight.getFleetId();
        this.flightId = flight.getId();
        this.airframeNameId = flight.getAirframeNameId();
        this.startDateTime = flight.getStartDateTime();
        this.endDateTime = flight.getEndDateTime();

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
    public boolean getSeriesData(Connection connection) throws SQLException {
        // get the time series data for altitude, latitude and longitude
        DoubleTimeSeries altMSLSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, Parameters.ALT_MSL);
        DoubleTimeSeries altAGLSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, Parameters.ALT_AGL);
        DoubleTimeSeries latitudeSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, Parameters.LATITUDE);
        DoubleTimeSeries longitudeSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, Parameters.LONGITUDE);
        DoubleTimeSeries indicatedAirspeedSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, Parameters.IAS);

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
        utc = StringTimeSeries.getStringTimeSeries(connection, flightId, Parameters.UTC_DATE_TIME);
        epochTime = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, Parameters.UNIX_TIME_SECONDS);

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
}
