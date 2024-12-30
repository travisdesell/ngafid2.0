package org.ngafid.flights;

import java.util.Map;

import static org.ngafid.flights.XPlaneParameters.*;

/**
 * Create exports for X-Plane 11
 *
 * @author <a href = mailto:apl1341@cs.rit.edu>Aidan LaBella @ RIT CS</a>
 */
public class XPlane11Export extends XPlaneExport {
    public XPlane11Export(int flightId, String aircraftPath, boolean useMSL) {
        super(flightId, aircraftPath, useMSL);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeFlightData(StringBuffer buffer, Map<String, Object> scopes) {
        int length = parameters.get(ALT).size();
        for (int i = 0; i < length; i++) {
            //make sure we dont log where the GPS wasn't recording coordinates as this will 
            //cause X-Plane to crash
            if (!Double.isNaN(parameters.get(LONGITUDE).get(i)) && !Double.isNaN(parameters.get(LATITUDE).get(i))) {
                double rpmVal = parameters.get(E1_RPM).get(i);
                double e1EGT = parameters.get(E1_EGT).get(i);
                buffer.append("DATA, " + i + "," + NULL_DATA + parameters.get(LONGITUDE).get(i) + "," + parameters.get(LATITUDE).get(i) + "," + parameters.get(ALT).get(i) + "," + getZeros(4) + parameters.get(PITCH).get(i) + "," + parameters.get(ROLL).get(i) + "," + parameters.get(HEADING).get(i) + "," + parameters.get(IAS).get(i) + getZeros(55) + (Double.isNaN(rpmVal) ? "0" : rpmVal) + getZeros(7) + (Double.isNaN(e1EGT) ? "0" : e1EGT) + getZeros(2) + "\n");
            }
        }
    }
}

