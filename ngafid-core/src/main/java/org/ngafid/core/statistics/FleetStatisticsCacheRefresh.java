package org.ngafid.core.statistics;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Rebuilds per-fleet rows in materialized statistics tables used by the summary
 * dashboard. Equivalent to the fleet-scoped subset of the Liquibase hourly/daily
 * materialized-view refresh scripts, without truncating other fleets.
 */
public final class FleetStatisticsCacheRefresh {

    private static final Logger LOG = Logger.getLogger(FleetStatisticsCacheRefresh.class.getName());

    private FleetStatisticsCacheRefresh() {
        // Utility class
    }

    /**
     * Refreshes cached flight, upload, and event statistics for one fleet.
     *
     * @param connection an open database connection
     * @param fleetId the fleet whose cache rows should be rebuilt
     * @throws SQLException if a refresh statement fails
     */
    public static void refreshForFleet(Connection connection, int fleetId) throws SQLException {
        refreshFlightStatistics(connection, fleetId);
        refreshUploadStatistics(connection, fleetId);
        refreshEventStatistics(connection, fleetId);
        LOG.info("Refreshed materialized statistics cache for fleet id " + fleetId);
    }

    /**
     * Same as {@link #refreshForFleet(Connection, int)} but logs and suppresses failures so
     * upload processing or deletion is not rolled back when cache refresh fails.
     *
     * @param connection an open database connection
     * @param fleetId the fleet whose cache rows should be rebuilt
     */
    public static void refreshForFleetQuietly(Connection connection, int fleetId) {
        try {
            refreshForFleet(connection, fleetId);
        } catch (SQLException e) {
            LOG.log(
                    Level.SEVERE,
                    "Failed to refresh materialized statistics cache for fleet id " + fleetId,
                    e);
        }
    }

    private static void refreshFlightStatistics(Connection connection, int fleetId) throws SQLException {
        replaceFromView(connection, fleetId, "m_fleet_monthly_flight_counts", "v_fleet_monthly_flight_counts");
        replaceFromView(connection, fleetId, "m_fleet_monthly_flight_time", "v_fleet_monthly_flight_time");
        replaceFromView(connection, fleetId, "m_fleet_30_day_flight_counts", "v_fleet_30_day_flight_counts");
        replaceFromView(connection, fleetId, "m_fleet_30_day_flight_time", "v_fleet_30_day_flight_time");
    }

    private static void refreshUploadStatistics(Connection connection, int fleetId) throws SQLException {
        replaceFromView(
                connection,
                fleetId,
                "m_fleet_monthly_upload_counts",
                "v_fleet_monthly_upload_counts",
                "fleet_id, year, month, upload_count, ok_count, warning_count, error_count");
    }

    private static void refreshEventStatistics(Connection connection, int fleetId) throws SQLException {
        replaceFromView(
                connection, fleetId, "m_fleet_airframe_monthly_event_counts", "v_fleet_airframe_monthly_event_counts");
        replaceFromView(
                connection, fleetId, "m_fleet_airframe_30_day_event_counts", "v_fleet_airframe_30_day_event_counts");
        replaceFromView(
                connection,
                fleetId,
                "m_fleet_airframe_event_processed_flight_count",
                "v_fleet_airframe_event_processed_flight_count");
    }

    private static void replaceFromView(Connection connection, int fleetId, String table, String view)
            throws SQLException {
        replaceFromView(connection, fleetId, table, view, null);
    }

    private static void replaceFromView(
            Connection connection, int fleetId, String table, String view, String columnList)
            throws SQLException {
        try (PreparedStatement delete =
                connection.prepareStatement("DELETE FROM " + table + " WHERE fleet_id = ?")) {
            delete.setInt(1, fleetId);
            delete.executeUpdate();
        }

        String insertSql = columnList == null
                ? "INSERT INTO " + table + " SELECT * FROM " + view + " WHERE fleet_id = ?"
                : "INSERT INTO "
                        + table
                        + " ("
                        + columnList
                        + ") SELECT "
                        + columnList
                        + " FROM "
                        + view
                        + " WHERE fleet_id = ?";

        try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
            insert.setInt(1, fleetId);
            insert.executeUpdate();
        }
    }
}
