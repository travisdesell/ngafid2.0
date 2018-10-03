package org.ngafid.airports;


public class Runway {

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

    /**
     *  Modified from:
     *  https://stackoverflow.com/questions/849211/shortest-distance-between-a-point-and-a-line-segment
     */
    public double getDistanceFt(double point_latitude, double point_longitude) {
        double A = point_longitude - lon1;
        double B = point_latitude - lat1;
        double C = lon2 - lon1;
        double D = lat2 - lat1;

        double dot = A * C + B * D;
        double len_sq = C * C + D * D;
        double param = -1;

        if (len_sq != 0) {
            //in case of 0 length line
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

        double dx = point_longitude - xx;
        double dy = point_latitude - yy;

        //return Math.sqrt(dx * dx + dy * dy);
        //return min_distance * 3280.84;
        return Airports.calculateDistanceInFeet(point_latitude, point_longitude, point_latitude + dy, point_longitude + dx);
    }

    public String toString() {
        if (hasCoordinates) {
            return "[RUNWAY " + siteNumber + ", " + name + ", " + lat1 + ", " + lon1 + ", " + lat2 + ", " + lon2 + "]";
        } else {
            return "[RUNWAY " + siteNumber + ", " + name + "]";
        }
    }
}
