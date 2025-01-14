package org.ngafid.flights.calculations;

import static org.ngafid.flights.calculations.Parameters.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;
import org.apache.commons.cli.*;
import org.ngafid.Database;
import org.ngafid.accounts.Fleet;
import org.ngafid.flights.*;

public class HeadingTrackDiff implements Calculation {
    private DoubleTimeSeries hdg, trk;
    private Flight flight;
    private static final Logger LOG = Logger.getLogger(HeadingTrackDiff.class.getName());

    public HeadingTrackDiff(Flight flight, Connection connection) throws SQLException {
        this.flight = flight;

        this.hdg = DoubleTimeSeries.getDoubleTimeSeries(connection, this.flight.getId(), HDG);
        this.trk = DoubleTimeSeries.getDoubleTimeSeries(connection, this.flight.getId(), TRK);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double calculate(int index) {
        double hdgInst = this.hdg.get(index);
        double trkInst = this.trk.get(index);

        double diff = hdgInst - trkInst;
        double degDiff = diff % 360;

        if (degDiff >= 0) {
            return degDiff > 180 ? 360 - degDiff : degDiff;
        } else {
            return degDiff < -180 ? -360 - degDiff : degDiff;
        }
    }

    public boolean existsInDB(Connection connection) throws SQLException {
        String sql = "SELECT EXISTS (SELECT id FROM double_series WHERE name_id IN (SELECT id FROM double_series_names WHERE name = ?) AND flight_id = ?)";
        PreparedStatement query = connection.prepareStatement(sql);

        query.setString(1, HDG_TRK_DIFF);
        query.setInt(2, this.flight.getId());

        ResultSet resultSet = query.executeQuery();
        if (resultSet.next()) {
            return resultSet.getBoolean(1);
        }

        return false;
    }

    public static void main(String [] args) {
        Options options = new Options();

        Option flightIds = new Option("f", "fleet_ids", true, "list of fleet ids to calculate for");
        flightIds.setRequired(false);
        flightIds.setArgs(Option.UNLIMITED_VALUES);
        options.addOption(flightIds);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;
        List<Fleet> fleets = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("ExtractFlights", options);

            System.exit(1);
        }

        Connection connection = Database.getConnection();

        String [] fleetIdStrs = cmd.getOptionValues('f');

        if (fleetIdStrs == null || fleetIdStrs.length <= 0) {
            fleets = Fleet.getAllFleets(connection);
        }

        try {
            for (Fleet fleet : fleets) {
                LOG.info("Calculating hdg/trk diff for fleet: " + fleet.getName());
                int fleetId = fleet.getId();
                // This may be slow for large fleets like UND
                List<Flight> flights = Flight.getFlights(connection, fleetId);

                for (Flight flight : flights) {
                    List<String> missingParams = flight.checkCalculationParameters(HDG_TRK_DEPENDENCIES);
                    if (missingParams.isEmpty()) {
                        HeadingTrackDiff calculation = new HeadingTrackDiff(flight, connection);
                        CalculatedDoubleTimeSeries hdgTrakDiff = new CalculatedDoubleTimeSeries(connection, HDG_TRK_DIFF, "degrees", true, flight);
                        if (!calculation.existsInDB(connection)) {
                            hdgTrakDiff.create(calculation);
                            hdgTrakDiff.updateDatabase(connection, flight.getId());
                        } else {
                            LOG.info("Already calculated for flight " + flight.toString());
                        }
                    } else {
                        //Cant be calculated.
                        LOG.severe("Skipping flight " + flight.toString());
                        LOG.severe("Missing columns: " + missingParams.toString());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        LOG.info("All done!");
        System.exit(0);
    }
}
