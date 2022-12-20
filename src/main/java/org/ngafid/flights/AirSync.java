package org.ngafid.flights;

import java.io.*;
import java.time.*;
import java.util.*;
import java.util.logging.Logger;

public class AirSync {
    private Upload upload;

    // How long the daemon will wait before making another request
    private static long WAIT_TIME = 10000;

    private static final Logger LOG = Logger.getLogger(AirSync.class.getName());

    private File uploadFile;
    private LocalDateTime uploadTime;

    private byte [] fileData;

    public AirSync(LocalDateTime uploadTime) {
        this.uploadTime = uploadTime;
    }

    public String getUploadPathFormat() {
        return "";
    }

    public static boolean hasUploadsWaiting() {
        //TODO: Implement a way to see if there are uploads waiting on the AirSync servers;
        //
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
                if (AirSync.hasUploadsWaiting()) {
                    LOG.info("Making request to AirSync server.");
                    AirSync airSync = new AirSync(LocalDateTime.now());
                }

                Thread.sleep(WAIT_TIME);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
