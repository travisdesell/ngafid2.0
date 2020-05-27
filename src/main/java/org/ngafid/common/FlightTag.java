package org.ngafid.common;

import java.lang.String;
import java.awt.Color;

import org.ngafid.flights.Flight;

import java.sql.ResultSet;
import java.sql.SQLException;

public class FlightTag{
    //the key in the database
    private int hashId;
    private String name, description;
    private String color;

    /**
     * Creates an instance of a tag
     * @param hash the id in SQL
     * @param name the user specified name
     * @param description the user specified description of the tag
     */
    public FlightTag(int hash, String name, String description, String color){
        this.hashId = hash;
        this.name = name;
        this.description = description;
        this.color = color;
    }

    /**
     * Creates an instance of a tag using a mySQL resultset
     * @param resultSet the result set containing the results of a query
     */
    public FlightTag(ResultSet resultSet) throws SQLException{
        hashId = resultSet.getInt(1);
        name = resultSet.getString(3);
        description = resultSet.getString(4);
        color = resultSet.getString(5);
    }

    /**
     * The hashcode of a tag is its key in the database
     * @return an int with the key/hashcode
     */
    @Override
    public int hashCode(){
        return this.hashId;
    }

    /**
     * Provides a description of the tag
     * @return a String with the description in it
     */
    public String getDescription(){
        return description;
    }

    /**
     * Gets the name associated with the tag
     * @return the name as a String
     */
    public String getName(){
        return name;
    }

    /**
     * Returns the given color for the tag
     * @return a String instance representing the tag's color
     */
    public String getColor(){
        return color;
    }

    /**
     * Checks for equality between two tags
     * @return a bool representing the reltionship between two tags
     */
    @Override
    public boolean equals(Object other){
        if(other instanceof FlightTag){
            FlightTag t = (FlightTag)other;
            return t.hashCode() == this.hashCode();
        }
        return false;
    }

    /**
     * Provides a string representation of the tag
     * @return a String with pertinent info
     */
    @Override
    public String toString(){
        return FlightTag.class.getName()+": "+"id: "+this.hashId+
            " name: "+this.name+" description: "+this.description+
            " color: "+this.color;
    }
}

