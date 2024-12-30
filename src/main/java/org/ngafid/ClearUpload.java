package org.ngafid;

import java.util.Arrays;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

import org.ngafid.flights.Upload;

public final class ClearUpload {
    private static final Logger LOG = Logger.getLogger(ProcessUpload.class.getName());

    private ClearUpload() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Main method to clear an upload
     * @param arguments main args
     */
    public static void main(String[] arguments) {
        LOG.info("Arguments are " + Arrays.toString(arguments));

        try (Connection connection = Database.getConnection()) {
            int uploadId = Integer.parseInt(arguments[0]);
            Upload.clearUpload(connection, uploadId);
        } catch (SQLException e) {
            LOG.severe("Error clearing upload: " + e);
            e.printStackTrace();
        }
    }
}
