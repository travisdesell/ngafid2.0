package org.ngafid.core.airports;

import org.apache.commons.lang3.mutable.MutableDouble;

import java.util.Collection;
import java.util.HashMap;

public class Airport {
    public final String iataCode;
    public final String siteNumber;
    public final String type;

    public final double latitude;
    public final double longitude;

    public final String geoHash;

    private HashMap<String, Runway> runways;

    public Airport(String iataCode, String siteNumber, String type, double latitude, double longitude) {
        this.iataCode = iataCode;
        this.siteNumber = siteNumber;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;

        this.geoHash = GeoHash.getGeoHash(latitude, longitude);

        runways = new HashMap<>();
    }

    public String toString() {
        return "[AIRPORT " + iataCode + ", " + type + ", " + latitude + ", " + longitude + ", " + geoHash + "]";
    }

    public int getNumberRunways() {
        return runways.size();
    }

    public Collection<Runway> getRunways() {
        return runways.values();
    }

    public Runway getRunway(String name) {
        return runways.get(name);
    }

    public void addRunway(Runway runway) {
        runways.put(runway.getName(), runway);
    }

    public Runway getNearestRunwayWithin(double lat, double lon, double maxDistanceFt,
                                         MutableDouble runwayDistance) {
        Runway nearestRunway = null;

        double minDistance = maxDistanceFt;
        for (Runway runway : runways.values()) {
            if (!runway.hasCoordinates) continue;

            double distanceFt = runway.getDistanceFt(lat, lon);

            if (distanceFt < minDistance) {
                minDistance = distanceFt;
                nearestRunway = runway;
                runwayDistance.setValue(minDistance);
            }
        }

        return nearestRunway;
    }

    public boolean hasRunways() {
        return !runways.isEmpty();
    }

}
