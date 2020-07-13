/**
 * Constants that are part of the FDR format definition
 * for X-Plane animations
 * @author <a href = mailto:apl1341@cs.rit.edu>Aidan LaBella</a>
 */
package org.ngafid.flights;

import java.lang.String;
import java.util.Map;

public interface XPlaneParameters {
    //the file extension
    static String FDR_FILE_EXTENSION = ".fdr";

	/**
	 * The FDR format needs to know what returns to use
	 * @param A is unix/posix endlines
	 * @param I is MS DOS/NT endlines
	 */
	static String POSIX_ENDL = "A";
	static String NT_ENDL = "I";

	//String constants
	
	static String ENDL = "endl_type";
    static String COMM = "comm";
    static String ACFT = "acft";
    static String TAIL = "tail";
    static String TIME = "time";
    static String DATE = "date";
    static String PRES = "pres";
    static String TEMP = "temp";
    static String WIND = "wind";
    static String CALI = "cali";
    static String WARN = "warn";
    static String TEXT = "text";
    static String MARK = "mark";
    static String EVNT = "evnt";
    static String DATA = "data";

	//Data segment parameters
	static String ALT_MSL = "altMSL";
	static String LATITUDE = "latitude";
	static String LONGITUDE = "longitude";
	static String HEADING = "heading";
	static String PITCH = "pitch";
	static String ROLL = "roll";
	static String IAS = "ias";
	static String E1_RPM = "e1RPM";
	static String E1_EGT = "e1EGT";

    static String NULL_DATA = "0,";

    //The current number of empty/untracked parameters in the format
    //NOTE: change this if we incorporate more params
    //static int NUM_NULL_PARAMS = 66;

    //The following are the aircraft specifications within X-Plane
    //
    static String C172 = "Cessna 172S";
    static String PA_28_181 = "PA-28-181";
    static String PA_44_180 = "PA-44-180";

    //A hashmap that maps aircraft strings to their respective X-Plane names/paths
    Map<String, String> xplaneNames = Map.of(C172, "Aircraft/General Aviation/Cessna 172SP/Cessna_172SP.acf",
                                PA_28_181, "Aircraft/General Aviation/Cessna 172SP/Cessna_172SP.acf",
                                PA_44_180, PA_44_180+"XP");

}
