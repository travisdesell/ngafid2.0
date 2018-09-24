package src.events;

import java.util.ArrayList;

//condition: High Altitude: msl_altitude > 15000
public class C182HighAltitudeEvent extends Event {

    private final static int c182HighAltitudeColumn = 8;
    private final static double c182HighAltitudeLimit = 15000;

    public C182HighAltitudeEvent(String startTime, String endTime, int startLine, int endLine) {
        super(startTime, endTime, startLine, endLine, 10);
    }

    public static boolean isOccuring(ArrayList<String> lineValues) {
        double c182HighAltitude = Double.parseDouble(lineValues.get(c182HighAltitudeColumn));
        if (c182HighAltitude > c182HighAltitudeLimit) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return "C182 HIGHT ALTITUDE EVENT " + super.toString();
    }
}


