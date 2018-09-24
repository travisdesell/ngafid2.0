//package src.events.c172;
/*
package src.events;

import java.util.ArrayList;

//Condition: Low Airspeed on Climout: indicated_airspeed < 52 AND vertical_airspeed > 0 AND (radio_altitude_derived BETWEEN 100 AND 500)
public class C172LowAirspeedOnClimboutEvent extends Event {

    private static final int c172IndicatedAirspeedColumn = 10;
    private static final int c172VerticalAirspeedColumn = 12;
    private static final int c172RadioAltitudeColumn = 6;
    private static final double c172AirspeedLimit = 52;
    private static final double c172VertialAirspeedLimit = 0;
    private static final double c172RadioAltitudeDerivedLowLimit = 100;
    private static final double c172RadioAltitudeDerivedHighLimit = 500;

    public C172LowAirspeedOnClimboutEvent(String startTime, String endTime, int startLine, int endLine) {
        super(startTime, endTime, startLine, endLine, 5);
    }

    public static boolean isOccuring(ArrayList<String> lineValues) {
        double c172IndicatedAirspeedOn = Double.parseDouble(lineValues.get(c172IndicatedAirspeedColumn));
        double c172VerticalAirspeedOn = Double.parseDouble(lineValues.get(c172VerticalAirspeedColumn));
        double c172RadioAltitude = Double.parseDouble(lineValues.get(c172RadioAltitudeColumn));

        if ((c172IndicatedAirspeedOn < c172AirspeedLimit & c172VerticalAirspeed > c172VertialAirspeedLimit) && (c172RadioAltitude < c172RadioAltitudeDerivedLowLimit & c172RadioAltitude > c172RadioAltitudeDerivedHighLimit)) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return "LOW AIR SPEED ON APPROACH EVENT " + super.toString();
    }
}
*/

//package src.events.c172;
package src.events;

import java.util.ArrayList;

//condition: High Altitude: msl_altitude > 12800
public class C172LowAirspeedOnClimboutEvent extends Event {

    private final static int c172HighAltitudeColumn = 8;
    private final static double c172HighAltitudeLimit = 12800;

    public C172LowAirspeedOnClimboutEvent(String startTime, String endTime, int startLine, int endLine) {
        super(startTime, endTime, startLine, endLine, 10);
    }

    public static boolean isOccuring(ArrayList<String> lineValues) {
        double c172HighAltitude = Double.parseDouble(lineValues.get(c172HighAltitudeColumn));
        if (c172HighAltitude > c172HighAltitudeLimit) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return "C172 HIGH ALTITUDE EVENT " + super.toString();
    }
}


