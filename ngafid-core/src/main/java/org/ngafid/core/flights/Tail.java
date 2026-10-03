package org.ngafid.core.flights;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A single aircraft tail record from the {@code tails} table: its system id, owning fleet, tail number, and whether
 * the tail number has been confirmed.
 */
public class Tail {
    private final String systemId;
    private final int fleetId;
    private final String tail;
    private final boolean confirmed;

    /**
     * Create a tail object from a resultSet from the database
     *
     * @param resultSet a row from the tails database table
     */
    public Tail(ResultSet resultSet) throws SQLException {
        systemId = resultSet.getString(1);
        fleetId = resultSet.getInt(2);
        tail = resultSet.getString(3);
        confirmed = resultSet.getBoolean(4);
    }

    /**
     * Returns a short human-readable summary of the tail (its tail number and system id).
     *
     * @return a debug string describing this tail
     */
    @Override
    public String toString() {
        return "Tail " + tail + ", sys. id: " + systemId;
    }
}
