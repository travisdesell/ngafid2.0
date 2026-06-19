/* 
For the purpose of this class, since Lat./Lon. plus altitude if available is all that we used for obstacles.
This will just have Lat and Lon, and it will have a similar calculation to that of the Airports.java
*/

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import org.ngafid.core.Config;
import org.ngafid.core.airports.Airports;
import org.ngafid.core.airports.GeoHash;

public final class Obstacles {
    private static final double FT_PER_KM = 3280.84;
    private static final Logger LOG = Logger.getLogger(Obstacle.class.getName());
    private static final HashMap<String, ArrayList<Obstacle>> GEO_HASH_TO_OBSTACLES;
    private static final HashMap<Int, Obstacle> OBJECTID_TO_OBSTACLES;
    
    private static final boolean TEST_MODE =
        Boolean.getBoolean("testMode") || "true".equalsIgnoreCase(System.getenv("TEST_MODE"));

    private Obstacles() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    static {
        GEO_HASH_TO_OBSTACLES = new HashMap<>();
        OBJECTID_TO_OBSTACLES = new HashMap<>();

        if (TEST_MODE) {
            LOG.info("TEST MODE: skipping reading airports and runways files");
            return;
        }

        LOG.info("Obstacle Class was ran");

        int maxHashSize = 0;
        int numberUniqueObstacles = 0;

        // Here is the code for the parsing of the Obstacles

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

                Obstacle obstacle = new Obstacle(id, lat, lon, type, agl, amsl, quantity);

                ArrayList<Obstacle> hashedObstacles = GEO_HASH_TO_OBSTACLES.computeIfAbsent(obstacle.getGeoHash(), k -> new ArrayList<>());
                hashedObstacles.add(obstacle);
                OBJECTID_TO_OBSTACLES.put(obstacle.getID(), obstacle);

                if (hashedObstacles.size() > maxHashSize) {maxHashSize = hashedObstacles.size();}
                numberUniqueObstacles++;

            }
        }

        LOG.info("Read "+ numberUniqueObstacles + " obstacles.");
        LOG.info("obstacles HashMap size: " + GEO_HASH_TO_OBSTACLES.size());
        LOG.info("max obstacle ArrayList: " + maxHashSize);
    }

    public static Obstacle getNearestObstacleWithin(
            double latitude, double longitude, double maxDistanceFt, MutableDouble obstacleDistance) {
        String[] geoHashes = GeoHash.getNearbyGeoHashes(latitude, longitude);

        double minDistance = maxDistanceFt;
        Obstacle nearestObstacle = null;
        
        for (String geoHash : geoHashes) {
            ArrayList<Obstacle> hashedObstacles = GEO_HASH_TO_OBSTACLES.get(geoHash);

            if (hashedAirports != null) {

                for (Obstacle obstacle : hashedObstacles) {
                    double distanceFt = calculateDistanceInFeet(latitude, longitude, obstacle.getLatitude(), obstacle.getLongitude());

                    if (distanceFt < minDistance) {
                        nearestObstacle = obstacle;
                        minDistance = distanceFt;
                        obstacleDistance.setValue(minDistance);
                    }
                }
            }
        }

        return nearestObstacle;
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

    public static boolean IsDoubleInRangeInclusive(double num, double bot, double top) {
        if ((num >= bot) && (num <= top)) {return true;}
        return false;
    }
}