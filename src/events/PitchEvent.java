package events;

import java.util.ArrayList;

public class PitchEvent extends Event {

    private final static int pitchColumn = 13;
    private final static double pitchLimit = 10.0;

    public PitchEvent(String startTime, String endTime, int startLine, int endLine) {
        super(startTime, endTime, startLine, endLine, 10);
    }

    public static boolean isOccuring(ArrayList<String> lineValues) {
        double pitch = Double.parseDouble(lineValues.get(pitchColumn));
        if (Math.abs(pitch) > pitchLimit) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return "PITCH EVENT " + super.toString();
    }
}
