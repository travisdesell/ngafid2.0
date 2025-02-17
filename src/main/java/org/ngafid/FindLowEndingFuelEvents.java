package org.ngafid;

import org.ngafid.accounts.Fleet;
import org.ngafid.common.TimeUtils;
import org.ngafid.events.CustomEvent;
import org.ngafid.events.EventDefinition;
import org.ngafid.events.EventStatistics;
import org.ngafid.flights.*;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Logger;

import static org.ngafid.events.CustomEvent.*;
import static org.ngafid.flights.Parameters.*;

public final class FindLowEndingFuelEvents {
    public static final Logger LOG = Logger.getLogger(FindLowEndingFuelEvents.class.getName());
    private static final Map<Integer, EventDefinition> EVENT_DEFS = new HashMap<>();
    private static final Map<Integer, Double> THRESHOLDS = new HashMap<>();

    private FindLowEndingFuelEvents() {
        throw new UnsupportedOperationException("Utility class not meant to be instantiated");
    }

    public static void findLowEndFuelEventsInUpload(Connection connection, Upload upload)
            throws FatalFlightFileException, IOException, MalformedFlightFileException, ParseException, SQLException {
        String whereClause = "upload_id = " + upload.getId() + " AND insert_completed = 1 AND NOT EXISTS " +
                "(SELECT flight_id FROM flight_processed WHERE (event_definition_id = " + getLowEndFuelPa28().getId()
                +
                " OR event_definition_id = " + getLowEndFuelPa44().getId() + " OR event_definition_id = "
                + getLowEndFuelCessna172().getId() +
                ") AND flight_processed.flight_id = flights.id)";

        List<Flight> flights = Flight.getFlights(connection, whereClause);

        for (Flight flight : flights) {
            findLowEndFuel(connection, flight);
        }
    }

    public static void findLowEndFuel(Connection connection, Flight flight)
            throws IOException, SQLException, FatalFlightFileException, MalformedFlightFileException {
        int airframeNameID = flight.getAirframeNameId();

        if (!EVENT_DEFS.containsKey(airframeNameID)) {
            EventDefinition lowEndEventDef = getLowEndFuelDefinition(flight.getAirframeNameId());

            if (lowEndEventDef == null) {
                return;
            }

            EVENT_DEFS.put(airframeNameID, lowEndEventDef);
            THRESHOLDS.put(airframeNameID, getThresholdValueFromText(EVENT_DEFS.get(airframeNameID).toHumanReadable()));
        }

        LOG.info("Processing flight " + flight.getId());

        EventDefinition eventDef = EVENT_DEFS.get(airframeNameID);
        double threshold = THRESHOLDS.get(airframeNameID);

        flight.checkCalculationParameters(TOTAL_FUEL, AVG_FUEL_DEPENDENCIES);

        DoubleTimeSeries fuel = flight.getDoubleTimeSeries(connection, TOTAL_FUEL);
        StringTimeSeries date = flight.getStringTimeSeries(connection, LCL_DATE);
        StringTimeSeries time = flight.getStringTimeSeries(connection, LCL_TIME);

        String[] lastValidDateAndIndex = date.getLastValidAndIndex();
        int i = Integer.parseInt(lastValidDateAndIndex[1]);
        LOG.info("last valid date and index: " + i);

        String endTime = lastValidDateAndIndex[0] + " " + time.getLastValid();

        String currentTime = endTime;
        double duration = 0;
        double fuelSum = 0;
        int fuelValues = 0;

        for (; duration <= 15 && i >= 0; i--) {
            currentTime = date.get(i) + " " + time.get(i);
            fuelSum += fuel.get(i);
            fuelValues++;

            if (currentTime.equals(" ")) {
                continue;
            }

            LOG.info("DATE = " + currentTime);
            duration = TimeUtils.calculateDurationInSeconds(currentTime, endTime, "yyyy-MM-dd HH:mm:ss");
        }

        int hadEvent = 0;
        if (duration >= 15) {
            double average = (fuelSum / fuelValues);
            if (average < threshold) {
                CustomEvent event = new CustomEvent(currentTime, endTime, i, flight.getNumberRows(), average, flight,
                        eventDef);

                event.updateDatabase(connection);
                event.updateStatistics(connection, flight.getFleetId(), flight.getAirframeTypeId(), eventDef.getId());
                EventStatistics.updateFlightsWithEvent(connection, flight.getFleetId(), flight.getAirframeNameId(),
                        eventDef.getId(), flight.getStartDateTime());
                hadEvent++;
            } else {
                EventStatistics.updateFlightsWithoutEvent(connection, flight.getFleetId(), flight.getAirframeNameId(),
                        eventDef.getId(), flight.getStartDateTime());
            }
        }

        setFlightProcessed(connection, flight, hadEvent);
    }

    private static double getThresholdValueFromText(String text) {
        return Double.parseDouble(text.substring(text.lastIndexOf(" ") + 1));
    }

    static void setFlightProcessed(Connection connection, Flight flight, int count) throws IOException, SQLException {
        String queryString = "INSERT INTO flight_processed SET " +
                "fleet_id = ?, flight_id = ?, event_definition_id = ?, count = ?, had_error = ?";

        try (PreparedStatement stmt = connection.prepareStatement(queryString)) {
            stmt.setInt(1, flight.getFleetId());
            stmt.setInt(2, flight.getId());
            stmt.setInt(3, getLowEndFuelDefinition(flight.getAirframeNameId()).getId());
            stmt.setInt(4, count);
            stmt.setInt(5, 0);

            stmt.executeUpdate();
        }
    }

    public static void main(String[] args) throws SQLException {
        List<Fleet> fleets = null;
        try (Connection connection = Database.getConnection()) {
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
                        try {
                            findLowEndFuelEventsInUpload(connection, upload);
                        } catch (Exception e) {
                            System.err.println(e);
                            e.printStackTrace();
                            System.exit(1);
                        }

                    }
                } catch (SQLException e) {
                    System.exit(1);
                }
            }
            LOG.info("Finished processing low fuel average events for all fleets");
            LOG.info("Upload size is " + uploadSize);
        }

    }

}
