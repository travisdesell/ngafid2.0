/**
 * An interface to help keep better track of what flight parameters actually do.
 * In the future, this may be a good place to store meta information about parameters, e.g.:
 * - Parameters that share the same name
 * - Parameters that are not always available
 * - Relationships between parameters
 *
 * @author <a href = mailto:josh@mail.rit.edu>Josh Karns</a>
 * @author <a href = mailto:apl@mail.rit.edu>Aidan LaBella</a>
 */
package org.ngafid.flights;

public interface Parameters {
    /**
     * JSON-specific parameters
     * EXC = Exceedences
     */
    String PARAM_JSON_LOSS_OF_CONTROL_EXC = "locExceedences";
    String PARAM_JSON_CENTER_LINE_EXC = "centerLineExceedences";
    String PARAM_JSON_SELF_DEFINED_GLIDE_PATH_ANGLE = "selfDefinedGlideAngle";
    String PARAM_JSON_OPTIMAL_DESCENT_WARN = "optimalDescentWarnings";
    String PARAM_JSON_OPTIMAL_DESCENT_EXC = "optimalDescentExceedences";
    String PARAM_JSON_LATITUDE = "lat";
    String PARAM_JSON_LONGITUDE = "lon";

    double STD_PRESS_INHG = 29.92;
    double COMP_CONV = Math.PI / 180.0;

    /**
     * Critical Values
     *
     * @param AOA_CRIT    this is the critical angle of attack that can be changes based on the FDM's guidelines
     * @param PROSPIN_LIM this is the crirtical value for the "Coordination Index", which can also be changed based on
     *                    certain guidelines
     */
    double AOA_CRIT = 15;
    double PROSPIN_LIM = 4;

    int YAW_RATE_LAG = 1;
    int VSI_LAG_DIFF = 1;

    /**
     * {@link DoubleTimeSeries} constants, column names
     */
    String LAT = "Latitude";
    String LON = "Longitude";
    String LAG_SUFFIX = "_lag";
    String LEAD_SUFFIX = "_lead";
    String HDG = "HDG";
    String TRK = "TRK";
    String NORM_AC = "NormAc";
    String LAT_AC = "LatAc";
    String IAS = "IAS";

    /*
     * How fast the aircraft is moving vertically.
     * This is called velocity instead of speed because the number actually has a direction associated with it,
     * the sign of the number. If it is > 0, then the aircraft is ascending; likewise a negative value means
     * the aircraft is descending.
     */ String VSPD = "VSpd";
    String DENSITY_RATIO = "DensityRatio";
    String OAT = "OAT";
    String BARO_A = "BaroA";
    String PITCH = "Pitch";
    String ROLL = "Roll";
    String ALT_AGL = "AltAGL";
    String ALT_MSL = "AltMSL";
    String ALT_MSL_LAG_DIFF = "AltMSL Lag Diff";
    String ALT_B = "AltB";
    String AOA_SIMPLE = "AOASimple";
    String E1_RPM = "E1 RPM";
    String TAS_FTMIN = "True Airspeed(ft/min)";
    String STALL_PROB = "Stall Index";
    String SPIN = "Spin Event(s)";
    String LOCI = "LOC-I Index";
    String PRO_SPIN_FORCE = "Coordination Index";
    String YAW_RATE = "Yaw Rate";
    String VSPD_CALCULATED = "VSpd Calculated";
    String CAS = "CAS";
    String GND_SPD = "GndSpd";
    String WIND_SPEED = "WndSpd";
    String WIND_DIRECTION = "WndDr";
    String TOTAL_FUEL = "Total Fuel";
    String LCL_DATE = "Lcl Date";
    String LCL_TIME = "Lcl Time";
    String UTC_OFFSET = "UTCOfst";
    String LATITUDE = "Latitude";
    String LONGITUDE = "Longitude";
    String STALL_PROBABILITY = "PStall";
    String LOSS_OF_CONTROL_PROBABILITY = "PLOCI";
    String HDG_TRK_DIFF = "HDG TRK Diff";
    String FUEL_QTY_LEFT = "FQtyL";
    String FUEL_QTY_RIGHT = "FQtyR";

    String NEAREST_RUNWAY = "NearestRunway";
    String RUNWAY_DISTANCE = "RunwayDistance";
    String NEAREST_AIRPORT = "NearestAirport";
    String AIRPORT_DISTANCE = "AirportDistance";

    /**
     * Units
     */
    enum Unit {
        FT("ft"),
        FT_AGL("ft agl"),
        FT_MSL("ft msl"),
        GALLONS("gals"),
        DEGREES_F("deg f"),
        IATA_CODE("IATA Code"),
        KNOTS("knots"),
        FT_PER_MINUTE("ft/min"),
        DEGREES("degrees"),
        INDEX("index"),
        RATIO("ratio");

        private final String value;

        Unit(String name) {
            value = name;
        }

        public String toString() {
            return value;
        }
    }

    /**
     * {@link Airframes} id's
     */
    int C172SP_ID = 1;

    /**
     * Strings that represent the parameters used in the Stall Index calculation
     *
     * @param ALT_B is used as the time reference
     * @param VSPD  not needed for cases where VSpd is drived from AltB
     */
    String[] STALL_DEPENDENCIES = {PITCH, /* VSPD, */ IAS, BARO_A, OAT, ALT_B};

    /**
     * Strings that represent the parameters used in the Yaw Rate calculation
     */
    String[] YAW_RATE_DEPENDENCIES = {HDG};

    /**
     * Strings that represent the parameters used in the LOCI calculation
     */
    String[] LOCI_DEPENDENCIES = {HDG, ROLL, TAS_FTMIN};
    //
    // use these for a real true airspeed (Shelbys method) /*GND_SPD, WIND_SPEED, WIND_DIRECTION};*/
    String[] SPIN_DEPENDENCIES = {IAS, VSPD_CALCULATED, NORM_AC, LAT_AC, ALT_AGL};

    // Params required for HDG TRK diff
    String[] HDG_TRK_DEPENDENCIES = {HDG, TRK};

    // Used to determine average fuel
    String[] AVG_FUEL_DEPENDENCIES = {TOTAL_FUEL};

    String[] EVENT_RECOGNITION_COLUMNS = {TOTAL_FUEL};

    /**
     * Strings that represent the supplementary metrics displayed in the UI
     */
    String[] UI_METRICS = {ROLL, IAS, PITCH, ALT_MSL, AOA_SIMPLE, E1_RPM, ALT_AGL};
    String[] DEFAULT_METRICS = {ROLL, PITCH, IAS, ALT_MSL, ALT_AGL, AOA_SIMPLE, E1_RPM};

}
