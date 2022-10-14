package org.ngafid.events;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.logging.Logger;

import org.ngafid.*;
import org.ngafid.accounts.User;


/**
 * This class is used for representing event annotations in the NGAFID with their event name and class name
 * Currently used for displaying rows in the event annotation table. Use EventAnnotation instead
 */
public class ReadableEventAnnotation extends Annotation {
    private String eventName, className, notes;

    private static Connection connection = Database.getConnection();
    private static final Logger LOG = Logger.getLogger(EventAnnotation.class.getName());

    private static final String DEFAULT_COLUMNS = "user_id, fleet_id, timestamp, event_id, class_id, notes";

    public ReadableEventAnnotation(String eventId, String className, User user, LocalDateTime timestamp, String notes) throws SQLException {
        super(user, timestamp);

        this.eventName = eventId;
        this.className = className;
        this.notes = notes;
    }

    /**
     * Instantiates this object in the order:
     * user_id, fleet_id, timestamp, event_id, class_id
     *
     * @param resultSet is the ResultSet that is used to make this instance
     */
    public ReadableEventAnnotation(ResultSet resultSet) throws SQLException {
        super(User.get(connection, resultSet.getInt(1), resultSet.getInt(2)), resultSet.getTimestamp(3).toLocalDateTime());

        this.eventName = resultSet.getString(4);
        this.className = resultSet.getString(5);
        this.notes = resultSet.getString(6);
    }

    @Override
    public boolean updateDatabase() throws SQLException {
        throw new UnsupportedOperationException("ReadableEventAnnotation is not meant to be used for updating the DB");
    }
}
