package org.ngafid.airports;

import java.io.BufferedReader;
import java.io.FileReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.ngafid.common.MutableDouble;


public class Airports {
    public final static double AVERAGE_RADIUS_OF_EARTH_KM = 6371;

    private static HashMap<String, ArrayList<Airport>> geoHashToAirport;
    private static HashMap<String, Airport> siteNumberToAirport;
    private static HashMap<String, Airport> iataToAirport;

    public static final String AIRPORTS_FILE;
    public static final String RUNWAYS_FILE;

    public static final double FT_PER_KM = 3280.84;

    static {
       //AIRPORTS_FILE = "/Users/fa3019/Data/airports/airports_parsed.csv";
        if (System.getenv("AIRPORTS_FILE") == null) {
            System.err.println("ERROR: 'AIRPORTS_FILE' environment variable not specified at runtime.");
            System.err.println("Please add the following to your ~/.bash_rc or ~/.profile file:");
            System.err.println("export AIRPORTS_FILE=<path_to_airports_file>");
            System.exit(1);
        }
        System.out.println("AIRPORTS_FILE: '" + System.getenv("AIRPORTS_FILE") + "'");

        AIRPORTS_FILE = System.getenv("AIRPORTS_FILE");
        
        //AIRPORTS_FILE = "/Users/fa3019/Data/airports/airports_parsed.csv";

        //RUNWAYS_FILE ="/Users/fa3019/Data/runways/runways_parsed.csv";
        if (System.getenv("RUNWAYS_FILE") == null) {
            System.err.println("ERROR: 'RUNWAYS_FILE' environment variable not specified at runtime.");
            System.err.println("Please add the following to your ~/.bash_rc or ~/.profile file:");
            System.err.println("export RUNWAYS_FILE=<path_to_runways_file>");
            System.exit(1);
        }

        RUNWAYS_FILE = System.getenv("RUNWAYS_FILE");
        //RUNWAYS_FILE ="/Users/fa3019/Data/runways/runways_parsed.csv";

        geoHashToAirport = new HashMap<String, ArrayList<Airport>>();
        siteNumberToAirport = new HashMap<String, Airport>();
        iataToAirport = new HashMap<String, Airport>();

        int maxHashSize = 0;
        int numberAirports = 0;

        try {
            BufferedReader br = new BufferedReader(new FileReader(AIRPORTS_FILE));

            String line;
            while ((line = br.readLine()) != null) {
                // process the line.

                String[] values = line.split(",");
                String iataCode = values[1];
                String siteNumber = values[2];
                String type = values[3];
                double latitude = Double.parseDouble(values[4]);
                double longitude = Double.parseDouble(values[5]);

                Airport airport = new Airport(iataCode, siteNumber, type, latitude, longitude);
                String geoHash = airport.geoHash;

                //System.err.println(airport);

                ArrayList<Airport> hashedAirports = geoHashToAirport.get(airport.geoHash);
                if (hashedAirports == null) {
                    hashedAirports = new ArrayList<Airport>();
                    geoHashToAirport.put(airport.geoHash, hashedAirports);
                }
                hashedAirports.add(airport);

                if (siteNumberToAirport.get(siteNumber) != null) {
                    System.err.println("ERROR: Airport " + airport + " already existed in siteNumberToAirport hash as " + siteNumberToAirport.get(siteNumber));
                    System.exit(1);

                }
                siteNumberToAirport.put(airport.siteNumber, airport);
                iataToAirport.put(airport.iataCode, airport);

                if (hashedAirports.size() > maxHashSize) maxHashSize = hashedAirports.size();
                //System.err.println("hashedAirports.size() now: " + hashedAirports.size() + ", max: " + maxHashSize);
                numberAirports++;
            }

            System.out.println("Creating buffered reader for '" + RUNWAYS_FILE + "'");
            //now read the runways file and add runways to airports
            br = new BufferedReader(new FileReader(RUNWAYS_FILE));
            System.out.println("buffered reader is ready? " + br.ready());

            while ((line = br.readLine()) != null) {
                //System.out.println("read runways line: " + line);

                String[] values = line.split(",");

                String id = values[0];
                String siteNumber = values[1];
                String name = values[2];

                Runway runway = null;
                if (values.length == 3) {
                    runway = new Runway(siteNumber, name);
                } else if (values.length == 7) {
                    double lat1 = Double.parseDouble(values[3]);
                    double lon1 = Double.parseDouble(values[4]);
                    double lat2 = Double.parseDouble(values[5]);
                    double lon2 = Double.parseDouble(values[6]);

                    runway = new Runway(siteNumber, name, lat1, lon1, lat2, lon2);
                } else {
                    System.err.println("ERROR: incorrect number of values in runways file: " + values.length);
                    System.err.println("line: '" + line + "'");
                    System.exit(1);
                }

                Airport airport = siteNumberToAirport.get(siteNumber);

                if (airport == null) {
                    System.err.println("ERROR: parsed runway for unknown airport, site number: " + siteNumber);
                    System.err.println("line: '" + line + "'");
                    System.exit(1);
                }

                airport.addRunway(runway);
                //System.out.println("Adding " + runway + " to " + airport);
             }
 
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        System.out.println("Read " + numberAirports + " airports.");
        System.out.println("airports HashMap size: " + geoHashToAirport.size());
        System.out.println("max airport ArrayList: " + maxHashSize);
    }

    /**
     * Grab a group of airports based on iataCodes
     * @param iataCodes the list of airport iata codes for which the airport object should be fetched
     * @return a map which maps iata code to airport object for the specified. It will only contain airports
     * specified in iataCodes
     */
    public static Map<String, Airport> getAirports(List<String> iataCodes) {
        return iataCodes.stream().collect(Collectors.toMap(Function.identity(), Airports::getAirport));
    }

    public static Airport getAirport(String iataCode) {
        return iataToAirport.get(iataCode);
    }

    /**
     *  Modified from:
     *  https://stackoverflow.com/questions/849211/shortest-distance-between-a-point-and-a-line-segment
     **/
    public final static double shortestDistanceBetweenLineAndPointFt(double plat, double plon,
                                                                     double lat1, double lon1,
                                                                     double lat2, double lon2) {
        double A = plon - lon1;
        double B = plat - lat1;
        double C = lon2 - lon1;
        double D = lat2 - lat1;

        double dot = A * C + B * D;
        double len_sq = C * C + D * D;
        double param = -1;

        if (len_sq != 0) {
            param = dot / len_sq;
        }

        double xx, yy;
        if (param < 0) {
            xx = lon1;
            yy = lat1;
        } else if (param > 1) {
            xx = lon2;
            yy = lat2;
        } else {
            xx = lon1 + param * C;
            yy = lat1 + param * D;
        }

        double dx = plon - xx;
        double dy = plat - yy;
        return Airports.calculateDistanceInFeet(plat, plon, plat + dy, plon + dx);
    }

    public final static double calculateDistanceInKilometer(double lat1, double lon1, double lat2, double lon2) {
        double latDistance = Math.toRadians(lat1 - lat2);
        double lngDistance = Math.toRadians(lon1 - lon2);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return AVERAGE_RADIUS_OF_EARTH_KM * c;
    }

    public final static double calculateDistanceInMeter(double lat1, double lon1, double lat2, double lon2) {
        return calculateDistanceInKilometer(lat1, lon1, lat2, lon2) * 1000.0;
    }

    public final static double calculateDistanceInFeet(double lat1, double lon1, double lat2, double lon2) {
        return calculateDistanceInKilometer(lat1, lon1, lat2, lon2) * Airports.FT_PER_KM;
    }


    public static Airport getNearestAirportWithin(double latitude, double longitude, double maxDistanceFt, MutableDouble airportDistance) {
        String geoHashes[] = GeoHash.getNearbyGeoHashes(latitude, longitude);

        double minDistance = maxDistanceFt;
        Airport nearestAirport = null;

        for (int i = 0; i < geoHashes.length; i++) {
            ArrayList<Airport> hashedAirports = geoHashToAirport.get(geoHashes[i]);

            if (hashedAirports != null) {
                // System.out.println("\t" + geoHashes[i] + " resulted in " + hashedAirports.size() + " airports.");
                for (int j = 0; j < hashedAirports.size(); j++) {
                    Airport airport = hashedAirports.get(j);
                    double distanceFt = calculateDistanceInFeet(latitude, longitude, airport.latitude, airport.longitude);
                    // System.out.println("\t\t" + airport + ", distanceFt: " + distanceFt);

                    if (distanceFt < minDistance) {
                        nearestAirport = airport;
                        minDistance = distanceFt;
                        airportDistance.set(minDistance);
                    }
                }
            }
        }

        /*
        if (nearestAirport != null) {
            System.out.println("nearest airport: " + nearestAirport + ", " + minDistance);
        } else {
            System.out.println("nearest airport: NULL");
        }
        */

        return nearestAirport;
    }

    public static boolean hasRunwayInfo(String iataCode) {
        System.out.println("checking to see if airport '" + iataCode + "' has runway info");
        return iataToAirport.get(iataCode).hasRunways();
    }

}
