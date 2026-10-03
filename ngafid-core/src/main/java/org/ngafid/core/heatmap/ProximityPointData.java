package org.ngafid.core.heatmap;

import java.time.OffsetDateTime;

/**
 * Immutable value holding a single sampled point of a proximity event: its position, timestamp, and altitude AGL.
 */
public class ProximityPointData {
    private final double latitude;
    private final double longitude;
    private final OffsetDateTime timestamp;
    private final double altitudeAGL;

    /**
     * Constructs an immutable sampled point for a proximity event: its position, timestamp, and altitude above ground.
     *
     * @param latitude the point's latitude, in degrees
     * @param longitude the point's longitude, in degrees
     * @param timestamp the time the sample was recorded
     * @param altitudeAGL the altitude above ground level, in feet
     */
    public ProximityPointData(double latitude, double longitude, OffsetDateTime timestamp, double altitudeAGL) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.altitudeAGL = altitudeAGL;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public double getAltitudeAGL() {
        return altitudeAGL;
    }
}
