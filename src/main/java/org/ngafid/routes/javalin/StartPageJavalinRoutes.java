package org.ngafid.routes.javalin;

import io.javalin.http.Context;
import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.flights.Airframes;
import org.ngafid.routes.ErrorResponse;
import org.ngafid.routes.MustacheHandler;
import org.ngafid.routes.Navbar;

import java.io.IOException;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import static org.ngafid.WebServer.gson;

public class StartPageJavalinRoutes {
    private static final String homeTemplateFileName = "home.html";
    private static final Logger LOG = Logger.getLogger(StartPageJavalinRoutes.class.getName());

    private static class Message {
        String type;
        String message;

        Message(String type, String message) {
            this.type = type;
            this.message = message;
        }
    }

    public static void getHome(Context ctx) throws IOException {
        Map<String, Object> scopes = new HashMap<>();
        ctx.contentType("text/html");

        if (ctx.queryParam("access_denied") != null) {
            scopes.put("access_denied", new Message("danger",
                    "You attempted to load a page you did not have access to " +
                            "or attempted to access a page while not logged in."));
        } else if (ctx.queryParam("logout_success") != null) {
            scopes.put("logout_success", new Message("success", "You have been successfully logged out."));
        }

        ctx.result(MustacheHandler.handle(homeTemplateFileName, scopes));
    }

    public static void getWaiting(Context ctx) {
        final String templateFile = "waiting.html";

        try  {
            ctx.contentType("text/html");
            ctx.result(MustacheHandler.handle(templateFile, new HashMap<>()));
        } catch (IOException e) {
            LOG.severe(e.toString());
        }
    }

    public static void getWelcome(Context ctx) {
        final String templateFile = "welcome.html";
        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        final int fleetId = user.getFleetId();

        try (Connection connection = Database.getConnection()) {
            Map<String, Object> scopes = new HashMap<String, Object>();
            scopes.put("navbar_js", Navbar.getJavascript(ctx));
            scopes.put("fleet_info_js", "var airframes = " + gson.toJson(Airframes.getAll(connection, fleetId)) + ";\n");

            ctx.contentType("text/html");
            ctx.result(MustacheHandler.handle(templateFile, scopes));
        } catch (Exception e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e));
        }
    }
}
