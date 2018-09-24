package org.ngafid.events;

import java.util.ArrayList;

import org.ngafid.events.Event;

//Condition: Excessive Speed: indicated_airspeed > 175
public class C182IndicatedAirspeedEvent extends Event {

    private static final int indicatedAirspeedColumn = 10;
    private static final double indicatedAirspeedLimit = 175;

    public C182IndicatedAirspeedEvent(String startTime, String endTime, int startLine, int endLine) {
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
