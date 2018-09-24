package src.events;

import java.util.ArrayList;

//condition: High Altitude: msl_altitude > 12800
public class PA44HighAltitudeEvent extends Event {

    private final static int pa44HighAltitudeColumn = 8;
    private final static double pa44HighAltitudeLimit = 12800;

    public PA44HighAltitudeEvent(String startTime, String endTime, int startLine, int endLine) {
        super(startTime, endTime, startLine, endLine, 10);
    }

    public static boolean isOccuring(ArrayList<String> lineValues) {
        double pa44HighAltitude = Double.parseDouble(lineValues.get(pa44HighAltitudeColumn));
        if (pa44HighAltitude > pa44HighAltitudeLimit) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return "PA44 HIGH ALTITUDE EVENT " + super.toString();
    }
}


