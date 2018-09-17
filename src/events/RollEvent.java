import java.util.ArrayList;

public class RollEvent extends Event {

    private static final int rollColumn = 14;
    private static final double rollLimit = 20.0;

    public RollEvent(String startTime, String endTime, int startLine, int endLine) {
        super(startTime, endTime, startLine, endLine, 5);
    }

    public static boolean isOccuring(ArrayList<String> lineValues) {
        double roll = Double.parseDouble(lineValues.get(rollColumn));
        if (Math.abs(roll) > rollLimit) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return "ROLL EVENT " + super.toString();
    }
}
