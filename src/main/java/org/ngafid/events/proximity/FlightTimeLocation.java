package org.ngafid.events.proximity;

import org.jline.utils.Log;
import org.ngafid.common.filters.Pair;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.Flight;
import org.ngafid.flights.Parameters;
import org.ngafid.flights.StringTimeSeries;

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


        if (startDateTime == null || endDateTime == null) {
            valid = false;
            return;
        }

        Pair<Double, Double> minMaxRPM1 = DoubleTimeSeries.getMinMax(connection, flightId, "E1 RPM");
        Pair<Double, Double> minMaxRPM2 = DoubleTimeSeries.getMinMax(connection, flightId, "E2 RPM");


        // Invalidate the flight if RPM is present and too low
        if ((minMaxRPM1 != null && minMaxRPM1.second() < 800) &&
                (minMaxRPM2 != null && minMaxRPM2.second() < 800)) {
            Log.info("Flight is not valid, RPM error");
            valid = false;
            return;
        }


        Pair<Double, Double> minMaxLatitude = DoubleTimeSeries.getMinMax(connection, flightId, "Latitude");
        Pair<Double, Double> minMaxLongitude = DoubleTimeSeries.getMinMax(connection, flightId, "Longitude");

         if (minMaxLatitude == null || minMaxLongitude == null) {
             Log.info("Flight is not valid, Longitude error");
            valid = false;
            return;
        }

        minLatitude = minMaxLatitude.first();
        maxLatitude = minMaxLatitude.second();
        minLongitude = minMaxLongitude.first();
        maxLongitude = minMaxLongitude.second();

        Pair<Double, Double> minMaxAltMSL = DoubleTimeSeries.getMinMax(connection, flightId, "AltMSL");

        if (minMaxAltMSL == null) {
            Log.info("Flight is not valid, AltMSL error");
            valid = false;
            return;
        }

        minAltMSL = minMaxAltMSL.first();
        maxAltMSL = minMaxAltMSL.second();

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

        DoubleTimeSeries altMSLSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, Parameters.ALT_MSL);
        DoubleTimeSeries altAGLSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, Parameters.ALT_AGL);
        DoubleTimeSeries latitudeSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, Parameters.LATITUDE);
        DoubleTimeSeries longitudeSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, Parameters.LONGITUDE);
        DoubleTimeSeries indicatedAirspeedSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, Parameters.IAS);

        if (altMSLSeries == null || altAGLSeries == null || latitudeSeries == null || longitudeSeries == null
                || indicatedAirspeedSeries == null) {
            return false;
        }

        altitudeMSL = altMSLSeries.innerArray();
        altitudeAGL = altAGLSeries.innerArray();
        latitude = latitudeSeries.innerArray();
        longitude = longitudeSeries.innerArray();
        indicatedAirspeed = indicatedAirspeedSeries.innerArray();

        utc = StringTimeSeries.getStringTimeSeries(connection, flightId, Parameters.UTC_DATE_TIME);
        epochTime = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, Parameters.UNIX_TIME_SECONDS);

        hasSeriesData = true;
        return true;
    }

    public boolean hasRegionOverlap(FlightTimeLocation other) {

        boolean overlap = other.maxLatitude >= this.minLatitude && other.minLatitude <= this.maxLatitude
                && other.maxLongitude >= this.minLongitude && other.minLongitude <= this.maxLongitude;
        //return overlap;
        return true;
    }

    public boolean isValid() {
        return valid;
    }

    public boolean hasSeriesData() {
        return hasSeriesData;
    }
}
