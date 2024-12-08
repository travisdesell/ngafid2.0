package org.ngafid.flights;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.util.HashMap;
import java.util.logging.Logger;

public class ErrorMessage {
    private static final Logger LOG = Logger.getLogger(ErrorMessage.class.getName());

    static HashMap<String, Integer> idMap = new HashMap<>();
    static HashMap<Integer, String> messageMap = new HashMap<>();

    public static int getMessageId(Connection connection, String message) throws SQLException {
        Integer id = idMap.get(message);

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
                        idMap.put(message, messageId);
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

    public static String getMessage(Connection connection, int messageId) throws SQLException {
        String message = messageMap.get(messageId);

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
                    messageMap.put(messageId, message);
                    return message;
                }
            }

            return "<MESSAGE LOOKUP IN DATABASE FAILED - THIS MEANS AN INVALID MESSAGE ID WAS USED>";
        }
    }
}
