package org.ngafid.core.heatmap;

import java.time.OffsetDateTime;

public class ProximityPointData {
    private final double latitude;
    private final double longitude;
    private final OffsetDateTime timestamp;
    private final double altitudeAGL;

    public ProximityPointData(double latitude, double longitude, OffsetDateTime timestamp,
                              double altitudeAGL) {
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
