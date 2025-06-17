package org.ngafid.www.routes;

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.ngafid.core.accounts.User;
import org.ngafid.www.Navbar;
import org.ngafid.service.proximity.ProximityEventService;
import java.io.IOException;
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

    public static void getAllProximityEvents(Context ctx) {
        try {
            User user = ctx.sessionAttribute("user");
            if (user == null) {
                ctx.status(401);
                ctx.result("User not logged in");
                return;
            }

            ctx.json(ProximityEventService.getAllProximityEvents());
        } catch (SQLException e) {
            LOG.severe(e.toString());
            ctx.status(500);
            ctx.result(e.toString());
        }
    }

    public static void bindRoutes(Javalin app) {
        app.get("/protected/proximity_map", ProximityMapJavalinRoutes::getProximityMap);
        app.get("/protected/all_proximity_events", ProximityMapJavalinRoutes::getAllProximityEvents);
        
        app.post("/protected/coordinates/time_range", ctx -> {
            try {
                User user = ctx.sessionAttribute("user");
                if (user == null) {
                    ctx.status(401);
                    ctx.result("User not logged in");
                    return;
                }

                Map<String, Object> body = ctx.bodyAsClass(Map.class);
                int flightId = ((Number) body.get("flightId")).intValue();
                long startTime = Long.parseLong(ctx.queryParam("start_time"));
                long endTime = Long.parseLong(ctx.queryParam("end_time"));

                ctx.json(ProximityEventService.getFlightCoordinatesForTimeRange(flightId, startTime, endTime));
            } catch (SQLException | IOException e) {
                LOG.severe(e.toString());
                ctx.status(500);
                ctx.result(e.toString());
            }
        });
    }
}
