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
                    // Go round-robin through each fleet and check to see if it has AirSync uploads waiting

                    //if (AirSync.hasUploadsWaiting(fleet)) {
                        //LOG.info("Making request to AirSync server.");
                        //AirSync airSync = new AirSync(LocalDateTime.now(), fleet);
                    //}
                }

                Thread.sleep(WAIT_TIME);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
