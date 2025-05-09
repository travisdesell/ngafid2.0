package org.ngafid.www.routes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.ngafid.core.Database;
import org.ngafid.core.accounts.User;
import org.ngafid.core.flights.Flight;
import org.ngafid.core.flights.Tail;
import org.ngafid.core.flights.Tails;
import org.ngafid.www.ErrorResponse;
import org.ngafid.www.Navbar;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

public class AircraftFleetTailsJavalinRoutes {
    private static final Logger LOG = Logger.getLogger(AircraftFleetTailsJavalinRoutes.class.getName());
    public static final Gson GSON = new GsonBuilder().serializeSpecialFloatingPointValues().create();

    public static class UpdateTailResponse {
        private final int fleetId;
        private final String systemId;
        private final String tail;
        private final int confirmed;

        public UpdateTailResponse(int fleetId, String systemId, String tail, int confirmed) {
            this.fleetId = fleetId;
            this.systemId = systemId;
            this.tail = tail;
            this.confirmed = confirmed;
        }
    }

    private static void getManageFleet(Context ctx) {
        final String templateFile = "manage_fleet.html";

        Map<String, Object> scopes = new HashMap<String, Object>();

        scopes.put("navbar_js", Navbar.getJavascript(ctx));

        final User user = ctx.sessionAttribute("user");
        scopes.put("user_js", "var user = JSON.parse('" + GSON.toJson(user) + "');");

        ctx.header("Content-Type", "text/html; charset=UTF-8");
        ctx.render(templateFile, scopes);
    }

    private static void postFleetNames(Context ctx) {
        try (Connection connection = Database.getConnection()) {
            List<String> names = new ArrayList<String>();

            try (PreparedStatement query = connection.prepareStatement("SELECT fleet_name FROM fleet ORDER BY fleet_name"); ResultSet resultSet = query.executeQuery()) {
                while (resultSet.next()) {
                    names.add(resultSet.getString(1));
                }
                ctx.json(names);
            }
        } catch (SQLException e) {
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    private static void getSystemIds(Context ctx) {
        final String templateFile = "system_ids.html";
        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        final int fleetId = user.getFleetId();

        try (Connection connection = Database.getConnection()) {
            List<Tail> tailInfo = Tails.getAll(connection, fleetId);
            Map<String, Object> scopes = new HashMap<String, Object>();

            scopes.put("navbar_js", Navbar.getJavascript(ctx));
            scopes.put("system_ids_js", "var systemIds = JSON.parse('" + GSON.toJson(tailInfo) + "');\n");

            ctx.header("Content-Type", "text/html; charset=UTF-8");
            ctx.render(templateFile, scopes);
        } catch (SQLException e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }


    private static void getSimAircraft(Context ctx) {
        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        final int fleetId = user.getFleetId();

        if (!user.hasViewAccess(fleetId)) {
            ctx.status(401);
            ctx.result("User did not have access to view acces for this fleet.");
            return;
        }

        try (Connection connection = Database.getConnection()) {
            List<String> paths = Flight.getSimAircraft(connection, fleetId);
            ctx.json(paths);
        } catch (SQLException e) {
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    private static void postSimAircraft(Context ctx) {
        final String CACHE = "cache";
        final String RMCACHE = "rmcache";

        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        final String type = Objects.requireNonNull(ctx.formParam("type"));
        final String path = Objects.requireNonNull(ctx.formParam("path"));
        final int fleetId = user.getFleetId();

        try (Connection connection = Database.getConnection()) {
            switch (type) {
                case CACHE:
                    List<String> currPaths = Flight.getSimAircraft(connection, fleetId);
                    if (!currPaths.contains(path)) {
                        Flight.addSimAircraft(connection, fleetId, path);
                        ctx.json("SUCCESS");
                    } else {
                        ctx.json("FAILURE");
                    }
                case RMCACHE:
                    Flight.removeSimAircraft(connection, fleetId, path);
                    ctx.json(Flight.getSimAircraft(connection, fleetId));

                default:
                    ctx.json("FAILURE");
            }

        } catch (Exception e) {
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    private static void postUpdateTail(Context ctx) {
        final String systemId = Objects.requireNonNull(ctx.formParam("systemId"));
        final String tail = Objects.requireNonNull(ctx.formParam("tail"));

        try (Connection connection = Database.getConnection()) {
            final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
            final int fleetId = user.getFleetId();

            // check to see if the user has upload access for this fleet.
            if (!user.hasUploadAccess(fleetId)) {
                ctx.status(401);
                ctx.result("User did not have access to view imports for this fleet.");
                return;
            }

            Tails.updateTail(connection, fleetId, systemId, tail);

            ctx.json(new UpdateTailResponse(fleetId, systemId, tail, 1));

        } catch (SQLException e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }


    public static void bindRoutes(Javalin app) {
        app.get("/protected/manage_fleet", AircraftFleetTailsJavalinRoutes::getManageFleet);
        app.get("/protected/system_ids", AircraftFleetTailsJavalinRoutes::getSystemIds);
    }
}
