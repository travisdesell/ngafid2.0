package org.ngafid.bin;

import org.ngafid.accounts.Fleet;
import org.ngafid.common.Database;
import org.ngafid.events.CustomEvent;
import org.ngafid.events.EventStatistics;
import org.ngafid.events.calculations.VSPDRegression;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.Flight;
import org.ngafid.flights.StringTimeSeries;
import org.ngafid.uploads.Upload;
import org.ngafid.uploads.process.MalformedFlightFileException;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import static org.ngafid.events.CustomEvent.getHighAltitudeSpin;
import static org.ngafid.events.CustomEvent.getLowAltitudeSpin;
import static org.ngafid.flights.Parameters.*;

/**
 * A Custom Exceedence calculation to find spin events.
 *
 * @author <a href = mailto:apl1341@cs.rit.edu>Aidan LaBella</a>
 */

public final class FindSpinEvents {
    private static final Logger LOG = Logger.getLogger(FindSpinEvents.class.getName());
    private static final int STOP_DELAY = 1;
    private static final double ALT_CONSTRAINT = 4000.d;
    private static final double AC_NORMAL = 0.1d;

    private FindSpinEvents() {
        throw new UnsupportedOperationException("Utility class not meant to be instantiated");
    }

    public static void findSpinEventsInUpload(Connection connection, Upload upload) {
        try {
            String whereClause = "upload_id = " + upload.getId() +
                    " AND insert_completed = 1 AND NOT EXISTS " +
                    "(SELECT flight_id FROM flight_processed WHERE " +
                    "(event_definition_id = " + getLowAltitudeSpin().getId() +
                    " OR event_definition_id = " + getHighAltitudeSpin().getId() + ")" +
                    " AND flight_processed.flight_id = flights.id)";

            List<Flight> flights = Flight.getFlights(connection, whereClause);
            System.out.println("Finding spin events for " + flights.size() + " flights.");

            for (Flight flight : flights) {
                try {
                    calculateVSPDDerived(connection, flight);
                    findSpinEvents(connection, flight, 250);
                } catch (MalformedFlightFileException mffe) {
                    LOG.severe("Can't process flight " + flight.getId());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void findSpinEvents(Connection connection, Flight flight, double altAglLimit) throws Exception {
        flight.checkCalculationParameters(SPIN, SPIN_DEPENDENCIES);

        List<CustomEvent> lowAltitudeSpins = new ArrayList<>();
        List<CustomEvent> highAltitudeSpins = new ArrayList<>();

        int hadError = 0;

        DoubleTimeSeries ias = flight.getDoubleTimeSeries(IAS);
        DoubleTimeSeries dVSI = flight.getDoubleTimeSeries(VSPD_CALCULATED);
        DoubleTimeSeries normAc = flight.getDoubleTimeSeries(NORM_AC);
        DoubleTimeSeries latAc = flight.getDoubleTimeSeries(LAT_AC);
        DoubleTimeSeries altAGL = flight.getDoubleTimeSeries(ALT_AGL);

        StringTimeSeries dateSeries = flight.getStringTimeSeries(connection, LCL_DATE);
        StringTimeSeries timeSeries = flight.getStringTimeSeries(connection, LCL_TIME);

        // Manually check the String Time Series
        if (dateSeries == null) {
            String errMsg = "Cannot calculate '" + "Spin Events" + "' as parameter '" + "LCL Date" + "' was missing.";
            LOG.severe("WARNING: " + errMsg);

            throw new MalformedFlightFileException(errMsg);
        } else if (timeSeries == null) {
            String errMsg = "Cannot calculate '" + "Spin Events" + "' as parameter '" + "LCL Time" + "' was missing.";
            LOG.severe("WARNING: " + errMsg);

            throw new MalformedFlightFileException(errMsg);
        }

        boolean airspeedIsLow = false;
        boolean spinStartFound = false;
        boolean altCstrViolated = false;

        double maxNormAc = 0.d;

        int lowAirspeedIndex = -1;
        int endSpinSeconds = 0;

        CustomEvent currentEvent = null;

        for (int i = 0; i < flight.getNumberRows(); i++) {
            // Get instantaneous altitudes
            double instIAS = ias.get(i);
            double instVSI = dVSI.get(i);
            double instAlt = altAGL.get(i);
            double normAcRel = Math.abs(normAc.get(i));
            double latAcRel = Math.abs(latAc.get(i));

            if (instAlt > altAglLimit) {
                if (!airspeedIsLow && instIAS < 50) {
                    airspeedIsLow = true;
                    lowAirspeedIndex = i;
                }

                if (airspeedIsLow) {
                    int lowAirspeedIndexDiff = i - lowAirspeedIndex;

                    // check for severity
                    if (normAcRel > maxNormAc) {
                        maxNormAc = normAcRel;
                    }

                    if (instAlt < ALT_CONSTRAINT) {
                        altCstrViolated = true;
                    }

                    if (lowAirspeedIndexDiff <= 2 && instVSI <= -3500) {
                        LOG.info("Spin start found!");

                        if (!spinStartFound) {
                            String start = dateSeries.get(lowAirspeedIndex) + " " + timeSeries.get(lowAirspeedIndex);
                            String end = dateSeries.get(i) + " " + timeSeries.get(i);

                            currentEvent = new CustomEvent(start, end, lowAirspeedIndex, i, maxNormAc, flight);

                            spinStartFound = true;
                        }
                    }

                    if (spinStartFound && (lowAirspeedIndexDiff > 3 && lowAirspeedIndexDiff <= 30)) {
                        // System.out.println("Looking for end of spin");

                        if (instIAS > 50 && normAcRel < AC_NORMAL && latAcRel < AC_NORMAL) {
                            String endTime = dateSeries.get(i) + " " + timeSeries.get(i);
                            currentEvent.updateEnd(endTime, i);

                            ++endSpinSeconds;
                        }
                    }

                    if (!spinStartFound && lowAirspeedIndexDiff >= 4) {
                        currentEvent = null;
                    }

                    if (endSpinSeconds >= STOP_DELAY || currentEvent == null) {
                        if (currentEvent != null) {
                            if (altCstrViolated) {
                                currentEvent.setDefinition(getLowAltitudeSpin());
                                lowAltitudeSpins.add(currentEvent);
                            } else {
                                currentEvent.setDefinition(getHighAltitudeSpin());
                                highAltitudeSpins.add(currentEvent);
                            }
                        }

                        spinStartFound = false;
                        airspeedIsLow = false;
                        altCstrViolated = false;

                        lowAirspeedIndex = -1;
                        endSpinSeconds = 0;
                        maxNormAc = 0.d;
                    }
                }
            }
        }

        int airframeNameId = flight.getAirframeNameId();
        int fleetId = flight.getFleetId();
        for (CustomEvent event : lowAltitudeSpins) {
            event.updateDatabase(connection);
            event.updateStatistics(connection, fleetId, airframeNameId, event.getDefinition().getId());
        }

        for (CustomEvent event : highAltitudeSpins) {
            event.updateDatabase(connection);
            event.updateStatistics(connection, fleetId, airframeNameId, event.getDefinition().getId());
        }

        String flightStartDateTime = flight.getStartDateTime();
        if (!highAltitudeSpins.isEmpty()) {
            EventStatistics.updateFlightsWithEvent(connection, fleetId,
                    airframeNameId, getHighAltitudeSpin().getId(), flightStartDateTime);
        } else {
            EventStatistics.updateFlightsWithoutEvent(connection, fleetId,
                    airframeNameId, getHighAltitudeSpin().getId(), flightStartDateTime);
        }

        if (!lowAltitudeSpins.isEmpty()) {
            EventStatistics.updateFlightsWithEvent(connection, fleetId,
                    airframeNameId, getLowAltitudeSpin().getId(), flightStartDateTime);
        } else {
            EventStatistics.updateFlightsWithoutEvent(connection, fleetId,
                    airframeNameId, getLowAltitudeSpin().getId(), flightStartDateTime);
        }

        setFlightProcessed(connection, flight, getHighAltitudeSpin().getId(), hadError, highAltitudeSpins.size());
        setFlightProcessed(connection, flight, getLowAltitudeSpin().getId(), hadError, lowAltitudeSpins.size());
    }

    /**
     * Calculates the derived vertical speed if it has not already been calculateds
     * and caches it in the db.
     *
     * @param connection the database connection
     * @param flight     the Flight to calculate dVSI for
     */
    static void calculateVSPDDerived(Connection connection, Flight flight)
            throws IOException, SQLException, MalformedFlightFileException {
        int flightId = flight.getId();
        DoubleTimeSeries dts = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, VSPD_CALCULATED);

        if (dts == null) {
            flight.checkCalculationParameters(VSPD_CALCULATED, ALT_B);
            DoubleTimeSeries dVSI =
                    DoubleTimeSeries.computed(VSPD_CALCULATED, "ft/min", flight.getNumberRows(), new VSPDRegression(flight.getDoubleTimeSeries(ALT_B)));
            flight.addDoubleTimeSeries(VSPD_CALCULATED, dVSI);
            dVSI.updateDatabase(connection, flightId);
        }
    }

    static void setFlightProcessed(Connection connection, Flight flight, int eventDefId, int hadError, int count)
            throws SQLException {
        String queryString = "INSERT INTO flight_processed " +
                "SET fleet_id = ?, flight_id = ?, event_definition_id = ?, count = ?, had_error = ?";

        try (PreparedStatement query = connection.prepareStatement(queryString)) {
            query.setInt(1, flight.getFleetId());
            query.setInt(2, flight.getId());
            query.setInt(3, eventDefId);
            query.setInt(4, count);
            query.setInt(5, hadError);

            query.executeUpdate();
        }
    }

    /**
     * Main method. Used for testing purposes only
     *
     * @param args command line arguments
     * @throws SQLException SQL Exception
     */
    public static void main(String[] args) throws SQLException {
        List<Fleet> fleets = null;
        Connection connection = Database.getConnection();

        if (args.length == 1) {
            int fleetId = Integer.parseInt(args[0]);
            try {
                fleets = List.of(Objects.requireNonNull(Fleet.get(connection, fleetId)));
            } catch (SQLException se) {
                se.printStackTrace();
            }
        } else {
            fleets = Fleet.getAllFleets(connection);

            // Print to the console in yellow...
            System.err.println("\u001B[33mCAUTION: PROCESSING SPIN EVENTS FOR ALL FLEETS: " +
                    "THIS MAY TAKE SOME TIME... \u001B[0m");
        }

        assert fleets != null;
        for (Fleet fleet : fleets) {
            int fleetId = fleet.getId();

            LOG.info("Processing spin events for fleet: " + fleetId);

            // try {
            // clearPreviousEvents(fleetId);

            // LOG.info("Cleared fleets previous events");
            // } catch (SQLException se) {
            // se.printStackTrace();
            // System.exit(1);
            // }

            try {
                List<Upload> uploads = Upload.getUploads(connection, fleetId);

                for (Upload upload : uploads) {
                    findSpinEventsInUpload(connection, upload);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        LOG.info("Finished processing spin events for all fleets!");
        System.exit(0);
    }
}
