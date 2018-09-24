package org.ngafid.events.c172;
//package org.ngafid.events;

import java.util.ArrayList;

import org.ngafid.events.Event;

//condition: High CHT: eng_1_cht_(1-4) > 500
public class C172HighCHTEvent extends Event {

    private final static int c172HighCHTColumn = 29;
    //private final static int c172HighCHTColumn2 = 30;
    //private final static int c172HighCHTColumn3 = 31;
    //private final static int c172HighCHTColumn4 = 32;

    private final static double c172HighCHTLimit = 500;

    public C172HighCHTEvent(String startTime, String endTime, int startLine, int endLine) {
        super(startTime, endTime, startLine, endLine, 10);
    }

    public static boolean isOccuring(ArrayList<String> lineValues) {
        double c172HighCHT = Double.parseDouble(lineValues.get(c172HighCHTColumn));
        if (c172HighCHT > c172HighCHTLimit) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return "C172 HIGH_CHT EVENT " + super.toString();
    }
}


