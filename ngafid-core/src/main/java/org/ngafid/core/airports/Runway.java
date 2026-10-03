package org.ngafid.core.airports;

import java.io.Serializable;

/**
 * Represents a single runway of an airport, optionally with its two endpoint coordinates.
 *
 * <p>When coordinates are unknown the lat/lon fields are NaN and {@code hasCoordinates} is false, signalling that
 * proximity-to-runway calculations are not meaningful for this runway.
 */
public class Runway implements Serializable {

    private final String siteNumber;
    private final String name;
    private final boolean hasCoordinates;

    private final double lat1;
    private final double lon1;
    private final double lat2;
    private final double lon2;

    /**
     * Constructs a runway without endpoint coordinates; its lat/lon fields are set to NaN and {@code hasCoordinates} is
     * false, so distance calculations against it are not meaningful.
     *
     * @param siteNumber the FAA site number of the owning airport
     * @param name the runway name/designator
     */
    public Runway(String siteNumber, String name) {
        this.siteNumber = siteNumber;
        this.name = name;
        this.lat1 = Double.NaN;
        this.lon1 = Double.NaN;
        this.lat2 = Double.NaN;
        this.lon2 = Double.NaN;
        this.hasCoordinates = false;
    }

    /**
     * Constructs a runway with its two endpoint coordinates, marking it as having coordinates so distance calculations
     * (e.g. {@link #getDistanceFt}) can be performed.
     *
     * @param siteNumber the FAA site number of the owning airport
     * @param name the runway name/designator
     * @param lat1 the latitude of the first runway endpoint, in degrees
     * @param lon1 the longitude of the first runway endpoint, in degrees
     * @param lat2 the latitude of the second runway endpoint, in degrees
     * @param lon2 the longitude of the second runway endpoint, in degrees
     */
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

    /**
     * Reports whether this runway has known endpoint coordinates (and therefore supports distance calculations).
     *
     * @return true if endpoint coordinates were provided at construction
     */
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

    /**
     * Returns a human-readable summary of the runway: its site number and name, including the endpoint coordinates when
     * they are available.
     *
     * @return a debug string describing this runway
     */
    @Override
    public String toString() {
        if (hasCoordinates) {
            return "[RUNWAY " + siteNumber + ", " + name + ", " + lat1 + ", " + lon1 + ", " + lat2 + ", " + lon2 + "]";
        } else {
            return "[RUNWAY " + siteNumber + ", " + name + "]";
        }
    }
}
