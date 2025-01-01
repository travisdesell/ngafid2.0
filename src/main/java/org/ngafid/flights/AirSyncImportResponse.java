package org.ngafid.flights;

import org.ngafid.Database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class AirSyncImportResponse {
    private int id;
    private int uploadId;
    private int flightId;
    private int fleetId;
    public String tail;
    private String status;
    private String timeReceived;
    public List<FlightWarning> warnings;

    // String sql = "SELECT a.id, a.time_received, a.upload_id, f.status,
    // a.flight_id, a.tail FROM airsync_imports AS a INNER JOIN flights AS f ON f.id
    // = a.flight_id WHERE a.fleet_id = ?";
    public AirSyncImportResponse(int fleetId, ResultSet resultSet) throws SQLException {
        this.id = resultSet.getInt(1);
        this.timeReceived = resultSet.getTimestamp(2).toLocalDateTime().toString();
        this.uploadId = resultSet.getInt(3);
        this.status = resultSet.getString(4);
        this.flightId = resultSet.getInt(5);
        this.tail = resultSet.getString(6);

        try (Connection connection = Database.getConnection()) {
            this.warnings = FlightWarning.getWarningsByFlight(Database.getConnection(), flightId);
        }
    }

}
