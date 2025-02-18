/**
 * This interface contains useful constants and values for the {@link LossOfControlCalculation} and
 * {@link StallCalculation}
 *
 * @author <a href = mailto:apl1341@cs.rit.edu>Aidan LaBella @ RIT CS</a>
 */

package org.ngafid.events.calculations;

//CHECKSTYLE:OFF

import org.ngafid.flights.Airframes;
import org.ngafid.flights.DoubleTimeSeries;
//CHECKSTYLE:ON

public interface CalculationParameters {
    double STD_PRESS_INHG = 29.92;
    double COMP_CONV = Math.PI / 180;

    /**
     * Critical Values
     *
     * @param AOA_CRIT this is the critical angle of attack that can be changes based on the FDM's guidelines
     * @param PROSPIN_LIM this is the crirtical value for the "Coordination Index", which can also be changed based
     * on certain guidelines
     */
    double AOA_CRIT = 15;
    double PROSPIN_LIM = 4;

    int YAW_RATE_LAG = 1;
    int VSI_LAG_DIFF = 1;

    /**
     * {@link DoubleTimeSeries} constants
     */
    String LAG_SUFFIX = "_lag";
    String LEAD_SUFFIX = "_lead";
    String HDG = "HDG";
    String NORM_AC = "NormAc";
    String LAT_AC = "LatAc";
    String IAS = "IAS";
    String VSPD = "VSPD";
    String DENSITY_RATIO = "DensityRatio";
    String OAT = "OAT";
    String BARO_A = "BaroA";
    String PITCH = "Pitch";
    String ROLL = "Roll";
    String ALT_AGL = "AltAGL";
    String ALT_MSL = "AltMSL";
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

    /**
     * {@link Airframes} id's
     */
    int C172SP_ID = 1;

    /**
     * Strings that represent the parameters used in the Stall Index calculation
     *
     * @param ALT_B is used as the time reference
     * @param VSPD not needed for cases where VSpd is drived from AltB
     */
    String[] STALL_DEPENDENCIES = {PITCH, /*VSPD,*/ IAS, BARO_A, OAT, ALT_B};

    /**
     * Strings that represent the parameters used in the Stall Index calculation
     */
    String[] LOCI_DEPENDENCIES = {HDG, ROLL};
    //
    // use these for a real true airspeed (Shelbys method) /*GND_SPD, WIND_SPEED, WIND_DIRECTION};*/
    String[] SPIN_DEPENDENCIES = {IAS, VSPD_CALCULATED, NORM_AC, LAT_AC, ALT_AGL};

    // Used to determine average fuel
    String[] AVG_FUEL_DEPENDENCIES = {TOTAL_FUEL};

    /**
     * Strings that represent the supplementary metrics displayed in the UI.
     */
    String[] UI_METRICS = {ROLL, IAS, PITCH, ALT_MSL, AOA_SIMPLE, E1_RPM, ALT_AGL};
    String[] DEFAULT_METRICS = {ROLL, PITCH, IAS, ALT_MSL, ALT_AGL, AOA_SIMPLE, E1_RPM};

}
