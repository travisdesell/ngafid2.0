package org.ngafid.flights;

import java.util.ArrayList;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;


public class UploadError {
    private int id;
    private int uploadId;
    private String message;
    private String stackTrace;

    public static ArrayList<UploadError> getUploadErrors(Connection connection, int uploadId) throws SQLException {
        PreparedStatement uploadQuery = connection.prepareStatement("SELECT id, upload_id, message, stack_trace FROM upload_errors WHERE upload_id = ?");
        uploadQuery.setInt(1, uploadId);
        ResultSet resultSet = uploadQuery.executeQuery();

        ArrayList<UploadError> uploads = new ArrayList<UploadError>();

        while (resultSet.next()) {
            uploads.add(new UploadError(resultSet));
        }

        return uploads;
    }

    public UploadError(ResultSet resultSet) throws SQLException {
        id = resultSet.getInt(1);
        uploadId = resultSet.getInt(2);
        message = resultSet.getString(3);
        stackTrace = resultSet.getString(4);
    }
}
