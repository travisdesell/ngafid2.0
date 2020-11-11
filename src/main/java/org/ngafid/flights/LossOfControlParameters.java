package org.ngafid.flights;

import java.util.Map;

public interface LossOfControlParameters {
	public static final double STD_PRESS_INHG = 29.92;
	public static final double COMP_CONV = (double) (Math.PI / 180); 
	public static final double AOA_CRIT = 15;
	public static final double PROSPIN_LIM = 4;

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
	public static final String STALL_PROB = "StallProbability";
	public static final String LOCI = "LOCI";
	public static final String PRO_SPIN_FORCE = "ProSpin Force";
	public static final String YAW_RATE = "Yaw Rate";

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

	public static final Map<String, String> metricNames = Map.of(ROLL, "Roll (degrees)",
																 IAS, "IAS (knots)",
																 PITCH, "Pitch (degrees)",
																 ALT_MSL, "Altitiude (MSL) [ft]",
																 AOA_SIMPLE, "Angle of Attack (simple) [degrees]",
																 E1_RPM, "Engine 1 RPM",
																 ALT_AGL, "Altitiude (AGL) [ft]");
}
