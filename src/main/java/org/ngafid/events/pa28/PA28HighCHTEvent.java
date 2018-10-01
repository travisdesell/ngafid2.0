package org.ngafid.events.pa28;

import java.util.ArrayList;

import org.ngafid.events.Event;

//condition: High CHT: N.A.
public class PA28HighCHTEvent extends Event {

    private final static int pa28HighCHTColumn = 29;
    //private final static int pa28HighCHTColumn2 = 30;
    //private final static int pa28HighCHTColumn3 = 31;
    //private final static int pa28HighCHTColumn4 = 32;

    private final static double pa28HighCHTLimit = 500;

    public PA28HighCHTEvent(String startTime, String endTime, int startLine, int endLine) {
        super(startTime, endTime, startLine, endLine, 10);
    }

    public static boolean isOccuring(ArrayList<String> lineValues) {
        double pa28HighCHT = Double.parseDouble(lineValues.get(pa28HighCHTColumn));
        if (pa28HighCHT > pa28HighCHTLimit) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return "PA28 HIGH_CHT EVENT " + super.toString();
    }
}


