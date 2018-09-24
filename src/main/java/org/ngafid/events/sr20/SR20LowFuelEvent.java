package org.ngafid.events;

import java.util.ArrayList;

import org.ngafid.events.Event;

//condition: Low Fuel: N.A.
public class SR20LowFuelEvent extends Event {

    private static final int sr20LowFuelLeftColumn = 23;
    private static final int sr20LowFuelRightColumn = 24;
    private static final double sr20LowFuelLimit = 8;

    public SR20LowFuelEvent(String startTime, String endTime, int startLine, int endLine) {
        super(startTime, endTime, startLine, endLine, 10);
    }

    public static boolean isOccuring(ArrayList<String> lineValues) {
        double sr20LowFuelLeft = Double.parseDouble(lineValues.get(sr20LowFuelLeftColumn));
        double sr20LowFuelRight = Double.parseDouble(lineValues.get(sr20LowFuelRightColumn));

        if ((sr20LowFuelLeft + sr20LowFuelRight) < sr20LowFuelLimit ) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return "LOW FUEL EVENT " + super.toString();
    }
}
