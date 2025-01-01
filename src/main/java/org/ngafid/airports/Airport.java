package org.ngafid.airports;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import org.ngafid.common.MutableDouble;

import java.util.Collection;
import java.util.HashMap;

public class Airport {
    private final String iataCode;
    private final String siteNumber;
    private final String type;

    private final double latitude;
    private final double longitude;

    private final String geoHash;

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
            if (!runway.isHasCoordinates()) continue;

            double distanceFt = runway.getDistanceFt(lat, lon);
            //System.err.println("distance between " + lat + ", " + lon + " and " + iataCode + " runway "
            // + runway.name + ": " + distanceFt + ", minDistance: " + minDistance);

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

    public JsonElement jsonify(Gson gson) {
        HashMap<String, Runway> runwaysMap = this.runways;
        this.runways = null;
        JsonElement je = gson.toJsonTree(this);
        this.runways = runwaysMap;
        return je;
    }

    public String getIataCode() {
        return iataCode;
    }

    public String getGeoHash() {
        return geoHash;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getSiteNumber() {
        return siteNumber;
    }
}
