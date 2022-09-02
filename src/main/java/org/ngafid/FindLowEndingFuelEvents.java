package org.ngafid;

import org.ngafid.accounts.Fleet;
import org.ngafid.common.TimeSeriesNode;
import org.ngafid.common.TimeSeriesQueue;
import org.ngafid.common.TimeUtils;
import org.ngafid.events.CustomEvent;
import org.ngafid.events.EventDefinition;
import org.ngafid.flights.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Logger;

import static org.ngafid.events.CustomEvent.getLowFuelDefinition;
import static org.ngafid.flights.CalculationParameters.*;

public class FindLowEndingFuelEvents {
    public static final Connection connection = Database.getConnection();
    public static final Logger LOG = Logger.getLogger(FindLowEndingFuelEvents.class.getName());

    public static void findLowFuelAvgEventsInUpload(Upload upload) {
        try {
            String whereClause = "upload_id = " + upload.getId() + " AND insert_completed = 1 AND NOT EXISTS (SELECT flight_id FROM events WHERE id = -4 OR id = -5 OR id = -6)";

            List<Flight> flights = Flight.getFlights(connection, whereClause);

            for (Flight flight : flights) {
                try {
                    findLowFuelAvgEvents(flight);
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

    public static void findLowFuelAvgEvents(Flight flight) throws SQLException, MalformedFlightFileException, ParseException {
        int airframeTypeID = flight.getAirframeTypeId();

//        if (!FUEL_THRESHOLDS.containsKey(airframeTypeID)) {
//            LOG.info("Ignoring flight " + flight.getId() + ". No total fuel data for given airframe.");
//
//            return;
//        }


        EventDefinition eventDef = getLowFuelDefinition(flight.getAirframeNameId());

        LOG.info("Processing flight " + flight.getId());

        double threshold = 0; // TODO: Update this

        flight.checkCalculationParameters(TOTAL_FUEL, AVG_FUEL_DEPENDENCIES);


        DoubleTimeSeries fuel = flight.getDoubleTimeSeries(connection, TOTAL_FUEL);
        StringTimeSeries date = flight.getStringTimeSeries(connection, LCL_DATE);
        StringTimeSeries time = flight.getStringTimeSeries(connection, LCL_TIME);


        // TODO: Loop from end to end - 15 sec
        for (;;) {


        }






            CustomEvent event = null; // TODO: update this
            event.updateDatabase(connection);
            event.updateStatistics(connection, flight.getFleetId(), flight.getAirframeTypeId(), eventDef.getId());

        setFlightProcessed(flight, 0, 0); // TODO: Update this
    }

    static void setFlightProcessed(Flight flight, int hadError, int count) throws SQLException {
        String queryString = "INSERT INTO flight_processed SET fleet_id = ?, flight_id = ?, event_definition_id = ?, count = ?, had_error = ?";

        PreparedStatement stmt = connection.prepareStatement(queryString);

        stmt.setInt(1, flight.getFleetId());
        stmt.setInt(2, flight.getId());
        stmt.setInt(3, getLowFuelDefinition(flight.getAirframeNameId()).getId());
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
