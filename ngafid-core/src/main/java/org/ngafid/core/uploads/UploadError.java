package org.ngafid.core.uploads;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Logger;
import org.ngafid.core.util.ErrorMessage;

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

    /**
     * Records an upload-level error, interning the message text to its message id and inserting a row into
     * {@code upload_errors}.
     *
     * @param connection the database connection
     * @param uploadId the upload the error belongs to
     * @param message the error message text
     * @throws SQLException if resolving the message id or the insert fails
     */
    public static void insertError(Connection connection, int uploadId, String message) throws SQLException {
        try (PreparedStatement exceptionPreparedStatement =
                connection.prepareStatement("INSERT INTO upload_errors (upload_id, message_id) VALUES (?, ?)")) {
            exceptionPreparedStatement.setInt(1, uploadId);
            exceptionPreparedStatement.setInt(2, ErrorMessage.getMessageId(connection, message));

            LOG.info(exceptionPreparedStatement.toString());

            exceptionPreparedStatement.executeUpdate();
        }
    }

    /**
     * Loads all upload-level errors recorded for an upload, resolving each error's message text from its interned
     * message id.
     *
     * @param connection the database connection
     * @param uploadId the upload whose errors to load
     * @return the upload's errors (empty if none)
     * @throws SQLException if the query fails
     */
    public static ArrayList<UploadError> getUploadErrors(Connection connection, int uploadId) throws SQLException {
        try (PreparedStatement uploadQuery = connection.prepareStatement(
                        "SELECT id, upload_id, message_id FROM upload_errors WHERE upload_id = " + uploadId);
                ResultSet resultSet = uploadQuery.executeQuery()) {
            ArrayList<UploadError> uploads = new ArrayList<UploadError>();

            while (resultSet.next()) {
                uploads.add(new UploadError(connection, resultSet));
            }

            return uploads;
        }
    }

    /**
     * Reconstructs an upload error from an {@code upload_errors} result row, resolving the error's message text from
     * its interned message id.
     *
     * @param connection the database connection used to resolve the message text
     * @param resultSet the result set positioned on the row to read
     * @throws SQLException if reading the row or resolving the message fails
     */
    public UploadError(Connection connection, ResultSet resultSet) throws SQLException {
        id = resultSet.getInt(1);
        uploadId = resultSet.getInt(2);
        message = ErrorMessage.getMessage(connection, resultSet.getInt(3));
    }
}
