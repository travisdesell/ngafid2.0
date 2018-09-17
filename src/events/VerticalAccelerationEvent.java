import java.util.ArrayList;

public class VerticalAccelerationEvent extends Event {

    private static final int verticalAccelerationColumn = 16;
    private static final double verticalAccelerationLowLimit = -1.76;
    private static final double verticalAccelerationHighLimit = 4.4;

    public VerticalAccelerationEvent(String startTime, String endTime, int startLine, int endLine) {
        super(startTime, endTime, startLine, endLine, 5);
    }

    public static boolean isOccuring(ArrayList<String> lineValues) {
        double verticalAcceleration = Double.parseDouble(lineValues.get(verticalAccelerationColumn));

        if (verticalAcceleration < verticalAccelerationLowLimit || verticalAcceleration > verticalAccelerationHighLimit) {
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return "VERTICAL ACCELERATION EVENT " + super.toString();
    }
}
