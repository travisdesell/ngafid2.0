package org.ngafid;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.logging.Logger;
import org.ngafid.flights.Upload;

public class ClearUpload {
    private static Connection connection = null;
    private static Logger LOG = Logger.getLogger(ProcessUpload.class.getName());
    private static final String ERROR_STATUS_STR = "ERROR";

    public static void main(String[] arguments) {
        System.out.println("arguments are:");
        System.out.println(Arrays.toString(arguments));

        try {
            connection = Database.getConnection();

            int uploadId = Integer.parseInt(arguments[0]);
            Upload.clearUpload(connection, uploadId);
        } catch (SQLException e) {
            System.err.println(e);
            e.printStackTrace();
        }
    }
}
