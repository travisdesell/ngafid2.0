import org.ngafid.Database;
import org.ngafid.events.CustomEvent;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.Flight;
import org.ngafid.flights.MalformedFlightFileException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.ngafid.flights.CalculationParameters.LOW_FUEL;

public class FindLowFuelAvgEvents {
    public static final Connection connection = Database.getConnection();
    public static final Logger LOG = Logger.getLogger(FindLowFuelAvgEvents.class.getName());
    private static final Map<Integer, Double> FUEL_THRESHOLDS = new HashMap<>();

    static {
        FUEL_THRESHOLDS.put(1, 8.25);
        FUEL_THRESHOLDS.put(1, 8.00);
        FUEL_THRESHOLDS.put(1, 17.56);
    }

    public static void findLowFuelAvgEvents(Flight flight) throws SQLException, MalformedFlightFileException {
        double threshold = FUEL_THRESHOLDS.get(flight.getAirframeTypeId());

        flight.checkCalculationParameters(LOW_FUEL, LOW_FUEL);

        List<CustomEvent> lowFuel = new ArrayList<>();
        int hadError = 0;

        DoubleTimeSeries fuel = flight.getDoubleTimeSeries(connection, LOW_FUEL);

        for (int i = 0; i < flight.getNumberRows(); i++) {

        }

    }

    public static void main(String[] args) {

    }

}
