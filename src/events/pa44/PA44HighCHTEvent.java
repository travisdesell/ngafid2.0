package src.events;

import java.util.ArrayList;

//condition: High CHT: eng_1_cht_(1-4) > 500
public class PA44HighCHTEvent extends Event {

    private final static int pa44HighCHTColumn = 29;
    // private final static int pa44HighCHTColumn2 = 30;
    // private final static int pa44HighCHTColumn3 = 31;
    // private final static int pa44HighCHTColumn4 = 32;

    private final static double pa44HighCHTLimit = 500;

    public PA44HighCHTEvent(String startTime, String endTime, int startLine, int endLine) {
        super(startTime, endTime, startLine, endLine, 10);
    }

    public static boolean isOccuring(ArrayList<String> lineValues) {
        double pa44HighCHT = Double.parseDouble(lineValues.get(pa44HighCHTColumn));
        if (pa44HighCHT > pa44HighCHTLimit) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return "PA44 HIGH_CHT EVENT " + super.toString();
    }
}


