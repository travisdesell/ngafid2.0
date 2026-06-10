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

    public String toString() {
        return "[Obstacle " + id + ", " + type + ", " + latitude + ", " + longitude + ", " + agl + ", " + geoHash + "]";
    }
}