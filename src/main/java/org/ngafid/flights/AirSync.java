package org.ngafid.flights;

import java.lang.*;
import java.util.*;
import java.util.logging.Logger;

public class AirSync {
    private Upload upload;

    // How long the daemon will wait before making another request
    private static long WAIT_TIME = 3600;

    private static final Logger LOG = Logger.getLogger(AirSync.class.getName());

    public static void main(String [] args) {
        LOG.info("AirSync daemon started");

        try {
            while (true) {
                LOG.info("Making request to AirSync server.");
                Thread.sleep(WAIT_TIME);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
