package src.events.c172;
//package src.events;

import java.util.ArrayList;

//Condition: Excessive Speed: indicated_airspeed > 163
public class C172IndicatedAirspeedEvent extends Event {

    private static final int c172indicatedAirspeedColumn = 10;
    private final static double c172indicatedAirspeedLimit = 163;

    public C172IndicatedAirspeedEvent(String startTime, String endTime, int startLine, int endLine) {
        super(startTime, endTime, startLine, endLine, 5);
    }

    public static boolean isOccuring(ArrayList<String> lineValues) {
        double c172indicatedAirspeed = Double.parseDouble(lineValues.get(c172indicatedAirspeedColumn));

        if (c172indicatedAirspeed > c172indicatedAirspeedLimit) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return "C172 INDICATED AIR SPEED EVENT " + super.toString();
    }
}
