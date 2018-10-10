package org.ngafid.events.pa44;

import java.util.ArrayList;

import org.ngafid.events.Event;

//condition: High CHT: eng_1_cht_(1-2) > 500
public class PA44HighCHTEvent extends Event {

    private final static int pa44HighCHTColumn = 29;
    private final static int pa44HighCHTColumn2 = 30;

    // When I am putting the limit as 250 or less as a limit, the event does not show in the  
    private final static double pa44HighCHTLimit = 500;
    private final static double pa44HighCHTLimit2 = 500;

    public PA44HighCHTEvent(String startTime, String endTime, int startLine, int endLine) {
        super(startTime, endTime, startLine, endLine, 5);
    }

    public static boolean isOccuring(ArrayList<String> lineValues) {
        double pa44HighCHT = Double.parseDouble(lineValues.get(pa44HighCHTColumn));
        double pa44HighCHT2 = Double.parseDouble(lineValues.get(pa44HighCHTColumn2));

        if (pa44HighCHT > pa44HighCHTLimit || pa44HighCHT2 > pa44HighCHTLimit2) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return "PA44 HIGH_CHT EVENT " + super.toString();
    }
}


