package src.events;

import java.util.ArrayList;

//condition: High CHT: eng_1_cht_(1-4) > 460
public class SR20HighCHTEvent extends Event {

    private final static int sr20HighCHTColumn = 29;
    // private final static int sr20HighCHTColumn1 = 30;
    // private final static int sr20HighCHTColumn2 = 31;
    // private final static int sr20HighCHTColumn3 = 32;

    private final static double sr20HighCHTLimit = 460;

    public SR20HighCHTEvent(String startTime, String endTime, int startLine, int endLine) {
        super(startTime, endTime, startLine, endLine, 10);
    }

    public static boolean isOccuring(ArrayList<String> lineValues) {
        double sr20HighCHT = Double.parseDouble(lineValues.get(sr20HighCHTColumn));
        if (sr20HighCHT > sr20HighCHTLimit) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return "SR20 HIGH_CHT EVENT " + super.toString();
    }
}


