package org.ngafid.www.routes;

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.ngafid.core.accounts.User;
import org.ngafid.core.proximity.ProximityPointsProcessor;
import org.ngafid.www.Navbar;
import org.ngafid.core.flights.Airframes;
import org.ngafid.core.Database;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

    public static void getAllProximityEvents(Context ctx) {
        try {
            User user = ctx.sessionAttribute("user");
            if (user == null) {
                ctx.status(401);
                ctx.result("User not logged in");
                return;
            }

            ctx.json(ProximityPointsProcessor.getProximityEvents());
        } catch (SQLException e) {
            LOG.severe(e.toString());
            ctx.status(500);
            ctx.result(e.toString());
        }
    }

    public static void bindRoutes(Javalin app) {
        app.get("/protected/proximity_map", ProximityMapJavalinRoutes::getProximityMap);
        app.get("/protected/heat_map", ProximityMapJavalinRoutes::getHeatMap);
        app.get("/protected/proximity_events", ProximityMapJavalinRoutes::getAllProximityEvents);
        app.get("/protected/proximity_points", ctx -> {
            User user = ctx.sessionAttribute("user");
            if (user == null) {
                ctx.status(401).result("User not logged in");
                return;
            }
            ctx.json(org.ngafid.core.proximity.ProximityPointsProcessor.getAllProximityPoints());
        });
        app.get("/protected/proximity_points_for_flight", ctx -> {
            User user = ctx.sessionAttribute("user");
            if (user == null) {
                ctx.status(401).result("User not logged in");
                return;
            }
            int eventId = Integer.parseInt(ctx.queryParam("event_id"));
            int flightId = Integer.parseInt(ctx.queryParam("flight_id"));
            ctx.json(org.ngafid.core.proximity.ProximityPointsProcessor.getProximityPointsForEventAndFlight(eventId, flightId));
        });
        app.get("/protected/proximity_events_in_box", ctx -> {
            User user = ctx.sessionAttribute("user");
            if (user == null) {
                ctx.status(401).result("User not logged in");
                return;
            }
            double minLat = Double.parseDouble(ctx.queryParam("min_latitude"));
            double maxLat = Double.parseDouble(ctx.queryParam("max_latitude"));
            double minLon = Double.parseDouble(ctx.queryParam("min_longitude"));
            double maxLon = Double.parseDouble(ctx.queryParam("max_longitude"));
            String startTime = ctx.queryParam("start_time");
            String endTime = ctx.queryParam("end_time");
            Double minSeverity = ctx.queryParam("min_severity") != null ? Double.parseDouble(ctx.queryParam("min_severity")) : 0.0;
            Double maxSeverity = ctx.queryParam("max_severity") != null ? Double.parseDouble(ctx.queryParam("max_severity")) : 1000.0;
            String airframe = ctx.queryParam("airframe");
            ctx.json(org.ngafid.core.proximity.ProximityPointsProcessor.getProximityEventsInBox(minLat, maxLat, minLon, maxLon, startTime, endTime, minSeverity, maxSeverity, airframe));
        });
    }
}
