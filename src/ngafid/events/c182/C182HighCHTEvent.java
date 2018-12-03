package org.ngafid.events.c182;

import java.util.ArrayList;

import org.ngafid.events.Event;

//condition: High CHT: eng_1_cht_(1-6) > 500
public class C182HighCHTEvent extends Event {

    private final static int c182HighCHTColumn = 29;
    private final static int c182HighCHTColumn2 = 30;
    private final static int c182HighCHTColumn3 = 31;
    private final static int c182HighCHTColumn4 = 32;
    /* i commented the HighCHT column 4 and 5, because there is no flight file for */
    // private final static int c182HighCHTColumn5 = 33;
    // private final static int c182HighCHTColumn6 = 34;

    // When I am putting the limit as 250 or less as a limit, the event does not show in the  
    private final static double c182HighCHTLimit = 500;
    private final static double c182HighCHTLimit2 = 500;
    private final static double c182HighCHTLimit3 = 500;
    private final static double c182HighCHTLimit4 = 500;
    /* i commented the HighCHT limit 4 and 5, because there is no flight file for */
    // private final static double c182HighCHTLimit5 = 500;
    // private final static double c182HighCHTLimit6 = 500;

    public C182HighCHTEvent(String startTime, String endTime, int startLine, int endLine) {
        super(startTime, endTime, startLine, endLine, 5);
    }

    public static boolean isOccuring(ArrayList<String> lineValues) {
        double c182HighCHT = Double.parseDouble(lineValues.get(c182HighCHTColumn));
        double c182HighCHT2 = Double.parseDouble(lineValues.get(c182HighCHTColumn2));
        double c182HighCHT3 = Double.parseDouble(lineValues.get(c182HighCHTColumn3));
        double c182HighCHT4 = Double.parseDouble(lineValues.get(c182HighCHTColumn4));
        /* i commented the HighCHT 4 and 5, because there is no flight file for */
        // double c182HighCHT5 = Double.parseDouble(lineValues.get(c182HighCHTColumn5));
        // double c182HighCHT6 = Double.parseDouble(lineValues.get(c182HighCHTColumn6));       
        if (c182HighCHT > c182HighCHTLimit || c182HighCHT2 > c182HighCHTLimit2 || c182HighCHT3 > c182HighCHTLimit3 || c182HighCHT4 > c182HighCHTLimit4 /*|| c182HighCHT5 > c182HighCHTLimit5 || c182HighCHT6 > c182HighCHTLimit6*/) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return "C182 HIGH_CHT EVENT " + super.toString();
    }
}

