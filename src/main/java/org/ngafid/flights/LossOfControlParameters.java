package org.ngafid.flights;

public interface LossOfControlParameters{
	public static final double STD_PRESS_INHG = 29.92;
	public static final double COMP_CONV = Math.PI / 180; 
	public static final double AOACrit = 15;
	public static final double proSpinLim = 4;

	/**
	 * Strings that represent the parameters used in this calculation
	 **/
	public static final String [] dtsParamStrings = {"HDG",
													"IAS",
													"VSPD",
													"OAT",
													"BaroA",
													"Pitch",
													"Roll",
													"AltAGL"};
}

