package org.ngafid;

import org.ngafid.common.TimeSeriesNode;
import org.ngafid.common.TimeSeriesQueue;
import org.ngafid.common.TimeUtils;
import org.ngafid.events.CustomEvent;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.Flight;
import org.ngafid.flights.MalformedFlightFileException;
import org.ngafid.flights.StringTimeSeries;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

import static org.ngafid.flights.CalculationParameters.*;

public class FindLowFuelAvgEvents {
    public static final Connection connection = Database.getConnection();
    public static final Logger LOG = Logger.getLogger(FindLowFuelAvgEvents.class.getName());
    private static final Map<Integer, Double> FUEL_THRESHOLDS = new HashMap<>();

    static {
        FUEL_THRESHOLDS.put(1, 8.25);
        FUEL_THRESHOLDS.put(1, 8.00);
        FUEL_THRESHOLDS.put(1, 17.56);
    }

    public static void findLowFuelAvgEvents(Flight flight) throws SQLException, MalformedFlightFileException, ParseException {
        double threshold = FUEL_THRESHOLDS.get(flight.getAirframeTypeId());
        TimeSeriesQueue<Object[]> timeSeriesQueue = new TimeSeriesQueue<Object[]>();

        flight.checkCalculationParameters(LOW_FUEL, LOW_FUEL);

        List<CustomEvent> lowFuelEvents = new ArrayList<>();
        int hadError = 0;

        DoubleTimeSeries fuel = flight.getDoubleTimeSeries(connection, LOW_FUEL);
        StringTimeSeries date = flight.getStringTimeSeries(connection, LCL_DATE);
        StringTimeSeries time = flight.getStringTimeSeries(connection, LCL_TIME);

        String startDateTimeStr = date.get(0) + time.get(0);
        timeSeriesQueue.enqueue(0, new Object[]{fuel.get(0), startDateTimeStr});
        int queueFuelIndex = 0;
        int queueDateTimeIndex = 1;

        for (int i = 1; i < flight.getNumberRows(); i++) {
            String currentDateTimeStr = date.get(i) + time.get(i);

            // TODO: Check if the strings are formatted correctly
            double currentTimeInSec = TimeUtils.calculateDurationInSeconds(startDateTimeStr, currentDateTimeStr);
            Object[] indexData = new Object[]{fuel.get(i), currentDateTimeStr};
            timeSeriesQueue.enqueue(currentTimeInSec, indexData);
            timeSeriesQueue.purge(15); // TODO: Maybe make it millis?


            double sum = 0;

            for (TimeSeriesNode<Object[]> node : timeSeriesQueue) {
                sum += (double) node.getValue()[queueFuelIndex];
            }

            double avg = sum / timeSeriesQueue.getSize();


            if (avg <= threshold) {
                LOG.info("Low Fuel Average Event Detected");

                // TODO: Pass in correct params when date/time setup

                lowFuelEvents.add(new CustomEvent("", currentDateTimeStr, 0, i, 0, flight, CustomEvent.LOW_FUEL))
            }
        }

    }

    public static void main(String[] args) {

    }

}
