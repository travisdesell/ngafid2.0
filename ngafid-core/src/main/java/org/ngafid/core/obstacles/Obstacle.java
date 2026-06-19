enum ObstacleRisk {
    HIGH,
    MEDIUM,
    LOW,
    NONE
}

public class Obstacle {
    private final int id;
    private final double latitude;
    private final double longitude;
    private final int agl;      //Above ground level
    private final int amsl;     //Above mean sea level
    private final String type;
    private final int quantity;

    private final String geoHash;

    public Obstacle(int id, double latitude, double longitude, String type, int agl, int amsl, int quantity) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.type = type;
        this.agl = agl;
        this.amsl = amsl;
        this.quantity = quantity;

        this.geoHash = GeoHash.getGeoHash(latitude, longitude);
    }

    public int getID() {return this.id;}
    public double getLatitude() {return this.latitude;}
    public double getLongitude() {return this.longitude;}
    public double getType() {return this.type;}
    public int getAGL() {return this.agl;}
    public int getAMSL() {return this.amsl;}
    public int getQuantity() {return this.quantity;}
    public String getGeoHash() {return this.geoHash;}

    public ObstacleRisk calculateRiskFromPoint(double latitude, double longitude, int agl) {
        double horizontalDistance = Airports.calculateDistanceInFeet(this.latitude, this.longitude, latitude, longitude);
        double verticalDistance = Math.abs(agl - this.agl);

        if ((horizontalDistance <= 500) || (verticalDistance <= 75)) {return ObstacleRisk.HIGH;}
        else if (((horizontalDistance <= 1000) && (Obstacles.IsDoubleInRangeInclusive(verticalDistance, 75, 200)))
            || ((Obstacles.IsDoubleInRangeInclusive(horizontalDistance, 500, 1000)) && (verticalDistance <= 200))) {return ObstacleRisk.MEDIUM;}
        else if (((horizontalDistance >= 500) && (horizontalDistance <= 1000))
            && ((verticalDistance >= 75) && (verticalDistance <= 200))) {return ObstacleRisk.LOW;}
        else {return ObstacleRisk.NONE;}
    }

    public String toString() {
        return "[Obstacle " + id + ", " + type + ", " + latitude + ", " + longitude + ", " + agl + ", " + geoHash + "]";
    }

    
}