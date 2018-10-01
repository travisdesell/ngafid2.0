package org.ngafid.events.pa44;

import java.util.ArrayList;

import org.ngafid.events.Event;
//Condition: Low Oil Pressure: eng_1_oil_press < 20 AND eng_1_rpm > 100
public class PA44LowOilPressureEvent extends Event {

    private static final int pa44LowOilPressurePressColumn = 27;
    private static final int pa44LowOilPressureRpmColumn = 28;
    private static final double pa44LowOilPressurePressLimit = 20;
    private static final double pa44LowOilPressureRpmLimit = 100;

    public PA44LowOilPressureEvent(String startTime, String endTime, int startLine, int endLine) {
        super(startTime, endTime, startLine, endLine, 10);
    }

    public static boolean isOccuring(ArrayList<String> lineValues) {
        double pa44LowOilPressurePress = Double.parseDouble(lineValues.get(pa44LowOilPressurePressColumn));
        double pa44LowOilPressureRpm = Double.parseDouble(lineValues.get(pa44LowOilPressureRpmColumn));

        if (pa44LowOilPressurePress < pa44LowOilPressurePressLimit && pa44LowOilPressureRpm > pa44LowOilPressureRpmLimit) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return "LOW OIL PRESSURE EVENT " + super.toString();
    }
}
