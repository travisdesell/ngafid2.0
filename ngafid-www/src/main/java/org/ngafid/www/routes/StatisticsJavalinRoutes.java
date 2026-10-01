package org.ngafid.www.routes;

import static org.ngafid.www.WebServer.GSON;

import io.javalin.Javalin;
import io.javalin.http.Context;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import org.ngafid.core.Database;
import org.ngafid.core.accounts.Fleet;
import org.ngafid.core.accounts.User;
import org.ngafid.core.event.EventDefinition;
import org.ngafid.core.flights.Airframes;
import org.ngafid.core.flights.Flight;
import org.ngafid.core.flights.Tails;
import org.ngafid.www.ErrorResponse;
import org.ngafid.www.EventStatistics;
import org.ngafid.www.Navbar;
import org.ngafid.www.flights.FlightStatistics;
import org.ngafid.www.uploads.UploadStatistics;

public class StatisticsJavalinRoutes {
    public static final Logger LOG = Logger.getLogger(StatisticsJavalinRoutes.class.getName());

    private StatisticsJavalinRoutes() {}

    public static class StatFetcher {
        private final Connection connection;
        private final Context context;
        private final User user;
        private final int fleetId;
        private final boolean aggregate;

        /**
         * Convenience constructor that resolves the current user from the request session, then delegates to
         * {@link #StatFetcher(Connection, Context, User, boolean)}.
         *
         * @param connection the database connection used for the statistics queries
         * @param context the Javalin request context, supplying the session user and query parameters
         * @param aggregate true to compute statistics across all fleets, false to scope them to the user's fleet
         */
        public StatFetcher(Connection connection, Context context, boolean aggregate) {
            this(connection, context, SessionUtility.INSTANCE.getUser(context), aggregate);
        }

        /**
         * Constructs a statistics fetcher. When {@code aggregate} is true the fleet id is set to -1 so every query
         * spans all fleets; otherwise it is the user's fleet id, scoping all statistics to that one fleet.
         *
         * @param connection the database connection used for the statistics queries
         * @param context the Javalin request context, read for the {@code startDate}, {@code endDate} and
         *     {@code airframeID} query parameters
         * @param user the user the statistics are computed for (determines the fleet when not aggregating)
         * @param aggregate true to compute statistics across all fleets, false to scope them to the user's fleet
         */
        public StatFetcher(Connection connection, Context context, User user, boolean aggregate) {
            this.connection = connection;
            this.context = context;
            this.user = user;

            if (aggregate) {
                this.fleetId = -1;
            } else {
                this.fleetId = user.getFleetId();
            }

            this.aggregate = aggregate;
        }

        public Connection getConnection() {
            return connection;
        }

        public Context getContext() {
            return context;
        }

        public User getUser() {
            return user;
        }

        public int getFleetId() {
            return fleetId;
        }

        public boolean isAggregate() {
            return aggregate;
        }

        boolean aggregate() {
            return this.fleetId <= 0;
        }

        /**
         * Returns the total flight time, in seconds, for this fetcher's scope. Honors the {@code startDate},
         * {@code endDate} and optional {@code airframeID} query parameters (defaulting to all dates and all
         * airframes), and uses the aggregate (all-fleet) or per-fleet {@link FlightStatistics} query accordingly.
         *
         * @return the total flight time in seconds
         * @throws SQLException if the query fails
         */
        public Double flightTime() throws SQLException {

            final String startDateIn = context.queryParam("startDate");
            final String endDateIn = context.queryParam("endDate");

            final LocalDate startDate = startDateIn != null ? LocalDate.parse(startDateIn) : LocalDate.MIN;
            final LocalDate endDate = endDateIn != null ? LocalDate.parse(endDateIn) : LocalDate.MAX;

            final String airframeIDParam = context.queryParam("airframeID");
            final int airframeID = airframeIDParam != null ? Integer.parseInt(airframeIDParam) : -1;

            if (aggregate())
                return FlightStatistics.getAggregateTotalFlightTimeDated(connection, startDate, endDate, airframeID);
            else return FlightStatistics.getTotalFlightTimeDated(connection, fleetId, startDate, endDate, airframeID);
        }

        /**
         * Returns the total flight time, in seconds, accumulated so far in the current calendar year for this
         * fetcher's scope (aggregate across all fleets, or the user's fleet).
         *
         * @return the current-year flight time in seconds
         * @throws SQLException if the query fails
         */
        public Double yearFlightTime() throws SQLException {
            if (aggregate()) {
                return FlightStatistics.getAggregateCurrentYearFlightTime(connection, 0);
            } else {
                return FlightStatistics.getCurrentYearFlightTime(connection, fleetId, 0);
            }
        }

        /**
         * Returns the total flight time, in seconds, over the last 30 days for this fetcher's scope (aggregate
         * across all fleets, or the user's fleet).
         *
         * @return the 30-day flight time in seconds
         * @throws SQLException if the query fails
         */
        public Double monthFlightTime() throws SQLException {
            if (aggregate()) {
                return FlightStatistics.getAggregate30DayFlightTime(connection, 0);
            }
            return FlightStatistics.get30DayFlightTime(connection, fleetId, 0);
        }

        /**
         * Returns the number of flights for this fetcher's scope, honoring the {@code startDate}, {@code endDate}
         * and optional {@code airframeID} query parameters (defaulting to all dates and all airframes), using the
         * aggregate or per-fleet query accordingly.
         *
         * @return the flight count
         * @throws SQLException if the query fails
         */
        public Integer numberFlights() throws SQLException {

            final String startDateIn = context.queryParam("startDate");
            final String endDateIn = context.queryParam("endDate");

            final LocalDate startDate = startDateIn != null ? LocalDate.parse(startDateIn) : LocalDate.MIN;
            final LocalDate endDate = endDateIn != null ? LocalDate.parse(endDateIn) : LocalDate.MAX;

            final String airframeIDParam = context.queryParam("airframeID");
            final int airframeID = airframeIDParam != null ? Integer.parseInt(airframeIDParam) : -1;

            if (aggregate())
                return FlightStatistics.getAggregateTotalFlightCountDated(connection, startDate, endDate, airframeID);
            else return FlightStatistics.getTotalFlightCountDated(connection, fleetId, startDate, endDate, airframeID);
        }

        /**
         * Returns the number of distinct aircraft (tail numbers) registered to this fetcher's fleet.
         *
         * @return the aircraft count for the fleet
         * @throws SQLException if the query fails
         */
        public Integer numberAircraft() throws SQLException {
            return Tails.getNumberTails(connection, fleetId);
        }

        /**
         * Returns the number of flights recorded so far in the current calendar year for this fetcher's scope.
         *
         * @return the current-year flight count
         * @throws SQLException if the query fails
         */
        public Integer yearNumberFlights() throws SQLException {
            if (aggregate()) {
                return FlightStatistics.getAggregateCurrentYearFlightCount(connection, 0);
            } else {
                return FlightStatistics.getCurrentYearFlightCount(connection, fleetId, 0);
            }
        }

        /**
         * Returns the number of flights recorded over the last 30 days for this fetcher's scope.
         *
         * @return the 30-day flight count
         * @throws SQLException if the query fails
         */
        public Integer monthNumberFlights() throws SQLException {
            if (aggregate()) {
                return FlightStatistics.getAggregate30DayFlightCount(connection, 0);
            } else {
                return FlightStatistics.get30DayFlightCount(connection, fleetId, 0);
            }
        }

        /**
         * Returns the total number of detected events for this fetcher's scope, honoring the {@code startDate},
         * {@code endDate} and optional {@code airframeID} query parameters (defaulting to all dates and all
         * airframes), using the aggregate or per-fleet {@link EventStatistics} query accordingly.
         *
         * @return the event count
         * @throws SQLException if the query fails
         */
        public Integer totalEvents() throws SQLException {

            final String startDateIn = context.queryParam("startDate");
            final String endDateIn = context.queryParam("endDate");

            final LocalDate startDate = startDateIn != null ? LocalDate.parse(startDateIn) : LocalDate.MIN;
            final LocalDate endDate = endDateIn != null ? LocalDate.parse(endDateIn) : LocalDate.MAX;

            final String airframeIDParam = context.queryParam("airframeID");
            final int airframeID = airframeIDParam != null ? Integer.parseInt(airframeIDParam) : -1;

            if (aggregate())
                return EventStatistics.getAggregateTotalEventCountDated(connection, startDate, endDate, airframeID);
            else return EventStatistics.getTotalEventCountDated(connection, fleetId, startDate, endDate, airframeID);
        }

        /**
         * Returns the number of events detected so far in the current calendar year for this fetcher's scope.
         *
         * @return the current-year event count
         * @throws SQLException if the query fails
         */
        public Integer yearEvents() throws SQLException {
            if (aggregate()) {
                return EventStatistics.getAggregateCurrentYearEventCount(connection);
            } else {
                return EventStatistics.getCurrentYearEventCount(connection, fleetId);
            }
        }

        /**
         * Returns the number of events detected in the current month for this fetcher's scope.
         *
         * @return the current-month event count
         * @throws SQLException if the query fails
         */
        public Integer monthEvents() throws SQLException {
            if (aggregate()) {
                return EventStatistics.getAggregateCurrentMonthEventCount(connection);
            } else {
                return EventStatistics.getCurrentMonthEventCount(connection, fleetId);
            }
        }

        /**
         * Returns the total number of fleets, but only in aggregate mode; returns {@code null} for a single-fleet
         * fetcher since the count is not meaningful there.
         *
         * @return the number of fleets when aggregating, or {@code null} otherwise
         * @throws SQLException if the query fails
         */
        public Integer numberFleets() throws SQLException {
            return aggregate ? Fleet.getNumberFleets(connection) : null;
        }

        /**
         * Returns the number of users for this fetcher's scope (all users when aggregating, since the fleet id is
         * -1, otherwise the users belonging to the fleet).
         *
         * @return the user count
         * @throws SQLException if the query fails
         */
        public Integer numberUsers() throws SQLException {
            return User.getNumberUsers(connection, fleetId);
        }

        /**
         * Returns the upload counts (total, OK, warning, error) for this fetcher's scope over the {@code startDate}
         * /{@code endDate} query-parameter range (defaulting to all dates), using the aggregate or per-fleet query.
         * This is the shared source for the individual {@code uploads*} accessors below.
         *
         * @return the upload counts for the scope and date range
         * @throws SQLException if the query fails
         */
        public UploadStatistics.UploadCounts getUploadCounts() throws SQLException {

            final String startDateIn = context.queryParam("startDate");
            final String endDateIn = context.queryParam("endDate");

            final LocalDate startDate = startDateIn != null ? LocalDate.parse(startDateIn) : LocalDate.MIN;
            final LocalDate endDate = endDateIn != null ? LocalDate.parse(endDateIn) : LocalDate.MAX;

            if (aggregate()) {
                return UploadStatistics.getAggregateUploadCountsDated(connection, startDate, endDate);
            } else {
                return UploadStatistics.getUploadCountsDated(connection, fleetId, startDate, endDate);
            }
        }

        /**
         * Returns the total number of uploads for this fetcher's scope and date range.
         *
         * @return the total upload count
         * @throws SQLException if the query fails
         */
        public Integer uploads() throws SQLException {
            return getUploadCounts().count();
        }

        /**
         * Returns the number of uploads that imported cleanly (no warnings or errors) for this fetcher's scope and
         * date range.
         *
         * @return the count of successfully imported uploads
         * @throws SQLException if the query fails
         */
        public Integer uploadsOK() throws SQLException {
            return getUploadCounts().okUploadCount();
        }

        /**
         * Returns the number of uploads that have not been imported, computed as the total minus those that
         * finished OK, with warnings, or with errors (i.e. uploads still pending or otherwise unaccounted for).
         *
         * @return the count of not-yet-imported uploads
         * @throws SQLException if the query fails
         */
        public Integer uploadsNotImported() throws SQLException {
            var counts = getUploadCounts();
            return counts.count() - (counts.okUploadCount() + counts.warningUploadCount() + counts.errorUploadCount());
        }

        /**
         * Returns the number of uploads that finished with at least one error, for this fetcher's scope and date
         * range.
         *
         * @return the count of uploads with errors
         * @throws SQLException if the query fails
         */
        public Integer uploadsWithError() throws SQLException {
            return getUploadIssueCounts().errorUploadCount();
        }

        /**
         * Returns the number of uploads that finished with warnings (but no errors), for this fetcher's scope and
         * date range.
         *
         * @return the count of uploads with warnings
         * @throws SQLException if the query fails
         */
        public Integer uploadsWithWarning() throws SQLException {
            return getUploadCounts().warningUploadCount();
        }

        /**
         * Returns the combined upload and flight outcome counts for this fetcher's scope over the {@code startDate}
         * /{@code endDate} query-parameter range (defaulting to all dates), in a single query. Passes a null fleet
         * id when aggregating so the query spans all fleets.
         *
         * @return the combined upload/flight outcome counts
         * @throws SQLException if the query fails
         */
        public UploadStatistics.UploadOutcomeCounts uploadOutcomes() throws SQLException {
            final String startDateIn = context.queryParam("startDate");
            final String endDateIn = context.queryParam("endDate");

            final LocalDate startDate = startDateIn != null ? LocalDate.parse(startDateIn) : LocalDate.MIN;
            final LocalDate endDate = endDateIn != null ? LocalDate.parse(endDateIn) : LocalDate.MAX;

            return UploadStatistics.getUploadOutcomeCountsDated(
                    connection, aggregate() ? null : fleetId, startDate, endDate);
        }

        /**
         * Returns the number of imported flights that have warnings, for this fetcher's scope and date range.
         *
         * @return the count of flights with warnings
         * @throws SQLException if the query fails
         */
        public Integer flightsWithWarning() throws SQLException {
            return getUploadIssueCounts().warningFlightCount();
        }

        /**
         * Returns the number of flights that imported successfully, for this fetcher's scope and date range.
         *
         * @return the count of successfully imported flights
         * @throws SQLException if the query fails
         */
        public Integer flightsImported() throws SQLException {
            return getUploadIssueCounts().successfulFlightCount();
        }

        /**
         * Returns the number of flights that failed to import (had errors), for this fetcher's scope and date
         * range.
         *
         * @return the count of flights with errors
         * @throws SQLException if the query fails
         */
        public Integer flightsWithError() throws SQLException {
            return getUploadIssueCounts().errorFlightCount();
        }

        private UploadStatistics.UploadIssueCounts getUploadIssueCounts() throws SQLException {
            final String startDateIn = context.queryParam("startDate");
            final String endDateIn = context.queryParam("endDate");

            final LocalDate startDate = startDateIn != null ? LocalDate.parse(startDateIn) : LocalDate.MIN;
            final LocalDate endDate = endDateIn != null ? LocalDate.parse(endDateIn) : LocalDate.MAX;

            return UploadStatistics.getUploadIssueCountsDated(connection, fleetId, startDate, endDate);
        }
    }

    /**
     * Builds a SQL predicate restricting rows to the given date range and, when an airframe is specified, to that
     * airframe. Delegates to {@link #buildDateClause} for the date portion and appends an {@code airframe_id}
     * equality unless {@code airframeID} is negative (meaning "all airframes").
     *
     * @param startDate the inclusive start of the date range
     * @param endDate the inclusive end of the date range
     * @param airframeID the airframe id to filter by, or a negative value for all airframes
     * @return a parenthesized SQL boolean expression suitable for a WHERE clause
     */
    public static String buildDateAirframeClause(LocalDate startDate, LocalDate endDate, int airframeID) {

        final String dateClause = buildDateClause(startDate, endDate);

        // Handle 'All Airframes'
        if (airframeID < 0) return dateClause;

        return String.format("(%s AND airframe_id = %d)", dateClause, airframeID);
    }

    /**
     * Builds a SQL predicate matching the given date range against the stored {@code year}/{@code month} columns.
     * When the range lies within a single year it produces a simple month-between clause; when it spans years it
     * produces a compound clause covering the partial start year, the partial end year, and any whole years in
     * between.
     *
     * @param startDate the inclusive start of the date range
     * @param endDate the inclusive end of the date range
     * @return a parenthesized SQL boolean expression over the year/month columns
     */
    public static String buildDateClause(LocalDate startDate, LocalDate endDate) {

        final int startYear = startDate.getYear();
        final int startMonth = startDate.getMonthValue();
        final int endYear = endDate.getYear();
        final int endMonth = endDate.getMonthValue();

        // Same year -> simple range
        if (startYear == endYear) {

            return String.format("(year = %d AND month >= %d AND month <= %d)", startYear, startMonth, endMonth);

            // Different years -> Resolve problems with month ranges
        } else {

            return String.format(
                    "((year = %d AND month >= %d) " + "OR (year = %d AND month <= %d) "
                            + "OR (year > %d AND year < %d))",
                    startYear, startMonth, endYear, endMonth, startYear, endYear);
        }
    }

    /**
     * Handles {@code GET /protected/aggregate}: renders the aggregate (cross-fleet) dashboard page, injecting the
     * list of all airframes for the client. Responds 401 if the user is not logged in or lacks aggregate-view
     * access, and 500 on a database error.
     *
     * @param ctx the Javalin request context, whose response is rendered or given an error status
     */
    public static void getAggregate(Context ctx) {
        final String templateFile = "aggregate.html";

        User user = ctx.sessionAttribute("user");
        if (user == null) {
            LOG.severe("INVALID ACCESS: user was not logged in.");
            ctx.status(401);
            return;
        }

        // check to see if the user has access to view aggregate information
        if (!user.hasAggregateView()) {
            LOG.severe("INVALID ACCESS: user did not have aggregate access to view aggregate dashboard.");
            ctx.status(401);
            ctx.result("User did not have aggregate access to view aggregate dashboard.");
            return;
        }

        try (Connection connection = Database.getConnection()) {
            Map<String, Object> scopes = new HashMap<String, Object>();

            scopes.put("navbar_js", Navbar.getJavascript(ctx));

            long startTime = System.currentTimeMillis();

            Airframes.AirframeNameID[] airframes = Airframes.getAllWithIds(connection);
            scopes.put("fleet_info_js", "var airframes = " + GSON.toJson(airframes) + ";\n");
            long endTime = System.currentTimeMillis();

            LOG.info("getting fleet info took " + (endTime - startTime) + "ms.");

            ctx.header("Content-Type", "text/html; charset=UTF-8");
            ctx.render(templateFile, scopes);
        } catch (SQLException e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    /**
     * Handles {@code GET /protected/aggregate_trends}: renders the aggregate trends page, injecting the airframes,
     * unique event names, and tag names needed by the client. Responds 401 if the user is not logged in or lacks
     * aggregate-view access, and 500 on a database error.
     *
     * @param ctx the Javalin request context, whose response is rendered or given an error status
     */
    public static void getAggregateTrends(Context ctx) {
        final String templateFile = "aggregate_trends.html";

        User user = ctx.sessionAttribute("user");
        if (user == null) {
            LOG.severe("INVALID ACCESS: user was not logged in.");
            ctx.status(401);
            return;
        }

        // check to see if the user has access to view aggregate information
        if (!user.hasAggregateView()) {
            LOG.severe("INVALID ACCESS: user did not have aggregate access to view aggregate dashboard.");
            ctx.status(401);
            ctx.result("User did not have aggregate access to view aggregate dashboard.");
            return;
        }

        try (Connection connection = Database.getConnection()) {
            Map<String, Object> scopes = new HashMap<String, Object>();

            scopes.put("navbar_js", Navbar.getJavascript(ctx));

            long startTime = System.currentTimeMillis();

            Airframes.AirframeNameID[] airframes = Airframes.getAllWithIds(connection);
            String fleetInfo = "var airframes = " + GSON.toJson(airframes) + ";\n" + "var eventNames = "
                    + GSON.toJson(EventDefinition.getUniqueNames(connection)) + ";\n" + "var tagNames = "
                    + GSON.toJson(Flight.getAllTagNames(connection)) + ";\n";

            scopes.put("fleet_info_js", fleetInfo);
            long endTime = System.currentTimeMillis();
            LOG.info("Getting aggregate data info took " + (endTime - startTime) + "ms.");

            ctx.header("Content-Type", "text/html; charset=UTF-8");
            ctx.render(templateFile, scopes);
        } catch (SQLException e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    /**
     * Handles a request for per-airframe event counts over the {@code startDate}/{@code endDate} range, writing a
     * JSON map of airframe to event counts. Requires a logged-in user with view access to their fleet, and
     * additionally aggregate-view access when {@code aggregate} is true (in which case counts span all fleets,
     * fleet id -1). Responds 401 on failed access checks and 500 on a database error.
     *
     * @param ctx the Javalin request context supplying the session user and date-range query parameters
     * @param aggregate true to return counts across all fleets, false to scope them to the user's fleet
     */
    public static void getAllEventCountsByAirframe(Context ctx, boolean aggregate) {
        // Defensive: check for user session
        User user = ctx.sessionAttribute("user");
        if (user == null) {
            LOG.severe("User session is null in getAllEventCountsByAirframe. Returning 401.");
            ctx.status(401).json(new ErrorResponse("Not logged in", "You must be logged in to access this endpoint."));
            return;
        }
        final String startDate = Objects.requireNonNull(ctx.queryParam("startDate"));
        final String endDate = Objects.requireNonNull(ctx.queryParam("endDate"));

        int fleetId = user.getFleetId();

        // check to see if the user has upload access for this fleet.
        if (!user.hasViewAccess(fleetId)) {
            LOG.severe("INVALID ACCESS: user did not have access to view events for this fleet.");
            ctx.status(401);
            ctx.result("User did not have access to view events for this fleet.");
            return;
        }

        if (aggregate && !user.hasAggregateView()) {
            LOG.severe("INVALID ACCESS: user did not have aggregate access to view all event counts.");
            ctx.status(401);
            ctx.result("User did not have aggregate access to view all event counts.");
            return;
        }

        try (Connection connection = Database.getConnection()) {
            if (aggregate) {
                fleetId = -1;
            }

            Map<String, EventStatistics.EventCounts> eventCountsMap = EventStatistics.getEventCounts(
                    connection, fleetId, LocalDate.parse(startDate), LocalDate.parse(endDate));
            ctx.json(eventCountsMap);
        } catch (SQLException e) {
            e.printStackTrace();
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    /**
     * Handles a request for the event statistics of a single airframe, identified by the {@code aid} path
     * parameter, writing the resulting {@link EventStatistics} as JSON scoped to the user's fleet. An {@code aid}
     * of 0 is treated as the generic (airframe-independent) statistics. Responds 500 on a database error.
     *
     * @param ctx the Javalin request context supplying the session user and the {@code aid} path parameter
     */
    public static void getOneEventCountsByAirframe(Context ctx) {
        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        final int fleetId = user.getFleetId();
        final int airframeNameId = Integer.parseInt(Objects.requireNonNull(ctx.pathParam("aid")));

        try (Connection connection = Database.getConnection()) {
            if (airframeNameId == 0) {
                ctx.json(new EventStatistics(connection, 0, "Generic", fleetId));
            } else {
                final Airframes.Airframe af = new Airframes.Airframe(connection, airframeNameId);
                ctx.json(new EventStatistics(connection, af.getId(), af.getName(), fleetId));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    /**
     * Handles a request for monthly event counts over the {@code startDate}/{@code endDate} range, writing the
     * result as JSON. The {@code aggregatePage} query parameter selects aggregate (all-fleet, requires
     * aggregate-view access) versus the user's fleet (requires fleet view access). When the optional
     * {@code eventName} parameter is present only that event's counts are returned; otherwise the full map is.
     * Responds 401 on failed access checks and 500 on a database error.
     *
     * @param ctx the Javalin request context supplying the session user and the date/scope/event query parameters
     */
    public static void getMonthlyEventCounts(Context ctx) {
        final String startDate = Objects.requireNonNull(ctx.queryParam("startDate"));
        final String endDate = Objects.requireNonNull(ctx.queryParam("endDate"));
        final boolean aggregateTrendsPage =
                Boolean.parseBoolean(Objects.requireNonNull(ctx.queryParam("aggregatePage")));
        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        final String eventName = ctx.queryParam("eventName"); // Might be null intentionally

        try (Connection connection = Database.getConnection()) {
            Map<String, Map<String, EventStatistics.MonthlyEventCounts>> map;

            if (aggregateTrendsPage) {
                if (!user.hasAggregateView()) {
                    LOG.severe("INVALID ACCESS: user did not have aggregate access to view aggregate trends page.");
                    ctx.status(401);
                    ctx.result("User did not have aggregate access to view aggregate trends page.");
                    return;
                }

                map = EventStatistics.getMonthlyEventCounts(
                        connection, -1, LocalDate.parse(startDate), LocalDate.parse(endDate));
            } else {

                int fleetId = user.getFleetId();
                // check to see if the user has upload access for this fleet.
                if (!user.hasViewAccess(fleetId)) {
                    LOG.severe("INVALID ACCESS: user did not have access view imports for this fleet.");
                    ctx.status(401);
                    ctx.result("User did not have access to view imports for this fleet.");
                    return;
                }

                map = EventStatistics.getMonthlyEventCounts(
                        connection, fleetId, LocalDate.parse(startDate), LocalDate.parse(endDate));
            }

            if (eventName == null) {
                LOG.info("");
                ctx.json(map);
            } else {
                ctx.json(map.get(eventName));
            }
        } catch (SQLException e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    /**
     * Handles {@code GET /protected/event_statistics}: renders the event-statistics page, injecting all event
     * definitions and the fleet's airframe id-to-name map for the client. Responds 500 on a database error.
     *
     * @param ctx the Javalin request context, whose response is rendered or given an error status
     */
    public static void getEventStatistics(Context ctx) {
        final String templateFile = "event_statistics.html";

        try (Connection connection = Database.getConnection()) {
            final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
            final int fleetId = user.getFleetId();
            Map<String, Object> scopes = new HashMap<>();

            scopes.put("navbar_js", Navbar.getJavascript(ctx));

            scopes.put(
                    "events_js",
                    // "var eventStats = JSON.parse('" + GSON.toJson(eventStatistics) + "');\n"
                    "var eventDefinitions = JSON.parse('" + GSON.toJson(EventDefinition.getAll(connection)) + "');\n"
                            + "var airframeMap = JSON.parse('"
                            + GSON.toJson(Airframes.getIdToNameMap(connection, fleetId)) + "');\n");

            ctx.header("Content-Type", "text/html; charset=UTF-8");
            ctx.render(templateFile, scopes);
        } catch (SQLException e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    /**
     * Registers this class's statistics routes (the aggregate dashboard, aggregate trends, and event-statistics
     * pages) on the given Javalin application.
     *
     * @param app the Javalin application to register the routes on
     */
    public static void bindRoutes(Javalin app) {
        app.get("/protected/aggregate", StatisticsJavalinRoutes::getAggregate);
        app.get("/protected/aggregate_trends", StatisticsJavalinRoutes::getAggregateTrends);

        // app.post("/protected/statistics/aggregate/event_counts", ctx -> postEventCounts(ctx, true));
        // app.post("/protected/statistics/all_event_counts", ctx -> postEventCounts(ctx, true));

        // app.post("/protected/statistics/event_counts", ctx -> postEventCounts(ctx, false));

        app.get("/protected/event_statistics", StatisticsJavalinRoutes::getEventStatistics);
        // app.post("/protected/monthly_event_counts", StatisticsJavalinRoutes::postMonthlyEventCounts);
    }
}
