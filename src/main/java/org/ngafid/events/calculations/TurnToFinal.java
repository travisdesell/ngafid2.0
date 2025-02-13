package org.ngafid.events.calculations;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ngafid.common.Compression;
import org.ngafid.common.airports.Airport;
import org.ngafid.common.airports.Airports;
import org.ngafid.common.airports.Runway;
import org.ngafid.flights.*;

import javax.sql.rowset.serial.SerialBlob;
import java.io.IOException;
import java.io.Serializable;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TurnToFinal implements Serializable {
    //                                             NGAFIDTTF0000L
    public static final long serialVersionUID = 0x46AF1D77F0001L;

    private static final Logger LOG = Logger.getLogger(TurnToFinal.class.getName());

    private static final double FEET_PER_MILE = 5280;

    // 0.1 miles of tolerance is allowed
    private static final double CENTER_LINE_DEVIATION_TOLERANCE_IN_MILES = 0.1;

    // List of (lat, long) coords (in that order) representing a turn to final
    private final double[] latitude, longitude, altitude, roll, stallProbability, locProbability, altMSL, distanceFromRunway;
    private double runwayAltitude;
    private double maxRoll;
    private final Runway runway;
    private String flightId;

    public final String airportIataCode;
    public final String flightStartDate;

    private int nTimesteps;

    private ArrayList<Integer> locExceedences;
    private ArrayList<Integer> centerLineExceedences;
    private ArrayList<Double> selfDefinedGlidePathDeviations;

    private double selfDefinedGlideAngle;

    /**
     * The timeseries arrays passed here should start from the 400 ft above the runway point, and end when the aircraft
     * touches the runway.
     *
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

    public void setFlightId(int id) {
        flightId = Integer.toString(id);
    }

    private double[] getExtendedRunwayCenterLine() {
        final double LEN = 2.0;
        double dlat = runway.lat1 - runway.lat2;
        double dlon = runway.lon1 - runway.lon2;
        double lat1 = runway.lat1 + LEN * dlat;
        double lon1 = runway.lon1 + LEN * dlon;
        double lat2 = runway.lat2 - LEN * dlat;
        double lon2 = runway.lon2 - LEN * dlon;
        return new double[]{lat1, lon1, lat2, lon2};
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
            if (this.altitude[i] - this.runwayAltitude < 400 &&
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
        return new double[]{latitude[timestep], longitude[timestep]};
    }

    public static void cacheTurnToFinal(Connection connection, int flightId, ArrayList<TurnToFinal> ttfs) throws IOException, SQLException {
        if (ttfs == null) {
            LOG.info("CANNOT INSERT NULL TTF");
            throw new NullPointerException();
        }

        byte[] data = Compression.compressObject(ttfs);

        for (var ttf : ttfs)
            ttf.setFlightId(flightId);

        try (PreparedStatement preparedStatement =
                     connection.prepareStatement("INSERT INTO turn_to_final (flight_id, version, data) VALUES (?, ?, ?)")) {

            Blob blob = new SerialBlob(data);

            preparedStatement.setInt(1, flightId);
            preparedStatement.setLong(2, TurnToFinal.serialVersionUID);
            preparedStatement.setBlob(3, blob);
            preparedStatement.execute();

            blob.free();
        }
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

        try {
            Object o = Compression.inflateTTFObject(values.getBytes(1, (int) values.length()));
            assert o instanceof ArrayList;

            @SuppressWarnings("unchecked")
            ArrayList<TurnToFinal> ttfs = (ArrayList<TurnToFinal>) o;
            return ttfs;
        } catch (ClassNotFoundException ce) {
            LOG.info("Serialization error: ");
            ce.printStackTrace();

            LOG.info("Deleting problematic ttf row.");

            query = connection.prepareStatement("DELETE FROM turn_to_final WHERE flight_id = ?");
            query.setInt(1, flight.getId());
            LOG.info(query.toString());
            query.execute();
            query.close();

            return null;
        }

    }

    public static ArrayList<TurnToFinal> calculateFlightTurnToFinals(
            Map<String, DoubleTimeSeries> doubleTimeSeries, List<Itinerary> itineraries, Airframes.Airframe airframe, String startTime
    ) {
        DoubleTimeSeries latTimeSeries = doubleTimeSeries.get(Parameters.LAT);
        DoubleTimeSeries lonTimeSeries = doubleTimeSeries.get(Parameters.LON);
        DoubleTimeSeries altTimeSeries = doubleTimeSeries.get(Parameters.ALT_AGL);
        DoubleTimeSeries altMSLTimeSeries = doubleTimeSeries.get(Parameters.ALT_MSL);
        DoubleTimeSeries rollTimeSeries = doubleTimeSeries.get(Parameters.ROLL);
        DoubleTimeSeries velocityTimeSeries = doubleTimeSeries.get(Parameters.GND_SPD);
        DoubleTimeSeries stallProbability = doubleTimeSeries.get(Parameters.STALL_PROBABILITY);
        DoubleTimeSeries locProbability = doubleTimeSeries.get(Parameters.LOSS_OF_CONTROL_PROBABILITY);

        if (Stream.of(latTimeSeries, lonTimeSeries, altTimeSeries, rollTimeSeries, velocityTimeSeries).anyMatch(Objects::isNull)) {
            return new ArrayList<>();
        }

        double[] lat = latTimeSeries.innerArray();
        double[] lon = lonTimeSeries.innerArray();
        double[] altitude = altTimeSeries.innerArray();
        // double[] roll = rollTimeSeries.innerArray();
        // double[] velocity = velocityTimeSeries.innerArray();
        assert lat.length == lon.length;

        ArrayList<TurnToFinal> ttfs = new ArrayList<>(itineraries.size());

        for (Itinerary it : itineraries) {
            int to = it.getMinAltitudeIndex();
            if (!it.wasApproach())
                continue;

            int from = to;

            Airport airport = Airports.getAirport(it.getAirport());
            Runway runway = airport.getRunway(it.getRunway());
            double runwayAltitude = altitude[to];

            for (; ; ) {
                if (to < 0) {
                    to = 0;
                    break;
                }

                if (altitude[to] > 15) // - runwayAltitude > 30)
                    break;

                to -= 1;
            }

            // Find the timestep at which the aircraft is 400ft above the runway's altitude
            for (; ; ) {
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

            TurnToFinal ttf = new TurnToFinal("",
                    airframe.getName(), runway, airport.iataCode, startTime, runwayAltitude,
                    altTimeSeries.sliceCopy(from, to),
                    altMSLTimeSeries.sliceCopy(from, to),
                    rollTimeSeries.sliceCopy(from, to),
                    latTimeSeries.sliceCopy(from, to),
                    lonTimeSeries.sliceCopy(from, to),
                    stallProbabilityArray, locProbabilityArray);
            ttfs.add(ttf);
        }

        return ttfs;
    }

    /**
     * Returns an array list of all of the turn to finals for the given flight that occur at the specified airport
     *
     * @param connection      database connection
     * @param flight          the flight for which the turn to finals should be analyzed
     * @param airportIataCode the IATA code for the airport. If this is null, all of the TTFs will be returned.
     * @return
     * @throws SQLException
     */
    public static ArrayList<TurnToFinal> getTurnToFinal(Connection connection, Flight flight, String airportIataCode) throws SQLException, IOException, ClassNotFoundException {
        ArrayList<TurnToFinal> turnToFinals = getTurnToFinalFromCache(connection, flight);

        if (turnToFinals == null)
            return new ArrayList<>();

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

    public record TurnToFinalJSON(ArrayList<Integer> locExceedences, ArrayList<Integer> centerLineExceedences,
                                  double selfDefinedGlideAngle,
                                  double[] latitude, double[] longitude, double[] AltMSL, double[] AltAGL,
                                  double[] distanceFromRunway,
                                  String flightId, Runway runway, String airportIataCode, String flightStartDate,
                                  double maxRoll, ArrayList<Double> selfDefinedGlidePathDeviations,
                                  double[] PLOCI, double[] PStall) {
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    public TurnToFinalJSON jsonify() {
        return new TurnToFinalJSON(
                locExceedences, centerLineExceedences, selfDefinedGlideAngle, latitude, longitude, altMSL, altitude, distanceFromRunway, flightId, runway, airportIataCode, flightStartDate, maxRoll, selfDefinedGlidePathDeviations, locProbability, stallProbability);
    }
}
