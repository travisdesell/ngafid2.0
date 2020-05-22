/**
 * Constants that are part of the FDR format definition
 * for X-Plane animations
 * @author <a href = mailto:apl1341@cs.rit.edu>Aidan LaBella</a>
 */
package org.ngafid.routes;

import java.lang.String;
import java.util.Map;

public interface XPlaneParameters {
    //the file extension
    static String FDR_FILE_EXTENSION = ".fdr";

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
    static String NULL_DATA = "0,";

    //The current number of empty/untracked parameters in the format
    //NOTE: change this if we incorporate more params
    static int NUM_NULL_PARAMS = 74;

    //The following are the aircraft specifications within X-Plane
    //
    static String C172 = "Cessna 172S";
    static String PA_28_181 = "PA-28-181";
    static String PA_44_180 = "PA-44-180";

    //A hashmap that maps aircraft strings to their respective X-Plane names/paths
    Map<String, String> xplaneNames = Map.of(C172, "Aircraft/General Aviation/Cessna 172SP/Cessna_172SP.acf",
                                PA_28_181, PA_28_181+"XP",//TODO: put xplane path in for these values
                                PA_44_180, PA_44_180+"XP");

}
