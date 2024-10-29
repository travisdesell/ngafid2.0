package org.ngafid.routes.javalin;

import io.javalin.http.Context;
import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.events.EventDefinition;
import org.ngafid.flights.Airframes;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.routes.ErrorResponse;
import org.ngafid.routes.MustacheHandler;
import org.ngafid.routes.Navbar;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.logging.Logger;

import static org.ngafid.WebServer.gson;

public class EventJavalinRoutes {
    private static final Logger LOG = Logger.getLogger(EventJavalinRoutes.class.getName());

    public static void getAllEventDefinitions(Context ctx) throws IOException {
        try (Connection connection = Database.getConnection()) {
            ctx.json(EventDefinition.getAll(connection));
        } catch (SQLException e) {
            LOG.severe(e.toString());
            ctx.status(500);
            ctx.result(e.toString());
        }
    }

    public static void getEventDefinition(Context ctx) {
        String templateFile = "event_definitions_display.html";

        try (Connection connection = Database.getConnection()) {
            User user = ctx.sessionAttribute("user");
            if (user == null) {
                LOG.severe("INVALID ACCESS: user was not logged in.");
                ctx.status(401);
                ctx.json(new ErrorResponse("Not Logged In", "No user logged in."));
                return;
            }

            int fleetId = user.getFleetId();

            Map<String, Object> scopes = new HashMap<>();
            scopes.put("navbar_js", Navbar.getJavascript(ctx));

            long startTime = System.currentTimeMillis();
            scopes.put("events_js",
                    // "var eventStats = JSON.parse('" + gson.toJson(eventStatistics) + "');\n"
                    "var eventNames = JSON.parse('" + gson.toJson(EventDefinition.getUniqueNames(connection, fleetId)) + "');\n");

            long endTime = System.currentTimeMillis();

            LOG.info("Event Definitions to JSON took " + (endTime - startTime) + "ms.");
            ctx.contentType("text/html");
            ctx.result(MustacheHandler.handle(templateFile, scopes));
        } catch (SQLException e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e));
        } catch (IOException e) {
            LOG.severe(e.toString());
        }
    }

    public static void getAllEventDescriptions(Context ctx) {
        final String query = "SELECT event_definitions.id, fleet_id, name, start_buffer, stop_buffer, airframe_id, condition_json, column_names, severity_column_names, severity_type, airframe " + "FROM event_definitions INNER JOIN airframes ON event_definitions.airframe_id=airframes.id";

        try (Connection connection = Database.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            LOG.info("preparedStatement: " + preparedStatement);

            ResultSet resultSet = preparedStatement.executeQuery();
            LOG.info("resultSet: " + resultSet);
            Map<String, Map<String, String>> definitions = new TreeMap<>();
            Map<Integer, String> airframeNames = new HashMap<>();

            while (resultSet.next()) {
                EventDefinition eventDefinition = new EventDefinition(resultSet);
                LOG.info("eventDefinition: " + eventDefinition);

                String text = eventDefinition.toHumanReadable();
                LOG.info("text: " + text);

                if (!definitions.containsKey(eventDefinition.getName())) {
                    definitions.put(eventDefinition.getName(), new HashMap<>());
                }

                if (!airframeNames.containsKey(eventDefinition.getAirframeNameId())) {
                    airframeNames.put(eventDefinition.getAirframeNameId(), resultSet.getString(11));
                }

                definitions.get(eventDefinition.getName()).put(airframeNames.get(eventDefinition.getAirframeNameId()), eventDefinition.toHumanReadable());
            }

            ctx.json(definitions);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public static void putEventDefinitions(Context ctx) throws IOException {
        EventDefinition updatedEvent = gson.fromJson(ctx.body(), EventDefinition.class);

        try (Connection connection = Database.getConnection()) {
            updatedEvent.updateSelf(connection);
        } catch (SQLException e) {
            ctx.status(500);
            ctx.json(new ErrorResponse(e));
        }
    }

    public static void deleteEventDefinitions(Context ctx) throws IOException {
        User user = ctx.sessionAttribute("user");
        if (user == null) {
            LOG.severe("INVALID ACCESS: user was not logged in.");
            ctx.status(401);
            ctx.json(new ErrorResponse("Not Logged In", "No user logged in."));
            return;
        }

        if (!user.isAdmin()) {
            LOG.severe("INVALID ACCESS: user did not have admin access.");
            ctx.json(new ErrorResponse("Not Admin", "No permissions to delete event definitions."));
        }

        String query = "DELETE FROM event_definitions WHERE id=?";
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, Integer.parseInt(Objects.requireNonNull(ctx.queryParam("eventDefinitionID"))));
            statement.executeUpdate();
            ctx.json("Successfully deleted event definition.");
        } catch (SQLException e) {
            ctx.status(500);
            ctx.json(new ErrorResponse(e));
        }
    }

    public static void getEventCounts(Context ctx) {
        LOG.warning("getEventCounts not implemented!");
    }

    public static void getEventCreator(Context ctx) throws IOException {
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

            scopes.put("create_event_js", "var airframes = JSON.parse('" + gson.toJson(Airframes.getAll(connection)) + "');\n" + "var doubleTimeSeriesNames = JSON.parse('" + gson.toJson(DoubleTimeSeries.getAllNames(connection, fleetId)) + "');\n" + "var airframeMap = JSON.parse('" + gson.toJson(Airframes.getIdToNameMap(connection)) + "');\n");

            ctx.contentType("text/html");
            ctx.result(MustacheHandler.handle(templateFile, scopes));

        } catch (SQLException e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e));
        } catch (IOException e) {
            LOG.severe(e.toString());
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
            scopes.put("event_manager_js", "var eventDefinitions = JSON.parse('" + gson.toJson(EventDefinition.getAll(connection)) + "');\n" + "var airframes = JSON.parse('" + gson.toJson(Airframes.getAll(connection)) + "');\n" + "var doubleTimeSeriesNames = JSON.parse('" + gson.toJson(DoubleTimeSeries.getAllNames(connection, fleetId)) + "');\n" + "var airframeMap = JSON.parse('" + gson.toJson(Airframes.getIdToNameMap(connection)) + "');\n");

            ctx.contentType("text/html");
            ctx.result(MustacheHandler.handle(templateFile, scopes));
        } catch (Exception e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e));
        }
    }
}
