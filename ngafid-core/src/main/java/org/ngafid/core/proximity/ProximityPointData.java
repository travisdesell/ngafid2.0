package org.ngafid.core.proximity;

import java.time.OffsetDateTime;

public class ProximityPointData {
    private final double latitude;
    private final double longitude;
    private final OffsetDateTime timestamp;
    private final double altitudeAGL;
    private final double lateralDistance;
    private final double verticalDistance;

    public ProximityPointData(double latitude, double longitude, OffsetDateTime timestamp,
                              double altitudeAGL, double lateralDistance, double verticalDistance) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.altitudeAGL = altitudeAGL;
        this.lateralDistance = lateralDistance;
        this.verticalDistance = verticalDistance;
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

    public double getLateralDistance() {
        return lateralDistance;
    }

    public double getVerticalDistance() {
        return verticalDistance;
    }

}
