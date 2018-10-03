package org.ngafid.events.c182;

import java.util.ArrayList;

import org.ngafid.events.Event;

//condition: Low Fuel: fuel_quantity_left_main + fuel_quantity_right_main < 8
public class C182LowFuelEvent extends Event {

    private static final int c182LowFuelLeftColumn = 23;
    private static final int c182LowFuelRightColumn = 24;
    private static final double c182LowFuelLimit = 8;

    public C182LowFuelEvent(String startTime, String endTime, int startLine, int endLine) {
        super(startTime, endTime, startLine, endLine, 10);
    }

    public static boolean isOccuring(ArrayList<String> lineValues) {
        double c182LowFuelLeft = Double.parseDouble(lineValues.get(c182LowFuelLeftColumn));
        double c182LowFuelRight = Double.parseDouble(lineValues.get(c182LowFuelRightColumn));

        if ((c182LowFuelLeft + c182LowFuelRight) < c182LowFuelLimit ) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return "LOW FUEL EVENT " + super.toString();
    }
}
