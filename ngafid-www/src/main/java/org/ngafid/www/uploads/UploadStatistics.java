package org.ngafid.www.uploads;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;

public enum UploadStatistics {
    ;

    public record UploadCounts(int count, int okUploadCount, int warningUploadCount, int errorUploadCount) {}

    public record UploadIssueCounts(
            int errorUploadCount, int successfulFlightCount, int warningFlightCount, int errorFlightCount) {}

    public record UploadOutcomeCounts(
            int uploadCount,
            int okUploadCount,
            int warningUploadCount,
            int failedUploadCount,
            int errorUploadCount,
            int successfulFlightCount,
            int warningFlightCount,
            int errorFlightCount) {}

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
        return getUploadCountsDatedImpl(connection, fleetId, startDate, endDate);
    }

    public static UploadCounts getAggregateUploadCounts(Connection connection) throws SQLException {
        return getUploadCountImpl(connection, "v_aggregate_upload_counts", null);
    }

    public static UploadCounts getAggregateUploadCountsDated(
            Connection connection, LocalDate startDate, LocalDate endDate) throws SQLException {
        return getUploadCountsDatedImpl(connection, null, startDate, endDate);
    }

    private static UploadCounts getUploadCountsDatedImpl(
            Connection connection, Integer fleetId, LocalDate startDate, LocalDate endDate) throws SQLException {
        UploadOutcomeCounts counts = getUploadOutcomeCountsDated(connection, fleetId, startDate, endDate);
        return new UploadCounts(
                counts.uploadCount(),
                counts.okUploadCount(),
                counts.warningUploadCount(),
                counts.failedUploadCount());
    }

    /**
     * Returns all upload and flight outcome totals in one query. Dashboard callers should use this method rather
     * than issuing a separate aggregate query for each displayed value.
     */
    public static UploadOutcomeCounts getUploadOutcomeCountsDated(
            Connection connection, Integer fleetId, LocalDate startDate, LocalDate endDate) throws SQLException {
        StringBuilder query = new StringBuilder("""
                SELECT
                    COUNT(*) AS upload_count,
                    COALESCE(SUM(CASE WHEN status = 'PROCESSED_OK' THEN 1 ELSE 0 END), 0) AS ok_count,
                    COALESCE(SUM(CASE WHEN status = 'PROCESSED_WARNING' THEN 1 ELSE 0 END), 0) AS warning_count,
                    COALESCE(SUM(CASE WHEN status LIKE 'FAILED%' THEN 1 ELSE 0 END), 0) AS failed_count,
                    COALESCE(SUM(CASE WHEN n_error_flights > 0 OR status LIKE 'FAILED%' THEN 1 ELSE 0 END), 0)
                        AS error_upload_count,
                    COALESCE(SUM(COALESCE(n_valid_flights, 0) + COALESCE(n_warning_flights, 0)), 0)
                        AS successful_flight_count,
                    COALESCE(SUM(n_warning_flights), 0) AS warning_flight_count,
                    COALESCE(SUM(n_error_flights), 0) AS error_flight_count
                FROM uploads
                WHERE status <> 'DERIVED'
                """);

        boolean filterStart = !LocalDate.MIN.equals(startDate);
        boolean filterEnd = !LocalDate.MAX.equals(endDate);
        if (fleetId != null) query.append(" AND fleet_id = ?");
        if (filterStart) query.append(" AND start_time >= ?");
        if (filterEnd) query.append(" AND start_time < ?");

        try (PreparedStatement statement = connection.prepareStatement(query.toString())) {
            int parameter = 1;
            if (fleetId != null) statement.setInt(parameter++, fleetId);
            if (filterStart) statement.setTimestamp(parameter++, Timestamp.valueOf(startDate.atStartOfDay()));
            if (filterEnd)
                statement.setTimestamp(parameter, Timestamp.valueOf(endDate.plusDays(1).atStartOfDay()));

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return new UploadOutcomeCounts(
                        resultSet.getInt("upload_count"),
                        resultSet.getInt("ok_count"),
                        resultSet.getInt("warning_count"),
                        resultSet.getInt("failed_count"),
                        resultSet.getInt("error_upload_count"),
                        resultSet.getInt("successful_flight_count"),
                        resultSet.getInt("warning_flight_count"),
                        resultSet.getInt("error_flight_count"));
            }
        }
    }

    /**
     * Returns issue totals for non-derived uploads in the requested fleet and date range: uploads that failed or
     * contain rejected flights, flights with warnings, and rejected flights. The flight counters are used because
     * {@code PROCESSED_WARNING} represents both warning-only and partially successful uploads.
     */
    public static UploadIssueCounts getUploadIssueCountsDated(
            Connection connection, int fleetId, LocalDate startDate, LocalDate endDate) throws SQLException {
        UploadOutcomeCounts counts =
                getUploadOutcomeCountsDated(connection, fleetId > 0 ? fleetId : null, startDate, endDate);
        return new UploadIssueCounts(
                counts.errorUploadCount(),
                counts.successfulFlightCount(),
                counts.warningFlightCount(),
                counts.errorFlightCount());
    }
}
