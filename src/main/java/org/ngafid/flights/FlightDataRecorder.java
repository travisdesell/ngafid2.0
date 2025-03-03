package org.ngafid.flights;

import org.ngafid.common.NormalizedColumn;

import java.sql.Connection;
import java.sql.SQLException;

public class FlightDataRecorder extends NormalizedColumn<FlightDataRecorder> {
    public FlightDataRecorder(int id, String name) {
        super(id, name);
    }

    public FlightDataRecorder(int id) {
        super(id);
    }

    public FlightDataRecorder(String name) {
        super(name);
    }

    public FlightDataRecorder(Connection connection, int id) throws SQLException {
        super(connection, id);
    }

    public FlightDataRecorder(Connection connection, String name) throws SQLException {
        super(connection, name);
    }

    @Override
    protected String getTableName() {
        return "flight_data_recorders";
    }
}
