package org.ngafid.core.event;

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

    public enum EventMetaDataKey {
        LATERAL_DISTANCE("lateral_distance"),
        VERTICAL_DISTANCE("vertical_distance");

        private final String name;

        EventMetaDataKey(String s) {
            name = s;
        }

        public String toString() {
            return this.name;
        }

        public static EventMetaDataKey fromString(String s) {
            return switch (s.toUpperCase()) {
                case "LATERAL_DISTANCE" -> LATERAL_DISTANCE;
                case "VERTICAL_DISTANCE" -> VERTICAL_DISTANCE;
                default -> throw new IllegalArgumentException("Unknown event meta data key: " + s);
            };
        }
    }

    private int eventId;

    private EventMetaDataKey name;

    private double value;

    public EventMetaData(EventMetaDataKey name, double value) {
        this.name = name;
        this.value = value;
    }

    public EventMetaData(String string, double value) {
        this.name = EventMetaDataKey.fromString(string);
        this.value = value;
    }

    public EventMetaData(ResultSet resultSet, int eventId) throws SQLException {
        this.eventId = eventId;
        this.name = EventMetaDataKey.fromString(resultSet.getString(1));
        this.value = resultSet.getDouble(2);
    }

    public void updateDatabase(Connection connection, int eventIdToUpdate) throws SQLException {
        try (PreparedStatement statement = connection
                .prepareStatement("INSERT INTO event_metadata (event_id, key_id, value) VALUES (?, ?, ?)")) {
            LOG.info(statement.toString());

            int eventMetaDataKeyId = this.getEventMetaDataKeyId(connection);
            statement.setInt(1, eventIdToUpdate);
            statement.setInt(2, eventMetaDataKeyId);
            statement.setDouble(3, this.value);
            statement.executeUpdate();
        }
    }

    private int getEventMetaDataKeyId(Connection connection) throws SQLException {

        int result = 0;
        try (PreparedStatement statement = connection
                .prepareStatement("SELECT id from event_metadata_keys where name = ?")) {
            statement.setString(1, this.name.toString());

            LOG.info(statement.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    result = resultSet.getInt(1);
                }
            }
        }

        return result;
    }

    public static List<EventMetaData> getEventMetaData(Connection connection, int eventId) throws SQLException {

        List<EventMetaData> metaDataList = new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT name, value FROM event_metadata JOIN " +
                        "event_metadata_keys as ek on ek.id = key_id WHERE event_id = " + eventId);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            LOG.info(preparedStatement.toString());

            while (resultSet.next()) {
                metaDataList.add(new EventMetaData(resultSet, eventId));
            }
        }

        return metaDataList;
    }
}
