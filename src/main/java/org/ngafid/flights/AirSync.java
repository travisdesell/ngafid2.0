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

    static PrintStream logFile;

    // How long the daemon will wait before making another request
    private static final long DEFAULT_WAIT_TIME = 10000;
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


    public static void handleAirSyncAPIException(Exception e, AirSyncAuth authentication) {
        String message = e.getMessage();

        LOG.severe("Caught " + message + " when making AirSync request!");

        if (message.contains("HTTP response code: 40")) {
            LOG.severe("Bearer token is no longer valid (someone may have requested one elsewhere, or this daemon is running somewhere else!).");
            authentication.requestAuthorization();
            logFile.println("Got exception at time " + LocalDateTime.now().toString() + ": " + e.getMessage());
        } else if (message.contains("HTTP response code: 502")) {
            LOG.severe("Got a 502 error!");
            crashGracefully(e);
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

    private static long getWaitTime(Connection connection) throws SQLException {
        String sql = "SELECT MIN(timeout - TIMESTAMPDIFF(MINUTE, last_upload_time, CURRENT_TIMESTAMP)) AS remaining_time FROM airsync_fleet_info";
        PreparedStatement query = connection.prepareStatement(sql);

        ResultSet resultSet = query.executeQuery();

        long waitTime = DEFAULT_WAIT_TIME;
        if (resultSet.next()) {
            waitTime = 1000 * 60 * resultSet.getLong(1);
        }

        return Math.max(waitTime, 0);
    }


    /**
     * This daemon's entry point
     *
     * @param args command line args
     */
    public static void main(String [] args) {
        LOG.info("AirSync daemon started");


        try {
            LocalDateTime now = LocalDateTime.now();
            String timeStamp = new String() + now.getYear() + now.getMonthValue() + now.getDayOfMonth() + "-" + now.getHour() + now.getMinute() + now.getSecond();

            //logFile = new PrintStream(new File("/var/log/ngafid/airsync_" + timeStamp + ".log"));
            //logFile.println("Starting AirSync daemon error log at: " + now.toString());

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

                        if (fleet.lock(connection)) {
                            String status = fleet.update(connection);
                            fleet.unlock(connection);

                            LOG.info("Update status: " + status);
                        } else {
                            LOG.info("Unable to lock fleet " + fleet.toString() + ", will skip for now. This usually means a user has requested to manually update the fleet.");
                        }
                    } else {
                        LOG.info(String.format(logMessage, "does not need to be updated, will skip."));
                    }
                }

                long waitTime = getWaitTime(connection);
                LOG.info("Sleeping for " + waitTime + "ms.");
                Thread.sleep(waitTime);
            }
        } catch (Exception e) {
            crashGracefully(e);
        }
    }
}
