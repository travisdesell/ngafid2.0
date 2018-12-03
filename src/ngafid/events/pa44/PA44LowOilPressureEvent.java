package org.ngafid.events.pa44;

import java.util.ArrayList;

import org.ngafid.events.Event;
//Condition: Low Oil Pressure: (eng_1_oil_press < 25 AND eng_1_rpm > 100) OR (eng_2_oil_press < 25 AND eng_2_rpm > 100)
public class PA44LowOilPressureEvent extends Event {

    private static final int pa44LowOilPressurePress1Column = 25;
    private static final double pa44LowOilPressurePress1Limit = 59; //Correct value is 20. 59 is test only
    private static final int pa44LowOilPressureRpm1Column = 27;
    private static final double pa44LowOilPressureRpm1Limit = 580.0; // the value 580 is only for test the correct value is 100.0

    private static final int pa44LowOilPressurePress2Column = 35;
    private static final double pa44LowOilPressurePress2Limit = 59; //Correct value is 20. 59 is test only
    private static final int pa44LowOilPressureRpm2Column = 37;
    private static final double pa44LowOilPressureRpm2Limit = 580.0; // the value 580 is only for test the correct value is 100.0

    public PA44LowOilPressureEvent(String startTime, String endTime, int startLine, int endLine) {
        super(startTime, endTime, startLine, endLine, 10);
    }

    public static boolean isOccuring(ArrayList<String> lineValues) {
        double pa44LowOilPressurePress1 = Double.parseDouble(lineValues.get(pa44LowOilPressurePress1Column));
        double pa44LowOilPressureRpm1 = Double.parseDouble(lineValues.get(pa44LowOilPressureRpm1Column));
        double pa44LowOilPressurePress2 = Double.parseDouble(lineValues.get(pa44LowOilPressurePress2Column));
        double pa44LowOilPressureRpm2 = Double.parseDouble(lineValues.get(pa44LowOilPressureRpm2Column));
        if (((pa44LowOilPressurePress1 < pa44LowOilPressurePress1Limit) && (pa44LowOilPressureRpm1 > pa44LowOilPressureRpm1Limit)) || ((pa44LowOilPressurePress2 < pa44LowOilPressurePress2Limit) && (pa44LowOilPressureRpm2 > pa44LowOilPressureRpm2Limit))) {
            return true;
        } else {
            return false;
        }
    }
    public String toString() {
        return "PA44 LOW OIL PRESSURE EVENT " + super.toString();
    }
}
