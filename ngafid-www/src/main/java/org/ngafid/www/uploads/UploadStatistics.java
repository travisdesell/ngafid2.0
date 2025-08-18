package org.ngafid.www.uploads;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

import static org.ngafid.www.routes.StatisticsJavalinRoutes.buildDateClause;

public enum UploadStatistics {
    ;

    public record UploadCounts(int count, int okUploadCount, int warningUploadCount, int errorUploadCount) {
    }

    private static UploadCounts getUploadCountImpl(Connection connection, String tableName, String condition) throws SQLException {
        String query = "SELECT upload_count, ok_count, warning_count, error_count FROM " + tableName;
        if (condition != null) {
            query += " WHERE " + condition;
        }
        try (PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return new UploadCounts(resultSet.getInt("upload_count"), resultSet.getInt("ok_count"), resultSet.getInt("warning_count"), resultSet.getInt("error_count"));
            } else {
                return new UploadCounts(0, 0, 0, 0);
            }
        }
    }

    public static UploadCounts getUploadCounts(Connection connection, int fleetId) throws SQLException {
        return getUploadCountImpl(connection, "v_fleet_upload_counts", "fleet_id = " + fleetId);
    }
    public static UploadCounts getUploadCountsDated(Connection connection, int fleetId, LocalDate startDate, LocalDate endDate) throws SQLException {

        String clause = buildDateClause(startDate, endDate);
            
        //Append the fleet ID condition
        clause += " AND fleet_id = " + fleetId;

        return getUploadCountImpl(connection, "v_fleet_monthly_upload_counts", clause);
    }

    public static UploadCounts getAggregateUploadCounts(Connection connection) throws SQLException {
        return getUploadCountImpl(connection, "v_aggregate_upload_counts", null);
    }
    public static UploadCounts getAggregateUploadCountsDated(Connection connection, LocalDate startDate, LocalDate endDate) throws SQLException {

        String clause = buildDateClause(startDate, endDate);

        return getUploadCountImpl(connection, "v_aggregate_monthly_upload_counts", clause);
    }

}
