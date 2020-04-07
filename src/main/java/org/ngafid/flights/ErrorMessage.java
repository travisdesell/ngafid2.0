package org.ngafid.flights;

import com.mysql.jdbc.Statement;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.util.HashMap;
import java.util.logging.Logger;

public class ErrorMessage {
    private static final Logger LOG = Logger.getLogger(ErrorMessage.class.getName());

    static HashMap<String,Integer> idMap = new HashMap<>();
    static HashMap<Integer,String> messageMap = new HashMap<>();

    public static int getMessageId(Connection connection, String message) throws SQLException {
        Integer id = idMap.get(message);

        if (id != null) {
            return id;

        } else {
            //id wasn't in the hashmap, look it up
            String queryString = "SELECT id FROM flight_messages WHERE message = ?";
            PreparedStatement query = connection.prepareStatement(queryString);
            query.setString(1, message);

            LOG.info(query.toString());
            System.out.println(query);
            ResultSet resultSet = query.executeQuery();

            if (resultSet.next()) {
                //message existed in the database, return the id
                int messageId = resultSet.getInt(1);
                idMap.put(message, messageId);
                resultSet.close();
                query.close();
                return messageId;

            } else {
                //message did not exist in the database, insert it and return it's generated id
                queryString = "INSERT INTO flight_messages SET message = ?";
                query = connection.prepareStatement(queryString, Statement.RETURN_GENERATED_KEYS);
                query.setString(1, message);

                LOG.info(query.toString());
                query.executeUpdate();
                resultSet.close();

                resultSet = query.getGeneratedKeys();
                resultSet.next();

                int messageId = resultSet.getInt(1);
                idMap.put(message, messageId);
                resultSet.close();
                query.close();

                return messageId;
            }
        }
    }

    public static String getMessage(Connection connection, int messageId) throws SQLException {
        String message = messageMap.get(messageId);

        if (message != null) {
            return message;

        } else {
            //id wasn't in the hashmap, look it up
            String queryString = "SELECT message FROM flight_messages WHERE id = ?";
            PreparedStatement query = connection.prepareStatement(queryString);
            query.setInt(1, messageId);

            LOG.info(query.toString());
            ResultSet resultSet = query.executeQuery();

            if (resultSet.next()) {
                //message existed in the database, return the id
                message = resultSet.getString(1);
                messageMap.put(messageId, message);
                resultSet.close();
                query.close();
                return message;

            } else {
                //message id did not exist in the database, this should not happen -- return null
                resultSet.close();
                query.close();
                return null;
            }
        }
    }
}
