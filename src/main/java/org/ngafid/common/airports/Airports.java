package org.ngafid.common.airports;

import ch.randelshofer.fastdoubleparser.JavaDoubleParser;
import org.ngafid.common.MutableDouble;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class Airports {
    private static final double AVERAGE_RADIUS_OF_EARTH_KM = 6371;
    private static final String AIRPORTS_FILE;
    private static final String RUNWAYS_FILE;
    private static final double FT_PER_KM = 3280.84;
    private static final Logger LOG = Logger.getLogger(Airports.class.getName());
    private static final HashMap<String, ArrayList<Airport>> GEO_HASH_TO_AIRPORT;
    private static final HashMap<String, Airport> SITE_NUMBER_TO_AIRPORT;
    private static final HashMap<String, Airport> IATA_TO_AIRPORT;

    private Airports() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    static {
        // AIRPORTS_FILE = "/Users/fa3019/Data/airports/airports_parsed.csv";
        if (System.getenv("AIRPORTS_FILE") == null) {
            System.err.println("ERROR: 'AIRPORTS_FILE' environment variable not specified at runtime.");
            System.err.println("Please add the following to your ~/.bash_rc or ~/.profile file:");
            System.err.println("export AIRPORTS_FILE=<path_to_airports_file>");
            System.exit(1);
        }
        LOG.info("AIRPORTS_FILE: '" + System.getenv("AIRPORTS_FILE") + "'");

        AIRPORTS_FILE = System.getenv("AIRPORTS_FILE");

        // AIRPORTS_FILE = "/Users/fa3019/Data/airports/airports_parsed.csv";

        // RUNWAYS_FILE ="/Users/fa3019/Data/runways/runways_parsed.csv";
        if (System.getenv("RUNWAYS_FILE") == null) {
            System.err.println("ERROR: 'RUNWAYS_FILE' environment variable not specified at runtime.");
            System.err.println("Please add the following to your ~/.bash_rc or ~/.profile file:");
            System.err.println("export RUNWAYS_FILE=<path_to_runways_file>");
            System.exit(1);
        }

        RUNWAYS_FILE = System.getenv("RUNWAYS_FILE");
        // RUNWAYS_FILE ="/Users/fa3019/Data/runways/runways_parsed.csv";

        GEO_HASH_TO_AIRPORT = new HashMap<String, ArrayList<Airport>>();
        SITE_NUMBER_TO_AIRPORT = new HashMap<String, Airport>();
        IATA_TO_AIRPORT = new HashMap<String, Airport>();

        int maxHashSize = 0;
        int numberAirports = 0;

        try (BufferedReader airportsReader = new BufferedReader(new FileReader(AIRPORTS_FILE));
             BufferedReader runwaysReader = new BufferedReader(new FileReader(RUNWAYS_FILE))) {

            String line;
            while ((line = airportsReader.readLine()) != null) {
                // process the line.

                String[] values = line.split(",");
                String iataCode = values[1];
                String siteNumber = values[2];
                String type = values[3];
                double latitude = JavaDoubleParser.parseDouble(values[4]);
                double longitude = JavaDoubleParser.parseDouble(values[5]);

                Airport airport = new Airport(iataCode, siteNumber, type, latitude, longitude);

                ArrayList<Airport> hashedAirports = GEO_HASH_TO_AIRPORT.computeIfAbsent(airport.getGeoHash(),
                        k -> new ArrayList<>());
                hashedAirports.add(airport);

                if (SITE_NUMBER_TO_AIRPORT.get(siteNumber) != null) {
                    System.err.println("ERROR: Airport " + airport + " already existed in siteNumberToAirport hash as"
                            + " " + SITE_NUMBER_TO_AIRPORT.get(siteNumber));
                    System.exit(1);

                }
                SITE_NUMBER_TO_AIRPORT.put(airport.getSiteNumber(), airport);
                IATA_TO_AIRPORT.put(airport.getIataCode(), airport);

                if (hashedAirports.size() > maxHashSize) maxHashSize = hashedAirports.size();
                // System.err.println("hashedAirports.size() now: " + hashedAirports.size() + ", max: " + maxHashSize);
                numberAirports++;
            }

            while ((line = runwaysReader.readLine()) != null) {
                // LOG.info("read runways line: " + line);

                String[] values = line.split(",");

                String siteNumber = values[1];
                String name = values[2];

                Runway runway = null;
                if (values.length == 3) {
                    runway = new Runway(siteNumber, name);
                } else if (values.length == 7) {
                    double lat1 = JavaDoubleParser.parseDouble(values[3]);
                    double lon1 = JavaDoubleParser.parseDouble(values[4]);
                    double lat2 = JavaDoubleParser.parseDouble(values[5]);
                    double lon2 = JavaDoubleParser.parseDouble(values[6]);

                    runway = new Runway(siteNumber, name, lat1, lon1, lat2, lon2);
                } else {
                    System.err.println("ERROR: incorrect number of values in runways file: " + values.length);
                    System.err.println("line: '" + line + "'");
                    System.exit(1);
                }

                Airport airport = SITE_NUMBER_TO_AIRPORT.get(siteNumber);

                if (airport == null) {
                    System.err.println("ERROR: parsed runway for unknown airport, site number: " + siteNumber);
                    System.err.println("line: '" + line + "'");
                    System.exit(1);
                }

                airport.addRunway(runway);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        LOG.info("Read " + numberAirports + " airports.");
        LOG.info("airports HashMap size: " + GEO_HASH_TO_AIRPORT.size());
        LOG.info("max airport ArrayList: " + maxHashSize);
    }

    /**
     * Grab a group of airports based on iataCodes
     *
     * @param iataCodes the list of airport iata codes for which the airport object should be fetched
     * @return a map which maps iata code to airport object for the specified. It will only contain airports
     * specified in iataCodes
     */
    public static Map<String, Airport> getAirports(List<String> iataCodes) {
        return iataCodes.stream().collect(Collectors.toMap(Function.identity(), Airports::getAirport));
    }

    public static Airport getAirport(String iataCode) {
        return IATA_TO_AIRPORT.get(iataCode);
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
    public static double shortestDistanceBetweenLineAndPointFt(double plat, double plon, double lat1,
                                                               double lon1, double lat2, double lon2) {
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

        double a =
                Math.sin(latDistance / 2) * Math.sin(latDistance / 2) + Math.cos(Math.toRadians(lat1)) *
                        Math.cos(Math.toRadians(lat2)) * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return AVERAGE_RADIUS_OF_EARTH_KM * c;
    }

    public static double calculateDistanceInMeter(double lat1, double lon1, double lat2, double lon2) {
        return calculateDistanceInKilometer(lat1, lon1, lat2, lon2) * 1000.0;
    }

    public static double calculateDistanceInFeet(double lat1, double lon1, double lat2, double lon2) {
        return calculateDistanceInKilometer(lat1, lon1, lat2, lon2) * Airports.FT_PER_KM;
    }

    public static Airport getNearestAirportWithin(double latitude, double longitude, double maxDistanceFt,
                                                  MutableDouble airportDistance) {
        String[] geoHashes = GeoHash.getNearbyGeoHashes(latitude, longitude);

        double minDistance = maxDistanceFt;
        Airport nearestAirport = null;

        for (String geoHash : geoHashes) {
            ArrayList<Airport> hashedAirports = GEO_HASH_TO_AIRPORT.get(geoHash);

            if (hashedAirports != null) {
                // LOG.info("\t" + geoHashes[i] + " resulted in " + hashedAirports.size() + " airports.");
                for (Airport airport : hashedAirports) {
                    double distanceFt = calculateDistanceInFeet(latitude, longitude, airport.getLatitude(),
                            airport.getLongitude());
                    // LOG.info("\t\t" + airport + ", distanceFt: " + distanceFt);

                    if (distanceFt < minDistance) {
                        nearestAirport = airport;
                        minDistance = distanceFt;
                        airportDistance.setValue(minDistance);
                    }
                }
            }
        }

        /*
         * if (nearestAirport != null) {
         * LOG.info("nearest airport: " + nearestAirport + ", " + minDistance);
         * } else {
         * LOG.info("nearest airport: NULL");
         * }
         */

        return nearestAirport;
    }

    public static boolean hasRunwayInfo(String iataCode) {
        System.out.println("checking to see if airport '" + iataCode + "' has runway info");
        Airport ap = IATA_TO_AIRPORT.get(iataCode);
        return ap != null && ap.hasRunways();
    }

}
