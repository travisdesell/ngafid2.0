package src.events;

import java.util.ArrayList;

//Condition: Low Airspeed on Climbout: indicated_airspeed < 52 AND vertical_airspeed > 0 AND (radio_altitude_derived BETWEEN 100 AND 500)
public class PA44LowAirspeedOnClimboutEvent extends Event {

    private static final int pa44IndicatedAirspeedColumn = 10;
    private static final int pa44VerticalAirspeedColumn = 12;
    private static final int pa44RadioAltitudeColumn = 6;
    private static final double pa44AirspeedLimit = 52;
    private static final double pa44VertialAirspeedLimit = 0;
    private static final double pa44RadioAltitudeDerivedLowLimit = 100;
    private static final double pa44RadioAltitudeDerivedHighLimit = 500;

    public PA44LowAirspeedOnClimboutEvent(String startTime, String endTime, int startLine, int endLine) {
        super(startTime, endTime, startLine, endLine, 5);
    }

    public static boolean isOccuring(ArrayList<String> lineValues) {
        double pa44IndicatedAirspeedOn = Double.parseDouble(lineValues.get(pa44IndicatedAirspeedColumn));
        double pa44VerticalAirspeedOn = Double.parseDouble(lineValues.get(pa44VerticalAirspeedColumn));
        double pa44RadioAltitude = Double.parseDouble(lineValues.get(pa44RadioAltitudeColumn));

        if ((pa44IndicatedAirspeedOn < pa44AirspeedLimit & pa44VerticalAirspeed > pa44VertialAirspeedLimit) && (pa44RadioAltitude < pa44RadioAltitudeDerivedLowLimit & pa44RadioAltitude > pa44RadioAltitudeDerivedHighLimit)) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return "LOW AIR SPEED ON APPROACH EVENT " + super.toString();
    }
}
