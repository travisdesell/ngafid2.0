package org.ngafid;

import java.util.*;

import java.io.*;

import org.ngafid.*;
import org.ngafid.accounts.Fleet;
import org.ngafid.flights.*;
import org.ngafid.events.*;

import java.sql.*;

import java.util.logging.Logger;

import static org.ngafid.flights.CalculationParameters.*;
import static org.ngafid.events.CustomEvent.*;

/**
 * A Custom Exceedence calculation to find spin events.
 *
 * @author <a href = mailto:apl1341@cs.rit.edu>Aidan LaBella</a>
 */

public class FindSpinEvents {
    static final Connection connection = Database.getConnection();
    static final Logger LOG = Logger.getLogger(FindSpinEvents.class.getName());

    public static void findSpinEventsInUpload(Upload upload) {
        try {
            String whereClause = "upload_id = " + upload.getId() + " AND insert_completed = 1 AND NOT EXISTS (SELECT flight_id FROM events WHERE id = -2)"; //change this to include spin ends eventually

            List<Flight> flights = Flight.getFlights(connection, whereClause);
            System.out.println("Finding spin events for " + flights.size() + " flights.");

            for (Flight flight : flights) {
                try {
                    findSpinEvents(flight, 250);
                } catch (MalformedFlightFileException mffe) {
                    System.out.println("Can't process flight " + flight.getId());
                }
            }
        } catch (Exception e)  {
            e.printStackTrace();
            System.exit(1);
        } 
    }

    public static void findSpinEvents(Flight flight, double altAglLimit) throws MalformedFlightFileException, SQLException, IOException {
        flight.checkCalculationParameters(SPIN, SPIN_DEPENDENCIES);

        List<CustomEvent> spinStarts = new ArrayList<>();

        int hadError = 0;

        DoubleTimeSeries ias = flight.getDoubleTimeSeries(connection, IAS);
        DoubleTimeSeries dVSI = flight.getDoubleTimeSeries(connection, VSPD_CALCULATED);
        DoubleTimeSeries normAc = flight.getDoubleTimeSeries(connection, NORM_AC);
        DoubleTimeSeries altAGL = flight.getDoubleTimeSeries(connection, ALT_AGL);

        StringTimeSeries dateSeries = flight.getStringTimeSeries(connection, LCL_DATE);
        StringTimeSeries timeSeries = flight.getStringTimeSeries(connection, LCL_TIME);

        boolean airspeedIsLow = false;
        boolean spinStartFound = false;

        double maxNormAc = 0.d;

        int lowAirspeedIndex = -1, maxNormAcIndex = -1;

        CustomEvent currentEvent = null;

        for (int i = 0; i < flight.getNumberRows(); i++) {
            // Get instantaneous altitudes
            double instIAS = ias.get(i);
            double instVSI = dVSI.get(i);
            double instAlt = altAGL.get(i);
            double normAcRel = Math.abs(normAc.get(i));

            if (instAlt > altAglLimit) {
                if (!airspeedIsLow && instIAS < 50) {
                    airspeedIsLow = true;
                    lowAirspeedIndex = i;
                }

                if (airspeedIsLow) {

                    // check for severity
                    if (normAcRel > maxNormAc) {
                        maxNormAc = normAcRel;
                        maxNormAcIndex = i;
                    }

                    if (i - lowAirspeedIndex <= 3 && instVSI <= -3500) {
                        System.out.println("Spin start found!");
                        if (!spinStartFound) {
                            String startTime = dateSeries.get(lowAirspeedIndex) + " " + timeSeries.get(lowAirspeedIndex);
                            String endTime = dateSeries.get(i) + " " + timeSeries.get(i);

                            currentEvent = new CustomEvent(startTime, endTime, lowAirspeedIndex, i, maxNormAc, flight, CustomEvent.SPIN_START);
                            spinStarts.add(currentEvent);

                            spinStartFound = true;
                        } 

                    } else {
                        if (spinStartFound) {
                            String endTime = dateSeries.get(i) + " " + timeSeries.get(i);
                            currentEvent.updateEnd(endTime, i);
                        }

                        spinStartFound = false;
                        airspeedIsLow = false;

                        lowAirspeedIndex = -1;
                        maxNormAcIndex = -1;

                        maxNormAc = 0.d;
                    }
                }
            }
        }

        LOG.info("Updating database with Spin Events." + spinStarts.size());
        for (CustomEvent event : spinStarts) {
            event.updateDatabase(connection);
            event.updateStatistics(connection, flight.getFleetId(), flight.getAirframeTypeId(), SPIN_START.getId());
        }

        setFlightProcessed(flight, hadError, spinStarts.size());
    }

    static void setFlightProcessed(Flight flight, int hadError, int count) throws SQLException {
        String queryString = "INSERT INTO flight_processed SET fleet_id = ?, flight_id = ?, event_definition_id = ?, count = ?, had_error = ?";

        PreparedStatement stmt = connection.prepareStatement(queryString);

        stmt.setInt(1, flight.getFleetId());
        stmt.setInt(2, flight.getId());
        stmt.setInt(3, SPIN_START.getId());
        stmt.setInt(4, count);
        stmt.setInt(5, hadError);

        stmt.executeUpdate();
        stmt.close();
    }

    /**
     * Used for testing purposes only
     */
    public static void main(String [] args) {
        List<Fleet> fleets = null;

        if (args.length == 1) {
            int fleetId = Integer.parseInt(args[0]);
            try {
                fleets = List.of(Fleet.get(connection, fleetId));
            } catch (SQLException se) {
                se.printStackTrace();
            }
        } else {
            fleets = Fleet.getAllFleets(connection);
        }

        for (Fleet fleet : fleets) {
            int fleetId = fleet.getId();

            LOG.info("Processing spin events for fleet: " + fleetId);

            try {
                List<Upload> uploads = Upload.getUploads(connection, fleetId);

                for (Upload upload : uploads) {
                    findSpinEventsInUpload(upload);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        LOG.info("Finished processing spin events for all fleets!");
        System.exit(0);
    }
}
