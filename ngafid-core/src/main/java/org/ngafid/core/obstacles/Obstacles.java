

package org.ngafid.core.obstacles;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import org.ngafid.core.Config;
import org.ngafid.core.Database;
import org.ngafid.core.airports.Airports;
import org.ngafid.core.airports.GeoHash;
import org.ngafid.core.obstacles.Obstacle.Lighting;

public final class Obstacles {
    private static final double AVERAGE_RADIUS_OF_EARTH_KM = 6371;
    private static final double FT_PER_KM = 3280.84;
    private static final Logger LOG = Logger.getLogger(Obstacle.class.getName());
    private static final HashMap<String, ArrayList<Obstacle>> GEO_HASH_TO_OBSTACLES;
    private static final HashMap<Integer, Obstacle> OBJECTID_TO_OBSTACLES;
    private static final HashMap<String, Integer> OBSTACLE_TYPE_MAP;
    
    private static final boolean TEST_MODE =
        Boolean.getBoolean("testMode") || "true".equalsIgnoreCase(System.getenv("TEST_MODE"));

    private Obstacles() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    static {
        GEO_HASH_TO_OBSTACLES = new HashMap<>();
        OBJECTID_TO_OBSTACLES = new HashMap<>();
        OBSTACLE_TYPE_MAP = new HashMap<>();
        
        if (TEST_MODE) {
            LOG.info("TEST MODE: skipping reading obstacles files");
        } else {

            try (Connection connection = Database.getConnection();) {
                
                parseObstaclesFromCSV(GEO_HASH_TO_OBSTACLES, OBJECTID_TO_OBSTACLES);

                getObstacleTypes(connection, OBSTACLE_TYPE_MAP);

                // Check if obstacles exist in the database. If not, begin parsing
                // if (verifyObstaclesInDatabase(connection) == false) {

                //     LOG.info("No obstacles found in Database. Begin parsing.");

                //     // Parse out the obstacles from the csv file in the Ostacles class
                //     parseObstaclesFromCSV(GEO_HASH_TO_OBSTACLES, OBJECTID_TO_OBSTACLES);

                //     getObstacleTypes(connection, OBSTACLE_TYPE_MAP);

                //     // Insert the obstacles into the database
                //     obstacleInsertion(connection, OBJECTID_TO_OBSTACLES, OBSTACLE_TYPE_MAP);
                // }
                // else {
                //     LOG.info("Obstacles tables are filled. Reading from the database.");

                //     // Parse obstacles from Database
                //     parseObstaclesFromDatabase(connection, GEO_HASH_TO_OBSTACLES, OBJECTID_TO_OBSTACLES);

                //     LOG.info("A total of " + OBJECTID_TO_OBSTACLES.size() + " obstacles have been read from the database");
                //     LOG.info("GeoHash Size: " + GEO_HASH_TO_OBSTACLES.size());

                //     getObstacleTypes(connection, OBSTACLE_TYPE_MAP);
                // }


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
    private static void parseObstaclesFromCSV(HashMap<String, ArrayList<Obstacle>> geoHash, HashMap<Integer, Obstacle> obstacleIDMap) throws Exception  {

        int maxHashSize = 0;
        int numberUniqueObstacles = 0;

        try (BufferedReader obstaclesReader = new BufferedReader(new FileReader(Config.OBSTACLES_FILE));) {
            String line;

            while ((line = obstaclesReader.readLine()) != null) {

                String[] values = line.split(",");
                int id = Integer.parseInt(values[2]);
                double lat = Double.parseDouble(values[10]);
                double lon = Double.parseDouble(values[11]);
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

    private static void parseObstaclesFromDatabase(Connection connection, HashMap<String, ArrayList<Obstacle>> geoHashMap, HashMap<Integer, Obstacle> obstacleMap) {
        
        String sql = """
                SELECT obstacles.id, latitude, longitude, agl_height, msl_height, obstacle_types.name, lighting_code, quantity FROM obstacles
                    INNER JOIN obstacle_types ON obstacles.type_id = obstacle_types.id;
                """;

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql);
                ResultSet resultSet = preparedStatement.executeQuery()) {
            LOG.info(preparedStatement.toString());

            while (resultSet.next()) {
                Obstacle obstacle = new Obstacle(resultSet);
                obstacleMap.put(obstacle.getID(), obstacle);
                geoHashMap.computeIfAbsent(obstacle.getGeoHash(), k -> new ArrayList<>());
            }
        } catch (SQLException e) {
            LOG.warning("Unable to parse obstacles from database.");
            e.printStackTrace();
        }
    }

    /**
     * Helper function that inserts obstacles into the database
     * @param connection
     */
    private static void obstacleInsertion(Connection connection, HashMap<Integer, Obstacle> obstacleMap, HashMap<String, Integer> obstacleTypeMap) {

        ArrayList<Obstacle> obstacles = new ArrayList<>();
        int count = 0;
        
        String sql = """
            INSERT INTO obstacles (id, latitude, longitude, agl_height, msl_height, type_id, lighting_code, quantity)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        
        for (Obstacle obstacle : obstacleMap.values()) {

            obstacles.add(obstacle);
            count++;
            
            // Split obstacles into batches of 10
            if (count >= 20) {
                try {
                    batchDatabaseUpdate(connection, sql, obstacles, obstacleTypeMap);
                    obstacles.clear();
                    count = 0;
                } catch (SQLException e) {
                    LOG.warning("Unable to insert batch of obstacles");
                    e.printStackTrace();
                }
            }
        }

        // Insert the rest of the obstacles
        try {
            batchDatabaseUpdate(connection, sql, obstacles, obstacleTypeMap);
            obstacles.clear();
        } catch (SQLException e) {
            LOG.warning("Unable to insert last batch of obstacles");
            e.printStackTrace();
        }

        
    }

    /**
     * Helper function to insert obstacles into database as batches
     * @param connection
     * @param sql
     * @param obstacles
     * @throws SQLException
     */
    private static void batchDatabaseUpdate(Connection connection, String sql, ArrayList<Obstacle> obstacles, HashMap<String, Integer> obstacleTypeMap) throws SQLException {

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            ArrayList<Integer> obstacleIDs = new ArrayList<>();

            for (Obstacle obstacle : obstacles) {

                if (!obstacleTypeMap.containsKey(obstacle.getType())) {
                    throw new RuntimeException("Unknown obstacle type of " + obstacle.getType() + ". Try adding it to the obstacle type table.");
                }

                obstacleIDs.add(obstacle.getID());
                
                preparedStatement.setInt(1, obstacle.getID());
                preparedStatement.setDouble(2, obstacle.getLatitude());
                preparedStatement.setDouble(3, obstacle.getLongitude());
                preparedStatement.setInt(4, obstacle.getAGL());
                preparedStatement.setInt(5, obstacle.getAMSL());
                preparedStatement.setInt(6, OBSTACLE_TYPE_MAP.get(obstacle.getType()));
                preparedStatement.setString(7, obstacle.getLighting().toString());
                preparedStatement.setInt(8, obstacle.getQuantity());
                preparedStatement.addBatch();
            }

            String ids = "";
            for (Integer id : obstacleIDs) {
                ids = ids + ", " + id;
            }

            preparedStatement.executeBatch();
            LOG.info("Executed batch obstacles insertion for obstacles of :" + ids);

        }
    }


    /**
     * Helper function that fetches all of the obstacles types from the database and stores it as a Hashmap of obstacle types and id as key-pair values 
     * @param connection
     * @return 
     * @throws SQLException
     */
    private static void getObstacleTypes(Connection connection, HashMap<String, Integer> obstacleTypeMap) throws SQLException {
        String query = "SELECT id, name FROM obstacle_types;";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query);
                ResultSet resultSet = preparedStatement.executeQuery()) {
            LOG.info(preparedStatement.toString());

            while (resultSet.next()) {
                Integer id = resultSet.getInt(1);
                String typeName = resultSet.getString(2);
                obstacleTypeMap.put(typeName, id);
            }
        }
    }

    /**
     * Helper function that checks if the obstacle table is empty in the database
     * @param connection
     * @return True if there are obstacles, False if it is empty
     * @throws SQLException 
     */
    private static Boolean verifyObstaclesInDatabase(Connection connection) throws SQLException {
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