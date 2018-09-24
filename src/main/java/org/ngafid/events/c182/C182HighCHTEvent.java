package org.ngafid.events;

import java.util.ArrayList;

import org.ngafid.events.Event;

//condition: High CHT: eng_1_cht_(1-4) > 500
public class C182HighCHTEvent extends Event {

    private final static int c182HighCHTColumn = 29;
    //private final static int c182HighCHTColumn2 = 30;
    //private final static int c182HighCHTColumn3 = 31;
    //private final static int c182HighCHTColumn4 = 32;

    private final static double c182HighCHTLimit = 500;

    public C182HighCHTEvent(String startTime, String endTime, int startLine, int endLine) {
        super(startTime, endTime, startLine, endLine, 10);
    }

    public static boolean isOccuring(ArrayList<String> lineValues) {
        double c182HighCHT = Double.parseDouble(lineValues.get(c182HighCHTColumn));
        if (c182HighCHT > c182HighCHTLimit) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return "C182 HIGH_CHT EVENT " + super.toString();
    }
}


