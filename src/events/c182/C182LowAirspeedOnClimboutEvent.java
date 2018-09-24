package src.events;

import java.util.ArrayList;

//Condition: Low Airspeed on Climbout: indicated_airspeed < 52 AND vertical_airspeed > 0 AND (radio_altitude_derived BETWEEN 100 AND 500)
public class C182LowAirspeedOnClimboutEvent extends Event {

    private static final int c182IndicatedAirspeedColumn = 10;
    private static final int c182VerticalAirspeedColumn = 12;
    private static final int c182RadioAltitudeColumn = 6;
    private static final double c182AirspeedLimit = 52;
    private static final double c182VertialAirspeedLimit = 0;
    private static final double c182RadioAltitudeDerivedLowLimit = 100;
    private static final double c182RadioAltitudeDerivedHighLimit = 500;

    public C182LowAirspeedOnClimboutEvent(String startTime, String endTime, int startLine, int endLine) {
        super(startTime, endTime, startLine, endLine, 5);
    }

    public static boolean isOccuring(ArrayList<String> lineValues) {
        double c182IndicatedAirspeedOn = Double.parseDouble(lineValues.get(c182IndicatedAirspeedColumn));
        double c182VerticalAirspeedOn = Double.parseDouble(lineValues.get(c182VerticalAirspeedColumn));
        double c182RadioAltitude = Double.parseDouble(lineValues.get(c182RadioAltitudeColumn));

        if ((c182IndicatedAirspeedOn < c182AirspeedLimit & c182VerticalAirspeed > c182VertialAirspeedLimit) && (c182RadioAltitude < c182RadioAltitudeDerivedLowLimit & c182RadioAltitude > c182RadioAltitudeDerivedHighLimit)) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return "LOW AIR SPEED ON APPROACH EVENT " + super.toString();
    }
}
