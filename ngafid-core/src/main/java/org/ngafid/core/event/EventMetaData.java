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

        /**
         * Returns the database column/name string for this metadata key.
         *
         * @return the key's stored name
         */
        public String toString() {
            return this.name;
        }

        /**
         * Parses an {@link EventMetaDataKey} from its string name (case-insensitive).
         *
         * @param s the key name to parse
         * @return the matching key
         * @throws IllegalArgumentException if {@code s} does not name a known key
         */
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

    /**
     * Constructs an event metadata item from a typed key and its value.
     *
     * @param name the metadata key
     * @param value the metadata value
     */
    public EventMetaData(EventMetaDataKey name, double value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Constructs an event metadata item from a key name (parsed into an {@link EventMetaDataKey}) and its value.
     *
     * @param string the metadata key name
     * @param value the metadata value
     */
    public EventMetaData(String string, double value) {
        this.name = EventMetaDataKey.fromString(string);
        this.value = value;
    }

    /**
     * Reconstructs an event metadata item from a result row (key name and value) for a given event.
     *
     * @param resultSet the result set positioned on the row to read
     * @param eventId the id of the event this metadata belongs to
     * @throws SQLException if reading the row fails
     */
    public EventMetaData(ResultSet resultSet, int eventId) throws SQLException {
        this.eventId = eventId;
        this.name = EventMetaDataKey.fromString(resultSet.getString(1));
        this.value = resultSet.getDouble(2);
    }

    /**
     * Persists this metadata item for an event by inserting a row into {@code event_metadata}, resolving the key's
     * id first.
     *
     * @param connection the database connection
     * @param eventIdToUpdate the event id to attach this metadata to
     * @throws SQLException if resolving the key id or the insert fails
     */
    public void updateDatabase(Connection connection, int eventIdToUpdate) throws SQLException {
        try (PreparedStatement statement =
                connection.prepareStatement("INSERT INTO event_metadata (event_id, key_id, value) VALUES (?, ?, ?)")) {
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
        try (PreparedStatement statement =
                connection.prepareStatement("SELECT id from event_metadata_keys where name = ?")) {
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

    /**
     * Loads all metadata items recorded for an event (joining {@code event_metadata} to its key names).
     *
     * @param connection the database connection
     * @param eventId the event whose metadata to load
     * @return the event's metadata items (empty if none)
     * @throws SQLException if the query fails
     */
    public static List<EventMetaData> getEventMetaData(Connection connection, int eventId) throws SQLException {

        List<EventMetaData> metaDataList = new ArrayList<>();
        try (PreparedStatement preparedStatement =
                        connection.prepareStatement("SELECT name, value FROM event_metadata JOIN "
                                + "event_metadata_keys as ek on ek.id = key_id WHERE event_id = " + eventId);
                ResultSet resultSet = preparedStatement.executeQuery()) {

            LOG.info(preparedStatement.toString());

            while (resultSet.next()) {
                metaDataList.add(new EventMetaData(resultSet, eventId));
            }
        }

        return metaDataList;
    }

    public EventMetaDataKey getName() {
        return name;
    }

    public double getValue() {
        return value;
    }
}
