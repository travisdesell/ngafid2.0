package org.ngafid.events.pa44;

import java.util.ArrayList;

import org.ngafid.events.Event;

//Excessive Speed: indicated_airspeed > 202
public class PA44IndicatedAirspeedEvent extends Event {

    private static final int indicatedAirspeedColumn = 10;
    private static final double indicatedAirspeedLimit = 202;

    public PA44IndicatedAirspeedEvent(String startTime, String endTime, int startLine, int endLine) {
        super(startTime, endTime, startLine, endLine, 5);
    }

    public static boolean isOccuring(ArrayList<String> lineValues) {
        double indicatedAirspeed = Double.parseDouble(lineValues.get(indicatedAirspeedColumn));

        if (indicatedAirspeed > indicatedAirspeedLimit) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return "INDICATED AIR SPEED EVENT " + super.toString();
    }
}
