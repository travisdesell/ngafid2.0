package org.ngafid.www.routes;

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.ngafid.core.Database;
import org.ngafid.core.accounts.Fleet;
import org.ngafid.core.accounts.User;
import org.ngafid.core.event.EventDefinition;
import org.ngafid.core.flights.Airframes;
import org.ngafid.core.flights.Flight;
import org.ngafid.core.flights.Tails;
import org.ngafid.www.uploads.UploadStatistics;
import org.ngafid.www.ErrorResponse;
import org.ngafid.www.EventStatistics;
import org.ngafid.www.Navbar;
import org.ngafid.www.flights.FlightStatistics;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import static org.ngafid.www.WebServer.gson;

public class StatisticsJavalinRoutes {
    public static final Logger LOG = Logger.getLogger(StatisticsJavalinRoutes.class.getName());

    public static class StatFetcher {
        final Connection connection;
        final User user;
        final int fleetId;
        final boolean aggregate;

        public StatFetcher(Connection connection, Context context, boolean aggregate) {
            this(connection, SessionUtility.INSTANCE.getUser(context), aggregate);
        }

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
            return this.fleetId <= 0;
        }

        public Double flightTime() throws SQLException {
            if (aggregate()) {
                return FlightStatistics.getAggregateTotalFlightTime(connection, 0);
            } else {
                return FlightStatistics.getTotalFlightTime(connection, fleetId, 0);
            }
        }

        public Double yearFlightTime() throws SQLException {
            if (aggregate()) {
                return FlightStatistics.getAggregateCurrentYearFlightTime(connection, 0);
            } else {
                return FlightStatistics.getCurrentYearFlightTime(connection, fleetId, 0);
            }
        }

        public Double monthFlightTime() throws SQLException {
            if (aggregate()) {
                return FlightStatistics.getAggregate30DayFlightTime(connection, 0);
            }
            return FlightStatistics.get30DayFlightTime(connection, fleetId, 0);
        }

        public Integer numberFlights() throws SQLException {
            if (aggregate()) {
                return FlightStatistics.getAggregateTotalFlightCount(connection, 0);
            } else {
                return FlightStatistics.getTotalFlightCount(connection, fleetId, 0);
            }
        }

        public Integer numberAircraft() throws SQLException {
            return Tails.getNumberTails(connection, fleetId);
        }

        public Integer yearNumberFlights() throws SQLException {
            if (aggregate()) {
                return FlightStatistics.getAggregateCurrentYearFlightCount(connection, 0);
            } else {
                return FlightStatistics.getCurrentYearFlightCount(connection, fleetId, 0);
            }
        }

        public Integer monthNumberFlights() throws SQLException {
            if (aggregate()) {
                return FlightStatistics.getAggregate30DayFlightCount(connection, 0);
            } else {
                return FlightStatistics.get30DayFlightCount(connection, fleetId, 0);
            }
        }

        public Integer totalEvents() throws SQLException {
            if (aggregate()) {
                return EventStatistics.getAggregateTotalEventCount(connection);
            } else {
                return EventStatistics.getTotalEventCount(connection, fleetId);
            }
        }

        public Integer yearEvents() throws SQLException {
            if (aggregate()) {
                return EventStatistics.getAggregateCurrentYearEventCount(connection);
            } else {
                return EventStatistics.getCurrentYearEventCount(connection, fleetId);
            }
        }

        public Integer monthEvents() throws SQLException {
            if (aggregate()) {
                return EventStatistics.getAggregateCurrentMonthEventCount(connection);
            } else {
                return EventStatistics.getCurrentMonthEventCount(connection, fleetId);
            }
        }

        public Integer numberFleets() throws SQLException {
            return aggregate ? Fleet.getNumberFleets(connection) : null;
        }

        public Integer numberUsers() throws SQLException {
            return User.getNumberUsers(connection, fleetId);
        }

        public UploadStatistics.UploadCounts getUploadCounts() throws SQLException {
            if (aggregate()) {
                return UploadStatistics.getAggregateUploadCounts(connection);
            } else {
                return UploadStatistics.getUploadCounts(connection, fleetId);
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
            return getUploadCounts().errorUploadCount();
        }

        public Integer uploadsWithWarning() throws SQLException {
            return getUploadCounts().warningUploadCount();
        }

        public Integer flightsWithWarning() throws SQLException {
            return getUploadCounts().warningUploadCount();
        }

        public Integer flightsWithError() throws SQLException {
            return getUploadCounts().errorUploadCount();
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
            scopes.put("fleet_info_js", "var airframes = " + gson.toJson(Airframes.getAll(connection)) + ";\n");
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
            String fleetInfo = "var airframes = " + gson.toJson(Airframes.getAll(connection)) + ";\n" + "var eventNames = " + gson.toJson(EventDefinition.getUniqueNames(connection)) + ";\n" + "var tagNames = " + gson.toJson(Flight.getAllTagNames(connection)) + ";\n";

            scopes.put("fleet_info_js", fleetInfo);
            long endTime = System.currentTimeMillis();
            LOG.info("getting aggreagte data info took " + (endTime - startTime) + "ms.");

            ctx.header("Content-Type", "text/html; charset=UTF-8");
            ctx.render(templateFile, scopes);
        } catch (SQLException e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    public static void getAllEventCountsByAirframe(Context ctx, boolean aggregate) {
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

            Map<String, EventStatistics.EventCounts> eventCountsMap = EventStatistics.getEventCounts(connection, fleetId, LocalDate.parse(startDate), LocalDate.parse(endDate));
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
        final boolean aggregateTrendsPage = Boolean.parseBoolean(Objects.requireNonNull(ctx.queryParam("aggregatePage")));
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

                map = EventStatistics.getMonthlyEventCounts(connection, -1, LocalDate.parse(startDate), LocalDate.parse(endDate));
            } else {

                int fleetId = user.getFleetId();
                // check to see if the user has upload access for this fleet.
                if (!user.hasViewAccess(fleetId)) {
                    LOG.severe("INVALID ACCESS: user did not have access view imports for this fleet.");
                    ctx.status(401);
                    ctx.result("User did not have access to view imports for this fleet.");
                    return;
                }

                map = EventStatistics.getMonthlyEventCounts(connection, fleetId, LocalDate.parse(startDate), LocalDate.parse(endDate));
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

            scopes.put("events_js",
                    // "var eventStats = JSON.parse('" + gson.toJson(eventStatistics) + "');\n"
                    "var eventDefinitions = JSON.parse('" + gson.toJson(EventDefinition.getAll(connection)) + "');\n" + "var airframeMap = JSON.parse('" + gson.toJson(Airframes.getIdToNameMap(connection, fleetId)) + "');\n");

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
