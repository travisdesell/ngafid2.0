package org.ngafid.flights;

public interface LossOfControlParameters{
	public static final double STD_PRESS_INHG = 29.92;
	public static final double COMP_CONV = (double) (Math.PI / 180); 
	public static final double AOACrit = 15;
	public static final double proSpinLim = 4;

	public static final String HDG = "HDG";
	public static final String IAS = "IAS";
	public static final String VSPD = "VSPD";
	public static final String OAT = "OAT";
	public static final String BARO_A = "BaroA";
	public static final String PITCH = "Pitch";
	public static final String ROLL = "Roll";
	public static final String ALT_AGL = "AltAGL";

	public static final int C172SP_ID = 1;

	/**
	 * Strings that represent the parameters used in this calculation
	 */
	public static final String [] dtsParamStrings = {HDG, IAS, VSPD, OAT, BARO_A,
														PITCH, ROLL, ALT_AGL};
}
