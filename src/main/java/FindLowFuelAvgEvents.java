import org.ngafid.Database;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class FindLowFuelAvgEvents {
    public static final Connection connection = Database.getConnection();
    public static final Logger LOG = Logger.getLogger(FindLowFuelAvgEvents.class.getName());
    private static final Map<Integer, Double> FUEL_THRESHOLDS = new HashMap<>();

    static {
        FUEL_THRESHOLDS.put(1, 8.25);
        FUEL_THRESHOLDS.put(1, 8.00);
        FUEL_THRESHOLDS.put(1, 17.56);
    }

}
