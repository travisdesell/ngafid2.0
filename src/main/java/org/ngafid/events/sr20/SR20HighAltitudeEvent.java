package org.ngafid.events;

import java.util.ArrayList;

import org.ngafid.events.Event;

//condition: High Altitude: msl_altitude > 12800
public class SR20HighAltitudeEvent extends Event {

    private final static int sr20HighAltitudeColumn = 8;
    private final static double sr20HighAltitudeLimit = 12800;

    public SR20HighAltitudeEvent(String startTime, String endTime, int startLine, int endLine) {
        super(startTime, endTime, startLine, endLine, 10);
    }

    public static boolean isOccuring(ArrayList<String> lineValues) {
        double sr20HighAltitude = Double.parseDouble(lineValues.get(sr20HighAltitudeColumn));
        if (sr20HighAltitude > sr20HighAltitudeLimit) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return "SR20 HIGH_CHT EVENT " + super.toString();
    }
}


