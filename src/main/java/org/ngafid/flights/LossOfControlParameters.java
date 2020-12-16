/**
 * This interface contains useful constants and values for the {@link LossOfControlCalculation} and {@link StallCalculation}
 *
 * @author <a href = mailto:apl1341@cs.rit.edu>Aidan LaBella @ RIT CS</a>
 */

package org.ngafid.flights;

public interface LossOfControlParameters {
    public static final double STD_PRESS_INHG = 29.92;
    public static final double COMP_CONV = (double) (Math.PI / 180); 

    /**
     * Critical Values
     *
     * @param AOA_CRIT this is the critical angle of attack that can be changes based on the FDM's guidelines
     * @param PROSPIN_LIM this is the crirtical value for the "Coordination Index", which can also be changed based on certain guidelines
     */
    public static final double AOA_CRIT = 15;
    public static final double PROSPIN_LIM = 4;

    public static final int YAW_RATE_LAG = 1;

    /**
     * {@link DoubleTimeSeries} constants
     */
    public static final String HDG = "HDG";
    public static final String IAS = "IAS";
    public static final String VSPD = "VSPD";
    public static final String OAT = "OAT";
    public static final String BARO_A = "BaroA";
    public static final String PITCH = "Pitch";
    public static final String ROLL = "Roll";
    public static final String ALT_AGL = "AltAGL";
    public static final String ALT_MSL = "AltMSL";
    public static final String AOA_SIMPLE = "AOASimple"; 
    public static final String E1_RPM = "E1 RPM";
    public static final String TAS_FTMIN = "True Airspeed(ft/min)";
    public static final String STALL_PROB = "Stall Index";
    public static final String LOCI = "LOC-I Index";
    public static final String PRO_SPIN_FORCE = "Coordination Index";
    public static final String YAW_RATE = "Yaw Rate";

    /**
     * {@link Airframes} id's
     */
    public static final int C172SP_ID = 1;

    /**
     * Strings that represent the parameters used in this calculation
     *
     * @param ALT_AGL is used as the time reference 
     */
    public static final String [] lociParamStrings = {HDG, IAS, VSPD, OAT, BARO_A, PITCH, ROLL, ALT_AGL};
    public static final String [] spParamStrings = {PITCH, VSPD, IAS, BARO_A, OAT, ALT_AGL};

    /**
     * Strings that represent the supplementary metrics displayed in the UI
     */
    public static final String [] uiMetrics = {ROLL, IAS, PITCH, ALT_MSL, AOA_SIMPLE, E1_RPM, ALT_AGL};
    public static final String [] defaultMetrics = {ROLL, PITCH, IAS, ALT_MSL, ALT_AGL, AOA_SIMPLE, E1_RPM};
}
