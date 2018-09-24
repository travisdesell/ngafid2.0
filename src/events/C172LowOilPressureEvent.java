//package src.events.c172;
package src.events;

import java.util.ArrayList;
//Condition: Low Oil Pressure: eng_1_oil_press < 20 AND eng_1_rpm > 100
public class C172LowOilPressureEvent extends Event {

    private static final int c172LowOilPressurePressColumn = 27;
    private static final int c172LowOilPressureRpmColumn = 28;
    private static final double c172LowOilPressurePressLimit = 20;
    private static final double c172LowOilPressureRpmLimit = 100;

    public C172LowOilPressureEvent(String startTime, String endTime, int startLine, int endLine) {
        super(startTime, endTime, startLine, endLine, 10);
    }

    public static boolean isOccuring(ArrayList<String> lineValues) {
        double c172LowOilPressurePress = Double.parseDouble(lineValues.get(c172LowOilPressurePressColumn));
        double c172LowOilPressureRpm = Double.parseDouble(lineValues.get(c172LowOilPressureRpmColumn));

        if (c172LowOilPressurePress < c172LowOilPressurePressLimit && c172LowOilPressureRpm > c172LowOilPressureRpmLimit) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return "LOW OIL PRESSURE EVENT " + super.toString();
    }
}
