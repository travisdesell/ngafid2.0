package org.ngafid.airports;

import java.util.ArrayList;

import org.ngafid.common.MutableDouble;

public class Airport {
    public final String iataCode;
    public final String siteNumber;
    public final String type;

    public final double latitude;
    public final double longitude;

    public final String geoHash;

    private ArrayList<Runway> runways;

    public Airport(String iataCode, String siteNumber, String type, double latitude, double longitude) {
        this.iataCode = iataCode;
        this.siteNumber = siteNumber;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;

        this.geoHash = GeoHash.getGeoHash(latitude, longitude);

        runways = new ArrayList<Runway>();
    }

    public String toString() {
        return "[AIRPORT " + iataCode + ", " + type + ", " + latitude + ", " + longitude + ", " + geoHash + "]";
    }

    public void addRunway(Runway runway) {
        runways.add(runway);
    }

    public Runway getNearestRunwayWithin(double latitude, double longitude, double maxDistanceFt, MutableDouble runwayDistance) {
        Runway nearestRunway = null;

        double minDistance = maxDistanceFt;
        for (int i = 0; i < runways.size(); i++) {
            Runway runway = runways.get(i);
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

}
