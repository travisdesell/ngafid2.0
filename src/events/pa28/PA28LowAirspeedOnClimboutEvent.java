package src.events;

import java.util.ArrayList;

//Condition: Low Airspeed on Climbout: indicated_airspeed < 52 AND vertical_airspeed > 0 AND (radio_altitude_derived BETWEEN 100 AND 500)
public class PA28LowAirspeedOnClimboutEvent extends Event {

    private static final int pa28IndicatedAirspeedColumn = 10;
    private static final int pa28VerticalAirspeedColumn = 12;
    private static final int pa28RadioAltitudeColumn = 6;
    private static final double pa28AirspeedLimit = 52;
    private static final double pa28VertialAirspeedLimit = 0;
    private static final double pa28RadioAltitudeDerivedLowLimit = 100;
    private static final double pa28RadioAltitudeDerivedHighLimit = 500;

    public PA28LowAirspeedOnClimboutEvent(String startTime, String endTime, int startLine, int endLine) {
        super(startTime, endTime, startLine, endLine, 5);
    }

    public static boolean isOccuring(ArrayList<String> lineValues) {
        double pa28IndicatedAirspeedOn = Double.parseDouble(lineValues.get(pa28IndicatedAirspeedColumn));
        double pa28VerticalAirspeedOn = Double.parseDouble(lineValues.get(pa28VerticalAirspeedColumn));
        double pa28RadioAltitude = Double.parseDouble(lineValues.get(pa28RadioAltitudeColumn));

        if ((pa28IndicatedAirspeedOn < pa28AirspeedLimit & pa28VerticalAirspeed > pa28VertialAirspeedLimit) && (pa28RadioAltitude < pa28RadioAltitudeDerivedLowLimit & pa28RadioAltitude > pa28RadioAltitudeDerivedHighLimit)) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return "LOW AIR SPEED ON APPROACH EVENT " + super.toString();
    }
}
