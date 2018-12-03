package org.ngafid.events.pa28;

import java.util.ArrayList;

import org.ngafid.events.Event;

//condition: High CHT: N.A.
public class PA28HighCHTEvent extends Event {

    private final static int pa28HighCHTColumn = 29;
    private final static int pa28HighCHTColumn2 = 30;
    private final static int pa28HighCHTColumn3 = 31;
    private final static int pa28HighCHTColumn4 = 32;

    // When I am putting the limit as 250 or less as a limit, the event does not show in the  
    private final static double pa28HighCHTLimit = 500;
    private final static double pa28HighCHTLimit2 = 500;
    private final static double pa28HighCHTLimit3 = 500;
    private final static double pa28HighCHTLimit4 = 500;

    public PA28HighCHTEvent(String startTime, String endTime, int startLine, int endLine) {
        super(startTime, endTime, startLine, endLine, 5);
    }

    public static boolean isOccuring(ArrayList<String> lineValues) {
        double pa28HighCHT = Double.parseDouble(lineValues.get(pa28HighCHTColumn));
        double pa28HighCHT2 = Double.parseDouble(lineValues.get(pa28HighCHTColumn2));
        double pa28HighCHT3 = Double.parseDouble(lineValues.get(pa28HighCHTColumn3));
        double pa28HighCHT4 = Double.parseDouble(lineValues.get(pa28HighCHTColumn4));
        if (pa28HighCHT > pa28HighCHTLimit || pa28HighCHT2 > pa28HighCHTLimit2 || pa28HighCHT3 > pa28HighCHTLimit3 || pa28HighCHT4 > pa28HighCHTLimit4) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return "pa28 HIGH_CHT EVENT " + super.toString();
    }
}
