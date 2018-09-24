package org.ngafid.events;

import java.util.ArrayList;

//condition: Excessive VSI on Final: vertical_airspeed < -1000 AND radio_altitude_derived < 1000
public class VsiEvent extends Event {

    private static final int verticalAirSpeedColumn = 12;
    private static final int radioAltitudeColumn = 12;
    private static final double verticalAirSpeedLimit = -1000;
    private static final double radioAltitudeLimit = 1000;

    public VsiEvent(String startTime, String endTime, int startLine, int endLine) {
        super(startTime, endTime, startLine, endLine, 10);
    }

    public static boolean isOccuring(ArrayList<String> lineValues) {
        double verticalAirSpeed = Double.parseDouble(lineValues.get(verticalAirSpeedColumn));
        double radioAltitude = Double.parseDouble(lineValues.get(radioAltitudeColumn));

        if (verticalAirSpeed < verticalAirSpeedLimit && radioAltitude > radioAltitudeLimit) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return "VSI ON FINAL EVENT " + super.toString();
    }
}
