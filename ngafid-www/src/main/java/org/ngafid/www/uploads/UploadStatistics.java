package org.ngafid.www.uploads;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public enum UploadStatistics {;

    public record UploadCounts(int count, int okUploadCount, int warningUploadCount, int errorUploadCount) {}

    private static UploadCounts getLiveUploadCounts(Connection connection, String condition) throws SQLException {
        String query = "SELECT "
                + "COALESCE(SUM(status != 'DERIVED'), 0) AS upload_count, "
                + "COALESCE(SUM(status = 'PROCESSED_OK'), 0) AS ok_count, "
                + "COALESCE(SUM(status = 'PROCESSED_WARNING'), 0) AS warning_count, "
                + "COALESCE(SUM(status LIKE 'FAILED%' OR status = 'UPLOADING_FAILED'), 0) AS error_count "
                + "FROM uploads";
        if (condition != null) {
            query += " WHERE " + condition;
        }

        try (PreparedStatement statement = connection.prepareStatement(query);
                ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return new UploadCounts(
                    resultSet.getInt("upload_count"),
                    resultSet.getInt("ok_count"),
                    resultSet.getInt("warning_count"),
                    resultSet.getInt("error_count"));
        }
    }

    private static String buildStartTimeDateClause(LocalDate startDate, LocalDate endDate) {
        final int startYear = startDate.getYear();
        final int startMonth = startDate.getMonthValue();
        final int endYear = endDate.getYear();
        final int endMonth = endDate.getMonthValue();

        if (startYear == endYear) {
            return String.format(
                    "(YEAR(start_time) = %d AND MONTH(start_time) >= %d AND MONTH(start_time) <= %d)",
                    startYear, startMonth, endMonth);
        } else {
            return String.format(
                    "((YEAR(start_time) = %d AND MONTH(start_time) >= %d) "
                            + "OR (YEAR(start_time) = %d AND MONTH(start_time) <= %d) "
                            + "OR (YEAR(start_time) > %d AND YEAR(start_time) < %d))",
                    startYear, startMonth, endYear, endMonth, startYear, endYear);
        }
    }

    public static UploadCounts getUploadCounts(Connection connection, int fleetId) throws SQLException {
        return getLiveUploadCounts(connection, "fleet_id = " + fleetId);
    }

    public static UploadCounts getUploadCountsDated(
            Connection connection, int fleetId, LocalDate startDate, LocalDate endDate) throws SQLException {

        String clause = buildStartTimeDateClause(startDate, endDate);

        // Append the fleet ID condition
        clause += " AND fleet_id = " + fleetId;

        return getLiveUploadCounts(connection, clause);
    }

    public static UploadCounts getAggregateUploadCounts(Connection connection) throws SQLException {
        return getLiveUploadCounts(connection, null);
    }

    public static UploadCounts getAggregateUploadCountsDated(
            Connection connection, LocalDate startDate, LocalDate endDate) throws SQLException {

        String clause = buildStartTimeDateClause(startDate, endDate);

        return getLiveUploadCounts(connection, clause);
    }

    public static Map<String, Integer> getUploadStatusCounts(Connection connection, int fleetId) throws SQLException {
        String query = "SELECT status, COUNT(*) AS status_count FROM uploads WHERE status != 'DERIVED'";
        if (fleetId > 0) {
            query += " AND fleet_id = ?";
        }
        query += " GROUP BY status";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            if (fleetId > 0) {
                statement.setInt(1, fleetId);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                Map<String, Integer> counts = new HashMap<>();

                while (resultSet.next()) {
                    counts.put(resultSet.getString("status"), resultSet.getInt("status_count"));
                }

                return counts;
            }
        }
    }
}
