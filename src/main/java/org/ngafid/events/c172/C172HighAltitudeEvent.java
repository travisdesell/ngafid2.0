package org.ngafid.events.c172;
//package org.ngafid.events;

import java.util.ArrayList;

import org.ngafid.events.Event;

//condition: High Altitude: msl_altitude > 12800
public class C172HighAltitudeEvent extends Event {

    private final static int c172HighAltitudeColumn = 8;
    private final static double c172HighAltitudeLimit = 12800;

    public C172HighAltitudeEvent(String startTime, String endTime, int startLine, int endLine) {
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


