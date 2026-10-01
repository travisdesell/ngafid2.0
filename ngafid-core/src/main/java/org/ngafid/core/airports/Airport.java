package org.ngafid.core.airports;

import java.util.Collection;
import java.util.HashMap;
import org.apache.commons.lang3.mutable.MutableDouble;

public class Airport {
    private final String iataCode;
    private final String siteNumber;
    private final String type;

    private final double latitude;
    private final double longitude;

    private final String geoHash;

    private final HashMap<String, Runway> runways;

    /**
     * Constructs an airport, computing and caching its geohash from the given coordinates and starting with an empty
     * runway set (runways are added later via {@link #addRunway}).
     *
     * @param iataCode the airport's IATA code
     * @param siteNumber the FAA site number identifying the airport
     * @param type the airport type (e.g. airport, heliport)
     * @param latitude the airport latitude, in degrees
     * @param longitude the airport longitude, in degrees
     */
    public Airport(String iataCode, String siteNumber, String type, double latitude, double longitude) {
        this.iataCode = iataCode;
        this.siteNumber = siteNumber;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;

        this.geoHash = GeoHash.getGeoHash(latitude, longitude);

        runways = new HashMap<>();
    }

    /**
     * Returns a human-readable summary of the airport: its IATA code, type, coordinates, and geohash.
     *
     * @return a debug string describing this airport
     */
    @Override
    public String toString() {
        return "[AIRPORT " + iataCode + ", " + type + ", " + latitude + ", " + longitude + ", " + geoHash + "]";
    }

    public int getNumberRunways() {
        return runways.size();
    }

    public Collection<Runway> getRunways() {
        return runways.values();
    }

    /**
     * Looks up a runway by name.
     *
     * @param name the runway name
     * @return the matching runway, or {@code null} if this airport has no runway with that name
     */
    public Runway getRunway(String name) {
        return runways.get(name);
    }

    /**
     * Adds a runway to this airport, keyed by the runway's name (replacing any existing runway with the same name).
     *
     * @param runway the runway to add
     */
    public void addRunway(Runway runway) {
        runways.put(runway.getName(), runway);
    }

    /**
     * Finds the runway nearest to a point, considering only runways with known coordinates that lie within
     * {@code maxDistanceFt}. When one is found, its distance from the point is written into {@code runwayDistance}.
     *
     * @param lat the query latitude, in degrees
     * @param lon the query longitude, in degrees
     * @param maxDistanceFt the maximum distance, in feet, a runway may be to qualify
     * @param runwayDistance an out-parameter set to the nearest runway's distance in feet (left unchanged
     *     if none is found)
     * @return the nearest qualifying runway, or {@code null} if none is within range
     */
    public Runway getNearestRunwayWithin(double lat, double lon, double maxDistanceFt, MutableDouble runwayDistance) {
        Runway nearestRunway = null;

        double minDistance = maxDistanceFt;
        for (Runway runway : runways.values()) {
            if (!runway.hasCoordinates()) {
                continue;
            }

            double distanceFt = runway.getDistanceFt(lat, lon);

            if (distanceFt < minDistance) {
                minDistance = distanceFt;
                nearestRunway = runway;
                runwayDistance.setValue(minDistance);
            }
        }

        return nearestRunway;
    }

    /**
     * Reports whether this airport has any runways recorded.
     *
     * @return true if at least one runway has been added
     */
    public boolean hasRunways() {
        return !runways.isEmpty();
    }

    public String getIataCode() {
        return iataCode;
    }

    public String getSiteNumber() {
        return siteNumber;
    }

    public String getType() {
        return type;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getGeoHash() {
        return geoHash;
    }
}
