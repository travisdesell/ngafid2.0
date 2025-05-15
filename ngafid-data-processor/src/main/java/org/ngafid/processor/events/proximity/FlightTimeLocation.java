package org.ngafid.processor.events.proximity;

import org.ngafid.core.flights.DoubleTimeSeries;
import org.ngafid.core.flights.Flight;
import org.ngafid.core.flights.Parameters;
import org.ngafid.core.flights.StringTimeSeries;
import org.ngafid.core.util.filters.Pair;
import org.ngafid.processor.format.CSVFileProcessor;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

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
    private static final Logger LOG = Logger.getLogger(FlightTimeLocation.class.getName());

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


        // then check and see if this flight had a latitude and longitude, if not we cannot calculate adjacency
        Pair<Double, Double> minMaxLatitude = DoubleTimeSeries.getMinMax(connection, flightId, "Latitude");
        Pair<Double, Double> minMaxLongitude = DoubleTimeSeries.getMinMax(connection, flightId, "Longitude");

        if (minMaxLatitude == null || minMaxLongitude == null) {
            LOG.severe("Flight" + flight.getAirframe() + "is not valid, Longitude error");
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

        boolean overlap = other.maxLatitude >= this.minLatitude &&
                other.minLatitude <= this.maxLatitude &&
                other.maxLongitude >= this.minLongitude &&
                other.minLongitude <= this.maxLongitude;

        LOG.info("Has region overlap: " + overlap);
        return overlap;
    }

    /**
     *  Checks whether the geographic bounding box of another flight overlaps
     *  with this flight's bounding box after expanding this flight's box by a given buffer.
     *  Used to identify candidate flights for proximity detection, even when
     *  their original bounding boxes do not overlap but their flight paths may have come
     *  close (e.g., within 1000 feet).
     * @param other FlightTimeLocation
     * @param degreeBuffer represent the desired proximity threshold (e.g. 0.003 degrees = 1000 feet)
     * @return
     */

    public boolean hasBufferedRegionOverlap(FlightTimeLocation other, double degreeBuffer) {
        double bufferedMinLat = this.minLatitude - degreeBuffer;
        double bufferedMaxLat = this.maxLatitude + degreeBuffer;
        double bufferedMinLon = this.minLongitude - degreeBuffer;
        double bufferedMaxLon = this.maxLongitude + degreeBuffer;

        boolean overlap = other.maxLatitude >= bufferedMinLat &&
                other.minLatitude <= bufferedMaxLat &&
                other.maxLongitude >= bufferedMinLon &&
                other.minLongitude <= bufferedMaxLon;


        LOG.info("Buffered overlap check: " + overlap);
        return overlap;
    }





    public boolean isValid() {
        return valid;
    }

    public boolean hasSeriesData() {
        return hasSeriesData;
    }
}

