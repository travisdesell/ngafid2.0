package org.ngafid.events.sr20;

import java.util.ArrayList;

import org.ngafid.events.Event;

//condition: High CHT: eng_1_cht_(1-6) > 460
public class SR20HighCHTEvent extends Event {

    private final static int sr20HighCHTColumn = 29;
    private final static int sr20HighCHTColumn2 = 30;
    private final static int sr20HighCHTColumn3 = 31;
    private final static int sr20HighCHTColumn4 = 32;
    /* i commented the HighCHT column 4 and 5, because there is no flight file for */
    // private final static int sr20HighCHTColumn5 = 33;
    // private final static int sr20HighCHTColumn6 = 34;

    // When I am putting the limit as 250 or less as a limit, the event does not show in the  
    private final static double sr20HighCHTLimit = 460;
    private final static double sr20HighCHTLimit2 = 460;
    private final static double sr20HighCHTLimit3 = 460;
    private final static double sr20HighCHTLimit4 = 460;
    /* i commented the HighCHT limit 4 and 5, because there is no flight file for */
    // private final static double sr20HighCHTLimit5 = 460;
    // private final static double sr20HighCHTLimit6 = 460;

    public SR20HighCHTEvent(String startTime, String endTime, int startLine, int endLine) {
        super(startTime, endTime, startLine, endLine, 5);
    }

    public static boolean isOccuring(ArrayList<String> lineValues) {
        double sr20HighCHT = Double.parseDouble(lineValues.get(sr20HighCHTColumn));
        double sr20HighCHT2 = Double.parseDouble(lineValues.get(sr20HighCHTColumn2));
        double sr20HighCHT3 = Double.parseDouble(lineValues.get(sr20HighCHTColumn3));
        double sr20HighCHT4 = Double.parseDouble(lineValues.get(sr20HighCHTColumn4));
        /* i commented the HighCHT 4 and 5, because there is no flight file for */
        // double sr20HighCHT5 = Double.parseDouble(lineValues.get(sr20HighCHTColumn5));
        // double sr20HighCHT6 = Double.parseDouble(lineValues.get(sr20HighCHTColumn6));
        if (sr20HighCHT > sr20HighCHTLimit || sr20HighCHT2 > sr20HighCHTLimit2 || sr20HighCHT3 > sr20HighCHTLimit3 || sr20HighCHT4 > sr20HighCHTLimit4 /*|| sr20HighCHT5 > sr20HighCHTLimit5 || sr20HighCHT6 > sr20HighCHTLimit6*/) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return "SR20 HIGH_CHT EVENT " + super.toString();
    }
}
