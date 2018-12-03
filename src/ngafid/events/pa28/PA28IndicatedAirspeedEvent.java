package org.ngafid.events.pa28;

import java.util.ArrayList;

import org.ngafid.events.Event;

//Condition: Excessive Speed: indicated_airspeed > 154
public class PA28IndicatedAirspeedEvent extends Event {

    private static final int pa28indicatedAirspeedColumn = 10;
    private static final double pa28indicatedAirspeedLimit = 154;

    public PA28IndicatedAirspeedEvent(String startTime, String endTime, int startLine, int endLine) {
        super(startTime, endTime, startLine, endLine, 5);
    }

    public static boolean isOccuring(ArrayList<String> lineValues) {
        double pa28indicatedAirspeed = Double.parseDouble(lineValues.get(pa28indicatedAirspeedColumn));

        if (pa28indicatedAirspeed > pa28indicatedAirspeedLimit) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return " PA28 INDICATED AIR SPEED EVENT " + super.toString();
    }
}
