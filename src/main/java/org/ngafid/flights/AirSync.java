package org.ngafid.flights;

import java.io.*;
import java.net.URL;
import java.time.*;
import java.util.*;
import java.sql.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.accounts.AirSyncAircraft;
import org.ngafid.accounts.AirSyncFleet;
import org.ngafid.accounts.Fleet;

public class AirSync {
    private Upload upload;

    // How long the daemon will wait before making another request
    private static final long WAIT_TIME = 10000;
    private static Connection connection = Database.getConnection();

    private static final Logger LOG = Logger.getLogger(AirSync.class.getName());

    private File uploadFile;
    private LocalDateTime uploadTime;

    private byte [] fileData;

    public AirSync(LocalDateTime uploadTime, Fleet fleet) {
        this.uploadTime = uploadTime;
    }

    // Format should be:
    // $NGAFID_ARCHIVE_DIR/<FLEET_ID>/<UPLOADER_ID>/<FLIGHT YYYY>/<FLIGHT MM>/<upload_id>__<upload_name>.zip
    public String getUploadPathFormat() {
        StringBuilder sb = new StringBuilder(WebServer.NGAFID_ARCHIVE_DIR + "/");
        sb.append("");
        return sb.toString();
    }

    public static boolean hasUploadsWaiting(AirSyncFleet fleet) throws IOException {
        //TODO: Implement a way to see if there are uploads waiting on the AirSync servers;
        System.out.println("fleet has creds: " + fleet.getAuth());

        return false;
    }

    public static List<Integer> getProcessedIds(Connection connection, int fleetId) throws SQLException {
        String sql = "SELECT id FROM airsync_imports WHERE fleet_id = ?";
        PreparedStatement query = connection.prepareStatement(sql);

        query.setInt(1, fleetId);

        ResultSet resultSet = query.executeQuery();
        List<Integer> ids = new LinkedList<>();

        while (resultSet.next()) {
            ids.add(resultSet.getInt(1));
        }

        return ids;
    }

    /**
     * This daemon's entry point
     *
     * @param args command line args
     */
    public static void main(String [] args) {
        LOG.info("AirSync daemon started");

        try {
            while (true) {
                AirSyncFleet [] airSyncFleets = AirSyncFleet.getAll(connection);

                LOG.info("Found AirSync-enabled fleets: " + Arrays.toString(airSyncFleets));

                if (airSyncFleets == null || airSyncFleets.length == 0) {
                    LOG.severe("This instance of the NGAFID does not have any AirSync fleets configured. Please check the database and try again");
                    System.exit(1);
                }

                for (AirSyncFleet fleet : airSyncFleets) {
                    //if (fleet.isQueryOutdated(connection)) {
                        List<AirSyncAircraft> aircraft = fleet.getAircraft();
                        for (AirSyncAircraft a : aircraft) {
                            List<Integer> processedIds = getProcessedIds(connection, fleet.getId());

                            LocalDateTime fleetLastImportTime = fleet.getLastImportTime(connection, a);

                            List<AirSyncImport> imports;

                            if (fleetLastImportTime != null) {
                                imports = a.getImportsAfterDate(connection, fleet, fleetLastImportTime);
                                LOG.info(String.format("Getting imports for fleet %s after %s.", fleet.getName(), fleetLastImportTime.toString()));
                            } else {
                                imports = a.getImports(connection, fleet);
                                LOG.info(String.format("Getting all imports for fleet %s, as there are no other uploads waiting for this fleet.", fleet.getName(), fleetLastImportTime));
                            }

                            if (imports != null) {
                                for (AirSyncImport i : imports) {
                                    if (processedIds.contains(i.getId())) {
                                        LOG.info("Skipping AirSync with upload id: " + i.getId() + " as it already exists in the database");
                                    } else {
                                        i.proccess(connection);
                                    }
                                }
                            } else {
                                LOG.severe("Unable to get imports for aircraft: " + a.getTailNumber());
                            }
                        }
                    //}

                    // Go round-robin through each fleet and check to see if it has AirSync uploads waiting

                    //if (AirSync.hasUploadsWaiting(fleet)) {
                        //LOG.info("Making request to AirSync server.");
                        //AirSync airSync = new AirSync(LocalDateTime.now(), fleet);
                    //}
                }

                LOG.info("Sleeping for " + WAIT_TIME + "ms.");
                Thread.sleep(WAIT_TIME);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
