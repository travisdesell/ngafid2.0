package org.ngafid.events;

import java.util.ArrayList;

import org.ngafid.events.Event;

//condition: Low Fuel: fuel_quantity_left_main + fuel_quantity_right_main < 8.25
public class PA28LowFuelEvent extends Event {

    private static final int pa28LowFuelLeftColumn = 23;
    private static final int pa28LowFuelRightColumn = 24;
    private static final double pa28LowFuelLimit = 8.25;

    public PA28LowFuelEvent(String startTime, String endTime, int startLine, int endLine) {
        super(startTime, endTime, startLine, endLine, 10);
    }

    public static boolean isOccuring(ArrayList<String> lineValues) {
        double pa28LowFuelLeft = Double.parseDouble(lineValues.get(pa28LowFuelLeftColumn));
        double pa28LowFuelRight = Double.parseDouble(lineValues.get(pa28LowFuelRightColumn));

        if ((pa28LowFuelLeft + pa28LowFuelRight) < pa28LowFuelLimit ) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return "LOW FUEL EVENT " + super.toString();
    }
}