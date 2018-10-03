package org.ngafid.events.c172;
//package org.ngafid.events;

import java.util.ArrayList;

import org.ngafid.events.Event;

//Condition: Low Airspeed on Approach: indicated_airspeed < 57 AND vertical_airspeed < 0 AND (radio_altitude_derived BETWEEN 100 AND 500)
public class C172LowAirspeedOnApproachEvent extends Event {

    private static final int c172IndicatedAirspeedColumn = 10;
    private static final double c172AirspeedLimit = 57.0;

    private static final int c172VerticalAirspeedColumn = 12;
    private static final double c172VerticalAirspeedLimit = 0;

    private static final int c172RadioAltitudeColumn = 6;

    //private static final double c172RadioAltitudeDerivedLowLimit = 100;
    // 860 and 861  is only test number. The actual number is line above
    private static final double c172RadioAltitudeDerivedLowLimit = 860;
    //private static final double c172RadioAltitudeDerivedHighLimit = 500;
    private static final double c172RadioAltitudeDerivedHighLimit = 861;

    public C172LowAirspeedOnApproachEvent(String startTime, String endTime, int startLine, int endLine) {
        super(startTime, endTime, startLine, endLine, 5);
    }

    public static boolean isOccuring(ArrayList<String> lineValues) {
        double c172IndicatedAirspeed = Double.parseDouble(lineValues.get(c172IndicatedAirspeedColumn));

        double c172VerticalAirspeed = Double.parseDouble(lineValues.get(c172VerticalAirspeedColumn));

        double c172RadioAltitude = Double.parseDouble(lineValues.get(c172RadioAltitudeColumn));


        if (((c172RadioAltitude > c172RadioAltitudeDerivedLowLimit) && (c172RadioAltitude < c172RadioAltitudeDerivedHighLimit))&& (c172IndicatedAirspeed < c172AirspeedLimit) && (c172VerticalAirspeed < c172VerticalAirspeedLimit)){
            return true;
        } else {
            return false;
        }

    }
    
    

    public String toString() {
        return "LOW AIR SPEED ON APPROACH EVENT " + super.toString();
    }
}
