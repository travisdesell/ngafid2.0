/**
 * Constants that are part of the FDR format definition
 * for X-Plane animations
 * @author <a href = mailto:apl1341@cs.rit.edu>Aidan LaBella</a>
 */
package org.ngafid.flights;


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
    //generic altitude col
    static String ALT = "altitude";
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
}
