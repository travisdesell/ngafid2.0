/**
 *
 * An interface to help keep better track of what flight parameters actually do.
 * In the future, this may be a good place to store meta information about parameters, e.g.:
 * - Parameters that share the same name
 * - Parameters that are not always available
 * - Relationships between parameters
 * @author <a href = mailto:josh@mail.rit.edu>Josh Karns</a>
 * @author <a href = mailto:apl@mail.rit.edu>Aidan LaBella</a>
 */
package org.ngafid.flights;

public interface Parameters {
    /**
     * JSON-specific parameters
     * EXC = Exceedences
     */
    public static final String PARAM_JSON_LOSS_OF_CONTROL_EXC = "locExceedences";
    public static final String PARAM_JSON_CENTER_LINE_EXC =  "centerLineExceedences";
    public static final String PARAM_JSON_SELF_DEFINED_GLIDE_PATH_ANGLE = "selfDefinedGlideAngle";
    public static final String PARAM_JSON_OPTIMAL_DESCENT_WARN = "optimalDescentWarnings";
    public static final String PARAM_JSON_OPTIMAL_DESCENT_EXC = "optimalDescentExceedences";
    public static final String PARAM_JSON_LATITUDE = "lat";
    public static final String PARAM_JSON_LONGITUDE = "lon";

    public static final double STD_PRESS_INHG = 29.92;
    public static final double COMP_CONV = Math.PI / 180.0; 

    /**
     * Critical Values
     *
     * @param AOA_CRIT this is the critical angle of attack that can be changes based on the FDM's guidelines
     * @param PROSPIN_LIM this is the crirtical value for the "Coordination Index", which can also be changed based on certain guidelines
     */
    public static final double AOA_CRIT = 15;
    public static final double PROSPIN_LIM = 4;

    public static final int YAW_RATE_LAG = 1;
    public static final int VSI_LAG_DIFF = 1;

    /**
     * {@link DoubleTimeSeries} constants, column names
     */
    public static final String LAT = "Latitude";
    public static final String LON = "Longitude";
    public static final String LAG_SUFFIX = "_lag";
    public static final String LEAD_SUFFIX = "_lead";
    public static final String HDG = "HDG";
    public static final String TRK = "TRK";
    public static final String NORM_AC = "NormAc";
    public static final String LAT_AC = "LatAc";
    public static final String IAS = "IAS";

    /*
     * How fast the aircraft is moving vertically.
     * This is called velocity instead of speed because the number actually has a direction associated with it,
     * the sign of the number. If it is > 0, then the aircraft is ascending; likewise a negative value means
     * the aircraft is descending.
     * */
    public static final String VSPD = "VSpd";
    public static final String DENSITY_RATIO = "DensityRatio";
    public static final String OAT = "OAT";
    public static final String BARO_A = "BaroA";
    public static final String PITCH = "Pitch";
    public static final String ROLL = "Roll";
    public static final String ALT_AGL = "AltAGL";
    public static final String ALT_MSL = "AltMSL";
    public static final String ALT_B = "AltB";
    public static final String AOA_SIMPLE = "AOASimple"; 
    public static final String E1_RPM = "E1 RPM";
    public static final String TAS_FTMIN = "True Airspeed(ft/min)";
    public static final String STALL_PROB = "Stall Index";
    public static final String SPIN = "Spin Event(s)";
    public static final String LOCI = "LOC-I Index";
    public static final String PRO_SPIN_FORCE = "Coordination Index";
    public static final String YAW_RATE = "Yaw Rate";
    public static final String VSPD_CALCULATED = "VSpd Calculated";
    public static final String CAS = "CAS";
    public static final String GND_SPD = "GndSpd";
    public static final String WIND_SPEED = "WndSpd";
    public static final String WIND_DIRECTION = "WndDr";
    public static final String TOTAL_FUEL = "Total Fuel";
    public static final String LCL_DATE = "Lcl Date";
    public static final String LCL_TIME = "Lcl Time";
    public static final String UTC_OFFSET = "UTCOfst";
    public static final String LATITUDE = "Latitude";
    public static final String LONGITUDE = "Longitude";
    public static final String STALL_PROBABILITY = "PStall";
    public static final String LOSS_OF_CONTROL_PROBABILITY = "PLOCI";
    public static final String HDG_TRK_DIFF = "HDG TRK Diff";

    public static final String NEAREST_RUNWAY = "NearestRunway";
    public static final String RUNWAY_DISTANCE = "RunwayDistance";
    public static final String NEAREST_AIRPORT = "NearestAirport";
    public static final String AIRPORT_DISTANCE = "AirportDistance";
   
    /**
     * Units
     **/
    public static final String UNIT_FT_AGL = "ft agl";

    /**
     * {@link Airframes} id's
     */
    public static final int C172SP_ID = 1;

    /**
     * Strings that represent the parameters used in the Stall Index calculation
     *
     * @param ALT_B is used as the time reference 
     * @param VSPD not needed for cases where VSpd is drived from AltB
     */
    public static final String [] STALL_DEPENDENCIES = {PITCH, /*VSPD,*/ IAS, BARO_A, OAT, ALT_B};
    
    /**
     * Strings that represent the parameters used in the Stall Index calculation
     */
    public static final String [] LOCI_DEPENDENCIES = {HDG, ROLL};
    //
    // use these for a real true airspeed (Shelbys method) /*GND_SPD, WIND_SPEED, WIND_DIRECTION};*/
    public static final String [] SPIN_DEPENDENCIES = {IAS, VSPD_CALCULATED, NORM_AC, LAT_AC, ALT_AGL};

    //Params required for HDG TRK diff
    public static final String [] HDG_TRK_DEPENDENCIES = {HDG, TRK};

    // Used to determine average fuel
    public static final String[] AVG_FUEL_DEPENDENCIES = {TOTAL_FUEL};

    /**
     * Strings that represent the supplementary metrics displayed in the UI
     */
    public static final String [] uiMetrics = {ROLL, IAS, PITCH, ALT_MSL, AOA_SIMPLE, E1_RPM, ALT_AGL};
    public static final String [] defaultMetrics = {ROLL, PITCH, IAS, ALT_MSL, ALT_AGL, AOA_SIMPLE, E1_RPM};

}
