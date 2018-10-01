package org.ngafid.events.sr20;

import java.util.ArrayList;

import org.ngafid.events.Event;

//Condition: Low Oil Pressure: eng_1_oil_press < 20 AND eng_1_rpm > 100
public class SR20LowOilPressureEvent extends Event {

    private static final int sr20LowOilPressurePressColumn = 27;
    private static final int sr20LowOilPressureRpmColumn = 28;
    private static final double sr20LowOilPressurePressLimit = 20;
    private static final double sr20LowOilPressureRpmLimit = 100;

    public SR20LowOilPressureEvent(String startTime, String endTime, int startLine, int endLine) {
        super(startTime, endTime, startLine, endLine, 10);
    }

    public static boolean isOccuring(ArrayList<String> lineValues) {
        double sr20LowOilPressurePress = Double.parseDouble(lineValues.get(sr20LowOilPressurePressColumn));
        double sr20LowOilPressureRpm = Double.parseDouble(lineValues.get(sr20LowOilPressureRpmColumn));

        if (sr20LowOilPressurePress < sr20LowOilPressurePressLimit && sr20LowOilPressureRpm > sr20LowOilPressureRpmLimit) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return "LOW OIL PRESSURE EVENT " + super.toString();
    }
}
