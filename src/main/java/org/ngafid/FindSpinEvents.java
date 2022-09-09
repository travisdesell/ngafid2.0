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
            String whereClause = "upload_id = " + upload.getId() + " AND insert_completed = 1 AND NOT EXISTS (SELECT flight_id FROM events WHERE id IN (" + HIGH_ALTITUDE_SPIN.getId() + ", " + LOW_ALTITUDE_SPIN.getId() + "))";

            List<Flight> flights = Flight.getFlights(connection, whereClause);
            System.out.println("Finding spin events for " + flights.size() + " flights.");

            for (Flight flight : flights) {
                try {
                    calculateVSPDDerived(flight);
                    findSpinEvents(flight, 250);
                } catch (MalformedFlightFileException mffe) {
                    LOG.severe("Can't process flight " + flight.getId());
                }
            }
        } catch (Exception e)  {
            e.printStackTrace();
            System.exit(1);
        } 
    }

    public static void clearPreviousEvents(int fleetId) throws SQLException {
        String sql = "DELETE FROM events WHERE event_definition_id IN (?,?) AND fleet_id = ?";
        PreparedStatement query = connection.prepareStatement(sql);

        query.setInt(1, LOW_ALTITUDE_SPIN.getId());
        query.setInt(2, HIGH_ALTITUDE_SPIN.getId());
        query.setInt(3, fleetId);

        query.executeUpdate();

        sql = "DELETE FROM flight_processed WHERE event_definition_id IN (?,?) AND fleet_id = ?";
        query = connection.prepareStatement(sql);

        query.setInt(1, LOW_ALTITUDE_SPIN.getId());
        query.setInt(2, HIGH_ALTITUDE_SPIN.getId());
        query.setInt(3, fleetId);

        query.executeUpdate();

        sql = "DELETE FROM event_statistics WHERE event_definition_id IN (?,?) AND fleet_id = ?";
        query = connection.prepareStatement(sql);

        query.setInt(1, LOW_ALTITUDE_SPIN.getId());
        query.setInt(2, HIGH_ALTITUDE_SPIN.getId());
        query.setInt(3, fleetId);

        query.executeUpdate();
    }

    public static void findSpinEvents(Flight flight, double altAglLimit) throws Exception {
        flight.checkCalculationParameters(SPIN, SPIN_DEPENDENCIES);

        List<CustomEvent> lowAltitudeSpins = new ArrayList<>();
        List<CustomEvent> highAltitudeSpins = new ArrayList<>();

        int hadError = 0;

        final int STOP_DELAY = 1;
        final double ALT_CONSTRAINT = 4000.d;
        final double AC_NORMAL = 0.1d;

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

        int lowAirspeedIndex = -1, maxNormAcIndex = -1, endSpinSeconds = 0;

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
                        maxNormAcIndex = i;
                    }

                    if (instAlt < ALT_CONSTRAINT) {
                        altCstrViolated = true;
                    }

                    if (lowAirspeedIndexDiff <= 2 && instVSI <= -3500) {
                        LOG.info("Spin start found!");

                        if (!spinStartFound) {
                            String startTime = dateSeries.get(lowAirspeedIndex) + " " + timeSeries.get(lowAirspeedIndex);
                            String endTime = dateSeries.get(i) + " " + timeSeries.get(i);
                            
                            currentEvent = new CustomEvent(startTime, endTime, lowAirspeedIndex, i, maxNormAc, flight);

                            spinStartFound = true;
                        } 
                    } 


                    if (spinStartFound && (lowAirspeedIndexDiff > 3 && lowAirspeedIndexDiff <= 30)) {
                        //System.out.println("Looking for end of spin");

                        if (instIAS > 50 && normAcRel < AC_NORMAL && latAcRel < AC_NORMAL) {
                            String endTime = dateSeries.get(i) + " " + timeSeries.get(i);
                            currentEvent.updateEnd(endTime, i);

                            ++endSpinSeconds;
                        }
                    }

                    if (!spinStartFound && lowAirspeedIndexDiff >= 4) {
                        currentEvent = null;
                    }

                    if (endSpinSeconds >= 1 || currentEvent == null) {
                        if (currentEvent != null) {
                            if (altCstrViolated) {
                                currentEvent.setDefinition(LOW_ALTITUDE_SPIN);
                                lowAltitudeSpins.add(currentEvent);
                            } else {
                                currentEvent.setDefinition(HIGH_ALTITUDE_SPIN);
                                highAltitudeSpins.add(currentEvent);
                            }
                        }

                        spinStartFound = false;
                        airspeedIsLow = false;
                        altCstrViolated = false;

                        lowAirspeedIndex = -1;
                        maxNormAcIndex = -1;
                        endSpinSeconds = 0;
                        maxNormAc = 0.d;
                    }
                }
            }
        }
        
        for (CustomEvent event : lowAltitudeSpins) {
            event.updateDatabase(connection);
            event.updateStatistics(connection, flight.getFleetId(), flight.getAirframeTypeId(), event.getDefinition().getId());
        }

        for (CustomEvent event : highAltitudeSpins) {
            event.updateDatabase(connection);
            event.updateStatistics(connection, flight.getFleetId(), flight.getAirframeTypeId(), event.getDefinition().getId());
        }

        setFlightProcessed(flight, HIGH_ALTITUDE_SPIN.getId(), hadError, highAltitudeSpins.size());
        setFlightProcessed(flight, LOW_ALTITUDE_SPIN.getId(), hadError, lowAltitudeSpins.size());
    }

    /**
     * Calculates the derived vertical speed if it has not already been calculateds and caches it in the db.
     *
     * @param flight the Flight to calculate dVSI for
     */
    static void calculateVSPDDerived(Flight flight) throws IOException, SQLException, MalformedFlightFileException {
        int flightId = flight.getId();
        DoubleTimeSeries dts = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, VSPD_CALCULATED);

        if (dts == null) {
            flight.checkCalculationParameters(VSPD_CALCULATED, ALT_B);
            CalculatedDoubleTimeSeries dVSI = new CalculatedDoubleTimeSeries(connection, VSPD_CALCULATED, "ft/min", true, flight);
            dVSI.create(new VSPDRegression(connection, flight));
            dVSI.updateDatabase(connection, flightId);
        }
    }

    static void setFlightProcessed(Flight flight, int eventDefinitonId, int hadError, int count) throws SQLException {
        String queryString = "INSERT INTO flight_processed SET fleet_id = ?, flight_id = ?, event_definition_id = ?, count = ?, had_error = ?";

        PreparedStatement query = connection.prepareStatement(queryString);

        query.setInt(1, flight.getFleetId());
        query.setInt(2, flight.getId());
        query.setInt(3, eventDefinitonId);
        query.setInt(4, count);
        query.setInt(5, hadError);

        query.executeUpdate();
        query.close();

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

            //Print to the console in yellow...
            System.err.println("\u001B[33mCAUTION: PROCESSING SPIN EVENTS FOR ALL FLEETS: THIS MAY TAKE SOME TIME... \u001B[0m");
        }

        for (Fleet fleet : fleets) {
            int fleetId = fleet.getId();

            LOG.info("Processing spin events for fleet: " + fleetId);

            try {
                clearPreviousEvents(fleetId);

                LOG.info("Cleared fleets previous events");
            } catch (SQLException se) {
                se.printStackTrace();
                System.exit(1);
            }

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
