/**
 * Constants that are part of the FDR format definition
 * for X-Plane animations
 *
 * @author <a href = mailto:apl1341@cs.rit.edu>Aidan LaBella</a>
 */
package org.ngafid.flights;

public interface XPlaneParameters {
    //the file extension
    String FDR_FILE_EXTENSION = ".fdr";

    /**
     * The FDR format needs to know what returns to use
     * @param A is unix/posix endlines
     * @param I is MS DOS/NT endlines
     */
    String POSIX_ENDL = "A";
    String NT_ENDL = "I";

    //String constants

    String ENDL = "endl_type";
    String COMM = "comm";
    String ACFT = "acft";
    String TAIL = "tail";
    String TIME = "time";
    String DATE = "date";
    String PRES = "pres";
    String TEMP = "temp";
    String WIND = "wind";
    String CALI = "cali";
    String WARN = "warn";
    String TEXT = "text";
    String MARK = "mark";
    String EVNT = "evnt";
    String DATA = "data";

    //Data segment parameters
    //generic altitude col
    String ALT = "altitude";
    String ALT_MSL = "altMSL";
    String LATITUDE = "latitude";
    String LONGITUDE = "longitude";
    String HEADING = "heading";
    String PITCH = "pitch";
    String ROLL = "roll";
    String IAS = "ias";
    String E1_RPM = "e1RPM";
    String E1_EGT = "e1EGT";

    String NULL_DATA = "0,";
}
