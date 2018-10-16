package org.ngafid.flights;

import java.util.HashMap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Itinerary {
    private String airport;
    private String runway;
    private HashMap<String, Integer> runwayCounts = new HashMap<String, Integer>();

    int minAltitudeIndex = -1;
    double minAltitude = Double.MAX_VALUE;
    double minAirportDistance = Double.MAX_VALUE;
    double minRunwayDistance = Double.MAX_VALUE;

    public String getAirport() {
        return airport;
    }

    public String getRunway() {
        return runway;
    }

    public Itinerary(String airport, String runway, int index, double altitudeAGL, double airportDistance, double runwayDistance) {
        this.airport = airport;
        update(runway, index, altitudeAGL, airportDistance, runwayDistance);
    }

    public void update(String runway, int index, double altitudeAGL, double airportDistance, double runwayDistance) {
        if (!Double.isNaN(altitudeAGL)) {
            if (minAltitude > altitudeAGL) {
                minAltitude = altitudeAGL;
                minAltitudeIndex = index;
            }
        }

        if (!Double.isNaN(airportDistance)) {
            if (minAirportDistance > airportDistance) minAirportDistance = airportDistance;
        }

        if (!Double.isNaN(runwayDistance)) {
            if (minRunwayDistance > runwayDistance) minRunwayDistance = runwayDistance;
        }

        if (runway == null || runway.equals("")) return;

        Integer count = runwayCounts.get(runway);

        if (count == null) {
            runwayCounts.put(runway, 1);
        } else {
            runwayCounts.put(runway, count + 1);
        }
    }

    public void selectBestRunway() {
        runway = null;
        int maxCount = 0;
        System.err.println("Selecting runway:");
        System.err.println("min airport distance: " + minAirportDistance);
        System.err.println("min runway distance: " + minRunwayDistance);
        System.err.println("min altitude agl: " + minAltitude);

        for (String key : runwayCounts.keySet()) {
            int count = runwayCounts.get(key);
            System.err.println("\trunway: " + key + ", count: " + count);

            if (count > maxCount) {
                runway = key;
                maxCount = count;
            }
        }

        System.err.println("selected runway '" + runway + "' with count: " + maxCount);
    }

    public boolean wasApproach() {
        if (minRunwayDistance != Double.MAX_VALUE) return true;
        if (minAltitude <= 400) return true;

        return false;
    }

    public void updateDatabase(Connection connection, int flightId, int order) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO itinerary (flight_id, `order`, min_altitude_index, min_altitude, airport, runway) VALUES (?, ?, ?, ?, ?, ?)");
        preparedStatement.setInt(1, flightId);
        preparedStatement.setInt(2, order);
        preparedStatement.setInt(3, minAltitudeIndex);
        preparedStatement.setDouble(4, minAltitude);
        preparedStatement.setString(5, airport);
        preparedStatement.setString(6, runway);

        System.err.println(preparedStatement);

        preparedStatement.executeUpdate();

        preparedStatement.close();
    }

    public String toString() {
        return airport + "(" + runway + ") -- altitude: " + minAltitude + ", airport distance: " + minAirportDistance + ", runway distance: " + minRunwayDistance;
    }

}
