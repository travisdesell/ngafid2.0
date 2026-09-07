

package org.ngafid.core.obstacles;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.ngafid.core.Config;
import org.ngafid.core.Database;
import org.ngafid.core.airports.Airports;
import org.ngafid.core.airports.GeoHash;
import org.ngafid.core.obstacles.Obstacle.Lighting;

/**
 * Loads and contains all of the obstacles.
 */
public final class Obstacles {
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

    /**
     * Helper function that parse obstacles from the database
     * @param connection
     * @param geoHashMap
     * @param obstacleMap
     */
    private static void parseObstaclesFromDatabase(Connection connection, HashMap<String, ArrayList<Obstacle>> geoHashMap, HashMap<Integer, Obstacle> obstacleMap) {
        
        int maxHashSize = 0;

        String sql = """
                SELECT obstacles.id, latitude, longitude, agl_height, msl_height, obstacle_types.name, lighting_code, quantity FROM obstacles
                    INNER JOIN obstacle_types ON obstacles.type_id = obstacle_types.id;
                """;

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql);
                ResultSet resultSet = preparedStatement.executeQuery()) {
            LOG.info(preparedStatement.toString());

            while (resultSet.next()) {
                Obstacle obstacle = new Obstacle(resultSet);
                
                ArrayList<Obstacle> hashedObstacles = geoHashMap.computeIfAbsent(obstacle.getGeoHash(), k -> new ArrayList<>());
                hashedObstacles.add(obstacle);
                obstacleMap.put(obstacle.getID(), obstacle);

                if (hashedObstacles.size() > maxHashSize) {maxHashSize = hashedObstacles.size();}
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
        int totalCount = 0;
        int batchCount = 0;
        
        String sql = """
            INSERT INTO obstacles (id, latitude, longitude, agl_height, msl_height, type_id, lighting_code, quantity)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        LOG.info("Inserting obstacles into database... This will take about 40 minutes.");
        for (Obstacle obstacle : obstacleMap.values()) {

            obstacles.add(obstacle);
            totalCount++;
            batchCount++;
            
            // Split obstacles into batches of 20
            if (batchCount >= 20) {
                try {
                    batchDatabaseUpdate(connection, sql, obstacles, obstacleTypeMap);
                    obstacles.clear();
                    batchCount = 0;
                    if (totalCount % 100000 == 0) {LOG.info(totalCount + " obstacles have been inserted");}
                } catch (SQLException e) {
                    LOG.warning("Unable to insert batch of obstacles");
                    e.printStackTrace();
                }
            }
        }

        // Insert the rest of the obstacles
        try {
            batchDatabaseUpdate(connection, sql, obstacles, obstacleTypeMap);
            LOG.info((totalCount + obstacles.size()) + " total obstacles have been inserted into the database");
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

            preparedStatement.executeBatch();
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
    public static ArrayList<MarkedObstacle> getNearbyObstaclesWithinRange(double latitude, double longitude, double altitude, String aircraftType) {
        
        String[] geoHashes = GeoHash.getNearbyGeoHashes(latitude, longitude);

        ArrayList<MarkedObstacle> nearbyObstacles = new ArrayList<>();
        
        for (String geoHash : geoHashes) {
            ArrayList<Obstacle> hashedObstacles = GEO_HASH_TO_OBSTACLES.get(geoHash);

            if (hashedObstacles != null) {

                for (Obstacle obstacle : hashedObstacles) {

                    // Reusing the calculate distance formula as airports
                    double horizontalDistanceFt = Airports.calculateDistanceInFeet(latitude, longitude, obstacle.getLatitude(), obstacle.getLongitude());
                    
                    double verticalDistance = Math.abs(obstacle.getAGL() - altitude);
                    double distance = Math.sqrt(Math.pow(horizontalDistanceFt, 2) + Math.pow(verticalDistance, 2));
                    
                    if (aircraftType == "Fixed Wing") {
                        nearbyObstacles.add(new FixedWingMarkedObstacle(obstacle, distance, horizontalDistanceFt, verticalDistance));
                    }
                    else {
                        nearbyObstacles.add(new RotercraftMarkedObstacle(obstacle, distance, horizontalDistanceFt, verticalDistance));
                    }
                }
            }
        }
        return nearbyObstacles;
    }

    /**
     * Use for TESTING ONLY
     * @param geo
     * @param obstacles
     * @param obstacle_types
     */
    public static void InjectTestData(Map<String, ArrayList<Obstacle>> geo, Map<Integer, Obstacle> obstacles) {
         if (!TEST_MODE) {
            throw new IllegalStateException("Can only inject test data in test mode");
        }

        GEO_HASH_TO_OBSTACLES.clear();
        GEO_HASH_TO_OBSTACLES.putAll(geo);
        OBJECTID_TO_OBSTACLES.clear();
        OBJECTID_TO_OBSTACLES.putAll(obstacles);
    }
}