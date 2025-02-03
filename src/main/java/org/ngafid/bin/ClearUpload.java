package org.ngafid.bin;

import org.ngafid.common.Database;
import org.ngafid.uploads.ProcessUpload;
import org.ngafid.uploads.Upload;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.logging.Logger;

public final class ClearUpload {
    private static final Logger LOG = Logger.getLogger(ProcessUpload.class.getName());

    private ClearUpload() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Main method to clear an upload
     *
     * @param arguments main args
     */
    public static void main(String[] arguments) {
        LOG.info("Arguments are " + Arrays.toString(arguments));

        int uploadId = Integer.parseInt(arguments[0]);
        try (Connection connection = Database.getConnection();
             Upload.LockedUpload upload = Upload.getLockedUpload(connection, uploadId)) {
            upload.clearUpload();
        } catch (SQLException e) {
            LOG.severe("Error clearing upload: " + e);
            e.printStackTrace();
        }
    }
}
