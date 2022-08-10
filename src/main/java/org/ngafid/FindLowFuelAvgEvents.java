package org.ngafid;

import org.ngafid.accounts.Fleet;
import org.ngafid.common.TimeSeriesNode;
import org.ngafid.common.TimeSeriesQueue;
import org.ngafid.common.TimeUtils;
import org.ngafid.events.CustomEvent;
import org.ngafid.flights.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Logger;

import static org.ngafid.events.CustomEvent.LOW_FUEL;
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
                    LOG.info("Error parsing date");
                    e.printStackTrace();
                } catch (
                        SQLException e) { // TODO: Another issue is updating database and getting IntegrityConstraintViolation with duplicate keys
                    LOG.info("SQL Exception. Database my not have been updated");
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
            LOG.info("Ignoring flight " + flight.getId() + ". No total fuel data for given airframe.");

            return;
        }

        LOG.info("Processing flight " + flight.getId());

        double threshold = FUEL_THRESHOLDS.get(airframeTypeID);
        TimeSeriesQueue<Object[]> timeSeriesQueue = new TimeSeriesQueue<>();

        flight.checkCalculationParameters(TOTAL_FUEL, TOTAL_FUEL);

        List<CustomEvent> lowFuelEvents = new ArrayList<>();
        int hadError = 0;

        DoubleTimeSeries fuel = flight.getDoubleTimeSeries(connection, TOTAL_FUEL);
        StringTimeSeries date = flight.getStringTimeSeries(connection, LCL_DATE);
        StringTimeSeries time = flight.getStringTimeSeries(connection, LCL_TIME);


        int index = 0;
        String startDateTimeStr = " ";
        while (startDateTimeStr.equals(" ")) {
            startDateTimeStr = date.get(index) + " " + time.get(index);
            index++;
        }

        timeSeriesQueue.enqueue(0, new Object[]{fuel.get(index), startDateTimeStr, index});
        final int queueFuelIndex = 0;
        final int queueDateTimeIndex = 1;
        final int queueLineIndex = 2;

        for (; index < flight.getNumberRows(); index++) {
            String currentDateTimeStr = date.get(index) + " " + time.get(index);

            if (currentDateTimeStr.equals(" ")) {
                continue;
            }

            double currentTimeInSec = TimeUtils.calculateDurationInSeconds(startDateTimeStr, currentDateTimeStr, "yyyy-MM-dd HH:mm:ss");
            Object[] indexData = new Object[]{fuel.get(index), currentDateTimeStr, index};

            timeSeriesQueue.enqueue(currentTimeInSec, indexData);
            timeSeriesQueue.purge(15);

            double sum = 0;
            double lowestFuelRecorded = Double.POSITIVE_INFINITY;

            for (TimeSeriesNode<Object[]> node : timeSeriesQueue) {
                double fuelValue = (double) node.getValue()[queueFuelIndex];

                sum += fuelValue;
                lowestFuelRecorded = Math.min(fuelValue, lowestFuelRecorded);
            }

            double avg = sum / timeSeriesQueue.getSize();

            // Check if average is below threshold and average is based on at least 10 seconds
            if (avg <= threshold && timeSeriesQueue.getTimeDiff() >= 10) {
                LOG.info("Low Fuel Average Detected: " + timeSeriesQueue);

                int startLine = (int) timeSeriesQueue.getFront().getValue()[queueLineIndex];

                String eventStartDateTimeStr = (String) timeSeriesQueue.getFront().getValue()[queueDateTimeIndex];
                String eventEndDateTimeStr = (String) timeSeriesQueue.getBack().getValue()[queueDateTimeIndex];

                CustomEvent lowFuelEvent = new CustomEvent(eventStartDateTimeStr, eventEndDateTimeStr, startLine, index, lowestFuelRecorded, flight, CustomEvent.LOW_FUEL);

                lowFuelEvents.add(lowFuelEvent);
                timeSeriesQueue.clear();


                // Finish going through loop to prevent spamming lowFuelEvents on page
                if (lowFuelEvents.size() >= 5) {
                    LOG.info(flight.getId() + ": Reached 5 or more low fuel events. Ending processing.");
                    break;
                }
            }

        }

        LOG.info("Successfully processed flight " + flight.getId() + " with " + lowFuelEvents.size() + " events.");

        for (CustomEvent event : lowFuelEvents) {
            event.updateDatabase(connection);
            event.updateStatistics(connection, flight.getFleetId(), flight.getAirframeTypeId(), LOW_FUEL.getId());
        }

        setFlightProcessed(flight, hadError, lowFuelEvents.size());
    }

    static void setFlightProcessed(Flight flight, int hadError, int count) throws SQLException {
        String queryString = "INSERT INTO flight_processed SET fleet_id = ?, flight_id = ?, event_definition_id = ?, count = ?, had_error = ?";

        PreparedStatement stmt = connection.prepareStatement(queryString);

        stmt.setInt(1, flight.getFleetId());
        stmt.setInt(2, flight.getId());
        stmt.setInt(3, LOW_FUEL.getId());
        stmt.setInt(4, count);
        stmt.setInt(5, hadError);

        stmt.executeUpdate();
        stmt.close();
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
            LOG.info("Fleets is null");
            System.exit(1);
        }
        int uploadSize = 0;
        for (Fleet fleet : fleets) {
            int fleetID = fleet.getId();

            LOG.info("Processing low fuel average events for fleet " + fleetID);

            try {
                List<Upload> uploads = Upload.getUploads(connection, fleetID);
                uploadSize = uploads.size();

                for (Upload upload : uploads) {
                    findLowFuelAvgEventsInUpload(upload);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        LOG.info("Finished processing low fuel average events for all fleets");
        LOG.info("Upload size is " + uploadSize);

        System.exit(0);

    }

}
