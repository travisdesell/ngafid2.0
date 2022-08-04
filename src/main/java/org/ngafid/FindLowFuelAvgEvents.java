package org.ngafid;

import org.ngafid.accounts.Fleet;
import org.ngafid.common.TimeSeriesNode;
import org.ngafid.common.TimeSeriesQueue;
import org.ngafid.common.TimeUtils;
import org.ngafid.events.CustomEvent;
import org.ngafid.flights.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Logger;

import static org.ngafid.FindSpinEvents.setFlightProcessed;
import static org.ngafid.events.CustomEvent.SPIN_START;
import static org.ngafid.flights.CalculationParameters.*;

public class FindLowFuelAvgEvents {
    public static final Connection connection = Database.getConnection();
    public static final Logger LOG = Logger.getLogger(FindLowFuelAvgEvents.class.getName());
    private static final Map<Integer, Double> FUEL_THRESHOLDS = new HashMap<>();

    static {
        FUEL_THRESHOLDS.put(1, 8.25);
        FUEL_THRESHOLDS.put(2, 8.00);
        FUEL_THRESHOLDS.put(3, 17.56);
    }

    public static void findLowFuelAvgEventsInUpload(Upload upload) {
        try {
            String whereClause = "upload_id = " + upload.getId() + " AND insert_completed = 1 AND NOT EXISTS (SELECT flight_id FROM events WHERE id <= -11)";

            List<Flight> flights = Flight.getFlights(connection, whereClause);

            for (Flight flight : flights) {
                try {
                    findLowFuelAvgEvents(flight);
                } catch (MalformedFlightFileException e) {
                    System.out.println("Could not process flight " + flight.getId());
                } catch (ParseException e) {
                    System.out.println("Error parsing date");
                    e.printStackTrace();
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void findLowFuelAvgEvents(Flight flight) throws SQLException, MalformedFlightFileException, ParseException {
        int airframeTypeID = flight.getAirframeTypeId();

        if (!FUEL_THRESHOLDS.containsKey(airframeTypeID)) {
            System.out.println("Ignoring flight " + flight.getId() + ". No low fuel data for given airframe.");

            return;
        }


        System.out.println("Processing flight " + flight.getId());

        double threshold = FUEL_THRESHOLDS.get(airframeTypeID);
        TimeSeriesQueue<Object[]> timeSeriesQueue = new TimeSeriesQueue<>();

        flight.checkCalculationParameters(TOTAL_FUEL, TOTAL_FUEL);

        List<CustomEvent> lowFuelEvents = new ArrayList<>();
        int hadError = 0;

        DoubleTimeSeries fuel = flight.getDoubleTimeSeries(connection, TOTAL_FUEL);
        StringTimeSeries date = flight.getStringTimeSeries(connection, LCL_DATE);
        StringTimeSeries time = flight.getStringTimeSeries(connection, LCL_TIME);


        // TODO: Band-aid fix. Will ignore some flights. Need to figure out missing date times
        if (date.get(0).equals("")) {
            System.out.println("No date");
            return;
        }

        String startDateTimeStr = date.get(0) + "T" + time.get(0) + "Z";
        System.out.println("date: " + date.get(0));
                System.out.println("time: " + time.get(0));

        timeSeriesQueue.enqueue(0, new Object[]{fuel.get(0), startDateTimeStr, 0});
        int queueFuelIndex = 0;
        int queueDateTimeIndex = 1;
        int queueLineIndex = 2;

        for (int i = 1; i < flight.getNumberRows(); i++) {
            System.out.println("time: " + time.get(0));
            String currentDateTimeStr = date.get(i) + "T" + time.get(i) + "Z";

            // TODO: Check if the strings are formatted correctly
            double currentTimeInSec = TimeUtils.calculateDurationInSeconds(startDateTimeStr, currentDateTimeStr);
            Object[] indexData = new Object[]{fuel.get(i), currentDateTimeStr, i};
            timeSeriesQueue.enqueue(currentTimeInSec, indexData);
            timeSeriesQueue.purge(15); // TODO: Maybe make it millis?

            double sum = 0;

            for (TimeSeriesNode<Object[]> node : timeSeriesQueue) {
                sum += (double) node.getValue()[queueFuelIndex];
            }

            double avg = sum / timeSeriesQueue.getSize();


            if (avg <= threshold) {
                LOG.info("Low Fuel Average Event Detected");

                int startLine = (int) timeSeriesQueue.getFront().getValue()[queueLineIndex];
                String eventStartDateTimeStr = (String) timeSeriesQueue.getFront().getValue()[queueDateTimeIndex];
                String eventEndDateTimeStr = (String) timeSeriesQueue.getBack().getValue()[queueDateTimeIndex];

                // TODO: Figure out severity value
                lowFuelEvents.add(new CustomEvent(eventStartDateTimeStr, eventEndDateTimeStr, startLine, i, 0, flight, CustomEvent.LOW_FUEL));
            }

            System.out.println("Successfully processed flight " + flight.getId() + " for low fuel average events.");
        }

        LOG.info("Updating database with Low Average Fuel events: " + lowFuelEvents.size());

        for (CustomEvent event : lowFuelEvents) {
            event.updateDatabase(connection);
            event.updateStatistics(connection, flight.getFleetId(), flight.getAirframeTypeId(), SPIN_START.getId());
        }

        // Function from Spin Events class
        setFlightProcessed(flight, hadError, lowFuelEvents.size());
    }

    public static void main(String[] args) {
        List<Fleet> fleets = null;

        if (args.length == 1) {
            try {
                fleets = List.of(Fleet.get(connection, Integer.parseInt(args[0])));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            fleets = Fleet.getAllFleets(connection);
        }

        if (fleets == null) {
            System.out.println("Fleets is null");
            System.exit(1);
        }

        for (Fleet fleet : fleets) {
            int fleetID = fleet.getId();

            LOG.info("Processing low fuel average events for fleet " + fleetID);

            try {
                List<Upload> uploads = Upload.getUploads(connection, fleetID);

                for (Upload upload : uploads) {
                    findLowFuelAvgEventsInUpload(upload);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        LOG.info("Finished processing low fuel average events for all fleets");
        System.exit(0);

    }

}
