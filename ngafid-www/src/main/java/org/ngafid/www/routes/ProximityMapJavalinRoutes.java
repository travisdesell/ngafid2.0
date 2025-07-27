package org.ngafid.www.routes;

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.ngafid.core.accounts.User;
import org.ngafid.www.Navbar;
import org.ngafid.core.flights.Airframes;
import org.ngafid.core.Database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

public class ProximityMapJavalinRoutes {
    private static final Logger LOG = Logger.getLogger(ProximityMapJavalinRoutes.class.getName());

    private static void getProximityMap(Context ctx) {
        final String templateFile = "proximity_map.html";
        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));

        Map<String, Object> scopes = new HashMap<>();
        scopes.put("navbar_js", Navbar.getJavascript(ctx));
        ctx.header("Content-Type", "text/html; charset=UTF-8");
        ctx.render(templateFile, scopes);
    }

    private static void getHeatMap(Context ctx) {
        final String templateFile = "heat_map.html";
        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));

        Map<String, Object> scopes = new HashMap<>();
        scopes.put("navbar_js", Navbar.getJavascript(ctx));
        // Inject airframes variable for frontend
        try (Connection connection = Database.getConnection()) {
            int fleetId = user.getFleetId();
            java.util.List<String> airframes = Airframes.getAll(connection, fleetId);
            com.google.gson.Gson gson = new com.google.gson.Gson();
            scopes.put("fleet_info_js", "var airframes = " + gson.toJson(airframes) + ";\n");
        } catch (Exception e) {
            // fallback: empty airframes
            scopes.put("fleet_info_js", "var airframes = [];\n");
        }
        ctx.header("Content-Type", "text/html; charset=UTF-8");
        ctx.render(templateFile, scopes);
    }


    public static void bindRoutes(Javalin app) {
        app.get("/protected/proximity_map", ProximityMapJavalinRoutes::getProximityMap);
        app.get("/protected/heat_map", ProximityMapJavalinRoutes::getHeatMap);
        app.get("/protected/proximity_points_for_event_and_flight", ctx -> {
            User user = ctx.sessionAttribute("user");
            if (user == null) {
                ctx.status(401).result("User not logged in");
                return;
            }
            try {
                String eventIdParam = ctx.queryParam("event_id");
                String flightIdParam = ctx.queryParam("flight_id");
                
                if (eventIdParam == null || flightIdParam == null) {
                    LOG.severe("Missing parameters: event_id=" + eventIdParam + ", flight_id=" + flightIdParam);
                    ctx.status(400).result("Missing required parameters: event_id and flight_id");
                    return;
                }
                
                int eventId = Integer.parseInt(eventIdParam);
                int flightId = Integer.parseInt(flightIdParam);
                
                LOG.info("Fetching proximity points for event_id=" + eventId + ", flight_id=" + flightId);
                
                Map<String, Object> result = org.ngafid.core.proximity.ProximityPointsProcessor.getCoordinates(eventId, flightId);
                ctx.json(result);
            } catch (NumberFormatException e) {
                LOG.severe("Invalid number format: " + e.getMessage());
                ctx.status(400).result("Invalid number format for event_id or flight_id");
            } catch (Exception e) {
                LOG.severe("Error fetching proximity points: " + e.getMessage());
                e.printStackTrace();
                ctx.status(500).result("Internal server error: " + e.getMessage());
            }
        });
        app.get("/protected/proximity_events_in_box", ctx -> {
            User user = ctx.sessionAttribute("user");
            if (user == null) {
                ctx.status(401).result("User not logged in");
                return;
            }
            String airframe = ctx.queryParam("airframe");
            String eventDefinitionIdsParam = ctx.queryParam("event_definition_ids");
            String startDate = ctx.queryParam("start_date");
            String endDate = ctx.queryParam("end_date");
            double areaMinLat = Double.parseDouble(ctx.queryParam("area_min_lat"));
            double areaMaxLat = Double.parseDouble(ctx.queryParam("area_max_lat"));
            double areaMinLon = Double.parseDouble(ctx.queryParam("area_min_lon"));
            double areaMaxLon = Double.parseDouble(ctx.queryParam("area_max_lon"));
            // Parse severity filters
            Double minSeverity = ctx.queryParam("min_severity") != null ? Double.valueOf(ctx.queryParam("min_severity")) : null;
            Double maxSeverity = ctx.queryParam("max_severity") != null ? Double.valueOf(ctx.queryParam("max_severity")) : null;

            // Parse event definition IDs
            List<Integer> eventDefinitionIds = new ArrayList<>();
            if (eventDefinitionIdsParam != null && !eventDefinitionIdsParam.isEmpty()) {
                String[] ids = eventDefinitionIdsParam.split(",");
                for (String id : ids) {
                    try {
                        eventDefinitionIds.add(Integer.valueOf(id.trim()));
                    } catch (NumberFormatException e) {
                        LOG.warning("Invalid event definition ID: " + id);
                    }
                }
            }

            try {
                List<java.util.Map<String, Object>> events = org.ngafid.core.proximity.ProximityPointsProcessor.getEvents(
                    airframe, eventDefinitionIds, java.sql.Date.valueOf(startDate), java.sql.Date.valueOf(endDate),
                    areaMinLat, areaMaxLat, areaMinLon, areaMaxLon,
                    minSeverity, maxSeverity
                );
                ctx.json(events);
            } catch (SQLException e) {
                LOG.severe(e.toString());
                ctx.status(500).result(e.toString());
            }
        });
    }
}
