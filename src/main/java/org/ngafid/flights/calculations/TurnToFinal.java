package org.ngafid.flights;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import org.ngafid.airports.Airport;
import org.ngafid.airports.Airports;
import org.ngafid.airports.Runway;
import org.ngafid.common.Compression;

import javax.sql.rowset.serial.SerialBlob;
import java.io.IOException;
import java.io.Serializable;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.ngafid.flights.Parameters.*; //eliminates the need to use Parameters.<PARAM>

public class TurnToFinal implements Serializable {
    //                                             NGAFIDTTF0000L
    public static final long serialVersionUID = 0x46AF1D77F0000L;

    private static final Logger LOG = Logger.getLogger(TurnToFinal.class.getName());

    private static final double FEET_PER_MILE = 5280;

    // 0.1 miles of tolerance is allowed
    private static final double CENTER_LINE_DEVIATION_TOLERANCE_IN_MILES = 0.1;

    // List of (lat, long) coords (in that order) representing a turn to final
    private final double[] latitude, longitude, altitude, roll, stallProbability, locProbability, altMSL, distanceFromRunway;
    private double runwayAltitude;
    private double maxRoll;
    private final Runway runway;
    private final String flightId;

    public final String airportIataCode;
    public final String flightStartDate;

    private int nTimesteps;

    private ArrayList<Integer> locExceedences;
    private ArrayList<Integer> centerLineExceedences;
    private ArrayList<Double>  selfDefinedGlidePathDeviations;

    private double selfDefinedGlideAngle;

    /**
     * The timeseries arrays passed here should start from the 400 ft above the runway point, and end when the aircraft
     * touches the runway.
     * @param altitude
     * @param roll
     * @param lat
     * @param lon
     */
    public TurnToFinal(String flightId, String airframe, Runway runway, String airportIataCode, String flightStartDate, double runwayAltitude, double[] altitude, double[] altMSL, double[] roll,
                       double[] lat, double[] lon, double[] stallProbability, double[] locProbability) {
        this.flightId = flightId;
        this.runway = runway;
        this.runwayAltitude = runwayAltitude;
        this.latitude = lat;
        this.longitude = lon;
        this.altitude = altitude;
        this.altMSL = altMSL;
        this.roll = roll;
        this.airportIataCode = airportIataCode;
        this.flightStartDate = flightStartDate;
        this.stallProbability = stallProbability;
        this.locProbability = locProbability;
        selfDefinedGlidePathDeviations = new ArrayList<>();

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

        calculateSelfDefindGlidePath();

        this.maxRoll = Arrays.stream(roll).map(Math::abs).max().getAsDouble();

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

    // Calculate self defined glide path + glide path angle
    private void calculateSelfDefindGlidePath() {
        //  
        //   A
        //   *
        //   |\
        //   | \
        //   |  \
        //   |   \
        //   |    \
        //   |     \ 
        //   |      *  D
        //   |      |\
        //   |      | \
        //   |      |  \
        //   |      |   \
        //   |__    |    \
        //   |##|   |     \ 
        //   *------*------*
        //   B      E       C
        //
        //
        // In this diagram, A is the altitude of the plane ~ 300 feet, projected back a bit to allow for calculation of the glide path
        // and deviations from it. B + C is the altitude of the aircraft when it / is closest to the runway. B can be considered the origin,
        // and the line BC is the distance between the lat / lon coordinates at the beginning of the turn to final.

        final int last = altMSL.length - 1;
        final double altA = altMSL[0];
        final double altC = altMSL[last];
        final double altB = altC;

        // This is all in terms of feet
        final double BC = Airports.calculateDistanceInFeet(latitude[0], longitude[0], latitude[last], longitude[last]);
        final double AB = altA - altB;
        selfDefinedGlideAngle = Math.toDegrees(Math.tanh(AB / BC));

        // Feet of descent per foot traveled towards the runway.
        final double expDescent = AB / BC;
        for (int i = 0; i < altMSL.length; i++) {
            double lat = latitude[i];
            double lon = longitude[i];
            double EC = Airports.calculateDistanceInFeet(lat, lon, latitude[last], longitude[last]);
            double expAlt = altB + expDescent * EC;
            double actualAlt = altMSL[i];
            double deviation = expAlt - actualAlt;
            selfDefinedGlidePathDeviations.add(deviation);
        }

        // System.out.printf("%f %f %f %f\n", expDescent, altA, altC, altA - expDescent);
    }

    public double[] getPosition(int timestep) {
        assert timestep < this.nTimesteps;
        return new double[] { latitude[timestep], longitude[timestep] };
    }

    public static void cacheTurnToFinal(Connection connection, int flight_id, ArrayList<TurnToFinal> ttfs) throws SQLException, IOException {
        if (ttfs == null) {
            LOG.info("CANNOT INSERT NULL TTF");
            throw new NullPointerException();
        }

        byte[] data = Compression.compressObject(ttfs);
        assert data != null;
        Blob blob = new SerialBlob(data);

        PreparedStatement preparedStatement =
                connection.prepareStatement("INSERT INTO turn_to_final (flight_id, version, data) VALUES (?, ?, ?)");
        preparedStatement.setInt(1, flight_id);
        preparedStatement.setLong(2, TurnToFinal.serialVersionUID);
        preparedStatement.setBlob(3, blob);

        preparedStatement.execute();
        preparedStatement.close();

        blob.free();
    }

    public static ArrayList<TurnToFinal> getTurnToFinalFromCache(Connection connection, Flight flight) throws SQLException, IOException, ClassNotFoundException {
        PreparedStatement query = connection.prepareStatement("SELECT * FROM turn_to_final WHERE flight_id = ?");
        query.setInt(1, flight.getId());
        LOG.info(query.toString());

        ResultSet resultSet = query.executeQuery();
        if (!resultSet.next()) {
            LOG.info("Flight not found in TurnToFinal cache");
            resultSet.close();
            query.close();
            return null;
        }

        long version = resultSet.getLong(2);
        Blob values = resultSet.getBlob(3);

        query.close();
        resultSet.close();

        if (version != TurnToFinal.serialVersionUID) {
            LOG.info("TTF VERSION OUTDATED");
            PreparedStatement deleteQuery = connection.prepareStatement("DELETE FROM turn_to_final WHERE flight_id = ?");
            query.setInt(1, flight.getId());
            deleteQuery.executeUpdate();
            deleteQuery.close();

            return null;
        }

        Object o = Compression.inflateObject(values.getBytes(1, (int) values.length()));
        assert o instanceof ArrayList;

        LOG.info("FOUND IN TTF CACHE: " + o.toString());
        
        return (ArrayList<TurnToFinal>) o;

        // If you want to get rid of the unchecked caste
        // if (o instanceof ArrayList<?>) {
        //     ArrayList<?> a = (ArrayList<?>) o;
        //     ArrayList<TurnToFinal> ttfs = new ArrayList<TurnToFinal>(a.size());
        //     for (Object i : a) {
        //         if (i instanceof TurnToFinal) {
        //             ttfs.add((TurnToFinal) i);
        //         } else {
        //             LOG.severe("Found incorrect element object in TurnToFinal cache.");
        //             return null;
        //         }
        //     }
        //     return ttfs;
        // } else {
        //     LOG.severe("Found incorrect object in TurnToFinal cache.");
        //     return null;
        // }
    }

    public static ArrayList<TurnToFinal> calculateFlightTurnToFinals(Connection connection, Flight flight) throws SQLException, IOException {
        DoubleTimeSeries latTimeSeries = flight.getDoubleTimeSeries(PARAM_LATITUDE);
        DoubleTimeSeries lonTimeSeries = flight.getDoubleTimeSeries(PARAM_LONGITUDE);
        DoubleTimeSeries altTimeSeries = flight.getDoubleTimeSeries(PARAM_ALTITUDE_ABOVE_GND_LEVEL);
        DoubleTimeSeries altMSLTimeSeries = flight.getDoubleTimeSeries(PARAM_ALTITUDE_ABOVE_SEA_LEVEL);
        DoubleTimeSeries rollTimeSeries = flight.getDoubleTimeSeries(PARAM_ROLL);
        DoubleTimeSeries velocityTimeSeries = flight.getDoubleTimeSeries(PARAM_GND_SPEED);
        DoubleTimeSeries stallProbability = flight.getDoubleTimeSeries(PARAM_STALL_PROBABILITY);
        DoubleTimeSeries locProbability = flight.getDoubleTimeSeries(PARAM_LOSS_OF_CONTROL_PROBABILITY);

        int flightId = flight.getId();

        if (Stream.of(latTimeSeries, lonTimeSeries, altTimeSeries, rollTimeSeries, velocityTimeSeries).anyMatch(Objects::isNull)) {
            ArrayList<TurnToFinal> ttfs = new ArrayList<>();
            cacheTurnToFinal(connection, flight.getId(), ttfs);
            return ttfs;
        }

        double[] lat = latTimeSeries.innerArray();
        double[] lon = lonTimeSeries.innerArray();
        double[] altitude = altTimeSeries.innerArray();
        // double[] roll = rollTimeSeries.innerArray();
        // double[] velocity = velocityTimeSeries.innerArray();
        assert lat.length == lon.length;

        ArrayList<Itinerary> itineraries = Itinerary.getItinerary(connection, flightId);
        ArrayList<TurnToFinal> ttfs = new ArrayList<>(itineraries.size());

        for (Itinerary it : itineraries) {
            int to = it.minAltitudeIndex;
            if (!it.wasApproach())
                continue;

            int from = to;

            Airport airport = Airports.getAirport(it.getAirport());
            Runway runway = airport.getRunway(it.getRunway());
            double runwayAltitude = altitude[to];

            for (;;) {
                if (to < 0) {
                    to = 0;
                    break;
                }

                if (altitude[to] > 15) // - runwayAltitude > 30)
                    break;

                to -= 1;
            }

            // Find the timestep at which the aircraft is 400ft above the runway's altitude
            for (;;) {
                if (from < 0) {
                    // We never found a point in time where there is a turn to final
                    // We assume all aircraft that perform a turn to final will reach 400 feet above the runway
                    from = 0;
                    break;
                }

                if (altitude[from] > 300) // - runwayAltitude > 400)
                    break;

                from -= 1;
            }

            if (to == from)
                continue;

            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            for (int i = from; i < to; i++) {
                min = Math.min(min, altitude[i]);
                max = Math.max(max, altitude[i]);
            }

            if (max - min < 60 || Double.isNaN(max - min))
                continue;
            if (min > 100 || Double.isNaN(min))
                continue;

            double[] stallProbabilityArray = null, locProbabilityArray = null;
            if (stallProbability != null)
                stallProbabilityArray = stallProbability.sliceCopy(from, to);
            if (locProbability != null)
                locProbabilityArray = locProbability.sliceCopy(from, to);

            TurnToFinal ttf = new TurnToFinal(Integer.toString(flightId),
                    flight.getAirframeType(), runway, airport.iataCode, flight.getStartDateTime(), runwayAltitude,
                    altTimeSeries.sliceCopy(from, to),
                    altMSLTimeSeries.sliceCopy(from, to),
                    rollTimeSeries.sliceCopy(from, to),
                    latTimeSeries.sliceCopy(from, to),
                    lonTimeSeries.sliceCopy(from, to),
                    stallProbabilityArray, locProbabilityArray);
            ttfs.add(ttf);
            //ttfs.add(new TurnToFinal(altitude, lat, lon, from, to));
        }
        cacheTurnToFinal(connection, flight.getId(), ttfs);

        return ttfs;
    }

    /**
     * Returns an array list of all of the turn to finals for the given flight that occur at the specified airport
     * @param connection database connection
     * @param flight the flight for which the turn to finals should be analyzed
     * @param airportIataCode the IATA code for the airport. If this is null, all of the TTFs will be returned.
     * @return
     * @throws SQLException
     */
    public static ArrayList<TurnToFinal> getTurnToFinal(Connection connection, Flight flight, String airportIataCode) throws SQLException, IOException, ClassNotFoundException {
        ArrayList<TurnToFinal> turnToFinals = getTurnToFinalFromCache(connection, flight);

        if (turnToFinals == null) {
            turnToFinals = calculateFlightTurnToFinals(connection, flight);
        }

        return turnToFinals.stream()
                .filter(ttf -> airportIataCode == null || ttf.airportIataCode.equals(airportIataCode))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public static ArrayList<TurnToFinal> getTurnToFinal(Connection connection, int flightId, String airportIataCode) throws SQLException, IOException, ClassNotFoundException {
        // For now just use the flight object to get lat and long series
        // In the future we could just get the lat and long series in isolation to speed things up
        Flight flight = Flight.getFlight(connection, flightId);
        assert flight != null;

        return getTurnToFinal(connection, flight, airportIataCode);
    }

    public JsonElement jsonify() {
        Gson gson = new Gson();
        try {
            return gson.toJsonTree(Map.ofEntries(
                    Map.entry(PARAM_JSON_LOSS_OF_CONTROL_EXC, this.locExceedences),
                    Map.entry(PARAM_JSON_CENTER_LINE_EXC, this.centerLineExceedences),
                    Map.entry(PARAM_JSON_SELF_DEFINED_GLIDE_PATH_ANGLE, this.selfDefinedGlideAngle),
                    Map.entry(PARAM_JSON_LATITUDE, this.latitude),
                    Map.entry(PARAM_JSON_LONGITUDE, this.longitude),
                    Map.entry(PARAM_ALTITUDE_ABOVE_SEA_LEVEL, this.altMSL),
                    Map.entry("AltAGL", this.altitude),
                    Map.entry("distanceFromRunway", this.distanceFromRunway),
                    Map.entry("flightId", this.flightId),
                    Map.entry("runway", this.runway),
                    Map.entry("airportIataCode", this.airportIataCode),
                    Map.entry("flightStartDate", this.flightStartDate),
                    Map.entry("maxRoll", this.maxRoll),
                    Map.entry("selfDefinedGlidePathDeviations", this.selfDefinedGlidePathDeviations),
                    Map.entry(PARAM_LOSS_OF_CONTROL_PROBABILITY, this.locProbability != null ? this.locProbability : false),
                    Map.entry(PARAM_STALL_PROBABILITY, this.stallProbability != null ? this.stallProbability : false))
            );
        }
        catch (IllegalArgumentException _iae) {
            // This means there were nans in some of the arrays which means we can't necissarily analyze it and it
            // won't parse on the front end since nan is not included in the JSON spec for some reason
            return null;
        }
    }
}
