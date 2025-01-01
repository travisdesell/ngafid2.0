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
            // Make sure we don't log where the GPS wasn't recording coordinates as this will cause X-Plane to crash
            if (!Double.isNaN(parameters.get(LONGITUDE).get(i)) && !Double.isNaN(parameters.get(LATITUDE).get(i))) {
                double rpmVal = parameters.get(E1_RPM).get(i);
                double e1EGT = parameters.get(E1_EGT).get(i);
                buffer.append("DATA, ").append(i).append(",").append(NULL_DATA).
                        append(parameters.get(LONGITUDE).get(i)).append(",").
                        append(parameters.get(LATITUDE).get(i)).append(",").
                        append(parameters.get(ALT).get(i)).append(",").
                        append(getZeros(4)).append(parameters.get(PITCH).get(i))
                        .append(",").append(parameters.get(ROLL).get(i)).append(",")
                        .append(parameters.get(HEADING).get(i))
                        .append(",").append(parameters.get(IAS).get(i))
                        .append(getZeros(55)).append(Double.isNaN(rpmVal) ? "0" : rpmVal)
                        .append(getZeros(7)).append(Double.isNaN(e1EGT) ? "0" : e1EGT)
                        .append(getZeros(2)).append("\n");
            }
        }
    }
}

