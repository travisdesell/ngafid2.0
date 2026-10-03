package org.ngafid.www.flights;

import static org.ngafid.www.routes.StatisticsJavalinRoutes.buildDateAirframeClause;
import static org.ngafid.www.routes.StatisticsJavalinRoutes.buildDateClause;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.logging.Logger;
import org.ngafid.core.util.TimeUtils;

/**
 * Computes aggregate flight-time totals for a fleet or across all fleets from the precomputed statistics tables.
 *
 * <p>The static helpers assemble SQL filter clauses (by fleet, airframe, and date range) and sum
 * {@code flight_time_seconds} from the relevant summary table, supporting both per-fleet and all-fleet
 * aggregate dashboards.
 */
public final class FlightStatistics {

    private FlightStatistics() {
        // Utility class; not instantiable.
    }

    private static final Logger LOG = Logger.getLogger(FlightStatistics.class.getName());

    private static double getFlightTime(Connection connection, int fleetId, int airframeId, String tableName)
            throws SQLException {
        return getFlightTime(connection, fleetId, airframeId, tableName, null);
    }

    private static double getFlightTime(
            Connection connection, int fleetId, int airframeId, String tableName, String condition)
            throws SQLException {
        String c = "fleet_id = " + fleetId;
        if (airframeId > 0) c += " AND airframe_id = " + airframeId;
        if (condition != null) c += " AND " + condition;
        return getFlightTimeImpl(connection, tableName, c);
    }

    private static double getAggregateFlightTime(
            Connection connection, int airframeId, String tableName, String condition) throws SQLException {
        String c = airframeId > 0 ? " airframe_id = " + airframeId : null;
        if (condition != null) {
            if (c == null) c = condition;
            else c += " AND " + condition;
        }
        return getFlightTimeImpl(connection, tableName, c);
    }

    private static double getFlightTimeImpl(Connection connection, String tableName, String condition)
            throws SQLException {
        String query = "SELECT SUM(flight_time_seconds) FROM " + tableName;

        if (condition != null) query += " WHERE " + condition;

        try (PreparedStatement statement = connection.prepareStatement(query);
                ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return (double) resultSet.getLong(1);
            } else {
                return 0.0;
            }
        }
    }

    /**
     * Returns the all-time total flight time, in seconds (summed {@code flight_time_seconds}), for a fleet,
     * optionally restricted to one airframe.
     *
     * @param connection the database connection
     * @param fleetId the fleet to scope to
     * @param airframeId the airframe to filter by, or a value &lt;= 0 for all airframes
     * @return the total flight time in seconds
     * @throws SQLException if the query fails
     */
    public static double getTotalFlightTime(Connection connection, int fleetId, int airframeId) throws SQLException {
        return getFlightTime(connection, fleetId, airframeId, "v_fleet_flight_time");
    }

    /**
     * Returns the total flight time, in seconds, for a fleet over the given date range, optionally restricted to
     * one airframe (a date-only or date-plus-airframe clause is applied to the dated view).
     *
     * @param connection the database connection
     * @param fleetId the fleet to scope to
     * @param startDate the inclusive start of the date range
     * @param endDate the inclusive end of the date range
     * @param airframeID the airframe to filter by, or a value &lt; 0 for all airframes
     * @return the total flight time in seconds over the range
     * @throws SQLException if the query fails
     */
    public static double getTotalFlightTimeDated(
            Connection connection, int fleetId, LocalDate startDate, LocalDate endDate, int airframeID)
            throws SQLException {

        String clause;
        if (airframeID < 0) clause = buildDateClause(startDate, endDate);
        else clause = buildDateAirframeClause(startDate, endDate, airframeID);

        return getFlightTime(connection, fleetId, airframeID, "v_fleet_flight_time_dated", clause);
    }

    /**
     * Returns the all-time total flight time, in seconds, across all fleets, optionally restricted to one airframe.
     *
     * @param connection the database connection
     * @param airframeId the airframe to filter by, or a value &lt;= 0 for all airframes
     * @return the aggregate total flight time in seconds
     * @throws SQLException if the query fails
     */
    public static double getAggregateTotalFlightTime(Connection connection, int airframeId) throws SQLException {
        return getAggregateFlightTime(connection, airframeId, "v_aggregate_flight_time", null);
    }

    /**
     * Returns the total flight time, in seconds, across all fleets over the given date range, optionally restricted
     * to one airframe.
     *
     * @param connection the database connection
     * @param startDate the inclusive start of the date range
     * @param endDate the inclusive end of the date range
     * @param airframeId the airframe to filter by, or a value &lt; 0 for all airframes
     * @return the aggregate total flight time in seconds over the range
     * @throws SQLException if the query fails
     */
    public static double getAggregateTotalFlightTimeDated(
            Connection connection, LocalDate startDate, LocalDate endDate, int airframeId) throws SQLException {

        String clause;
        if (airframeId < 0) clause = buildDateClause(startDate, endDate);
        else clause = buildDateAirframeClause(startDate, endDate, airframeId);

        return getAggregateFlightTime(connection, airframeId, "v_aggregate_flight_time_dated", clause);
    }

    /**
     * Returns the flight time, in seconds, over the last 30 days for a fleet, optionally restricted to one
     * airframe.
     *
     * @param connection the database connection
     * @param fleetId the fleet to scope to
     * @param airframeId the airframe to filter by, or a value &lt;= 0 for all airframes
     * @return the 30-day flight time in seconds
     * @throws SQLException if the query fails
     */
    public static double get30DayFlightTime(Connection connection, int fleetId, int airframeId) throws SQLException {
        return getFlightTime(connection, fleetId, airframeId, "v_fleet_30_day_flight_time");
    }

    /**
     * Returns the flight time, in seconds, over the last 30 days across all fleets, optionally restricted to one
     * airframe.
     *
     * @param connection the database connection
     * @param airframeId the airframe to filter by, or a value &lt;= 0 for all airframes
     * @return the aggregate 30-day flight time in seconds
     * @throws SQLException if the query fails
     */
    public static double getAggregate30DayFlightTime(Connection connection, int airframeId) throws SQLException {
        return getAggregateFlightTime(connection, airframeId, "v_aggregate_30_day_flight_time", null);
    }

    /**
     * Returns the flight time, in seconds, accumulated so far in the current UTC calendar year for a fleet,
     * optionally restricted to one airframe.
     *
     * @param connection the database connection
     * @param fleetId the fleet to scope to
     * @param airframeId the airframe to filter by, or a value &lt;= 0 for all airframes
     * @return the current-year flight time in seconds
     * @throws SQLException if the query fails
     */
    public static double getCurrentYearFlightTime(Connection connection, int fleetId, int airframeId)
            throws SQLException {
        return getFlightTime(
                connection,
                fleetId,
                airframeId,
                "v_fleet_yearly_flight_time",
                " year = " + TimeUtils.getCurrentYearUTC());
    }

    /**
     * Returns the flight time, in seconds, accumulated so far in the current UTC calendar year across all fleets,
     * optionally restricted to one airframe.
     *
     * @param connection the database connection
     * @param airframeId the airframe to filter by, or a value &lt;= 0 for all airframes
     * @return the aggregate current-year flight time in seconds
     * @throws SQLException if the query fails
     */
    public static double getAggregateCurrentYearFlightTime(Connection connection, int airframeId) throws SQLException {
        return getAggregateFlightTime(
                connection, airframeId, "v_aggregate_yearly_flight_time", "year = " + TimeUtils.getCurrentYearUTC());
    }

    /**
     * Returns the flight time, in seconds, for the current UTC year and month for a fleet, optionally restricted to
     * one airframe. Delegates to {@link #getMonthFlightTime} with the current UTC year/month.
     *
     * @param connection the database connection
     * @param fleetId the fleet to scope to
     * @param airframeId the airframe to filter by, or a value &lt;= 0 for all airframes
     * @return the current-month flight time in seconds
     * @throws SQLException if the query fails
     */
    public static double getCurrentMonthFlightTime(Connection connection, int fleetId, int airframeId)
            throws SQLException {
        // The 'm' here is very important. It refers to the cached version of this table.
        return getMonthFlightTime(
                connection, fleetId, airframeId, TimeUtils.getCurrentYearUTC(), TimeUtils.getCurrentMonthUTC());
    }

    /**
     * Returns the flight time, in seconds, for a specific year and month for a fleet, optionally restricted to one
     * airframe (read from the cached {@code m_fleet_monthly_flight_time} table).
     *
     * @param connection the database connection
     * @param fleetId the fleet to scope to
     * @param airframeId the airframe to filter by, or a value &lt;= 0 for all airframes
     * @param year the calendar year to query
     * @param month the month of the year to query (1-12)
     * @return the flight time in seconds for that month
     * @throws SQLException if the query fails
     */
    public static double getMonthFlightTime(Connection connection, int fleetId, int airframeId, int year, int month)
            throws SQLException {
        return getFlightTime(
                connection,
                fleetId,
                airframeId,
                "m_fleet_monthly_flight_time",
                " year = " + year + " AND month = " + month);
    }

    /**
     * Returns the flight time, in seconds, for the current UTC month across all fleets, optionally restricted to
     * one airframe.
     *
     * @param connection the database connection
     * @param airframeId the airframe to filter by, or a value &lt;= 0 for all airframes
     * @return the aggregate current-month flight time in seconds
     * @throws SQLException if the query fails
     */
    public static double getAggregateCurrentMonthFlightTime(Connection connection, int airframeId) throws SQLException {
        return getAggregateFlightTime(
                connection,
                airframeId,
                "v_aggregate_monthly_flight_time",
                "year = " + TimeUtils.getCurrentYearUTC() + " AND month = " + TimeUtils.getCurrentMonthUTC());
    }

    private static int getFlightCount(
            Connection connection, int fleetId, int airframeId, String tableName, String condition)
            throws SQLException {
        String c = "fleet_id = " + fleetId;
        if (airframeId > 0) c += " AND airframe_id = " + airframeId;

        if (condition != null) c += " AND " + condition;
        return getFlightCountImpl(connection, tableName, c);
    }

    private static int getAggregateFlightCount(
            Connection connection, int airframeId, String tableName, String condition) throws SQLException {
        String c = airframeId > 0 ? " airframe_id = " + airframeId : null;
        if (condition != null) {
            if (c == null) c = condition;
            else c += " AND " + condition;
        }
        return getFlightCountImpl(connection, tableName, c);
    }

    private static int getFlightCountImpl(Connection connection, String tableName, String condition)
            throws SQLException {
        String query = "SELECT SUM(count) FROM " + tableName;
        if (condition != null) query += " WHERE " + condition;

        try (PreparedStatement statement = connection.prepareStatement(query);
                ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getInt(1);
            } else {
                return 0;
            }
        }
    }

    /**
     * Returns the all-time number of flights for a fleet, optionally restricted to one airframe.
     *
     * @param connection the database connection
     * @param fleetId the fleet to scope to
     * @param airframeId the airframe to filter by, or a value &lt;= 0 for all airframes
     * @return the flight count
     * @throws SQLException if the query fails
     */
    public static int getTotalFlightCount(Connection connection, int fleetId, int airframeId) throws SQLException {
        return getFlightCount(connection, fleetId, airframeId, "v_fleet_flight_counts", null);
    }

    /**
     * Returns the number of flights for a fleet over the given date range, optionally restricted to one airframe.
     *
     * @param connection the database connection
     * @param fleetId the fleet to scope to
     * @param startDate the inclusive start of the date range
     * @param endDate the inclusive end of the date range
     * @param airframeId the airframe to filter by, or a value &lt; 0 for all airframes
     * @return the flight count over the range
     * @throws SQLException if the query fails
     */
    public static int getTotalFlightCountDated(
            Connection connection, int fleetId, LocalDate startDate, LocalDate endDate, int airframeId)
            throws SQLException {

        String clause;
        if (airframeId < 0) clause = buildDateClause(startDate, endDate);
        else clause = buildDateAirframeClause(startDate, endDate, airframeId);

        return getFlightCount(connection, fleetId, airframeId, "v_fleet_flight_counts_dated", clause);
    }

    /**
     * Returns the all-time number of flights across all fleets, optionally restricted to one airframe.
     *
     * @param connection the database connection
     * @param airframeId the airframe to filter by, or a value &lt;= 0 for all airframes
     * @return the aggregate flight count
     * @throws SQLException if the query fails
     */
    public static int getAggregateTotalFlightCount(Connection connection, int airframeId) throws SQLException {
        return getAggregateFlightCount(connection, airframeId, "v_aggregate_flight_counts", null);
    }

    /**
     * Returns the number of flights across all fleets over the given date range, optionally restricted to one
     * airframe.
     *
     * @param connection the database connection
     * @param startDate the inclusive start of the date range
     * @param endDate the inclusive end of the date range
     * @param airframeId the airframe to filter by, or a value &lt; 0 for all airframes
     * @return the aggregate flight count over the range
     * @throws SQLException if the query fails
     */
    public static int getAggregateTotalFlightCountDated(
            Connection connection, LocalDate startDate, LocalDate endDate, int airframeId) throws SQLException {

        String clause;
        if (airframeId < 0) clause = buildDateClause(startDate, endDate);
        else clause = buildDateAirframeClause(startDate, endDate, airframeId);

        return getAggregateFlightCount(connection, airframeId, "v_aggregate_flight_counts_dated", clause);
    }

    /**
     * Returns the number of flights recorded so far in the current UTC calendar year for a fleet, optionally
     * restricted to one airframe.
     *
     * @param connection the database connection
     * @param fleetId the fleet to scope to
     * @param airframeId the airframe to filter by, or a value &lt;= 0 for all airframes
     * @return the current-year flight count
     * @throws SQLException if the query fails
     */
    public static int getCurrentYearFlightCount(Connection connection, int fleetId, int airframeId)
            throws SQLException {
        return getYearFlightCount(connection, fleetId, airframeId, TimeUtils.getCurrentYearUTC());
    }

    /**
     * Returns the number of flights in a specific calendar year for a fleet, optionally restricted to one airframe.
     *
     * @param connection the database connection
     * @param fleetId the fleet to scope to
     * @param airframeId the airframe to filter by, or a value &lt;= 0 for all airframes
     * @param year the calendar year to query
     * @return the flight count for that year
     * @throws SQLException if the query fails
     */
    public static int getYearFlightCount(Connection connection, int fleetId, int airframeId, int year)
            throws SQLException {
        return getFlightCount(connection, fleetId, airframeId, "v_fleet_yearly_flight_counts", "year = " + year);
    }

    /**
     * Returns the number of flights recorded so far in the current UTC calendar year across all fleets, optionally
     * restricted to one airframe.
     *
     * @param connection the database connection
     * @param airframeId the airframe to filter by, or a value &lt;= 0 for all airframes
     * @return the aggregate current-year flight count
     * @throws SQLException if the query fails
     */
    public static int getAggregateCurrentYearFlightCount(Connection connection, int airframeId) throws SQLException {
        return getAggregateYearFlightCount(connection, airframeId, TimeUtils.getCurrentYearUTC());
    }

    /**
     * Returns the number of flights in a specific calendar year across all fleets, optionally restricted to one
     * airframe.
     *
     * @param connection the database connection
     * @param airframeId the airframe to filter by, or a value &lt;= 0 for all airframes
     * @param year the calendar year to query
     * @return the aggregate flight count for that year
     * @throws SQLException if the query fails
     */
    public static int getAggregateYearFlightCount(Connection connection, int airframeId, int year) throws SQLException {
        return getAggregateFlightCount(connection, airframeId, "v_aggregate_yearly_flight_counts", "year = " + year);
    }

    /**
     * Returns the number of flights in a specific year and month for a fleet, optionally restricted to one
     * airframe.
     *
     * @param connection the database connection
     * @param fleetId the fleet to scope to
     * @param airframeId the airframe to filter by, or a value &lt;= 0 for all airframes
     * @param year the calendar year to query
     * @param month the month of the year to query (1-12)
     * @return the flight count for that month
     * @throws SQLException if the query fails
     */
    public static int getMonthFlightCount(Connection connection, int fleetId, int airframeId, int year, int month)
            throws SQLException {
        return getFlightCount(
                connection,
                fleetId,
                airframeId,
                "v_fleet_monthly_flight_counts",
                "year = " + year + " AND month = " + month);
    }

    /**
     * Returns the number of flights in a specific year and month across all fleets, optionally restricted to one
     * airframe.
     *
     * @param connection the database connection
     * @param airframeId the airframe to filter by, or a value &lt;= 0 for all airframes
     * @param year the calendar year to query
     * @param month the month of the year to query (1-12)
     * @return the aggregate flight count for that month
     * @throws SQLException if the query fails
     */
    public static int getAggregateMonthFlightCount(Connection connection, int airframeId, int year, int month)
            throws SQLException {
        return getAggregateFlightCount(
                connection, airframeId, "v_fleet_monthly_flight_counts", "year = " + year + " AND month = " + month);
    }

    /**
     * Returns the number of flights over the last 30 days for a fleet, optionally restricted to one airframe.
     *
     * @param connection the database connection
     * @param fleetId the fleet to scope to
     * @param airframeId the airframe to filter by, or a value &lt;= 0 for all airframes
     * @return the 30-day flight count
     * @throws SQLException if the query fails
     */
    public static int get30DayFlightCount(Connection connection, int fleetId, int airframeId) throws SQLException {
        return getFlightCount(connection, fleetId, airframeId, "v_fleet_30_day_flight_counts", null);
    }

    /**
     * Returns the number of flights over the last 30 days across all fleets, optionally restricted to one airframe.
     *
     * @param connection the database connection
     * @param airframeId the airframe to filter by, or a value &lt;= 0 for all airframes
     * @return the aggregate 30-day flight count
     * @throws SQLException if the query fails
     */
    public static int getAggregate30DayFlightCount(Connection connection, int airframeId) throws SQLException {
        return getAggregateFlightCount(connection, airframeId, "v_aggregate_30_day_flight_counts", null);
    }
}
