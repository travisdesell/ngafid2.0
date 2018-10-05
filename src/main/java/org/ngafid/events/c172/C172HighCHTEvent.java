package org.ngafid.events.c172;

import java.util.ArrayList;

import org.ngafid.events.Event;

//condition: High CHT: eng_1_cht_(1-4) > 500
public class C172HighCHTEvent extends Event {

    private final static int c172HighCHTColumn = 29;
    private final static int c172HighCHTColumn2 = 30;
    private final static int c172HighCHTColumn3 = 31;
    private final static int c172HighCHTColumn4 = 32;

    // When I am putting the limit as 250 or less as a limit, the event does not show in the  
    private final static double c172HighCHTLimit = 500;
    private final static double c172HighCHTLimit2 = 500;
    private final static double c172HighCHTLimit3 = 500;
    private final static double c172HighCHTLimit4 = 500;

    public C172HighCHTEvent(String startTime, String endTime, int startLine, int endLine) {
        super(startTime, endTime, startLine, endLine, 5);
    }

    public static boolean isOccuring(ArrayList<String> lineValues) {
        double c172HighCHT = Double.parseDouble(lineValues.get(c172HighCHTColumn));
        double c172HighCHT2 = Double.parseDouble(lineValues.get(c172HighCHTColumn2));
        double c172HighCHT3 = Double.parseDouble(lineValues.get(c172HighCHTColumn3));
        double c172HighCHT4 = Double.parseDouble(lineValues.get(c172HighCHTColumn4));
        if (c172HighCHT > c172HighCHTLimit || c172HighCHT2 > c172HighCHTLimit2 || c172HighCHT3 > c172HighCHTLimit3 || c172HighCHT4 > c172HighCHTLimit4) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return "C172 HIGH_CHT EVENT " + super.toString();
    }
}


