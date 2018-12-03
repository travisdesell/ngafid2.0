package org.ngafid.events;

import java.util.ArrayList;

public class LongitudinalAccelerationEvent extends Event {

    private static final int longitudinalAccelerationColumn = 16;
    private static final double longitudinalAccelerationLowLimit = -1.76;
    private static final double longitudinalAccelerationHighLimit = 4.4;

    public LongitudinalAccelerationEvent(String startTime, String endTime, int startLine, int endLine) {
        super(startTime, endTime, startLine, endLine, 5);
    }

    public static boolean isOccuring(ArrayList<String> lineValues) {
        double longitudinalAcceleration = Double.parseDouble(lineValues.get(longitudinalAccelerationColumn));

        if (longitudinalAcceleration < longitudinalAccelerationLowLimit || longitudinalAcceleration > longitudinalAccelerationHighLimit) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return "LONGITUDINAL ACCELERATION EVENT " + super.toString();
    }
}
