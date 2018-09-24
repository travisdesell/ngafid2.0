package org.ngafid.events;

import java.util.ArrayList;

public class LateralAccelerationEvent extends Event {

    private static final int lateralAccelerationColumn = 15;
    private static final double lateralAccelerationLowLimit = -0.3;
    private static final double lateralAccelerationHighLimit = 0.4;
    //private static final double lateralAccelerationLowLimit = -1.7;
    //private static final double lateralAccelerationHighLimit = 4.4;

    public LateralAccelerationEvent(String startTime, String endTime, int startLine, int endLine) {
        super(startTime, endTime, startLine, endLine, 5);
    }

    public static boolean isOccuring(ArrayList<String> lineValues) {
        double lateralAcceleration = Double.parseDouble(lineValues.get(lateralAccelerationColumn));

        if (lateralAcceleration < lateralAccelerationLowLimit || lateralAcceleration > lateralAccelerationHighLimit) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return "LATERAL ACCELERATION EVENT " + super.toString();
    }
}
