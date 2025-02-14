package org.ngafid.airports;

import java.util.Collection;
import java.util.HashMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.annotations.Expose;
import org.ngafid.common.MutableDouble;

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
        runways.put(runway.name, runway);
    }

    public Runway getNearestRunwayWithin(double latitude, double longitude, double maxDistanceFt, MutableDouble runwayDistance) {
        Runway nearestRunway = null;

        double minDistance = maxDistanceFt;
        for (Runway runway : runways.values()) {
            if (!runway.hasCoordinates) continue;

            double distanceFt = runway.getDistanceFt(latitude, longitude);
            //System.err.println("distance between " + latitude + ", " + longitude + " and " + iataCode + " runway " + runway.name + ": " + distanceFt + ", minDistance: " + minDistance);

            if (distanceFt < minDistance) {
                minDistance = distanceFt;
                nearestRunway = runway;
                runwayDistance.set(minDistance);
            }
        }

        return nearestRunway;
    }

    public boolean hasRunways() {
        return runways.size() > 0;
    }

    public JsonElement jsonify(Gson gson) {
        HashMap<String, Runway> runways = this.runways;
        this.runways = null;
        JsonElement je = gson.toJsonTree(this);
        this.runways = runways;
        return je;
    }
}
