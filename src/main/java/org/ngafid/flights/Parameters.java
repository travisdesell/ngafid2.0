package org.ngafid.flights;

// A static class to help keep better track of what flight parameters actually do.
// In the future, this may be a good place to store meta information about parameters, e.g.:
//  - Parameters that share the same name
//  - Parameters that are not always available
//  - Relationships between parameters
public class Parameters {

    /* Latitude of the aircraft */
    public final static String PARAM_LATITUDE = "Latitude";

    /* Longitude of the aircraft */
    public final static String PARAM_LONGITUDE = "Longitude";


    public final static String PARAM_ALTITUDE_ABOVE_SEA_LEVEL = "AltMSL";
    public final static String PARAM_ALTITUDE_ABOVE_GND_LEVEL = "AltAGL";
    public final static String PARAM_PITCH = "Pitch";
    public final static String PARAM_ROLL = "Roll";

    /*
     * How fast the aircraft is moving vertically.
     * This is called velocity instead of speed because the number actually has a direction associated with it,
     * the sign of the number. If it is > 0, then the aircraft is ascending; likewise a negative value means
     * the aircraft is descending.
     * */
    public final static String PARAM_VERTICAL_VELOCITY = "VSpd";
    public final static String PARAM_GND_SPEED = "GndSpd";
    public final static String PARAM_ = "";
    // public final static String PARAM_ = "";
    // public final static String PARAM_ = "";
    // public final static String PARAM_ = "";
    // public final static String PARAM_ = "";
    // public final static String PARAM_ = "";
}
