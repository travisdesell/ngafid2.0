package org.ngafid.flights;

import com.google.gson.Gson;
import org.ngafid.airports.Airport;
import org.ngafid.airports.Airports;
import org.ngafid.airports.Runway;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.Collections.*;
import java.util.function.Function;
import java.util.logging.Logger;

public class TurnToFinal {
    private static final Logger LOG = Logger.getLogger(TurnToFinal.class.getName());

    private static final double FEET_PER_MILE = 5280;

    // The optimal descent is 300 feet vertically down for every mile traveled horizontally
    private static final double OPTIMAL_DESCENT_PER_MILE = 300;

    // 0.1 miles of tolerance is allowed
    private static final double CENTER_LINE_DEVIATION_TOLERANCE_IN_MILES = 0.1;

    private static final Map<String, Double> AIRFRAME_TO_OPTIMAL_DESCENT_SPEED =
            Map.of("Cessna 172S", 61.0,
                    "PA-28-181", 66.0);

    // List of (lat, long) coords (in that order) representing a turn to final
    private final double[] latitude, longitude, altitude, roll, velocity;
    private double runwayAltitude;
    private final Runway runway;
    private final String airframe;

    private int nTimesteps;

    private ArrayList<Integer> locExceedences;
    private ArrayList<Integer> centerLineExceedences;
    private double selfDefinedGlideAngle = Float.NaN;
    private ArrayList<Integer> optimalDescentSlopeWarnings;
    private ArrayList<Integer> optimalDescentSlopeExceedences;
    private ArrayList<Integer> optimalDescentSpeedExceedences;

    // TODO: Think about using ArrayList<Double> to avoid the conversion steps
    // TODO: Consider saving the whole flight object here

    /**
     * The timeseries arrays passed here should start from the 400 ft above the runway point, and end when the aircraft
     * touches the runway.
     * @param altitude
     * @param roll
     * @param lat
     * @param lon
     */
    public TurnToFinal(String airframe, Runway runway, double runwayAltitude, double[] altitude, double[] roll,
                       double[] lat, double[] lon, double[] velocity) {
        this.runway = runway;
        this.runwayAltitude = runwayAltitude;
        this.latitude = lat;
        this.longitude = lon;
        this.altitude = altitude;
        this.roll = roll;
        this.airframe = airframe;
        this.velocity = velocity;

        assert this.latitude.length == this.longitude.length;
        assert this.latitude.length == this.altitude.length;

        this.nTimesteps = this.altitude.length;

        // "Loss of control" exceedences
        // We define a loss of control exceedence as:
        // 30 degrees or greater of roll while less than 400ft above the runway
        this.locExceedences = new ArrayList<>();
        calculateLocExceedences();

        // "Center line exceedences"
        // We define a center line exceedence as: (but this is subject to change)
        // deviation > 0.1 nm from the runway center line while less than 400ft above the runway
        this.centerLineExceedences = new ArrayList<>();
        calculateCenterLineExceedences();

        // This is the angle from the ground between the aircrafts position at 400ft and the touchdown point
        // There should be one entry here per itinerary object
        this.selfDefinedGlideAngle = Double.NaN;
        calculateSelfDefinedGlideAngle();

        // "Optimal descent exceedence"
        // The optimal descent ratio is 300ft descent for every 1 nautical mile.
        // The optimal line can be easily constructed starting from the 400ft above the runway point,
        // and we look for deviations > 5 ft and > 10 ft
        this.optimalDescentSlopeExceedences = new ArrayList<>();
        this.optimalDescentSlopeWarnings = new ArrayList<>();
        this.optimalDescentSpeedExceedences = new ArrayList<>();
        calculateOptimalDescentExceedences();


        // TODO: write methods to populate these lists
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

    private void calculateCenterLineExceedences() {
        double[] extendedRunwayCenterLine = this.getExtendedRunwayCenterLine();
        double lat1 = extendedRunwayCenterLine[0];
        double lon1 = extendedRunwayCenterLine[1];
        double lat2 = extendedRunwayCenterLine[2];
        double lon2 = extendedRunwayCenterLine[3];

        double tolerance = FEET_PER_MILE * CENTER_LINE_DEVIATION_TOLERANCE_IN_MILES;

        for (int i = 0; i < this.latitude.length; i++) {
            double plat = this.latitude[i];
            double plon = this.longitude[i];
            double minDistance = Airports.shortestDistanceBetweenLineAndPointFt(plat, plon, lat1, lon1, lat2, lon2);
            if (minDistance > tolerance)
                this.centerLineExceedences.add(i);
        }
    }

    private void calculateLocExceedences() {
        for (int i = 0; i < this.roll.length; i++) {
            if (    this.altitude[i] - this.runwayAltitude < 400 &&
                    Math.abs(this.roll[i]) > 30)
                this.locExceedences.add(i);
        }
    }

    private void calculateSelfDefinedGlideAngle() {
        //     *
        //     | \
        //     |   \  c = sqrt(a*a + b*b);
        //  a  |     \
        //     *_______*
        //         b
        //
        // Angle bc is the self defined glide angle if:
        //   - vertex ac is the location of the plane
        //   - a is the vertical distance of the plane to the runway (plane height - runway height)
        //   - b is the horizontal distance of the plane to the runway
        //
        // 1. We must find the length of side c using the pythagorean theorem
        // 2. We the use arcsin to find the angle:
        //              sin(angle_bc) = b / c
        //      thus:   arcsin(b / c) = angle_bc


        int firstIndex = 0; // This is constant for now but just in case it isnt in the future
        double a = this.altitude[firstIndex] - runwayAltitude;

        double startLat = this.latitude[firstIndex];
        double startLon = this.longitude[firstIndex];
        int lastIndex = this.latitude.length - 1;
        double touchdownLat = this.latitude[lastIndex];
        double touchdownLon = this.longitude[lastIndex];
        double b = Airports.calculateDistanceInFeet(startLat, startLon, touchdownLat, touchdownLon);

        double c = Math.sqrt(a * a + b * b);

        this.selfDefinedGlideAngle = Math.asin(b / c);
    }

    private void calculateOptimalDescentExceedences() {
        // 1. Figure out what the slope should be for the optimal descent (i.e. 300ft / 1 mile)
        // 2. Use this slope to figure out what the altitude should be at any point in time
        // 3. If the planes altitude is > 5 ft different than the optimal altitude,
        //      then add a warning
        // 4. If the planes altitude > 10ft different than the optimal altitude,
        //      then add an error
        // So, our list will contain valid and invalid indices, so we must check them when we want to use them or there
        // will be some index out of bounds stuff.
        //
        // The aircraft are supposed strive to have a constant speed while descending, so we also check to ensure they are
        // close to the optimal. We will use a percentage threshold: if your speed is X% different, then you we'll
        // consider it an exceedence. This X value will be a double between 0 and 1, but the appropriate value is yet
        // to be decided

        // TODO: Figure out if the assumptions made in this method are acceptable:
        //      1. The aircraft is may not be traveling stright towards the runway when it hits the
        //          400 ft mark. This means the "optimal slope" may not be correct.

        int start_idx = 0;
        int end_index = this.altitude.length - 1;

        // How far the aircraft is from the touchdown point horizontally, in feet
        double distance = Airports.calculateDistanceInFeet(
                latitude[start_idx], longitude[start_idx], latitude[end_index], longitude[end_index]);
        double altitude = this.altitude[start_idx];

        // How the aircraft should descend optimally
        // 300 ft / 1 mile = x feet / distance miles
        // (300 ft / 1 mile) * distance miles = x feet
        double optimal_slope = OPTIMAL_DESCENT_PER_MILE;

        for (int i = 1; i < this.altitude.length; i++) {
            // This is probably not a cheap calculation
            double next_distance = Airports.calculateDistanceInFeet(
                    latitude[i], longitude[i], latitude[end_index], longitude[end_index]);

            // This is the distance traveled directly towards the touchdown point.
            // We may want to change this to the absolute amount of distance traveled
            double dist_horizontally_traveled_ft = distance - next_distance;
            double dist_horizontally_traveled_miles = dist_horizontally_traveled_ft / FEET_PER_MILE;
            double proper_descent_amount = optimal_slope * dist_horizontally_traveled_miles;
            double actual_altitude = this.altitude[i];

            // The difference between actual altitude and the expected altitude
            double delta = Math.abs(actual_altitude - (altitude - proper_descent_amount));

            if (delta > 10) {
                this.optimalDescentSlopeExceedences.add(i);
            } else if (delta > 5) {
                this.optimalDescentSlopeWarnings.add(i);
            }

            double velocity =

            // Carry our calculated optimal altitude.
            altitude = altitude - proper_descent_amount;
            distance = next_distance;
        }

    }

    public double[] getPosition(int timestep) {
        assert timestep < this.nTimesteps;
        return new double[] { latitude[timestep], longitude[timestep] };
    }

    public static ArrayList<TurnToFinal> getTurnToFinal(Connection connection, int flightId) throws SQLException {
        // For now just use the flight object to get lat and long series
        // In the future we could just get the lat and long series in isolation to speed things up
        Flight flight = Flight.getFlight(connection, flightId);
        DoubleTimeSeries latTimeSeries = flight.getDoubleTimeSeries("Latitude");
        DoubleTimeSeries lonTimeSeries = flight.getDoubleTimeSeries("Longitude");
        // TODO: Verify that these are the correct names
        DoubleTimeSeries altTimeSeries = flight.getDoubleTimeSeries("Altitude");
        DoubleTimeSeries rollTimeSeries = flight.getDoubleTimeSeries("Roll");
        DoubleTimeSeries velocityTimeSeries = flight.getDoubleTimeSeries("GndSpd");

        assert latTimeSeries != null;
        assert lonTimeSeries != null;

        double[] lat = latTimeSeries.innerArray();
        double[] lon = lonTimeSeries.innerArray();
        double[] altitude = altTimeSeries.innerArray();
        double[] roll = rollTimeSeries.innerArray();
        double[] velocity = velocityTimeSeries.innerArray();
        assert lat.length == lon.length;

        ArrayList<Itinerary> itineraries = Itinerary.getItinerary(connection, flightId);

        // I don't think this should happen?
        if (itineraries.size() == 0) return new ArrayList<>();

        ArrayList<TurnToFinal> ttfs = new ArrayList<>(itineraries.size());

        for (Itinerary it : itineraries) {
            int to = it.minAltitudeIndex;
            int from = to;

            Airport airport = Airports.getAirport(it.getAirport());
            Runway runway = airport.getRunway(it.getRunway());

            double runwayAltitude = altitude[to];

            // Find the timestep at which the aircraft is 400ft above the runway's altitude
            for (;;) {
                if (from < 0) {
                    from = 0;
                    break;
                }
                if (altitude[from] - runwayAltitude > 400)
                    break;
                from -= 1;
            }
            TurnToFinal ttf = new TurnToFinal(flight.getAirframeType(), runway, runwayAltitude,
                                altTimeSeries.sliceCopy(from, to),
                                rollTimeSeries.sliceCopy(from, to),
                                latTimeSeries.sliceCopy(from, to),
                                lonTimeSeries.sliceCopy(from, to),
                                velocityTimeSeries.sliceCopy(from, to));
            ttfs.add(ttf);
            //ttfs.add(new TurnToFinal(altitude, lat, lon, from, to));
        }

        return ttfs;
    }

    public String jsonify() {
        Gson gson = new Gson();
        return gson.toJson(Map.of(
                "locExceedences", gson.toJson(this.locExceedences),
                "centerLineExceedences", gson.toJson(this.centerLineExceedences),
                "selfDefinedGlideAngle", gson.toJson(this.selfDefinedGlideAngle),
                "optimalDescentSlopeWarnings", gson.toJson(this.optimalDescentSlopeWarnings),
                "optimalDescentSlopeExceedences", gson.toJson(this.optimalDescentSlopeExceedences)
        ));
    }
}
