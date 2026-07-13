package org.ngafid.www.uploads;

import static org.ngafid.www.routes.StatisticsJavalinRoutes.buildDateClause;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;

public enum UploadStatistics {
    ;

    public record UploadCounts(int count, int okUploadCount, int warningUploadCount, int errorUploadCount) {}

    public record UploadIssueCounts(int errorUploadCount, int warningFlightCount, int errorFlightCount) {}

    private static UploadCounts getUploadCountImpl(Connection connection, String tableName, String condition)
            throws SQLException {
        String query = "SELECT upload_count, ok_count, warning_count, error_count FROM " + tableName;
        if (condition != null) {
            query += " WHERE " + condition;
        }
        try (PreparedStatement statement = connection.prepareStatement(query);
                ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return new UploadCounts(
                        resultSet.getInt("upload_count"),
                        resultSet.getInt("ok_count"),
                        resultSet.getInt("warning_count"),
                        resultSet.getInt("error_count"));
            } else {
                return new UploadCounts(0, 0, 0, 0);
            }
        }
    }

    public static UploadCounts getUploadCounts(Connection connection, int fleetId) throws SQLException {
        return getUploadCountImpl(connection, "v_fleet_upload_counts", "fleet_id = " + fleetId);
    }

    public static UploadCounts getUploadCountsDated(
            Connection connection, int fleetId, LocalDate startDate, LocalDate endDate) throws SQLException {

        String clause = buildDateClause(startDate, endDate);

        // Append the fleet ID condition
        clause += " AND fleet_id = " + fleetId;

        return getUploadCountImpl(connection, "v_fleet_monthly_upload_counts", clause);
    }

    public static UploadCounts getAggregateUploadCounts(Connection connection) throws SQLException {
        return getUploadCountImpl(connection, "v_aggregate_upload_counts", null);
    }

    public static UploadCounts getAggregateUploadCountsDated(
            Connection connection, LocalDate startDate, LocalDate endDate) throws SQLException {

        String clause = buildDateClause(startDate, endDate);

        return getUploadCountImpl(connection, "v_aggregate_monthly_upload_counts", clause);
    }

    /**
     * Returns issue totals for non-derived uploads in the requested fleet and date range: uploads that failed or
     * contain rejected flights, flights with warnings, and rejected flights. The flight counters are used because
     * {@code PROCESSED_WARNING} represents both warning-only and partially successful uploads.
     */
    public static UploadIssueCounts getUploadIssueCountsDated(
            Connection connection, int fleetId, LocalDate startDate, LocalDate endDate) throws SQLException {
        StringBuilder query = new StringBuilder("""
                SELECT
                    COALESCE(SUM(CASE WHEN n_error_flights > 0 OR status LIKE 'FAILED%' THEN 1 ELSE 0 END), 0),
                    COALESCE(SUM(n_warning_flights), 0),
                    COALESCE(SUM(n_error_flights), 0)
                FROM uploads
                WHERE status <> 'DERIVED'
                """);

        boolean filterStart = !LocalDate.MIN.equals(startDate);
        boolean filterEnd = !LocalDate.MAX.equals(endDate);
        if (fleetId > 0) query.append(" AND fleet_id = ?");
        if (filterStart) query.append(" AND start_time >= ?");
        if (filterEnd) query.append(" AND start_time < ?");

        try (PreparedStatement statement = connection.prepareStatement(query.toString())) {
            int parameter = 1;
            if (fleetId > 0) statement.setInt(parameter++, fleetId);
            if (filterStart) statement.setTimestamp(parameter++, Timestamp.valueOf(startDate.atStartOfDay()));
            if (filterEnd)
                statement.setTimestamp(
                        parameter, Timestamp.valueOf(endDate.plusDays(1).atStartOfDay()));

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return new UploadIssueCounts(resultSet.getInt(1), resultSet.getInt(2), resultSet.getInt(3));
            }
        }
    }
}
