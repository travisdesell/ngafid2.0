package org.ngafid.core.flights;

import java.sql.ResultSet;
import java.sql.SQLException;

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

    public String toString() {
        return "Tail " + tail + ", sys. id: " + systemId;
    }
}
