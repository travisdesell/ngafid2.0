package org.ngafid.events.c172;
//package org.ngafid.events;

import java.util.ArrayList;

import org.ngafid.events.Event;

//Condition: Low Fuel: fuel_quantity_left_main + fuel_quantity_right_main < 8
public class C172LowFuelEvent extends Event {

    private static final int c172LowFuelLeftColumn = 23;
    private static final int c172LowFuelRightColumn = 24;
    private static final double c172LowFuelLimit = 8;

    public C172LowFuelEvent(String startTime, String endTime, int startLine, int endLine) {
        super(startTime, endTime, startLine, endLine, 10);
    }

    public static boolean isOccuring(ArrayList<String> lineValues) {
        double c172LowFuelLeft = Double.parseDouble(lineValues.get(c172LowFuelLeftColumn));
        double c172LowFuelRight = Double.parseDouble(lineValues.get(c172LowFuelRightColumn));

        if ((c172LowFuelLeft + c172LowFuelRight) < c172LowFuelLimit ) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return "LOW FUEL EVENT " + super.toString();
    }
}
