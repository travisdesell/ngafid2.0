package org.ngafid.www.routes;

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.ngafid.core.accounts.User;
import org.ngafid.www.ErrorResponse;
import org.ngafid.www.Navbar;
import org.ngafid.www.services.ProximityEventService;
import org.ngafid.www.WebServer;

import java.sql.SQLException;
import java.util.HashMap;
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

    public static void getProximityEvents(Context ctx) {
        try {
            User user = ctx.sessionAttribute("user");
            if (user == null) {
                ctx.status(401);
                ctx.result("User not logged in");
                return;
            }

            int fleetId = user.getFleetId();
            ctx.json(ProximityEventService.getProximityEvents(fleetId));
        } catch (SQLException e) {
            LOG.severe(e.toString());
            ctx.status(500);
            ctx.result(e.toString());
        }
    }

    public static void getAllProximityEvents(Context ctx) {
        try {
            User user = ctx.sessionAttribute("user");
            if (user == null) {
                ctx.status(401);
                ctx.result("User not logged in");
                return;
            }

            // Only allow admin users to get all events
            /*if (!user.isAdmin()) {
                ctx.status(403);
                ctx.result("Only administrators can access all proximity events");
                return;
            }*/

            ctx.json(ProximityEventService.getAllProximityEvents());
        } catch (SQLException e) {
            LOG.severe(e.toString());
            ctx.status(500);
            ctx.result(e.toString());
        }
    }

    public static void getProximityEventDetails(Context ctx) {
        try {
            User user = ctx.sessionAttribute("user");
            if (user == null) {
                ctx.status(401);
                ctx.result("User not logged in");
                return;
            }

            int eventId = Integer.parseInt(ctx.pathParam("eventId"));
            ctx.json(ProximityEventService.getProximityEventDetails(eventId));
        } catch (SQLException e) {
            LOG.severe(e.toString());
            ctx.status(500);
            ctx.result(e.toString());
        }
    }

    public static void bindRoutes(Javalin app) {
        app.get("/protected/proximity_map", ProximityMapJavalinRoutes::getProximityMap);
        app.get("/protected/proximity_events", ProximityMapJavalinRoutes::getProximityEvents);
        app.get("/protected/all_proximity_events", ProximityMapJavalinRoutes::getAllProximityEvents);
        app.get("/protected/proximity_event_details/{eventId}", ProximityMapJavalinRoutes::getProximityEventDetails);
    }
} 