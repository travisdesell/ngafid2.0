package org.ngafid.events.pa44;

import java.util.ArrayList;

import org.ngafid.events.Event;

//Condition: Low Airspeed on Approach: indicated_airspeed < 66 AND vertical_airspeed < 0 AND (radio_altitude_derived BETWEEN 100 AND 500)
public class PA44LowAirspeedOnApproachEvent extends Event {

    private static final int pa44IndicatedAirspeedColumn = 10;
    private static final double pa44AirspeedLimit = 66.0;

    private static final int pa44VerticalAirspeedColumn = 12;
    private static final double pa44VerticalAirspeedLimit = 0;

    private static final int pa44RadioAltitudeColumn = 6;
    private static final double pa44RadioAltitudeDerivedLowLimit = 860; // 860 and 861  is only test number. The actual number is 100
    private static final double pa44RadioAltitudeDerivedHighLimit = 861; // 861 and 861  is only test number. The actual number is 500

    public PA44LowAirspeedOnApproachEvent(String startTime, String endTime, int startLine, int endLine) {
        super(startTime, endTime, startLine, endLine, 5);
    }

    public static boolean isOccuring(ArrayList<String> lineValues) {
        double pa44IndicatedAirspeed = Double.parseDouble(lineValues.get(pa44IndicatedAirspeedColumn));
        double pa44VerticalAirspeed = Double.parseDouble(lineValues.get(pa44VerticalAirspeedColumn));
        double pa44RadioAltitude = Double.parseDouble(lineValues.get(pa44RadioAltitudeColumn));

        if (((pa44RadioAltitude > pa44RadioAltitudeDerivedLowLimit) && (pa44RadioAltitude < pa44RadioAltitudeDerivedHighLimit))&& (pa44IndicatedAirspeed < pa44AirspeedLimit) && (pa44VerticalAirspeed < pa44VerticalAirspeedLimit)){
            return true;
        } else {
            return false;
        }

    }
    
    public String toString() {
        return "PA44 LOW AIR SPEED ON APPROACH EVENT " + super.toString();
    }
}
