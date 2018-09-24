package src.events;

import java.util.ArrayList;

//condition: High Altitude: msl_altitude > 12800
public class PA28HighAltitudeEvent extends Event {

    private final static int pa28HighAltitudeColumn = 8;
    private final static double pa28HighAltitudeLimit = 12800;

    public PA28HighAltitudeEvent(String startTime, String endTime, int startLine, int endLine) {
        super(startTime, endTime, startLine, endLine, 10);
    }

    public static boolean isOccuring(ArrayList<String> lineValues) {
        double pa28HighAltitude = Double.parseDouble(lineValues.get(pa28HighAltitudeColumn));
        if (pa28HighAltitude > pa28HighAltitudeLimit) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return "PA28 HIGH ALTITUDE EVENT " + super.toString();
    }
}


