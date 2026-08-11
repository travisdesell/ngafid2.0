package org.ngafid.core.obstacles;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.ngafid.core.airports.GeoHash;

/**
 * Basic representation of an obstacle.
 * Obstacle
 */
public class Obstacle {

    public enum Lighting {
        R("R"), S("S"), 
        C("C"), D("D"), 
        U("U"), F("F"), 
        W("W"), H("H"), 
        L("L"), M("M"), 
        N("N");

        private String value;
        Lighting(String value) {this.value = value;}

        @Override
        public String toString() {
            return value;
        }
    }

    private final int id;
    private final double latitude;
    private final double longitude;
    private final int agl;      //Above ground level
    private final int amsl;     //Above mean sea level
    private final String type;
    private final int quantity;
    private final Lighting lighting;

    private final String geoHash;

    /**
     * Basic obstacle constructor
     * @param id
     * @param latitude
     * @param longitude
     * @param type
     * @param agl
     * @param amsl
     * @param quantity
     * @param lighting
     */
    public Obstacle(int id, double latitude, double longitude, String type, int agl, int amsl, int quantity, Lighting lighting) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.type = type;
        this.agl = agl;
        this.amsl = amsl;
        this.quantity = quantity;
        this.lighting = lighting;

        this.geoHash = GeoHash.getGeoHash(latitude, longitude);
    }

    /**
     * Creates an obstacle from the database sql return result
     * @param result
     * @throws SQLException
     */
    public Obstacle(ResultSet result) throws SQLException {
        this.id = result.getInt(1);
        this.latitude = result.getDouble(2);
        this.longitude = result.getDouble(3);
        this.agl = result.getInt(4);
        this.amsl = result.getInt(5);
        this.type = result.getString(6);
        this.lighting = Lighting.valueOf(result.getString(7));
        this.quantity = result.getInt(8);

        this.geoHash = GeoHash.getGeoHash(latitude, longitude);
    }

    public int getID() {return this.id;}
    public double getLatitude() {return this.latitude;}
    public double getLongitude() {return this.longitude;}
    public String getType() {return this.type;}
    public int getAGL() {return this.agl;}
    public int getAMSL() {return this.amsl;}
    public int getQuantity() {return this.quantity;}
    public String getGeoHash() {return this.geoHash;}
    public Lighting getLighting() {return this.lighting;}


    public String toString() {
        return "[Obstacle " + id + ", " + type + ", " + latitude + ", " + longitude + ", " + agl + ", " + geoHash + "]";
    }
    
}