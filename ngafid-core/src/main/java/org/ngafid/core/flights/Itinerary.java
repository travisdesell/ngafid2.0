package org.ngafid.core.flights;

import org.ngafid.core.airports.Airport;
import org.ngafid.core.airports.Airports;
import org.ngafid.core.airports.Runway;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class Itinerary {
    private static final Logger LOG = Logger.getLogger(DoubleTimeSeries.class.getName());
    private static final String GO_AROUND = "go_around";
    private static final String TOUCH_AND_GO = "touch_and_go";
    private static final String TAKEOFF = "takeoff";
    private static final String LANDING = "landing";
    private double minAirportDistance = Double.MAX_VALUE;
    private double minRunwayDistance = Double.MAX_VALUE;
    private int order = -1;
    private final String airport;
    private String runway;
    private final HashMap<String, Integer> runwayCounts = new HashMap<String, Integer>();
    private int minAltitudeIndex = -1;
    private double minAltitude = Double.MAX_VALUE;
    private int startOfApproach = -1;
    private int endOfApproach = -1;
    private int startOfTakeoff = -1;
    private int endOfTakeoff = -1;
    private int finalIndex;
    private int takeoffCounter = 0;
    private String type = GO_AROUND; // go_around is the default -> will be updated or set if otherwise

    public Itinerary(ResultSet resultSet) throws SQLException {
        order = resultSet.getInt(1);
        minAltitudeIndex = resultSet.getInt(2);
        minAltitude = resultSet.getDouble(3);
        airport = resultSet.getString(4);
        runway = resultSet.getString(5);
        minAirportDistance = resultSet.getDouble(6);
        minRunwayDistance = resultSet.getDouble(7);
        startOfApproach = resultSet.getInt(8);
        endOfApproach = resultSet.getInt(9);
        startOfTakeoff = resultSet.getInt(10);
        endOfTakeoff = resultSet.getInt(11);
        type = resultSet.getString(12);
    }

    public Itinerary(String airport, String runway, int index, double altitudeAGL, double airportDistance,
                     double runwayDistance, double groundSpeed, double rpm) {
        this.airport = airport;
        update(runway, index, altitudeAGL, airportDistance, runwayDistance, groundSpeed, rpm);
    }

    public Itinerary(String airport, String runway, int index, double altitudeAGL, double airportDistance,
                     double runwayDistance, double groundSpeed) {
        this.airport = airport;
        update(runway, index, altitudeAGL, airportDistance, runwayDistance, groundSpeed);
    }

    public Itinerary(int startTakeoff, int endTakeoff, int startApproach, int endApproach, String airport,
                     String runway) {
        this.startOfTakeoff = startTakeoff;
        this.endOfTakeoff = endTakeoff;
        this.startOfApproach = startApproach;
        this.endOfApproach = endApproach;
        this.airport = airport;
        this.runway = runway;
    }

    public static ArrayList<Itinerary> getItinerary(Connection connection, int flightId) throws SQLException {
        String queryString = "SELECT `order`, min_altitude_index, min_altitude, airport, runway, " +
                "min_airport_distance, min_runway_distance, start_of_approach, end_of_approach, " +
                "start_of_takeoff, end_of_takeoff, type FROM itinerary WHERE flight_id = ? ORDER BY `order`";
        try (PreparedStatement query = connection.prepareStatement(queryString)) {
            query.setInt(1, flightId);

            try (ResultSet resultSet = query.executeQuery()) {
                ArrayList<Itinerary> itinerary = new ArrayList<Itinerary>();
                while (resultSet.next()) {
                    itinerary.add(new Itinerary(resultSet));
                }

                return itinerary;
            }
        }
    }

    public static List<String> getAllAirports(Connection connection, int fleetId) throws SQLException {
        List<String> airports = new ArrayList<>();

        String queryString = "SELECT airport FROM visited_airports WHERE fleet_id = ? ORDER BY airport";
        try (PreparedStatement query = connection.prepareStatement(queryString)) {
            query.setInt(1, fleetId);

            LOG.info(query.toString());
            try (ResultSet resultSet = query.executeQuery()) {
                while (resultSet.next()) {
                    // airport existed in the database, return the id
                    String airport = resultSet.getString(1);
                    // This is a fix for bugs caused by an empty IATA airport code being in the database. Not sure how
                    // that got there exactly.
                    if (airport.isEmpty()) continue;
                    airports.add(airport);
                }
                LOG.info("airports.length: " + airports.size());
                return airports;
            }
        }
    }

    public static ArrayList<String> getAllAirportRunways(Connection connection, int fleetId) throws SQLException {
        String queryString = "SELECT runway FROM visited_runways WHERE fleet_id = ? ORDER BY runway";
        try (PreparedStatement query = connection.prepareStatement(queryString)) {
            query.setInt(1, fleetId);

            LOG.info(query.toString());
            try (ResultSet resultSet = query.executeQuery()) {
                ArrayList<String> runways = new ArrayList<String>();

                while (resultSet.next()) {
                    // airport existed in the database, return the id
                    String runway = resultSet.getString(1);
                    runways.add(runway);
                }

                LOG.info("runways.length: " + runways.size());
                return runways;
            }
        }
    }

    /**
     * Gets all runways that have coordinates (lat / long) available. It is returned in a map, where they key is the
     * airport iataCode and the values are the runways to that airport.
     *
     * @param connection database connection
     * @param fleetId    the fleetId for which we should be gathering airports from (we get all runways from all
     *                   airports in this fleet
     * @return a map of airport iataCodes to a list of runways that have coordinates
     * @throws SQLException
     */
    public static Map<String, List<Runway>> getAllRunwaysWithCoordinates(Connection connection, int fleetId)
            throws SQLException {
        List<String> airports = getAllAirports(connection, fleetId);
        Map<String, List<Runway>> runways = new HashMap<>(1024);

        for (String iataCode : airports) {
            Airport airport = Airports.getAirport(iataCode);
            List<Runway> rws = new ArrayList<>();

            if (airport == null) {
                LOG.info("Airport '" + iataCode + "' is null!");
                continue;
            }

            for (Runway rw : airport.getRunways())
                if (rw.hasCoordinates) rws.add(rw);
            runways.put(airport.iataCode, rws);
        }

        return runways;
    }

    public static PreparedStatement createAirportPreparedStatement(Connection connection) throws SQLException {
        return connection.prepareStatement("INSERT IGNORE INTO visited_airports SET fleet_id = ?, airport = ?");
    }

    public static PreparedStatement createRunwayPreparedStatement(Connection connection) throws SQLException {
        return connection.prepareStatement("INSERT IGNORE INTO visited_runways SET fleet_id = ?, runway = ?");
    }

    public static PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
        return connection.prepareStatement("INSERT INTO itinerary (flight_id, `order`, min_altitude_index, " +
                "min_altitude, min_airport_distance, min_runway_distance, airport, runway, start_of_approach, " +
                "end_of_approach, start_of_takeoff, end_of_takeoff, type) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
                + " ?)");
    }

    public String getAirport() {
        return airport;
    }

    public String getRunway() {
        return runway;
    }

    public void update(String rWay, int index, double altitudeAGL, double airportDistance, double runwayDistance,
                       double groundSpeed, double rpm) {
        // track finalIndex
        finalIndex = index;

        // track takeoff criteria
        if (rpm >= 2100 && groundSpeed > 14.5 && groundSpeed < 80) {
            // set start index in case of takeoff event
            if (startOfTakeoff == -1) {
                startOfTakeoff = index;
            } else if (takeoffCounter >= 15) { // if takeoff started and sustained for 15 seconds
                endOfTakeoff = index;
            }

            // increment counter to ensure criteria sustained for 15 seconds
            takeoffCounter++;
        } else {
            // reset counter
            takeoffCounter = 0;

            // reset takeoff start if criteria not sustained
            if (endOfTakeoff == -1) {
                startOfTakeoff = -1;
            }

        }

        if (!Double.isNaN(altitudeAGL)) {
            if (minAltitude > altitudeAGL) {
                minAltitude = altitudeAGL;
                minAltitudeIndex = index;
            }

            if (altitudeAGL <= 5) { // if grounded
                // end approach phase
                if (startOfApproach != -1) {
                    endOfApproach = index;
                }
            } else if (altitudeAGL > 6) {
                // log beginning of approach phase
                if (startOfApproach == -1) { // if first update & not initial takeoff
                    startOfApproach = index;
                }
            }
        }

        if (!Double.isNaN(airportDistance)) {
            if (minAirportDistance > airportDistance) minAirportDistance = airportDistance;
        }

        if (!Double.isNaN(runwayDistance)) {
            if (minRunwayDistance > runwayDistance) minRunwayDistance = runwayDistance;
        }

        if (rWay == null || rWay.isEmpty()) return;

        runwayCounts.merge(rWay, 1, Integer::sum);
    }

    public void update(String runwayUpdated, int index, double altitudeAGL, double airportDistance,
                       double runwayDistance, double groundSpeed) {
        // track finalIndex
        finalIndex = index;

        // track takeoff criteria
        if (groundSpeed > 14.5 && groundSpeed < 80) {
            // set start index in case of takeoff event
            if (startOfTakeoff == -1) {
                startOfTakeoff = index;
            } else if (takeoffCounter >= 15) { // if takeoff started and sustained for 15 seconds
                endOfTakeoff = index;
            }

            // increment counter to ensure criteria sustained for 15 seconds
            takeoffCounter++;
        } else {
            // reset counter
            takeoffCounter = 0;

            // reset takeoff start if criteria not sustained
            if (endOfTakeoff == -1) {
                startOfTakeoff = -1;
            }

        }

        if (!Double.isNaN(altitudeAGL)) {
            if (minAltitude > altitudeAGL) {
                minAltitude = altitudeAGL;
                minAltitudeIndex = index;
            }

            if (altitudeAGL <= 5) { // if grounded
                // end approach phase
                if (startOfApproach != -1) {
                    endOfApproach = index;
                }
            } else if (altitudeAGL > 6) {
                // log beginning of approach phase
                if (startOfApproach == -1) { // if first update & not initial takeoff
                    startOfApproach = index;
                }
            }
        }

        if (!Double.isNaN(airportDistance)) {
            if (minAirportDistance > airportDistance) minAirportDistance = airportDistance;
        }

        if (!Double.isNaN(runwayDistance)) {
            if (minRunwayDistance > runwayDistance) minRunwayDistance = runwayDistance;
        }

        if (runwayUpdated == null || runwayUpdated.equals("")) return;

        Integer count = runwayCounts.get(runwayUpdated);

        if (count == null) {
            runwayCounts.put(runwayUpdated, 1);
        } else {
            runwayCounts.put(runwayUpdated, count + 1);
        }
    }

    public void selectBestRunway() {
        runway = null;
        int maxCount = 0;

        for (String key : runwayCounts.keySet()) {
            int count = runwayCounts.get(key);

            if (count > maxCount) {
                runway = key;
                maxCount = count;
            }
        }
    }

    public boolean wasApproach() {
        if (minRunwayDistance != Double.MAX_VALUE) {
            return true;
        } else if (Airports.hasRunwayInfo(airport)) {
            // if it didn't get within 100 ft of a runway then
            // it wasn't an approach
            return false;
        } else {
            // this airport didn't have runway information so it
            // is most likely a very small airport, we can use
            // a metric of being within 1000 ft and below 200 ft
            //
            return minAirportDistance <= 1000 && minAltitude <= 200;
        }
    }

    public void addBatch(PreparedStatement itineraryStatement, PreparedStatement airportStatement,
                         PreparedStatement runwayStatement, int fleetId, int flightId, int orderToSet)
            throws SQLException {
        this.order = orderToSet;

        airportStatement.setInt(1, fleetId);
        airportStatement.setString(2, airport);
        airportStatement.addBatch();

        runwayStatement.setInt(1, fleetId);
        runwayStatement.setString(2, airport + " - " + runway);
        runwayStatement.addBatch();

        itineraryStatement.setInt(1, flightId);
        itineraryStatement.setInt(2, orderToSet);
        itineraryStatement.setInt(3, minAltitudeIndex);
        itineraryStatement.setDouble(4, minAltitude);
        itineraryStatement.setDouble(5, minAirportDistance);
        itineraryStatement.setDouble(6, minRunwayDistance);
        itineraryStatement.setString(7, airport);
        itineraryStatement.setString(8, runway);
        itineraryStatement.setInt(9, startOfApproach);
        itineraryStatement.setInt(10, endOfApproach);
        itineraryStatement.setInt(11, startOfTakeoff);
        itineraryStatement.setInt(12, endOfTakeoff);
        itineraryStatement.setString(13, type);
        itineraryStatement.addBatch();
    }

    public void updateDatabase(Connection connection, int fleetId, int flightId, int orderToAdd) throws SQLException {
        // insert new visited airports and runways -- will ignore if it already exists
        try (PreparedStatement statement = createPreparedStatement(connection); PreparedStatement airportStatement =
                createAirportPreparedStatement(connection); PreparedStatement runwayStatement =
                     createRunwayPreparedStatement(connection)) {

            this.addBatch(statement, airportStatement, runwayStatement, fleetId, flightId, orderToAdd);

            statement.executeBatch();
            airportStatement.executeBatch();
            runwayStatement.executeBatch();
        }
    }

    public String toString() {
        return airport + "(" + runway + ") -- altitude: " + minAltitude + ", airport distance: " +
                minAirportDistance + ", runway distance: " + minRunwayDistance;
    }

    // Simple setter for type variable (might want some defensive checks given use of strings)***
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Accessor for the minAltitudeIndex
     *
     * @return the minAltitudeIndex, as an int
     */
    public int getMinAltitudeIndex() {
        return this.minAltitudeIndex;
    }

    // method to determine if itinerary stop is a touch and go or go around
    public void determineType() {
        int approachTime = endOfApproach - startOfApproach;
        int runwayTime = startOfTakeoff - endOfApproach;

        if (startOfTakeoff != -1 && (endOfApproach == -1 || approachTime < 10)) {
            type = TAKEOFF;
        } else if (endOfTakeoff == -1 && endOfApproach != -1) {
            type = LANDING;
        } else if (runwayTime >= 5) {
            type = TOUCH_AND_GO;
        } else {
            type = GO_AROUND;
            endOfApproach = finalIndex;
        }
    }
}
