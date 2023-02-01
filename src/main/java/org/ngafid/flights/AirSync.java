package org.ngafid.flights;

import java.io.*;
import java.net.URL;
import java.time.*;
import java.util.*;
import java.sql.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.codehaus.plexus.util.ExceptionUtils;
import org.ngafid.Database;
import org.ngafid.SendEmail;
import org.ngafid.WebServer;
import org.ngafid.accounts.AirSyncAircraft;
import org.ngafid.accounts.AirSyncAuth;
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

    public static void handleAirSyncAPIException(Exception e, AirSyncAuth authentication) {
        String message = e.getMessage();

        LOG.severe("Caught " + message + " when making AirSync request!");

        if (message.contains("HTTP response code: 401")) {
            LOG.warning("Bearer token is no longer valid (someone may have requested one elsewhere, or this daemon is running somewhere else!). Requesting a new one...");
            authentication.requestAuthorization();
        } else if (message.contains("HTTP response code: 502")) {
            LOG.warning("Got a 502 error!");
        } else {
            crashGracefully(e);
        }
    }

    public static void sendAdminCrashNotification(String message) {
        String NGAFID_ADMIN_EMAILS = System.getenv("NGAFID_ADMIN_EMAILS");
        ArrayList<String> adminEmails = new ArrayList<String>(Arrays.asList(NGAFID_ADMIN_EMAILS.split(";")));

        ArrayList<String> bccRecipients = new ArrayList<String>();
        SendEmail.sendEmail(adminEmails, bccRecipients, "CRITICAL: AirSync Daemon Exception!", message);
    }

    public static void crashGracefully(Exception e) {
        System.err.println("FATAL: Exiting due to error " + e.getMessage() + "!");
        e.printStackTrace();

        //TODO: format this as html!
        StringBuilder sb = new StringBuilder("The NGAFID AirSync daemon has crashed at " + LocalDateTime.now().toString() + "!\n");
        sb.append("Exception caught: " + e.getMessage() + "\n");
        sb.append("Stack trace:\n");
        sb.append(ExceptionUtils.getStackTrace(e));

        String message = sb.toString();

        System.err.println(message);
        sendAdminCrashNotification(message);

        System.exit(1);
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

                if (airSyncFleets == null || airSyncFleets.length == 0) {
                    LOG.severe("This instance of the NGAFID does not have any AirSync fleets configured. Please check the database and try again");
                    System.exit(1);
                }

                for (AirSyncFleet fleet : airSyncFleets) {
                    String logMessage = "Fleet " + fleet.getName() + ": %s";

                    if (fleet.isQueryOutdated(connection)) {
                        LOG.info(String.format(logMessage, "past timeout! Checking with the AirSync servers now."));

                        List<AirSyncAircraft> aircraft = fleet.getAircraft();
                        for (AirSyncAircraft a : aircraft) {
                            List<Integer> processedIds = getProcessedIds(connection, fleet.getId());

                            Optional<LocalDateTime> aircraftLastImportTime = a.getLastImportTime(connection);

                            List<AirSyncImport> imports;

                            if (aircraftLastImportTime.isPresent()) {
                                // We must make the interval exclusive when asking the server for flights
                                LocalDateTime importTime = aircraftLastImportTime.get().plusSeconds(1);
                                imports = a.getImportsAfterDate(connection, fleet, importTime);
                                LOG.info(String.format("Getting imports for fleet %s after %s.", fleet.getName(), importTime.toString()));
                            } else {
                                imports = a.getImports(connection, fleet);
                                LOG.info(String.format("Getting all imports for fleet %s, as there are no other uploads waiting for this fleet.", fleet.getName()));
                            }

                            if (imports != null && !imports.isEmpty()) {
                                for (AirSyncImport i : imports) {
                                    if (processedIds.contains(i.getId())) {
                                        LOG.info("Skipping AirSync with upload id: " + i.getId() + " as it already exists in the database");
                                    } else {
                                        i.proccess(connection);
                                    }
                                }
                            } else {
                                LOG.info("No imports found for aircraft: " + a.getTailNumber() + " in fleet " + fleet.getName() + ", continuing.");
                            }
                        }

                        fleet.setLastQueryTime(connection);
                    } else {
                        LOG.info(String.format(logMessage, "does not need to be updated, will skip."));
                    }
                }

                LOG.info("Sleeping for " + WAIT_TIME + "ms.");
                Thread.sleep(WAIT_TIME);
            }
        } catch (Exception e) {
            crashGracefully(e);
        }
    }
}
