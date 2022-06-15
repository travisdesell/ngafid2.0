package org.ngafid.events;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.ngafid.*;
import org.ngafid.accounts.User;

/**
 * This class is used for creating event annotations in the NGAFID
 *
 * @author <a href=mailto:apl1341@cs.rit.edu>Aidan LaBella @ RIT CS</a>
 */
public class EventAnnotation extends Annotation {
    private int eventId, classId;

    private static Connection connection = Database.getConnection();

    public EventAnnotation(int eventId, String className, User user, LocalDateTime timestamp) throws SQLException {
        super(user, timestamp);

        this.eventId = eventId;
        this.classId = this.getClassId(className);
    }

    /**
     * Instantiates this object in the order:
     * user_id, fleet_id, timestamp, event_id, class_id
     *
     * @param resultSet is the ResultSet that is used to make this instance
     */
    public EventAnnotation(ResultSet resultSet) throws SQLException {
        super(User.get(connection, resultSet.getInt(1), resultSet.getInt(2)), resultSet.getTimestamp(3).toLocalDateTime());

        this.eventId = resultSet.getInt(4);
        this.classId = resultSet.getInt(5);
    }

    /**
     * Checks this annotations primary key to determine if it is in the db.
     *
     * @return a boolean representing the state of this annotation in the db.
     */
    public boolean alreadyExists() throws SQLException {
        String queryString = "SELECT EXISTS (SELECT * FROM event_annotations WHERE fleet_id = ? AND user_id = ? AND event_id = ?)";

        PreparedStatement query = connection.prepareStatement(queryString);

        query.setInt(1, this.getFleetId());
        query.setInt(2, this.getUserId());
        query.setInt(3, this.getEventId());

        ResultSet resultSet = query.executeQuery();

        if (resultSet.next()) {
            return resultSet.getBoolean(1);
        }

        return false;
    }

    public int getEventId() {
        return this.eventId;
    }

    private int getClassId(String name) throws SQLException {
        String queryString = "SELECT id FROM loci_event_classes WHERE name = ? AND fleet_id = ?";

        PreparedStatement query = connection.prepareStatement(queryString);

        query.setString(1, name);
        query.setInt(2, this.getFleetId());

        ResultSet resultSet = query.executeQuery();

        if (resultSet.next()) {
            return resultSet.getInt(1);
        }

        // this shouldnt happen!
        return -1;
    }

    public boolean changeAnnotation(LocalDateTime timestamp) throws SQLException {
        String queryString = "UPDATE event_annotations SET class_id = ?, timestamp = ? WHERE user_id = ? AND fleet_id = ? AND event_id = ?";
        PreparedStatement query = connection.prepareStatement(queryString);

        query.setInt(1, this.classId);
        query.setTimestamp(2, Timestamp.valueOf(super.getTimestamp()));
        query.setInt(3, this.getUserId());
        query.setInt(4, this.getFleetId());
        query.setInt(5, this.getEventId());

        return query.executeUpdate() == 1;
    }

    public boolean updateDatabase() throws SQLException {
        String queryString = "INSERT INTO event_annotations(fleet_id, user_id, event_id, class_id, timestamp) VALUES (?,?,?,?,?)";
        PreparedStatement query = connection.prepareStatement(queryString);

        query.setInt(1, this.getFleetId());
        query.setInt(2, this.getUserId());
        query.setInt(3, this.getEventId());
        query.setInt(4, this.classId);
        query.setTimestamp(5, Timestamp.valueOf(super.getTimestamp()));

        return query.executeUpdate() == 1;
    }

    /**
     * Gets a list of the current event annotations associated with an event
     *
     * @param eventId the event id of the annotated event
     * @param currentUserId the id of the user retrieving information about the event
     *
     * @return a list of annotations
     */
    public static List<Annotation> getAnnotationsByEvent(int eventId, int currentUserId) throws SQLException {
        List<Annotation> annotations = new ArrayList<>();

        String queryString = "SELECT user_id, fleet_id, timestamp, event_id, class_id FROM event_annotations WHERE event_id = ?";

        PreparedStatement query = connection.prepareStatement(queryString);
        query.setInt(1, eventId);

        ResultSet resultSet = query.executeQuery();

        while (resultSet.next()) {
            EventAnnotation ea = new EventAnnotation(resultSet);
            Annotation annotation = (Annotation) ea;

            //Mask the class the user chose if not the requesting user
            if (currentUserId != annotation.getUserId()) {
                ea.classId = -1;
            }

            annotations.add(annotation);
        }

        return annotations;
    }
}
