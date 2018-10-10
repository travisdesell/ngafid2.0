package org.ngafid.events.pa28;

import java.util.ArrayList;

import org.ngafid.events.Event;
//Condition: Low Oil Pressure: eng_1_oil_press < 25 AND eng_1_rpm > 100
public class PA28LowOilPressureEvent extends Event {

    private static final int pa28LowOilPressurePressColumn = 27;
    private static final double pa28LowOilPressurePressLimit = 59; // Correct value is 25. 59 is test only
    private static final int pa28LowOilPressureRpmColumn = 28;
    private static final double pa28LowOilPressureRpmLimit = 580.0; // the value 580 is only for test the correct value is 100.0

    public PA28LowOilPressureEvent(String startTime, String endTime, int startLine, int endLine) {
        super(startTime, endTime, startLine, endLine, 10);
    }

    public static boolean isOccuring(ArrayList<String> lineValues) {
        double pa28LowOilPressurePress = Double.parseDouble(lineValues.get(pa28LowOilPressurePressColumn));
        double pa28LowOilPressureRpm = Double.parseDouble(lineValues.get(pa28LowOilPressureRpmColumn));
        if ((pa28LowOilPressurePress < pa28LowOilPressurePressLimit) && (pa28LowOilPressureRpm > pa28LowOilPressureRpmLimit)) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return "PA28 LOW OIL PRESSURE EVENT " + super.toString();
    }
}
