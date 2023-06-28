package org.ngafid.flights;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Tail {
    public final String systemId;
    public final int fleetId;
    public final String tail;
    public final boolean confirmed;
    public final boolean airSyncEquipped;

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
        airSyncEquipped = resultSet.getBoolean(5);
    }

    public String toString() {
        return "Tail " + tail + ", sys. id: " + systemId;
    }
}
