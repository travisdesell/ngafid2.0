package src.events;

import java.util.ArrayList;

//condition: Low Fuel: fuel_quantity_left_main + fuel_quantity_right_main < 8.75
public class PA44LowFuelEvent extends Event {

    private static final int pa44LowFuelLeftColumn = 23;
    private static final int pa44LowFuelRightColumn = 24;
    private static final double pa44LowFuelLimit = 8.75;

    public PA44LowFuelEvent(String startTime, String endTime, int startLine, int endLine) {
        super(startTime, endTime, startLine, endLine, 10);
    }

    public static boolean isOccuring(ArrayList<String> lineValues) {
        double pa44LowFuelLeft = Double.parseDouble(lineValues.get(pa44LowFuelLeftColumn));
        double pa44LowFuelRight = Double.parseDouble(lineValues.get(pa44LowFuelRightColumn));

        if ((pa44LowFuelLeft + pa44LowFuelRight) < pa44LowFuelLimit ) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return "LOW FUEL EVENT " + super.toString();
    }
}