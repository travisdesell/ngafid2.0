package org.ngafid.events;

import java.sql.*;
import java.time.LocalDateTime;

import org.ngafid.*;
import org.ngafid.accounts.User;

/**
 * This class is used for creating event annotations in the NGAFID
 *
 * @author <a href=mailto:apl1341@cs.rit.edu>Aidan LaBella @ RIT CS</a>
 */
public abstract class Annotation {
    private User user;
    private LocalDateTime timestamp;

    public Annotation(User user, LocalDateTime timestamp) {
        this.user = user;
        this.timestamp = timestamp;
    }

    /**
     * Get the user that made the annotation's ID
     *
     * @return the id of the user
     */
    public int getUserId() {
        return this.user.getId();
    }

    public boolean userIsAdmin() {
        return this.user.isAdmin();
    }
    
    /**
     * Get the users fleet ID
     *
     * @return the id of the fleet
     */
    public int getFleetId() {
        return this.user.getFleetId();
    }

    /**
     * Gets the timestamp of the annotation
     *
     * @return a LocalDateTime object
     */
    public LocalDateTime getTimestamp() {
        return this.timestamp;
    }
    
    /**
     * Updates the database with the annotation
     *
     * @return if the db transaction was successful
     */
    public abstract boolean updateDatabase() throws SQLException;
}
