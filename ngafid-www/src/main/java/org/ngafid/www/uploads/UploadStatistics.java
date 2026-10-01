package org.ngafid.www.uploads;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;

public final class UploadStatistics {

    private UploadStatistics() {
        // Utility class; not instantiable.
    }

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

    /**
     * Returns the all-time upload counts (total, OK, warning, error) for a fleet.
     *
     * @param connection the database connection
     * @param fleetId the fleet to scope to
     * @return the fleet's upload counts
     * @throws SQLException if the query fails
     */
    public static UploadCounts getUploadCounts(Connection connection, int fleetId) throws SQLException {
        return getUploadCountImpl(connection, "v_fleet_upload_counts", "fleet_id = " + fleetId);
    }

    /**
     * Returns the upload counts for a fleet over the given date range.
     *
     * @param connection the database connection
     * @param fleetId the fleet to scope to
     * @param startDate the inclusive start of the date range
     * @param endDate the inclusive end of the date range
     * @return the fleet's upload counts over the range
     * @throws SQLException if the query fails
     */
    public static UploadCounts getUploadCountsDated(
            Connection connection, int fleetId, LocalDate startDate, LocalDate endDate) throws SQLException {
        return getUploadCountsDatedImpl(connection, fleetId, startDate, endDate);
    }

    /**
     * Returns the all-time upload counts (total, OK, warning, error) across all fleets.
     *
     * @param connection the database connection
     * @return the aggregate upload counts
     * @throws SQLException if the query fails
     */
    public static UploadCounts getAggregateUploadCounts(Connection connection) throws SQLException {
        return getUploadCountImpl(connection, "v_aggregate_upload_counts", null);
    }

    /**
     * Returns the upload counts across all fleets over the given date range.
     *
     * @param connection the database connection
     * @param startDate the inclusive start of the date range
     * @param endDate the inclusive end of the date range
     * @return the aggregate upload counts over the range
     * @throws SQLException if the query fails
     */
    public static UploadCounts getAggregateUploadCountsDated(
            Connection connection, LocalDate startDate, LocalDate endDate) throws SQLException {
        return getUploadCountsDatedImpl(connection, null, startDate, endDate);
    }

    private static UploadCounts getUploadCountsDatedImpl(
            Connection connection, Integer fleetId, LocalDate startDate, LocalDate endDate) throws SQLException {
        UploadOutcomeCounts counts = getUploadOutcomeCountsDated(connection, fleetId, startDate, endDate);
        return new UploadCounts(
                counts.uploadCount(), counts.okUploadCount(), counts.warningUploadCount(), counts.failedUploadCount());
    }

    /**
     * Returns all upload and flight outcome totals in one query. Dashboard callers should use this method rather
     * than issuing a separate aggregate query for each displayed value.
     *
     * @param connection the database connection to query
     * @param fleetId the fleet to filter by, or {@code null} to include all fleets
     * @param startDate the inclusive lower bound on upload start time ({@link LocalDate#MIN} to disable)
     * @param endDate the exclusive upper bound on upload start time ({@link LocalDate#MAX} to disable)
     * @return the aggregated upload and flight outcome counts for the matching uploads
     * @throws SQLException if the query fails
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
                statement.setTimestamp(
                        parameter, Timestamp.valueOf(endDate.plusDays(1).atStartOfDay()));

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
     *
     * @param connection the database connection to query
     * @param fleetId the fleet to filter by, or a value {@code <= 0} to include all fleets
     * @param startDate the inclusive lower bound on upload start time ({@link LocalDate#MIN} to disable)
     * @param endDate the exclusive upper bound on upload start time ({@link LocalDate#MAX} to disable)
     * @return the aggregated upload and flight issue counts for the matching uploads
     * @throws SQLException if the query fails
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
