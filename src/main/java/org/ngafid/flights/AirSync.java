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
import org.ngafid.accounts.EmailType;
import org.ngafid.WebServer;
import org.ngafid.accounts.AirSyncAircraft;
import org.ngafid.accounts.AirSyncAuth;
import org.ngafid.accounts.AirSyncFleet;
import org.ngafid.accounts.Fleet;

/**
 * This class contains code for controlling the AirSync daemon, as well
 * as many other methods may be used by the daemon
 */
public class AirSync {
    // How long the daemon will wait before making another request
    private static final long DEFAULT_WAIT_TIME = 10000;

    private static final Logger LOG = Logger.getLogger(AirSync.class.getName());

    /**
     * Gracefully handles an exception from the AirSync API
     *
     * @param e              the exception caught
     * @param authentication the authentication used at the time. We can use this
     *                       to request a new one if its simply outdated.
     */
    public static void handleAirSyncAPIException(Exception e, AirSyncAuth authentication) {
        String message = e.getMessage();

        LOG.severe("Caught " + message + " when making AirSync request!");

        if (message.contains("HTTP response code: 40")) {
            LOG.severe(
                    "Bearer token is no longer valid (someone may have requested one elsewhere, or this daemon is running somewhere else!).");
            authentication.requestAuthorization();
        } else if (message.contains("HTTP response code: 502")) {
            LOG.severe("Got a 502 error!");
            crashGracefully(e);
        } else {
            crashGracefully(e);
        }
    }

    /**
     * Sends a notification to NGAFID admins that this daemon has crashed
     * gracefully.
     *
     * @param message the message that needs to be sent
     */
    public static void sendAdminCrashNotification(String message) throws SQLException {
        String NGAFID_ADMIN_EMAILS = System.getenv("NGAFID_ADMIN_EMAILS");
        ArrayList<String> adminEmails = new ArrayList<String>(Arrays.asList(NGAFID_ADMIN_EMAILS.split(";")));

        ArrayList<String> bccRecipients = new ArrayList<String>();
        SendEmail.sendEmail(adminEmails, bccRecipients, "CRITICAL: AirSync Daemon Exception!", message,
                EmailType.AIRSYNC_DAEMON_CRASH);
    }

    /**
     * Handles a caught {@link Exception} and crashes gracefully
     *
     * @param e the exception caught
     */
    public static void crashGracefully(Exception e) {
        System.err.println("FATAL: Exiting due to error " + e.getMessage() + "!");
        e.printStackTrace();

        // TODO: format this as html!
        StringBuilder sb = new StringBuilder(
                "The NGAFID AirSync daemon has crashed at " + LocalDateTime.now().toString() + "!\n");
        sb.append("Exception caught: " + e.getMessage() + "\n");
        sb.append("Stack trace:\n");
        sb.append(ExceptionUtils.getStackTrace(e));

        String message = sb.toString();

        System.err.println(message);
        // sendAdminCrashNotification(message);

        System.exit(1);
    }

    static void cli(String[] args) throws IOException, SQLException {
        switch (args[0]) {
            case "reset-all":
                System.out
                        .println("Do you really want to delete all airsync uploads and associated flights? (y/n)");
                int c = System.in.read();
                if (c == 'y') {
                    try (Connection connection = Database.getConnection();
                            PreparedStatement query = connection.prepareStatement(
                                    "SELECT " + Upload.DEFAULT_COLUMNS
                                            + " FROM uploads WHERE kind = 'AIRSYNC'");
                            ResultSet results = query.executeQuery()) {
                        while (results.next()) {
                            Upload upload = new Upload(results);
                            upload.reset(connection);
                        }
                    }
                }

                return;

            case "remove-all":
                break;
        }
    }

    /**
     * This daemon's entry point.
     * This is where the logic for how the daemon operates will be defined.
     *
     * @param args command line args
     */
    public static void main(String[] args) {
        LOG.info("AirSync daemon started");

        if (args.length != 0) {
            try {
                cli(args);
            } catch (IOException | SQLException e) {
                e.printStackTrace();
            }
            return;
        }

        while (true) {
            try (Connection connection = Database.getConnection()) {
                // When an airsync upload is created in placed into the `uploads` table, it will have the 'UPLOADING'
                // state.
                // These uploads will never get processed if they're state is not set to 'UPLOADED'.
                //
                // If the airsync daemmon crashes for some reason, we may end up with such a scenario. So we set any
                // airsync
                // uploads with
                // UPLOADING to have state UPLOADED.
                try (PreparedStatement query = connection.prepareStatement(
                        "SELECT " + Upload.DEFAULT_COLUMNS
                                + " FROM uploads WHERE kind = 'AIRSYNC' AND status = 'UPLOADING'");
                        ResultSet results = query.executeQuery()) {
                    while (results.next()) {
                        Upload upload = new Upload(results);
                        upload.complete(connection);
                    }
                }
                AirSyncFleet[] airSyncFleets = AirSyncFleet.getAll(connection);

                if (airSyncFleets == null || airSyncFleets.length == 0) {
                    LOG.severe(
                            "This instance of the NGAFID does not have any AirSync fleets configured. Please check the database and try again");
                    System.exit(1);
                }

                for (AirSyncFleet fleet : airSyncFleets) {
                    String logMessage = "Fleet " + fleet.getName() + ": %s";
                    LOG.info("Override = " + fleet.getOverride(connection));

                    if (fleet.getOverride(connection) || fleet.isQueryOutdated(connection)) {
                        LOG.info(String.format(logMessage, "past timeout! Checking with the AirSync servers now."));
                        fleet.setOverride(connection, false);

                        String status = fleet.update(connection);

                        LOG.info("Update status: " + status);
                    }
                }

                long waitTime = 30000;
                LOG.info("Sleeping for " + waitTime / 1000 + "s.");
                Thread.sleep(waitTime);
            } catch (InterruptedException | SQLException | IOException e) {
                LOG.severe("Encountered the following error: ");
                e.printStackTrace();
                continue;
            }
        }
    }
}
