package org.ngafid.common.airports;

import java.io.Serializable;

public class Runway implements Serializable {

    public final String siteNumber;
    public final String name;
    public final boolean hasCoordinates;

    public final double lat1;
    public final double lon1;
    public final double lat2;
    public final double lon2;

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
