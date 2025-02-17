package org.ngafid.uploads;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.ngafid.common.ErrorMessage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Logger;

public class UploadError {
    private static final Logger LOG = Logger.getLogger(UploadError.class.getName());

    @JsonProperty
    private int id;
    @JsonProperty
    private int uploadId;
    @JsonProperty
    private String message;
    @JsonProperty
    private String stackTrace;

    public static void insertError(Connection connection, int uploadId, String message) throws SQLException {
        try (PreparedStatement exceptionPreparedStatement = connection
                .prepareStatement("INSERT INTO upload_errors (upload_id, message_id) VALUES (?, ?)")) {
            exceptionPreparedStatement.setInt(1, uploadId);
            exceptionPreparedStatement.setInt(2, ErrorMessage.getMessageId(connection, message));

            LOG.info(exceptionPreparedStatement.toString());

            exceptionPreparedStatement.executeUpdate();
        }
    }

    public static ArrayList<UploadError> getUploadErrors(Connection connection, int uploadId) throws SQLException {
        try (PreparedStatement uploadQuery = connection
                .prepareStatement("SELECT id, upload_id, message_id FROM upload_errors WHERE upload_id = " + uploadId);
             ResultSet resultSet = uploadQuery.executeQuery()) {
            ArrayList<UploadError> uploads = new ArrayList<UploadError>();

            while (resultSet.next()) {
                uploads.add(new UploadError(connection, resultSet));
            }

            return uploads;
        }
    }

    public UploadError(Connection connection, ResultSet resultSet) throws SQLException {
        id = resultSet.getInt(1);
        uploadId = resultSet.getInt(2);
        message = ErrorMessage.getMessage(connection, resultSet.getInt(3));
    }
}
