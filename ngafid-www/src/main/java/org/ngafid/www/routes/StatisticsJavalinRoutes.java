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
import org.ngafid.core.flights.Airframes.AircraftCategory;
import org.ngafid.core.flights.Flight;
import org.ngafid.core.flights.Tails;
import org.ngafid.core.util.TimeUtils;
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

        public StatFetcher(Connection connection, Context context, boolean aggregate) {
            this(connection, context, SessionUtility.INSTANCE.getUser(context), aggregate);
        }

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

        private AircraftCategory aircraftCategory() {
            return AircraftCategory.fromQueryValue(context.queryParam("aircraftType"));
        }

        private Integer selectedFleetId() {
            return aggregate() ? null : fleetId;
        }

        private int selectedAirframeId() {
            final String airframeId = context.queryParam("airframeID");
            return airframeId != null ? Integer.parseInt(airframeId) : -1;
        }

        public Double flightTime() throws SQLException {

            final String startDateIn = context.queryParam("startDate");
            final String endDateIn = context.queryParam("endDate");

            final LocalDate startDate = startDateIn != null ? LocalDate.parse(startDateIn) : LocalDate.MIN;
            final LocalDate endDate = endDateIn != null ? LocalDate.parse(endDateIn) : LocalDate.MAX;

            final int airframeID = selectedAirframeId();

            if (airframeID < 0 && aircraftCategory() != AircraftCategory.ALL) {
                return FlightStatistics.getFlightTimeByAircraftCategory(
                        connection, selectedFleetId(), startDate, endDate, aircraftCategory());
            }

            if (aggregate())
                return FlightStatistics.getAggregateTotalFlightTimeDated(connection, startDate, endDate, airframeID);
            else return FlightStatistics.getTotalFlightTimeDated(connection, fleetId, startDate, endDate, airframeID);
        }

        public Double yearFlightTime() throws SQLException {
            final int airframeID = selectedAirframeId();
            if (airframeID < 0 && aircraftCategory() != AircraftCategory.ALL) {
                int year = TimeUtils.getCurrentYearUTC();
                return FlightStatistics.getFlightTimeByAircraftCategory(
                        connection,
                        selectedFleetId(),
                        LocalDate.of(year, 1, 1),
                        LocalDate.of(year, 12, 31),
                        aircraftCategory());
            }
            if (aggregate()) {
                return FlightStatistics.getAggregateCurrentYearFlightTime(connection, airframeID);
            } else {
                return FlightStatistics.getCurrentYearFlightTime(connection, fleetId, airframeID);
            }
        }

        public Double monthFlightTime() throws SQLException {
            final int airframeID = selectedAirframeId();
            if (airframeID < 0 && aircraftCategory() != AircraftCategory.ALL) {
                return FlightStatistics.get30DayFlightTimeByAircraftCategory(
                        connection, selectedFleetId(), aircraftCategory());
            }
            if (aggregate()) {
                return FlightStatistics.getAggregate30DayFlightTime(connection, airframeID);
            }
            return FlightStatistics.get30DayFlightTime(connection, fleetId, airframeID);
        }

        public Integer numberFlights() throws SQLException {

            final String startDateIn = context.queryParam("startDate");
            final String endDateIn = context.queryParam("endDate");

            final LocalDate startDate = startDateIn != null ? LocalDate.parse(startDateIn) : LocalDate.MIN;
            final LocalDate endDate = endDateIn != null ? LocalDate.parse(endDateIn) : LocalDate.MAX;

            final int airframeID = selectedAirframeId();

            if (airframeID < 0 && aircraftCategory() != AircraftCategory.ALL) {
                return FlightStatistics.getFlightCountByAircraftCategory(
                        connection, selectedFleetId(), startDate, endDate, aircraftCategory());
            }

            if (aggregate())
                return FlightStatistics.getAggregateTotalFlightCountDated(connection, startDate, endDate, airframeID);
            else return FlightStatistics.getTotalFlightCountDated(connection, fleetId, startDate, endDate, airframeID);
        }

        public Integer numberAircraft() throws SQLException {
            return Tails.getNumberTails(connection, fleetId, aircraftCategory(), selectedAirframeId());
        }

        public Integer yearNumberFlights() throws SQLException {
            final int airframeID = selectedAirframeId();
            if (airframeID < 0 && aircraftCategory() != AircraftCategory.ALL) {
                int year = TimeUtils.getCurrentYearUTC();
                return FlightStatistics.getFlightCountByAircraftCategory(
                        connection,
                        selectedFleetId(),
                        LocalDate.of(year, 1, 1),
                        LocalDate.of(year, 12, 31),
                        aircraftCategory());
            }
            if (aggregate()) {
                return FlightStatistics.getAggregateCurrentYearFlightCount(connection, airframeID);
            } else {
                return FlightStatistics.getCurrentYearFlightCount(connection, fleetId, airframeID);
            }
        }

        public Integer monthNumberFlights() throws SQLException {
            final int airframeID = selectedAirframeId();
            if (airframeID < 0 && aircraftCategory() != AircraftCategory.ALL) {
                return FlightStatistics.get30DayFlightCountByAircraftCategory(
                        connection, selectedFleetId(), aircraftCategory());
            }
            if (aggregate()) {
                return FlightStatistics.getAggregate30DayFlightCount(connection, airframeID);
            } else {
                return FlightStatistics.get30DayFlightCount(connection, fleetId, airframeID);
            }
        }

        public Integer totalEvents() throws SQLException {

            final String startDateIn = context.queryParam("startDate");
            final String endDateIn = context.queryParam("endDate");

            final LocalDate startDate = startDateIn != null ? LocalDate.parse(startDateIn) : LocalDate.MIN;
            final LocalDate endDate = endDateIn != null ? LocalDate.parse(endDateIn) : LocalDate.MAX;

            final int airframeID = selectedAirframeId();

            if (airframeID < 0 && aircraftCategory() != AircraftCategory.ALL) {
                return EventStatistics.getEventCountByAircraftCategory(
                        connection, selectedFleetId(), startDate, endDate, aircraftCategory());
            }

            if (aggregate())
                return EventStatistics.getAggregateTotalEventCountDated(connection, startDate, endDate, airframeID);
            else return EventStatistics.getTotalEventCountDated(connection, fleetId, startDate, endDate, airframeID);
        }

        public Integer yearEvents() throws SQLException {
            final int airframeID = selectedAirframeId();
            int year = TimeUtils.getCurrentYearUTC();
            LocalDate firstDay = LocalDate.of(year, 1, 1);
            LocalDate lastDay = LocalDate.of(year, 12, 31);
            if (airframeID < 0 && aircraftCategory() != AircraftCategory.ALL) {
                return EventStatistics.getEventCountByAircraftCategory(
                        connection, selectedFleetId(), firstDay, lastDay, aircraftCategory());
            }
            if (airframeID < 0) {
                return aggregate()
                        ? EventStatistics.getAggregateCurrentYearEventCount(connection)
                        : EventStatistics.getCurrentYearEventCount(connection, fleetId);
            }
            if (aggregate()) {
                return EventStatistics.getAggregateTotalEventCountDated(connection, firstDay, lastDay, airframeID);
            } else {
                return EventStatistics.getTotalEventCountDated(connection, fleetId, firstDay, lastDay, airframeID);
            }
        }

        public Integer monthEvents() throws SQLException {
            final int airframeID = selectedAirframeId();
            int year = TimeUtils.getCurrentYearUTC();
            int month = TimeUtils.getCurrentMonthUTC();
            LocalDate firstDay = LocalDate.of(year, month, 1);
            LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());
            if (airframeID < 0 && aircraftCategory() != AircraftCategory.ALL) {
                return EventStatistics.getEventCountByAircraftCategory(connection, selectedFleetId(), firstDay,
                        lastDay, aircraftCategory());
            }
            if (airframeID < 0) {
                return aggregate()
                        ? EventStatistics.getAggregateCurrentMonthEventCount(connection)
                        : EventStatistics.getCurrentMonthEventCount(connection, fleetId);
            }
            if (aggregate()) {
                return EventStatistics.getAggregateTotalEventCountDated(connection, firstDay, lastDay, airframeID);
            } else {
                return EventStatistics.getTotalEventCountDated(connection, fleetId, firstDay, lastDay, airframeID);
            }
        }

        public Integer numberFleets() throws SQLException {
            return aggregate ? Fleet.getNumberFleets(connection) : null;
        }

        public Integer numberUsers() throws SQLException {
            return User.getNumberUsers(connection, fleetId);
        }

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

        public Integer uploads() throws SQLException {
            return getUploadCounts().count();
        }

        public Integer uploadsOK() throws SQLException {
            return getUploadCounts().okUploadCount();
        }

        public Integer uploadsNotImported() throws SQLException {
            var counts = getUploadCounts();
            return counts.count() - (counts.okUploadCount() + counts.warningUploadCount() + counts.errorUploadCount());
        }

        public Integer uploadsWithError() throws SQLException {
            return getUploadIssueCounts().errorUploadCount();
        }

        public Integer uploadsWithWarning() throws SQLException {
            return getUploadCounts().warningUploadCount();
        }

        public UploadStatistics.UploadOutcomeCounts uploadOutcomes() throws SQLException {
            final String startDateIn = context.queryParam("startDate");
            final String endDateIn = context.queryParam("endDate");

            final LocalDate startDate = startDateIn != null ? LocalDate.parse(startDateIn) : LocalDate.MIN;
            final LocalDate endDate = endDateIn != null ? LocalDate.parse(endDateIn) : LocalDate.MAX;

            final int airframeId = selectedAirframeId();
            final AircraftCategory category = aircraftCategory();
            if (airframeId >= 0 || category != AircraftCategory.ALL) {
                int successfulFlightCount = UploadStatistics.getSuccessfulFlightCountDated(
                        connection, aggregate() ? null : fleetId, startDate, endDate, category, airframeId);
                return new UploadStatistics.UploadOutcomeCounts(0, 0, 0, 0, 0, successfulFlightCount, 0, 0);
            }

            return UploadStatistics.getUploadOutcomeCountsDated(
                    connection, aggregate() ? null : fleetId, startDate, endDate);
        }

        public Integer flightsWithWarning() throws SQLException {
            return getUploadIssueCounts().warningFlightCount();
        }

        public Integer flightsImported() throws SQLException {
            return getUploadIssueCounts().successfulFlightCount();
        }

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

    public static String buildDateAirframeClause(LocalDate startDate, LocalDate endDate, int airframeID) {

        final String dateClause = buildDateClause(startDate, endDate);

        // Handle 'All Airframes'
        if (airframeID < 0) return dateClause;

        return String.format("(%s AND airframe_id = %d)", dateClause, airframeID);
    }

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

            Airframes.TypedAirframeNameID[] airframes = Airframes.getAllWithIdsAndTypes(connection);
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

            Airframes.TypedAirframeNameID[] airframes = Airframes.getAllWithIdsAndTypes(connection);
            Map<String, String> aircraftTypesByName = new HashMap<>();
            for (Airframes.TypedAirframeNameID item : airframes) {
                aircraftTypesByName.put(item.name(), item.type());
            }
            String fleetInfo = "var airframes = " + GSON.toJson(airframes) + ";\n" + "var eventNames = "
                    + GSON.toJson(EventDefinition.getUniqueNames(connection)) + ";\n" + "var tagNames = "
                    + GSON.toJson(Flight.getAllTagNames(connection)) + ";\n" + "var aircraftTypesByName = "
                    + GSON.toJson(aircraftTypesByName) + ";\n";

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
            AircraftCategory category = AircraftCategory.fromQueryValue(ctx.queryParam("aircraftType"));
            if (category != AircraftCategory.ALL) {
                Map<String, String> typesByName = new HashMap<>();
                for (Airframes.TypedAirframeNameID airframe : Airframes.getAllWithIdsAndTypes(connection)) {
                    typesByName.put(airframe.name(), airframe.type());
                }
                eventCountsMap.entrySet().removeIf(entry -> !category.matches(typesByName.get(entry.getKey())));
            }
            ctx.json(eventCountsMap);
        } catch (SQLException e) {
            e.printStackTrace();
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

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
