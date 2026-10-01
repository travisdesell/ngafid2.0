package org.ngafid.core.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ErrorMessage {
    private static final Map<String, Integer> ID_MAP = new ConcurrentHashMap<>();
    private static final Map<Integer, String> MESSAGE_MAP = new ConcurrentHashMap<>();

    private ErrorMessage() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated.");
    }

    /**
     * Interns a message string to its id in {@code flight_messages}, so repeated error/warning text is stored once.
     * Checks the in-memory cache first; on a miss, looks the message up in the database (caching and returning its id),
     * and if it is not present inserts it ({@code INSERT IGNORE}) and recurses to fetch the generated id.
     *
     * @param connection the database connection
     * @param message the message text to resolve to an id
     * @return the id for the message
     * @throws SQLException if the lookup or insert fails
     */
    public static int getMessageId(Connection connection, String message) throws SQLException {
        Integer id = ID_MAP.get(message);

        if (id != null) {
            return id;
        } else {
            // id wasn't in the hashmap, look it up
            String queryString = "SELECT id FROM flight_messages WHERE message = ?";

            try (PreparedStatement query = connection.prepareStatement(queryString)) {
                query.setString(1, message);

                try (ResultSet resultSet = query.executeQuery()) {
                    if (resultSet.next()) {
                        // message existed in the database, return the id
                        int messageId = resultSet.getInt(1);
                        ID_MAP.put(message, messageId);
                        return messageId;
                    }
                }
            }

            // message did not exist in the database, insert it and return it's generated id
            queryString = "INSERT IGNORE INTO flight_messages SET message = ?";

            try (PreparedStatement insertQuery = connection.prepareStatement(queryString)) {
                insertQuery.setString(1, message);
                insertQuery.executeUpdate();

                return getMessageId(connection, message);
            }
        }
    }

    /**
     * Resolves an interned message id back to its text, checking the in-memory cache first and falling back to a
     * {@code flight_messages} lookup (which it caches). Returns a sentinel error string rather than throwing if the id
     * is not found.
     *
     * @param connection the database connection
     * @param messageId the message id to resolve
     * @return the message text, or a sentinel string if the id does not exist
     * @throws SQLException if the query fails
     */
    public static String getMessage(Connection connection, int messageId) throws SQLException {
        String message = MESSAGE_MAP.get(messageId);

        if (message != null) {
            return message;
        } else {
            // id wasn't in the hashmap, look it up
            String queryString = "SELECT message FROM flight_messages WHERE id = " + messageId;

            try (PreparedStatement query = connection.prepareStatement(queryString);
                    ResultSet resultSet = query.executeQuery()) {

                if (resultSet.next()) {
                    // message existed in the database, return the id
                    message = resultSet.getString(1);
                    MESSAGE_MAP.put(messageId, message);
                    return message;
                }
            }

            return "<MESSAGE LOOKUP IN DATABASE FAILED - THIS MEANS AN INVALID MESSAGE ID WAS USED>";
        }
    }
}
