package org.ngafid.www.routes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.javalin.Javalin;
import io.javalin.http.Context;

import org.ngafid.core.Database;
import org.ngafid.core.accounts.User;
import org.ngafid.core.event.EventDefinition;
import org.ngafid.core.flights.Airframes;
import org.ngafid.core.flights.DoubleTimeSeries;
import org.ngafid.www.ErrorResponse;
import org.ngafid.www.EventStatistics;
import org.ngafid.www.Navbar;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import org.ngafid.www.WebServer;

public class EventJavalinRoutes {
    private static final Logger LOG = Logger.getLogger(EventJavalinRoutes.class.getName());
    public static final Gson GSON = WebServer.gson;



    public static void getEventDefinition(Context ctx) {
        final String templateFile = "event_definitions_display.html";

        try (Connection connection = Database.getConnection()) {
            final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
            final int fleetId = user.getFleetId();

            Map<String, Object> scopes = new HashMap<>();
            scopes.put("navbar_js", Navbar.getJavascript(ctx));

            long startTime = System.currentTimeMillis();
            scopes.put("events_js",
                    // "var eventStats = JSON.parse('" + gson.toJson(eventStatistics) + "');\n"
                    "var eventNames = JSON.parse('" + GSON.toJson(EventDefinition.getUniqueNames(connection, fleetId)) + "');\n");

            long endTime = System.currentTimeMillis();

            LOG.info("Event Definitions to JSON took " + (endTime - startTime) + "ms.");
            ctx.header("Content-Type", "text/html; charset=UTF-8");
            ctx.render(templateFile, scopes);
        } catch (SQLException e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    public static void getEventCreator(Context ctx) {
        final String templateFile = "create_event.html";

        try (Connection connection = Database.getConnection()) {
            Map<String, Object> scopes = new HashMap<String, Object>();

            scopes.put("navbar_js", Navbar.getJavascript(ctx));

            User user = ctx.sessionAttribute("user");
            if (user == null) {
                LOG.severe("INVALID ACCESS: user was not logged in.");
                ctx.status(401);
                ctx.json(new ErrorResponse("Not Logged In", "No user logged in."));
                return;
            }

            int fleetId = user.getFleetId();
            if (!user.isAdmin()) {
                LOG.severe("INVALID ACCESS: user does not have access to this page.");
                ctx.status(401);
                ctx.json(new ErrorResponse("Not Admin", "No permissions to create events."));
                return;
            }

            scopes.put("create_event_js", "var airframes = JSON.parse('" + GSON.toJson(Airframes.getAll(connection)) + "');\n" + "var doubleTimeSeriesNames = JSON.parse('" + GSON.toJson(DoubleTimeSeries.getAllNames(connection, fleetId)) + "');\n" + "var airframeMap = JSON.parse('" + GSON.toJson(Airframes.getIdToNameMap(connection)) + "');\n");

            ctx.header("Content-Type", "text/html; charset=UTF-8");
            ctx.render(templateFile, scopes);

        } catch (SQLException e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    public static void getEventManager(Context ctx) {
        final String templateFile = "manage_events.html";

        try (Connection connection = Database.getConnection()) {
            Map<String, Object> scopes = new HashMap<String, Object>();

            User user = ctx.sessionAttribute("user");
            if (user == null) {
                LOG.severe("INVALID ACCESS: user was not logged in.");
                ctx.status(401);
                ctx.json(new ErrorResponse("Not Logged In", "No user logged in."));
                return;
            }

            int fleetId = user.getFleetId();

            if (!user.isAdmin()) {
                LOG.severe("INVALID ACCESS: user does not have access to this page.");
                ctx.status(401);
                ctx.json(new ErrorResponse("Not Admin", "No permissions to manage events."));
                return;
            }

            scopes.put("navbar_js", Navbar.getJavascript(ctx));
            scopes.put("event_manager_js", "var eventDefinitions = JSON.parse('" + GSON.toJson(EventDefinition.getAll(connection)) + "');\n" + "var airframes = JSON.parse('" + GSON.toJson(Airframes.getAll(connection)) + "');\n" + "var doubleTimeSeriesNames = JSON.parse('" + GSON.toJson(DoubleTimeSeries.getAllNames(connection, fleetId)) + "');\n" + "var airframeMap = JSON.parse('" + GSON.toJson(Airframes.getIdToNameMap(connection)) + "');\n");

            ctx.header("Content-Type", "text/html; charset=UTF-8");
            ctx.render(templateFile, scopes);
        } catch (Exception e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    public static void getUpdateEvent(Context ctx) {
        final String templateFile = "update_event.html";

        try (Connection connection = Database.getConnection()) {
            Map<String, Object> scopes = new HashMap<String, Object>();
            scopes.put("navbar_js", Navbar.getJavascript(ctx));

            final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
            final int fleetId = user.getFleetId();

            if (!user.isAdmin()) {
                LOG.severe("INVALID ACCESS: user does not have access to this page.");
                ctx.status(401);
                ctx.result("User did not have access to this page.");
                return;
            }

            scopes.put("update_event_js", "var airframes = JSON.parse('" + GSON.toJson(Airframes.getAll(connection)) + "');\n" + "var doubleTimeSeriesNames = JSON.parse('" + GSON.toJson(DoubleTimeSeries.getAllNames(connection, fleetId)) + "');\n" + "var eventDefinitions = JSON.parse('" + GSON.toJson(EventDefinition.getAll(connection)) + "');\n" + "var airframeMap = JSON.parse('" + GSON.toJson(Airframes.getIdToNameMap(connection)) + "');\n");

            ctx.header("Content-Type", "text/html; charset=UTF-8");
            ctx.render(templateFile, scopes);
        } catch (SQLException e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    public static void bindRoutes(Javalin app) {
        // app.get("/protected/manage_event_definitions", EventJavalinRoutes::getAllEventDefinitions);

        app.get("/protected/event_definitions", EventJavalinRoutes::getEventDefinition);
        // app.put("/protected/event_definitions", EventJavalinRoutes::putEventDefinitions);
        // app.delete("/protected/event_definitions", EventJavalinRoutes::deleteEventDefinitions);

        // app.get("/protected/get_event_description", EventJavalinRoutes::getEventDescription);
        // app.get("/protected/get_all_event_descriptions", EventJavalinRoutes::getAllEventDescriptions);
        // app.get("/protected/event_counts", EventJavalinRoutes::getEventCounts);

        app.get("/protected/create_event", EventJavalinRoutes::getEventCreator);
        // app.post("/protected/create_event", EventJavalinRoutes::postCreateEvent);

        app.get("/protected/manage_events", EventJavalinRoutes::getEventManager);

        // app.post("/protected/events", EventJavalinRoutes::postEvents);
        // app.post("/protected/event_metadata", EventJavalinRoutes::postEventMetaData);
        // app.post("/protected/event_stat", EventJavalinRoutes::postEventStatistics);

        app.get("/protected/update_event", EventJavalinRoutes::getUpdateEvent);
        // app.post("/protected/update_event", EventJavalinRoutes::postUpdateEvent);
    }
}
