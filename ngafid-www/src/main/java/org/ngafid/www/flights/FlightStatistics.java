package org.ngafid.www.flights;

import org.ngafid.core.util.TimeUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

public enum FlightStatistics {
    ;

    private static final Logger LOG = Logger.getLogger(FlightStatistics.class.getName());

    private static double getFlightTime(Connection connection, int fleetId, int airframeId, String tableName) throws SQLException {
        return getFlightTime(connection, fleetId, airframeId, tableName, null);
    }

    private static double getFlightTime(Connection connection, int fleetId, int airframeId, String tableName, String condition) throws SQLException {
        String c = "fleet_id = " + fleetId;
        if (airframeId > 0)
            c += " AND airframe_id = " + airframeId;
        if (condition != null)
            c += " AND " + condition;
        return getFlightTimeImpl(connection, tableName, c);
    }

    private static double getAggregateFlightTime(Connection connection, int airframeId, String tableName, String condition) throws SQLException {
        String c = airframeId > 0 ? " airframe_id = " + airframeId : null;
        if (condition != null) {
            if (c == null)
                c = condition;
            else
                c += " AND " + condition;
        }
        return getFlightTimeImpl(connection, tableName, c);
    }

    private static double getFlightTimeImpl(Connection connection, String tableName, String condition) throws SQLException {
        String query = "SELECT SUM(flight_time_seconds) FROM " + tableName;

        if (condition != null)
            query += " WHERE " + condition;

        try (PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return (double) resultSet.getLong(1);
            } else {
                return 0.0;
            }
        }
    }

    public static double getTotalFlightTime(Connection connection, int fleetId, int airframeId) throws SQLException {
        return getFlightTime(connection, fleetId, airframeId, "v_fleet_flight_time");
    }

    public static double getAggregateTotalFlightTime(Connection connection, int airframeId) throws SQLException {
        return getAggregateFlightTime(connection, airframeId, "v_aggregate_flight_time", null);
    }

    public static double get30DayFlightTime(Connection connection, int fleetId, int airframeId) throws SQLException {
        return getFlightTime(connection, fleetId, airframeId, "v_fleet_30_day_flight_time");
    }

    public static double getAggregate30DayFlightTime(Connection connection, int airframeId) throws SQLException {
        return getAggregateFlightTime(connection, airframeId, "v_aggregate_30_day_flight_time", null);
    }

    public static double getCurrentYearFlightTime(Connection connection, int fleetId, int airframeId) throws SQLException {
        return getFlightTime(connection, fleetId, airframeId, "v_fleet_yearly_flight_time", " year = " + TimeUtils.getCurrentYearUTC());
    }

    public static double getAggregateCurrentYearFlightTime(Connection connection, int airframeId) throws SQLException {
        return getAggregateFlightTime(connection, airframeId, "v_aggregate_yearly_flight_time", "year = " + TimeUtils.getCurrentYearUTC());
    }

    public static double getCurrentMonthFlightTime(Connection connection, int fleetId, int airframeId) throws SQLException {
        // The 'm' here is very important. It refers to the cached version of this table.
        return getMonthFlightTime(connection, fleetId, airframeId, TimeUtils.getCurrentYearUTC(), TimeUtils.getCurrentMonthUTC());
    }


    public static double getMonthFlightTime(Connection connection, int fleetId, int airframeId, int year, int month) throws SQLException {
        return getFlightTime(connection, fleetId, airframeId, "m_fleet_monthly_flight_time", " year = " + year + " AND month = " + month);
    }

    public static double getAggregateCurrentMonthFlightTime(Connection connection, int airframeId) throws SQLException {
        return getAggregateFlightTime(connection, airframeId, "v_aggregate_monthly_flight_time", "year = " + TimeUtils.getCurrentYearUTC() + " AND month = " + TimeUtils.getCurrentMonthUTC());
    }

    private static int getFlightCount(Connection connection, int fleetId, int airframeId, String tableName, String condition) throws SQLException {
        String c = "fleet_id = " + fleetId;
        if (airframeId > 0)
            c += " AND airframe_id = " + airframeId;

        if (condition != null)
            c += " AND " + condition;
        return getFlightCountImpl(connection, tableName, c);
    }

    private static int getAggregateFlightCount(Connection connection, int airframeId, String tableName, String condition) throws SQLException {
        String c = airframeId > 0 ? " airframe_id = " + airframeId : null;
        if (condition != null) {
            if (c == null)
                c = condition;
            else
                c += " AND " + condition;
        }
        return getFlightCountImpl(connection, tableName, c);
    }

    private static int getFlightCountImpl(Connection connection, String tableName, String condition) throws SQLException {
        String query = "SELECT SUM(count) FROM " + tableName;
        if (condition != null)
            query += " WHERE " + condition;

        try (PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            } else {
                return 0;
            }
        }
    }

    public static int getTotalFlightCount(Connection connection, int fleetId, int airframeId) throws SQLException {
        return getFlightCount(connection, fleetId, airframeId, "v_fleet_flight_counts", null);
    }

    public static int getAggregateTotalFlightCount(Connection connection, int airframeId) throws SQLException {
        return getAggregateFlightCount(connection, airframeId, "v_aggregate_flight_counts", null);
    }

    public static int getCurrentYearFlightCount(Connection connection, int fleetId, int airframeId) throws SQLException {
        return getYearFlightCount(connection, fleetId, airframeId, TimeUtils.getCurrentYearUTC());
    }

    public static int getYearFlightCount(Connection connection, int fleetId, int airframeId, int year) throws SQLException {
        return getFlightCount(connection, fleetId, airframeId, "v_fleet_yearly_flight_counts", "year = " + year);
    }

    public static int getAggregateCurrentYearFlightCount(Connection connection, int airframeId) throws SQLException {
        return getAggregateYearFlightCount(connection, airframeId, TimeUtils.getCurrentYearUTC());
    }

    public static int getAggregateYearFlightCount(Connection connection, int airframeId, int year) throws SQLException {
        return getAggregateFlightCount(connection, airframeId, "v_aggregate_yearly_flight_counts", "year = " + year);
    }

    public static int getMonthFlightCount(Connection connection, int fleetId, int airframeId, int year, int month) throws SQLException {
        return getFlightCount(connection, fleetId, airframeId, "v_fleet_monthly_flight_counts", "year = " + year + " AND month = " + month);
    }

    public static int getAggregateMonthFlightCount(Connection connection, int airframeId, int year, int month) throws SQLException {
        return getAggregateFlightCount(connection, airframeId, "v_fleet_monthly_flight_counts", "year = " + year + " AND month = " + month);
    }

    public static int get30DayFlightCount(Connection connection, int fleetId, int airframeId) throws SQLException {
        return getFlightCount(connection, fleetId, airframeId, "v_fleet_30_day_flight_counts", null);
    }

    public static int getAggregate30DayFlightCount(Connection connection, int airframeId) throws SQLException {
        return getAggregateFlightCount(connection, airframeId, "v_aggregate_30_day_flight_counts", null);
    }

}
