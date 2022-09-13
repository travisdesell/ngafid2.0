package org.ngafid;

import org.ngafid.accounts.Fleet;
import org.ngafid.common.TimeUtils;
import org.ngafid.events.CustomEvent;
import org.ngafid.events.EventDefinition;
import org.ngafid.filters.Filter;
import org.ngafid.flights.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Logger;

import static org.ngafid.events.CustomEvent.getLowEndFuelDefinition;
import static org.ngafid.flights.CalculationParameters.*;

public class FindLowEndingFuelEvents {
    public static final Connection connection = Database.getConnection();
    public static final Logger LOG = Logger.getLogger(FindLowEndingFuelEvents.class.getName());
    private static Map<Integer, EventDefinition> eventDefs = new HashMap<>();
    private static Map<Integer, Double> thresholds = new HashMap<>();


    public static void findLowEndFuelEventsInUpload(Upload upload) {
        try {
            String whereClause = "upload_id = " + upload.getId() + " AND insert_completed = 1 AND NOT EXISTS (SELECT flight_id FROM events WHERE id = -4 OR id = -5 OR id = -6)";

            List<Flight> flights = Flight.getFlights(connection, whereClause);

            for (Flight flight : flights) {
                try {
                    findLowEndFuel(flight);
                } catch (MalformedFlightFileException e) {
                    System.out.println("Could not process flight " + flight.getId());
                } catch (ParseException e) {
                    LOG.info("Error parsing date");
                    e.printStackTrace();
                } catch (SQLException e) {
                    LOG.info("SQL Exception. Database my not have been updated");
                    e.printStackTrace();
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void findLowEndFuel(Flight flight) throws SQLException, MalformedFlightFileException, ParseException {
        int airframeNameID = flight.getAirframeNameId();

        if (!eventDefs.containsKey(airframeNameID)) {
            EventDefinition lowEndEventDef = getLowEndFuelDefinition(flight.getAirframeNameId());

            if (lowEndEventDef == null) {
                return;
            }

            eventDefs.put(airframeNameID, lowEndEventDef);
            thresholds.put(airframeNameID, getThresholdValueFromText(eventDefs.get(airframeNameID).toHumanReadable()));
        }


        LOG.info("Processing flight " + flight.getId());

        EventDefinition eventDef = eventDefs.get(airframeNameID);
        double threshold = thresholds.get(airframeNameID);

        flight.checkCalculationParameters(TOTAL_FUEL, AVG_FUEL_DEPENDENCIES);

        DoubleTimeSeries fuel = flight.getDoubleTimeSeries(connection, TOTAL_FUEL);
        StringTimeSeries date = flight.getStringTimeSeries(connection, LCL_DATE);
        StringTimeSeries time = flight.getStringTimeSeries(connection, LCL_TIME);

        String[] lastValidDateAndIndex = date.getLastValidAndIndex();
        int i = Integer.parseInt(lastValidDateAndIndex[1]);

        String endTime = lastValidDateAndIndex[0] + " " + time.getLastValid();

        String currentTime = endTime;
        double duration = 0;
        double fuelSum = 0;
        int fuelValues = 0;

        for (; duration <= 15; i--) {
            currentTime = date.get(i) + " " + time.get(i);
            fuelSum += fuel.get(i);
            fuelValues++;

            if (currentTime.equals(" ")) continue;

            duration = TimeUtils.calculateDurationInSeconds(currentTime, endTime, "yyyy-MM-dd HH:mm:ss");
        }

        double average = (fuelSum / fuelValues);
        int hadEvent = 0;
        if (average < threshold) {
            CustomEvent event = new CustomEvent(currentTime, endTime, i, flight.getNumberRows(), average, flight, eventDef);

            event.updateDatabase(connection);
            event.updateStatistics(connection, flight.getFleetId(), flight.getAirframeTypeId(), eventDef.getId());
            hadEvent++;
        }

        setFlightProcessed(flight, hadEvent);
    }

    private static double getThresholdValueFromText(String text) {
        return Double.parseDouble(text.substring(text.lastIndexOf(" ") + 1));
    }

    static void setFlightProcessed(Flight flight, int count) throws SQLException {
        String queryString = "INSERT INTO flight_processed SET fleet_id = ?, flight_id = ?, event_definition_id = ?, count = ?, had_error = ?";

        PreparedStatement stmt = connection.prepareStatement(queryString);

        stmt.setInt(1, flight.getFleetId());
        stmt.setInt(2, flight.getId());
        stmt.setInt(3, getLowEndFuelDefinition(flight.getAirframeNameId()).getId());
        stmt.setInt(4, count);
        stmt.setInt(5, 0);

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
                    findLowEndFuelEventsInUpload(upload);
                }
            } catch (SQLException e) {
                System.exit(1);
            }
        }

        LOG.info("Finished processing low fuel average events for all fleets");
        LOG.info("Upload size is " + uploadSize);

        System.exit(0);

    }

}
