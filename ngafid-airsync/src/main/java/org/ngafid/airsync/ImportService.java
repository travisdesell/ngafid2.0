package org.ngafid.airsync;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.ngafid.core.Database;
import org.ngafid.core.uploads.Upload;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.logging.Logger;

/**
 * This class contains code for controlling the AirSync daemon, as well
 * as many other methods may be used by the daemon
 */
public final class ImportService {
    // How long the daemon will wait before making another request
    private static final long DEFAULT_WAIT_TIME = 10000;

    private static final Logger LOG = Logger.getLogger(ImportService.class.getName());

    private ImportService() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

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
                    "Bearer token is no longer valid (someone may have requested one elsewhere, " +
                            "or this daemon is running somewhere else!).");
            authentication.requestAuthorization();
        } else if (message.contains("HTTP response code: 502")) {
            LOG.severe("Got a 502 error!");
            crashGracefully(e);
        } else {
            crashGracefully(e);
        }
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
        sb.append("Exception caught: ").append(e.getMessage()).append("\n");
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
                            try (Upload.LockedUpload locked = upload.getLockedUpload(connection)) {
                                locked.reset();
                            }
                        }
                    }
                }

            case "remove-all":
                break;
            default:
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
                        try (Upload.LockedUpload locked = upload.getLockedUpload(connection)) {
                            locked.complete();
                        }
                    }
                }
                AirSyncFleet[] airSyncFleets = AirSyncFleet.getAll(connection);

                if (airSyncFleets == null || airSyncFleets.length == 0) {
                    LOG.severe(
                            "This instance of the NGAFID does not have any AirSync fleets configured." +
                                    " Please check the database and try again");
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
            } catch (SQLException | IOException e) {
                LOG.severe("Encountered the following error: ");
                e.printStackTrace();
            }

            long waitTime = 30000;
            LOG.info("Sleeping for " + waitTime / 1000 + "s.");
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException ignored) {
            }
        }
    }
}
