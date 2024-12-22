package org.ngafid.routes.javalin;

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.events.Event;
import org.ngafid.events.EventDefinition;
import org.ngafid.events.EventMetaData;
import org.ngafid.events.EventStatistics;
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
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Logger;

import static org.ngafid.WebServer.gson;

public class EventJavalinRoutes {
    private static final Logger LOG = Logger.getLogger(EventJavalinRoutes.class.getName());

    private static void getAllEventDefinitions(Context ctx) {
        try (Connection connection = Database.getConnection()) {
            ctx.json(EventDefinition.getAll(connection));
        } catch (SQLException e) {
            LOG.severe(e.toString());
            ctx.status(500);
            ctx.result(e.toString());
        }
    }

    private static void getEventDefinition(Context ctx) {
        final String templateFile = "event_definitions_display.html";

        try (Connection connection = Database.getConnection()) {
            final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
            final int fleetId = user.getFleetId();

            Map<String, Object> scopes = new HashMap<>();
            scopes.put("navbar_js", Navbar.getJavascript(ctx));

            long startTime = System.currentTimeMillis();
            scopes.put("events_js",
                    // "var eventStats = JSON.parse('" + gson.toJson(eventStatistics) + "');\n"
                    "var eventNames = JSON.parse('" + gson.toJson(EventDefinition.getUniqueNames(connection, fleetId)) + "');\n");

            long endTime = System.currentTimeMillis();

            LOG.info("Event Definitions to JSON took " + (endTime - startTime) + "ms.");
            ctx.header("Content-Type", "text/html; charset=UTF-8");
            ctx.render(templateFile, scopes);
        } catch (SQLException e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    private static void getEventDescription(Context ctx) {
        final String expectedName = Objects.requireNonNull(ctx.formParam("eventName"));
        final String query = "SELECT id, fleet_id, name, start_buffer, stop_buffer, airframe_id, condition_json, column_names, severity_column_names, severity_type FROM event_definitions WHERE event_definitions.name = "
                + "\"" + expectedName + "\"";

        try (Connection connection = Database.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            ResultSet resultSet = preparedStatement.executeQuery();
            resultSet.next();

            ctx.result(gson.toJson(new EventDefinition(resultSet).toHumanReadable()));
        } catch (SQLException e) {
            ctx.json(new ErrorResponse(e)).status(500);
        }

    }

    private static void getAllEventDescriptions(Context ctx) {
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


    private static void putEventDefinitions(Context ctx) {
        EventDefinition updatedEvent = gson.fromJson(ctx.body(), EventDefinition.class);

        try (Connection connection = Database.getConnection()) {
            updatedEvent.updateSelf(connection);
        } catch (SQLException e) {
            ctx.status(500);
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    private static void deleteEventDefinitions(Context ctx) {
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
            statement.setInt(1, Integer.parseInt(Objects.requireNonNull(ctx.formParam("eventDefinitionID"))));
            statement.executeUpdate();
            ctx.json("Successfully deleted event definition.");
        } catch (SQLException e) {
            ctx.status(500);
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    private static void getEventCounts(Context ctx) {
        LOG.warning("getEventCounts not implemented!");
    }

    private static void getEventCreator(Context ctx) {
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

            ctx.header("Content-Type", "text/html; charset=UTF-8");
            ctx.render(templateFile, scopes);

        } catch (SQLException e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    private static void getEventManager(Context ctx) {
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

            ctx.header("Content-Type", "text/html; charset=UTF-8");
            ctx.render(templateFile, scopes);
        } catch (Exception e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    private static void postCreateEvent(Context ctx) {
        final int fleetId = 0; // all events work on all fleets for now
        final String eventName = Objects.requireNonNull(ctx.formParam("eventName"));
        final int startBuffer = Integer.parseInt(Objects.requireNonNull(ctx.formParam("startBuffer")));
        final int stopBuffer = Integer.parseInt(Objects.requireNonNull(ctx.formParam("stopBuffer")));
        final String airframe = Objects.requireNonNull(ctx.formParam("airframe"));
        final String filterJSON = Objects.requireNonNull(ctx.formParam("filterQuery"));
        final String severityColumnNamesJSON = Objects.requireNonNull(ctx.formParam("severityColumnNames"));
        final String severityType = Objects.requireNonNull(ctx.formParam("severityType"));

        try (Connection connection = Database.getConnection()) {
            EventDefinition.insert(connection, fleetId, eventName, startBuffer, stopBuffer, airframe, filterJSON,
                    severityColumnNamesJSON, severityType);

            ctx.contentType("application/json");
            ctx.result("{}");
        } catch (SQLException e) {
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    private static void postAllEventCounts(Context ctx) {
        final String startDate = Objects.requireNonNull(ctx.formParam("startDate"));
        final String endDate = Objects.requireNonNull(ctx.formParam("endDate"));

        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));

        // check to see if the user has access to view aggregate information
        if (!user.hasAggregateView()) {
            LOG.severe("INVALID ACCESS: user did not have aggregate access to view all event counts.");
            ctx.status(401);
            ctx.result("User did not have aggregate access to view all event counts.");
            return;
        }

        try (Connection connection = Database.getConnection()) {
            Map<String, EventStatistics.EventCounts> eventCountsMap = EventStatistics.getEventCounts(connection,
                    LocalDate.parse(startDate), LocalDate.parse(endDate));
            ctx.json(eventCountsMap);
        } catch (SQLException e) {
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }
    
    private static void getUpdateEvent(Context ctx) {
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

            scopes.put("update_event_js",
                    "var airframes = JSON.parse('" + gson.toJson(Airframes.getAll(connection)) + "');\n" +
                            "var doubleTimeSeriesNames = JSON.parse('"
                            + gson.toJson(DoubleTimeSeries.getAllNames(connection, fleetId)) + "');\n" +
                            "var eventDefinitions = JSON.parse('" + gson.toJson(EventDefinition.getAll(connection))
                            + "');\n" +
                            "var airframeMap = JSON.parse('" + gson.toJson(Airframes.getIdToNameMap(connection))
                            + "');\n");
            
            ctx.header("Content-Type", "text/html; charset=UTF-8");
            ctx.render(templateFile, scopes);
        } catch (SQLException e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    private static void postUpdateEvent(Context ctx) {
        final int fleetId = 0; // all events work on all fleets for now
        final int eventId = Integer.parseInt(Objects.requireNonNull(ctx.formParam("eventId")));
        final String eventName = Objects.requireNonNull(ctx.formParam("eventName"));
        final int startBuffer = Integer.parseInt(Objects.requireNonNull(ctx.formParam("startBuffer")));
        final int stopBuffer = Integer.parseInt(Objects.requireNonNull(ctx.formParam("stopBuffer")));
        final String airframe = Objects.requireNonNull(ctx.formParam("airframe"));
        final String filterJSON = Objects.requireNonNull(ctx.formParam("filterQuery"));
        final String severityColumnNamesJSON = Objects.requireNonNull(ctx.formParam("severityColumnNames"));
        final String severityType = Objects.requireNonNull(ctx.formParam("severityType"));

        try (Connection connection = Database.getConnection()) {
            EventDefinition.update(connection, fleetId, eventId, eventName, startBuffer, stopBuffer, airframe,
                    filterJSON, severityColumnNamesJSON, severityType);

            ctx.contentType("application/json");
            ctx.result("{}");
        } catch (SQLException e) {
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    private static void postEventMetaData(Context ctx) {
        LOG.info("handling rate of closure route");
        int eventId = Integer.parseInt(Objects.requireNonNull(ctx.formParam("eventId")));
        try (Connection connection = Database.getConnection()) {
            List<EventMetaData> metaDataList = EventMetaData.getEventMetaData(connection, eventId);
            if (!metaDataList.isEmpty()) {
                ctx.json(metaDataList);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            ctx.json(new ErrorResponse(e)).status(500);
        }
        ctx.json(null);
    }

    private static void postEvents(Context ctx) {
        class EventInfo {
            private final List<Event> events;
            private final List<EventDefinition> definitions;

            public EventInfo(List<Event> events, List<EventDefinition> definitions) {
                this.events = events;
                this.definitions = definitions;
            }
        }
        
        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        final int flightId = Integer.parseInt(Objects.requireNonNull(ctx.formParam("flightId")));
        final boolean eventDefinitionsLoaded = Boolean.parseBoolean(Objects.requireNonNull(ctx.formParam("eventDefinitionsLoaded")));

        try (Connection connection = Database.getConnection()) {
            // check to see if the user has access to this data
            if (!user.hasFlightAccess(connection, flightId)) {
                LOG.severe("INVALID ACCESS: user did not have access to this flight.");
                ctx.status(401);
                ctx.result("User did not have access to this flight.");
                return;
            }

            List<Event> events = Event.getAll(connection, flightId);
            List<EventDefinition> definitions = null;

            if (!eventDefinitionsLoaded) {
                definitions = EventDefinition.getAll(connection);
            }

            EventInfo eventInfo = new EventInfo(events, definitions);

            // System.out.println(gson.toJson(uploadDetails));
            String output = gson.toJson(eventInfo);
            // need to convert NaNs to null so they can be parsed by JSON
            output = output.replaceAll("NaN", "null");
            ctx.contentType("application/json");
            ctx.result(output);
        } catch (SQLException e) {
            e.printStackTrace();
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    private static void postEventStatistics(Context ctx) {
        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        final int fleetId = user.getFleetId();
        final int airframeNameId = Integer.parseInt(Objects.requireNonNull(ctx.formParam("airframeNameId")));
        final String airframeName = Objects.requireNonNull(ctx.formParam("airframeName"));

        try (Connection connection = Database.getConnection()) {
            ctx.json(new EventStatistics(connection, airframeNameId, airframeName, fleetId));
        } catch (SQLException e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    public static void bindRoutes(Javalin app) {
        app.get("/protected/event_definitions", EventJavalinRoutes::getAllEventDefinitions);

        app.get("/protected/manage_event_definitions", EventJavalinRoutes::getEventDefinition);
        app.put("/protected/manage_event_definitions", EventJavalinRoutes::putEventDefinitions);
        app.delete("/protected/manage_event_definitions", EventJavalinRoutes::deleteEventDefinitions);

        app.get("/protected/event_description", EventJavalinRoutes::getEventDescription);
        app.get("/protected/event_descriptions", EventJavalinRoutes::getAllEventDescriptions);
        app.get("/protected/event_counts", EventJavalinRoutes::getEventCounts);

        app.get("/protected/create_event", EventJavalinRoutes::getEventCreator);
        app.post("/protected/create_event", EventJavalinRoutes::postCreateEvent);

        app.get("/protected/manage_events", EventJavalinRoutes::getEventManager);

        app.post("/protected/events", EventJavalinRoutes::postEvents);
        app.post("/protected/event_metadata", EventJavalinRoutes::postEventMetaData);
        app.post("/protected/event_stat", EventJavalinRoutes::postEventStatistics);

        app.get("/protected/update_event", EventJavalinRoutes::getUpdateEvent);
        app.post("/protected/update_event", EventJavalinRoutes::postUpdateEvent);
    }
}
