

package org.ngafid.core.obstacles;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.management.RuntimeErrorException;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.ngafid.core.Config;
import org.ngafid.core.Database;
import org.ngafid.core.airports.Airports;
import org.ngafid.core.airports.GeoHash;
import org.ngafid.core.event.EventDefinition;
import org.ngafid.core.obstacles.Obstacle.Lighting;

public final class Obstacles {
    private static final double AVERAGE_RADIUS_OF_EARTH_KM = 6371;
    private static final double FT_PER_KM = 3280.84;
    private static final Logger LOG = Logger.getLogger(Obstacle.class.getName());
    private static final HashMap<String, ArrayList<Obstacle>> GEO_HASH_TO_OBSTACLES;
    private static final HashMap<Integer, Obstacle> OBJECTID_TO_OBSTACLES;
    private static HashMap<String, Integer> OBSTACLE_TYPE_MAP;
    
    private static final boolean TEST_MODE =
        Boolean.getBoolean("testMode") || "true".equalsIgnoreCase(System.getenv("TEST_MODE"));

    private Obstacles() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    static {
        GEO_HASH_TO_OBSTACLES = new HashMap<>();
        OBJECTID_TO_OBSTACLES = new HashMap<>();
        
        if (TEST_MODE) {
            LOG.info("TEST MODE: skipping reading obstacles files");
        } else {
            LOG.info("Obstacle Class was ran");

            try (Connection connection = Database.getConnection();) {
                
                // Check if obstacles exist in the database. If not, begin parsing
                if (VerifyObstaclesInDatabase(connection) == false) {

                    // Parse out the obstacles from the csv file in the Ostacles class
                    ParseObstacles(GEO_HASH_TO_OBSTACLES, OBJECTID_TO_OBSTACLES);

                    // Insert the obstacles into the database
                    ObstacleInsertion(connection, OBJECTID_TO_OBSTACLES);
                }
                else {LOG.info("Obstacles tables are filled. Parsing is skipped.");}
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * Helper function that parses the obstacles from the obstacle csv file
     * @param geoHash
     * @param obstacleIDMap
     * @throws Exception
     */
    private static void ParseObstacles(HashMap<String, ArrayList<Obstacle>> geoHash, HashMap<Integer, Obstacle> obstacleIDMap) throws Exception  {

        int maxHashSize = 0;
        int numberUniqueObstacles = 0;

        try (BufferedReader obstaclesReader = new BufferedReader(new FileReader(Config.OBSTACLES_FILE));) {
            String line;

            while ((line = obstaclesReader.readLine()) != null) {

                String[] values = line.split(",");
                int id = Integer.parseInt(values[2]);
                Double lat = Double.parseDouble(values[10]);
                Double lon = Double.parseDouble(values[11]);
                int agl = Integer.parseInt(values[14]);
                int amsl = Integer.parseInt(values[15]);
                String type = values[12];
                int quantity = Integer.parseInt(values[13]);
                Lighting lighting = Lighting.valueOf(values[16]);

                Obstacle obstacle = new Obstacle(id, lat, lon, type, agl, amsl, quantity, lighting);

                ArrayList<Obstacle> hashedObstacles = geoHash.computeIfAbsent(obstacle.getGeoHash(), k -> new ArrayList<>());
                hashedObstacles.add(obstacle);
                obstacleIDMap.put(obstacle.getID(), obstacle);

                if (hashedObstacles.size() > maxHashSize) {maxHashSize = hashedObstacles.size();}
                numberUniqueObstacles++;
            }
        }

        LOG.info("Read "+ numberUniqueObstacles + " obstacles.");
        LOG.info("obstacles HashMap size: " + geoHash.size());
        LOG.info("max obstacle ArrayList: " + maxHashSize);
    }

    /**
     * Helper function that inserts obstacles into the database
     * @param connection
     */
    private static void ObstacleInsertion(Connection connection, HashMap<Integer, Obstacle> obstacleMap) {

        String sql = """
            INSERT INTO obstacles (id, agl_height, msl_height, type_id, lighting_code)
                VALUES (?, ?, ?, ?, ?)
        """;

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            OBSTACLE_TYPE_MAP = getObstacleTypes(connection);
            for (Obstacle obstacle : obstacleMap.values()) {

                if (!OBSTACLE_TYPE_MAP.containsKey(obstacle.getType())) {
                    throw new RuntimeException("Unknown obstacle type of " + obstacle.getType() + ". Try adding it to the obstacle type table.");
                }

                preparedStatement.setInt(1, obstacle.getID());
                preparedStatement.setInt(2, obstacle.getAGL());
                preparedStatement.setInt(3, obstacle.getAMSL());
                preparedStatement.setInt(4, OBSTACLE_TYPE_MAP.get(obstacle.getType()));
                preparedStatement.setString(5, obstacle.getLighting().toString());
                preparedStatement.addBatch();
            }

            preparedStatement.executeBatch();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Helper function that fetches all of the obstacles types from the database
     * @param connection
     * @return A Hashmap of obstacle types and id as key-pair values 
     * @throws SQLException
     */
    private static HashMap<String, Integer> getObstacleTypes(Connection connection) throws SQLException {
        String query = "SELECT id, name FROM obstacle_types;";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query);
                ResultSet resultSet = preparedStatement.executeQuery()) {
            LOG.info(preparedStatement.toString());
            
            HashMap<String, Integer> obstacleTypeToID = new HashMap<>();

            while (resultSet.next()) {
                Integer id = resultSet.getInt(1);
                String typeName = resultSet.getString(2);
                obstacleTypeToID.put(typeName, id);
            }

            return obstacleTypeToID;
        }
    }

    /**
     * Helper function that checks if the obstacle table is empty in the database
     * @param connection
     * @return True if there are obstacles, False if it is empty
     * @throws SQLException 
     */
    private static Boolean VerifyObstaclesInDatabase(Connection connection) throws SQLException {
        String query = "SELECT COUNT(DISTINCT id) FROM obstacles";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query);
                ResultSet resultSet = preparedStatement.executeQuery()) {

                LOG.info(preparedStatement.toString());
                resultSet.next();
                if (resultSet.getInt(1) > 0) {return true;}
            }

            return false;
    }
    

    /**
     * Checks for nearby obstacle that is within a given distance
     * @param latitude
     * @param longitude
     * @param altitude
     * @param maxDistanceFt
     * @return
     */
    public static ArrayList<MarkedObstacle> getNearbyObstaclesWithinRange(double latitude, double longitude, double altitude, double maxDistanceFt) {
        String[] geoHashes = GeoHash.getNearbyGeoHashes(latitude, longitude);

        ArrayList<MarkedObstacle> nearbyObstacles = new ArrayList<>();
        
        for (String geoHash : geoHashes) {
            ArrayList<Obstacle> hashedObstacles = GEO_HASH_TO_OBSTACLES.get(geoHash);

            if (hashedObstacles != null) {

                for (Obstacle obstacle : hashedObstacles) {

                    double horizontalDistanceFt = calculateDistanceInFeet(latitude, longitude, obstacle.getLatitude(), obstacle.getLongitude());
                    double verticalDistance = Math.abs(obstacle.getAGL() - altitude);

                    double distance = Math.sqrt(Math.pow(horizontalDistanceFt, 2) + Math.pow(verticalDistance, 2));
                    
                    if (distance <= maxDistanceFt) {
                        nearbyObstacles.add(new MarkedObstacle(obstacle, distance, horizontalDistanceFt, verticalDistance));
                    }
                }
            }
        }
        return nearbyObstacles;
    }
    
    /**
     * Calculate the shortest distance between a point and a line segment
     * Modified from:
     * https://stackoverflow.com/questions/849211/shortest-distance-between-a-point-and-a-line-segment
     *
     * @param plat Point latitude
     * @param plon Point longitude
     * @param lat1 Latitude of the first point of the line segment
     * @param lon1 Longitude of the first point of the line segment
     * @param lat2 Latitude of the second point of the line segment
     * @param lon2 Longitude of the second point of the line segment
     * @return the shortest distance between the point and the line segment
     */
    public static double shortestDistanceBetweenLineAndPointFt(
            double plat, double plon, double lat1, double lon1, double lat2, double lon2) {
        double a = plon - lon1;
        double b = plat - lat1;
        double c = lon2 - lon1;
        double d = lat2 - lat1;

        double dot = a * c + b * d;
        double lenSq = c * c + d * d;
        double param = -1;

        if (lenSq != 0) {
            param = dot / lenSq;
        }

        double xx;
        double yy;
        if (param < 0) {
            xx = lon1;
            yy = lat1;
        } else if (param > 1) {
            xx = lon2;
            yy = lat2;
        } else {
            xx = lon1 + param * c;
            yy = lat1 + param * d;
        }

        double dx = plon - xx;
        double dy = plat - yy;
        return Airports.calculateDistanceInFeet(plat, plon, plat + dy, plon + dx);
    }

    public static double calculateDistanceInKilometer(double lat1, double lon1, double lat2, double lon2) {
        double latDistance = Math.toRadians(lat1 - lat2);
        double lngDistance = Math.toRadians(lon1 - lon2);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1))
                        * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lngDistance / 2)
                        * Math.sin(lngDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return AVERAGE_RADIUS_OF_EARTH_KM * c;
    }

    public static double calculateDistanceInMeter(double lat1, double lon1, double lat2, double lon2) {
        return calculateDistanceInKilometer(lat1, lon1, lat2, lon2) * 1000.0;
    }

    public static double calculateDistanceInFeet(double lat1, double lon1, double lat2, double lon2) {
        return calculateDistanceInKilometer(lat1, lon1, lat2, lon2) * Obstacles.FT_PER_KM;
    }
}