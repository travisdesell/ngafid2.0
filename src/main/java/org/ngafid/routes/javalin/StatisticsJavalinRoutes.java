package org.ngafid.routes.javalin;

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.ngafid.Database;
import org.ngafid.accounts.Fleet;
import org.ngafid.accounts.User;
import org.ngafid.events.EventDefinition;
import org.ngafid.events.EventStatistics;
import org.ngafid.flights.*;
import org.ngafid.routes.ErrorResponse;
import org.ngafid.routes.MustacheHandler;
import org.ngafid.routes.Navbar;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import static org.ngafid.WebServer.gson;

public class StatisticsJavalinRoutes implements JavalinRoutes {
    private static final Logger LOG = Logger.getLogger(StatisticsJavalinRoutes.class.getName());

    public static class StatFetcher {
        public interface StatFunction<T> {
            T execute(StatFetcher f) throws SQLException;
        }

        public static Map<String, StatFunction<Object>> function_map = Map.ofEntries(Map.entry("flightTime", StatFetcher::flightTime), Map.entry("yearFlightTime", StatFetcher::yearFlightTime), Map.entry("monthFlightTime", StatFetcher::monthFlightTime),

                Map.entry("numberFlights", StatFetcher::numberFlights), Map.entry("numberAircraft", StatFetcher::numberAircraft), Map.entry("yearNumberFlights", StatFetcher::yearNumberFlights), Map.entry("monthNumberFlights", StatFetcher::monthNumberFlights), Map.entry("totalEvents", StatFetcher::totalEvents), Map.entry("yearEvents", StatFetcher::yearEvents), Map.entry("monthEvents", StatFetcher::monthEvents), Map.entry("numberFleets", StatFetcher::numberFleets), Map.entry("numberUsers", StatFetcher::numberUsers), Map.entry("uploads", StatFetcher::uploads), Map.entry("uploadsNotImported", StatFetcher::uploadsNotImported), Map.entry("uploadsWithError", StatFetcher::uploadsWithError), Map.entry("flightsWithWarning", StatFetcher::flightsWithWarning), Map.entry("flightsWithError", StatFetcher::flightsWithError));

        final Connection connection;
        final User user;
        final int fleetId;
        final boolean aggregate;

        public StatFetcher(Connection connection, User user, boolean aggregate) {
            this.connection = connection;
            this.user = user;

            if (aggregate) {
                this.fleetId = -1;
            } else {
                this.fleetId = user.getFleetId();
            }

            this.aggregate = aggregate;
        }

        boolean aggregate() {
            return this.fleetId > 0;
        }

        LocalDate thirtyDaysAgo() {
            return LocalDate.now().minusDays(30);
        }

        LocalDate firstOfMonth() {
            return LocalDate.now().with(TemporalAdjusters.firstDayOfMonth());
        }

        LocalDate firstOfYear() {
            return LocalDate.now().with(TemporalAdjusters.firstDayOfYear());
        }

        String lastThirtyDaysQuery() {
            return "start_time >= '" + thirtyDaysAgo().toString() + "'";
        }

        String yearQuery() {
            return "start_time >= '" + firstOfYear().toString() + "'";
        }

        Long flightTime() throws SQLException {
            return Flight.getTotalFlightTime(connection, fleetId, null);
        }

        Long yearFlightTime() throws SQLException {
            return Flight.getTotalFlightTime(connection, yearQuery(), fleetId);
        }

        Long monthFlightTime() throws SQLException {
            return Flight.getTotalFlightTime(connection, lastThirtyDaysQuery(), fleetId);
        }

        Integer numberFlights() throws SQLException {
            return Flight.getNumFlights(connection, fleetId);
        }

        Integer numberAircraft() throws SQLException {
            return Tails.getNumberTails(connection, fleetId);
        }

        Integer yearNumberFlights() throws SQLException {
            return Flight.getNumFlights(connection, yearQuery(), fleetId);
        }

        Integer monthNumberFlights() throws SQLException {
            return Flight.getNumFlights(connection, lastThirtyDaysQuery(), fleetId);
        }

        Integer totalEvents() throws SQLException {
            return EventStatistics.getEventCount(connection, fleetId, null, null);
        }

        Integer yearEvents() throws SQLException {
            return EventStatistics.getEventCount(connection, fleetId, firstOfYear(), null);
        }

        Integer monthEvents() throws SQLException {
            return EventStatistics.getEventCount(connection, fleetId, firstOfMonth(), null);
        }

        Integer numberFleets() throws SQLException {
            return aggregate ? Fleet.getNumberFleets(connection) : null;
        }

        Integer numberUsers() throws SQLException {
            return User.getNumberUsers(connection, fleetId);
        }

        Integer uploads() throws SQLException {
            return Upload.getNumUploads(connection, fleetId, "");
        }

        Integer uploadsNotImported() throws SQLException {
            return Upload.getNumUploads(connection, fleetId, " AND status = 'UPLOADED'");
        }

        Integer uploadsWithError() throws SQLException {
            return Upload.getNumUploads(connection, fleetId, " AND status = 'ERROR'");
        }

        Integer flightsWithWarning() throws SQLException {
            return FlightWarning.getCount(connection, fleetId);
        }

        Integer flightsWithError() throws SQLException {
            return FlightError.getCount(connection, fleetId);
        }
    }

    record SummaryStatistics(boolean aggregate,

                             int numberFlights, int numberAircraft, int yearNumberFlights, int monthNumberFlights,
                             int totalEvents, int yearEvents, int monthEvents, int numberUsers,

                             // These should be null if `aggregate` is false.
                             Integer numberFleets,

                             // Null if aggregate is true
                             Integer uploads, Integer uploadsNotImported, Integer uploadsWithError,
                             Integer flightsWithWarning, Integer flightsWithError,

                             long flightTime, long yearFlightTime, long monthFlightTime) {
    }

    public static void postStatistic(Context ctx, boolean aggregate) {
        final User user = ctx.sessionAttribute("user");

        try (Connection connection = Database.getConnection()) {
            Map<String, Object> statistics = new HashMap<>();
            StatFetcher fetcher = new StatFetcher(connection, user, aggregate);
            String[] stats;

            ctx.pathParam("splat");
            if (!ctx.pathParam("splat").isEmpty()) {
                stats = ctx.pathParam("splat").split("/");
            } else {
                stats = gson.fromJson(ctx.body(), String[].class);
            }


            for (String stat : stats) {
                statistics.put(stat, StatFetcher.function_map.get(stat).execute(fetcher));
            }

            ctx.json(statistics);
        } catch (SQLException e) {
            e.printStackTrace();
            ctx.json(new ErrorResponse(e));
        }
    }

    public static void postSummaryStatistics(Context ctx, boolean aggregate) {
        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        final int fleetId = aggregate ? -1 : user.getFleetId();

        if (!user.hasViewAccess(fleetId)) {
            LOG.severe("INVALID ACCESS: user did not have access to view events for this fleet.");
            ctx.status(401);
            ctx.result("User did not have access to view events for this fleet.");
            return;
        }

        // check to see if the user has access to view aggregate information
        if (aggregate && !user.hasAggregateView()) {
            LOG.severe("INVALID ACCESS: user did not have aggregate access to view aggregate dashboard.");
            ctx.status(401);
            ctx.result("User did not have aggregate access to view aggregate dashboard.");
            return;
        }

        LocalDate firstOfMonth = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth());
        LocalDate firstOfYear = LocalDate.now().with(TemporalAdjusters.firstDayOfYear());
        LocalDate lastThirtyDays = LocalDate.now().minusDays(30);

        String lastThirtyDaysQuery = "start_time >= '" + lastThirtyDays + "'";
        String yearQuery = "start_time >= '" + firstOfYear + "'";

        try (Connection connection = Database.getConnection()) {
            final int numberFlights = Flight.getNumFlights(connection, fleetId);
            final int numberAircraft = Tails.getNumberTails(connection, fleetId);
            final long flightTime = Flight.getTotalFlightTime(connection, fleetId, null);
            final int yearNumberFlights = Flight.getNumFlights(connection, yearQuery, fleetId);
            final long yearFlightTime = Flight.getTotalFlightTime(connection, yearQuery, fleetId);
            final int monthNumberFlights = Flight.getNumFlights(connection, lastThirtyDaysQuery, fleetId);
            final long monthFlightTime = Flight.getTotalFlightTime(connection, lastThirtyDaysQuery, fleetId);

            final int totalEvents = EventStatistics.getEventCount(connection, fleetId, null, null);
            final int yearEvents = EventStatistics.getEventCount(connection, fleetId, firstOfYear, null);
            final int monthEvents = EventStatistics.getEventCount(connection, fleetId, firstOfMonth, null);

            final int numberFleets = aggregate ? Fleet.getNumberFleets(connection) : null;

            final int numberUsers = User.getNumberUsers(connection, fleetId);
            final int uploads = Upload.getNumUploads(connection, fleetId, "");
            final int uploadsNotImported = Upload.getNumUploads(connection, fleetId, " AND status = 'UPLOADED'");
            final int uploadsWithError = Upload.getNumUploads(connection, fleetId, " AND status = 'ERROR'");
            final int flightsWithWarning = FlightWarning.getCount(connection, fleetId);
            final int flightsWithError = FlightError.getCount(connection, fleetId);

            ctx.json(new SummaryStatistics(aggregate, numberFlights, numberAircraft, yearNumberFlights, monthNumberFlights, totalEvents, yearEvents, monthEvents, numberUsers, numberFleets, uploads, uploadsNotImported, uploadsWithError, flightsWithWarning, flightsWithError, flightTime, yearFlightTime, monthFlightTime));
        } catch (SQLException e) {
            ctx.json(new ErrorResponse(e));
        }
    }


    public static void getAggregate(Context ctx) throws IOException {
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
            scopes.put("fleet_info_js", "var airframes = " + gson.toJson(Airframes.getAll(connection)) + ";\n");
            long endTime = System.currentTimeMillis();

            LOG.info("getting fleet info took " + (endTime - startTime) + "ms.");

            ctx.contentType("text/html");
            ctx.result(MustacheHandler.handle(templateFile, scopes));
        } catch (SQLException e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e));

        } catch (IOException e) {
            LOG.severe(e.toString());
        }
    }

    public static void getAggregateTrends(Context ctx) throws IOException {
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
            String fleetInfo = "var airframes = " + gson.toJson(Airframes.getAll(connection)) + ";\n" +
                    "var eventNames = " + gson.toJson(EventDefinition.getUniqueNames(connection)) + ";\n" +
                    "var tagNames = " + gson.toJson(Flight.getAllTagNames(connection)) + ";\n";

            scopes.put("fleet_info_js", fleetInfo);
            long endTime = System.currentTimeMillis();
            LOG.info("getting aggreagte data info took " + (endTime - startTime) + "ms.");

            ctx.contentType("text/html");
            ctx.result(MustacheHandler.handle(templateFile, scopes));
        } catch (SQLException e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e));
        } catch (IOException e) {
            LOG.severe(e.toString());
        }
    }

    private static void postEventCounts(Context ctx, boolean aggregate) {
        final String startDate = Objects.requireNonNull(ctx.queryParam("startDate"));
        final String endDate = Objects.requireNonNull(ctx.queryParam("endDate"));

        User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
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

            Map<String, EventStatistics.EventCounts> eventCountsMap = EventStatistics.getEventCounts(connection,
                    fleetId, LocalDate.parse(startDate), LocalDate.parse(endDate));
            ctx.json(eventCountsMap);
        } catch (SQLException e) {
            ctx.json(new ErrorResponse(e));
        }
    }

    @Override
    public void bindRoutes(Javalin app) {
        app.get("/protected/aggregate", StatisticsJavalinRoutes::getAggregate);
        app.get("/protected/aggregate_trends", StatisticsJavalinRoutes::getAggregateTrends);

        app.post("/protected/statistics/summary", ctx -> postSummaryStatistics(ctx, false));
        app.post("/protected/statistics/event_counts", ctx -> postEventCounts(ctx, false));
        app.post("/protected/statistics/*", ctx -> postStatistic(ctx, false));


        app.post("/protected/statistics/aggregate/summary", ctx -> postSummaryStatistics(ctx, true));
        app.post("/protected/statistics/aggregate/event_counts", ctx -> postEventCounts(ctx, true));
        app.post("/protected/statistics/aggregate/*", ctx -> postStatistic(ctx, true));
    }
}
