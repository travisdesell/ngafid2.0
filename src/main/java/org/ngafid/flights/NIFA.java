package org.ngafid.flights;

import org.ngafid.airports.Airport;
import org.ngafid.airports.Airports;
import org.ngafid.airports.Runway;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.ngafid.flights.Parameters.*;
import static org.ngafid.flights.Parameters.PARAM_LOSS_OF_CONTROL_PROBABILITY;

public class NIFA implements Serializable {

    private static final Logger LOG = Logger.getLogger(NIFA.class.getName());

    private static final double FEET_PER_MILE = 5280;

    private final double[] latitude, longitude, altitude, altMSL, distanceFromRunway;

    private double runwayAltitude;
    private final Runway runway;
    private final String flightId;

    public final String airportIataCode;
    public final String flightStartDate;

    private int nTimesteps;

    public NIFA(double[] latitude, double[] longitude, double[] altitude, double[] altMSL, Runway runway, String flightId, String airportIataCode, String flightStartDate) {
        // TODO pass in speed?
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.altMSL = altMSL;
        this.runway = runway;

        this.flightId = flightId;
        this.airportIataCode = airportIataCode;
        this.flightStartDate = flightStartDate;

        this.nTimesteps = this.altitude.length;

        this.distanceFromRunway = new double[this.latitude.length];
        int last = this.longitude.length;
        for (int i = 0; i < last - 1; i++)
            this.distanceFromRunway[i] = Airports.calculateDistanceInFeet(latitude[i], longitude[i], latitude[last - 1], longitude[last - 1]);
    }

    private double[] getExtendedRunwayCenterLine() {
        final double LEN = 2.0;
        double dlat = runway.lat1 - runway.lat2;
        double dlon = runway.lon1 - runway.lon2;
        double lat1 = runway.lat1 + LEN * dlat;
        double lon1 = runway.lon1 + LEN * dlon;
        double lat2 = runway.lat2 - LEN * dlat;
        double lon2 = runway.lon2 - LEN * dlon;
        return new double[] {lat1, lon1, lat2, lon2};
    }

    public double[] getPosition(int timestep) {
        assert timestep < this.nTimesteps;
        return new double[] { latitude[timestep], longitude[timestep] };
    }

    // Shamelessly taken from https://stackoverflow.com/questions/9457988/bearing-from-one-coordinate-to-another
    protected static double bearing(double lat1, double lon1, double lat2, double lon2) {
        double latitude1 = Math.toRadians(lat1);
        double latitude2 = Math.toRadians(lat2);
        double longDiff = Math.toRadians(lon2 - lon1);
        double y = Math.sin(longDiff) * Math.cos(latitude2);
        double x = Math.cos(latitude1) * Math.sin(latitude2) - Math.sin(latitude1) * Math.cos(latitude2) * Math.cos(longDiff);

        return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    }

    public static ArrayList<NIFA> calculateNIFAFlight(Connection connection, Flight flight) throws SQLException {
        DoubleTimeSeries latTimeSeries = flight.getDoubleTimeSeries(PARAM_LATITUDE);
        DoubleTimeSeries lonTimeSeries = flight.getDoubleTimeSeries(PARAM_LONGITUDE);
        DoubleTimeSeries altTimeSeries = flight.getDoubleTimeSeries(PARAM_ALTITUDE_ABOVE_GND_LEVEL);
        DoubleTimeSeries altMSLTimeSeries = flight.getDoubleTimeSeries(PARAM_ALTITUDE_ABOVE_SEA_LEVEL);
        DoubleTimeSeries velocityTimeSeries = flight.getDoubleTimeSeries(PARAM_GND_SPEED);

        int flightId = flight.getId();

        if (Stream.of(latTimeSeries, lonTimeSeries, altTimeSeries, velocityTimeSeries).anyMatch(Objects::isNull)) {
            ArrayList<NIFA> n = new ArrayList<>();
            return n;
        }

        double[] lat = latTimeSeries.innerArray();
        double[] lon = lonTimeSeries.innerArray();
        double[] altitude = altTimeSeries.innerArray();
        double[] velocity = velocityTimeSeries.innerArray();
        assert lat.length == lon.length;

        ArrayList<Itinerary> itineraries = Itinerary.getItinerary(connection, flightId);
        ArrayList<NIFA> n = new ArrayList<>(itineraries.size());

        // Getting normal distance from Runway heading (at least I think)
//        double normal = Runway.getDistanceFt(lat, lon);

        // Notes: one entry per second

        // For each flight itinerary, we need to trace the flight path
        for (Itinerary it : itineraries) {
            int to = it.minAltitudeIndex;
            if (!it.wasApproach())
                continue;

            int from = to;

            // Grab the start and end indices for the time series
            // (taken from TTF so probably inaccurate)
            if (to < 0 || altitude[to] <= 15) {
                to = 0;
            }
            if (from < 0 || altitude[from] <= 300) {
                from = 0;
            }
            if (to == from)
                continue;

            // Need airport/runway
            Airport airport = Airports.getAirport(it.getAirport());
            Runway runway = airport.getRunway(it.getRunway());
            double runwayAltitude = altitude[0];

            // Finding takeoff: (given runway)
            // Need altitude to increase (maybe > 100ft)
            // Ideally, heading should remain +/-10 degrees




            // FIXME mark beginning index of each state only? seems hacky
            ArrayList<Integer> upwindTracking = new ArrayList<>();
            ArrayList<Integer> downwindTracking = new ArrayList<>();
            ArrayList<Integer> crossToDownwindTracking = new ArrayList<>();
            ArrayList<Integer> crossToUpwindTracking = new ArrayList<>();

            double trailingBearing = bearing(runway.lat1, runway.lon1, runway.lat2, runway.lon2);
            double runwayBearing = bearing(runway.lat1, runway.lon1, runway.lat2, runway.lon2);
            double[] bears = new double[60];
            double[] bearDiffs = new double[60];
            // ^^ will be cheeky ~ overwrite this index equal to i mod 60


            // Path tracing:
            // Precondition - assume index 0 is aircraft beginning takeoff
            // 1. derive heading of aircraft for every adjacent point
            // 2. store max change in heading between 60 seconds (60 points)
            // 3. classify the point as:
            //      Tracking: change is < 30 degrees
            //      Turn: otherwise
            // 4. assign state to trailing point of window
            double maxBearingChange = -1.0;
            double windowB;
            int start = 1, end = lat.length;
            for (int i = start; i < end; i++) {
                double normDeg = (trailingBearing - bearing(lat[i-1], lon[i-1], lat[i], lon[i])) % 360;
                double bDiff = Math.min(360-normDeg, normDeg);

//                normDeg = (runwayBearing - bearing(lat[i-1], lon[i-1], lat[i], lon[i])) % 360;
//                double b = Math.min(360-normDeg, normDeg);
                double b = bearing(lat[i-1], lon[i-1], lat[i], lon[i]);

                // 0, 1, 2, 4, 2, 0, 1, 5, 10, 20, 30, 40, 50, 60, 70, 80, 85, 88, 91, 92, 90


                // Store new bearing in current window
                if (i-1 < 60) {
                    bears[i-1] = b;
                    bearDiffs[i-1] = bDiff;
                    windowB = (bears[0] - b) % 360;
                    windowB = Math.min(360-windowB, windowB);
                } else {
                    bears[(i-1) % 60] = b;
                    bearDiffs[(i-1) % 60] = bDiff;
                    trailingBearing = bearDiffs[i % 60];
                    windowB = bDiff;
                }

                int turnBeginIndex;

                if (bearDiffs[i-1] >= 30) {
                    turnBeginIndex = i - 15;
                }


                // Get max bearing change within the window
                double maxB = -1;
                for (double x : bearDiffs) {
//                    maxB = Math.max(x, maxB);
                    if (x > maxB) {
                        maxB = x;
                    }
                }

                // if maxB > 30, I think we can assume the whole window is a turn
                // TODO this, but also double check above logic makes sense
                //  also make sure states are recorded correctly


                // path DFA:
                // upTrack -> turnCross1 -> crossTrack1 -> turnDown -> downTrack -> turnCross2 -> crossTrack2
                // -> turnFinal or turnUp -> upTrack

            }

        }


        return n;
    }
}
