package org.ngafid.core.airports;

import java.io.Serializable;

public class Runway implements Serializable {

    private final String siteNumber;
    private final String name;
    private final boolean hasCoordinates;

    private final double lat1;
    private final double lon1;
    private final double lat2;
    private final double lon2;

    public Runway(String siteNumber, String name) {
        this.siteNumber = siteNumber;
        this.name = name;
        this.lat1 = Double.NaN;
        this.lon1 = Double.NaN;
        this.lat2 = Double.NaN;
        this.lon2 = Double.NaN;
        this.hasCoordinates = false;
    }

    public Runway(String siteNumber, String name, double lat1, double lon1, double lat2, double lon2) {
        this.siteNumber = siteNumber;
        this.name = name;
        this.lat1 = lat1;
        this.lon1 = lon1;
        this.lat2 = lat2;
        this.lon2 = lon2;
        this.hasCoordinates = true;
    }

    public String getName() {
        return name;
    }

    public String getSiteNumber() {
        return siteNumber;
    }

    public boolean hasCoordinates() {
        return hasCoordinates;
    }

    public double getLat1() {
        return lat1;
    }

    public double getLon1() {
        return lon1;
    }

    public double getLat2() {
        return lat2;
    }

    public double getLon2() {
        return lon2;
    }

    /**
     * @param pointLatitude  latitude of the point
     * @param pointLongitude longitude of the point
     * @return the distance in feet
     */
    public double getDistanceFt(double pointLatitude, double pointLongitude) {
        return Airports.shortestDistanceBetweenLineAndPointFt(pointLatitude, pointLongitude, lat1, lon1, lat2, lon2);
    }

    public String toString() {
        if (hasCoordinates) {
            return "[RUNWAY " + siteNumber + ", " + name + ", " + lat1 + ", " + lon1 + ", " + lat2 + ", " + lon2 + "]";
        } else {
            return "[RUNWAY " + siteNumber + ", " + name + "]";
        }
    }
}
