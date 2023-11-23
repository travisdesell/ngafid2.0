package org.ngafid.events;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * EventMetaData
 */
public class EventMetaData {

    private static final Logger LOG = Logger.getLogger(EventMetaData.class.getName());

    private int eventId;

    private String name;

    private double value;

    public EventMetaData(String name, double value) {
        
        this.name = name;
        this.value = value;
    }

    public EventMetaData(ResultSet resultSet, int eventId) {

        try {
            this.eventId = eventId;
            this.name = resultSet.getString(1);
            this.value = resultSet.getDouble(2);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public void updateDatabase(Connection connection, int eventId) {
        
        try {
            int eventMetaDataKeyId = this.getEventMetaDataKeyId(connection);
            PreparedStatement statement = connection.prepareStatement("INSERT INTO event_metadata (event_id, key_id, value) VALUES (?, ?, ?)");
            LOG.info(statement.toString());
            statement.setInt(1, eventId);
            statement.setInt(2, eventMetaDataKeyId);
            statement.setDouble(3, this.value);
            statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
            System.err.println("Error committing EventMetaData for EventId : " + eventId);
            throw new RuntimeException(e); 
        }

    }

    private int getEventMetaDataKeyId(Connection connection) {
        
        int result = 0;
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT id from event_metadata_keys where name = ?");
            LOG.info(statement.toString());
            statement.setString(1, this.name);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                result = resultSet.getInt(1);
            }
            statement.close();
        } catch (SQLException e) {
            System.err.println("Error getting event key id for name : " + this.name);
        }

        return result;
    }

    public static List<EventMetaData> getEventMetaData(Connection connection, int eventId) {
        
        List<EventMetaData> metaDataList = new ArrayList<>();
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT name, value FROM event_metadata JOIN  event_metadata_keys as ek on ek.id = key_id WHERE event_id = ?");
            LOG.info(preparedStatement.toString());
            preparedStatement.setInt(1, eventId);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
               metaDataList.add(new EventMetaData(resultSet, eventId)); 
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return metaDataList;
    }
}
