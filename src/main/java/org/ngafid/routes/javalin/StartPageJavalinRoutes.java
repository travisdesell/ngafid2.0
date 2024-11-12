package org.ngafid.routes.javalin;

import io.javalin.Javalin;
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

    private static void getHome(Context ctx, Message message) throws IOException {
        Map<String, Object> scopes = new HashMap<>();
        ctx.contentType("text/html");

        if (message != null) {
            scopes.put("messages", new Message[]{message});
        }

        ctx.result(MustacheHandler.handle(homeTemplateFileName, scopes));
    }

    private static void getWaiting(Context ctx) {
        final String templateFile = "waiting.html";

        try  {
            ctx.contentType("text/html");
            ctx.result(MustacheHandler.handle(templateFile, new HashMap<>()));
        } catch (IOException e) {
            LOG.severe(e.toString());
        }
    }

    private static void getWelcome(Context ctx, Message message) {
        final String templateFile = "welcome.html";
        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        final int fleetId = user.getFleetId();

        try (Connection connection = Database.getConnection()) {
            Map<String, Object> scopes = new HashMap<String, Object>();
            scopes.put("navbar_js", Navbar.getJavascript(ctx));
            scopes.put("fleet_info_js", "var airframes = " + gson.toJson(Airframes.getAll(connection, fleetId)) + ";\n");
            if (message != null) {
                scopes.put("messages", new Message[]{message});
            }

            ctx.contentType("text/html");
            ctx.result(MustacheHandler.handle(templateFile, scopes));
        } catch (Exception e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e));
        }
    }
    
    
    public static void bindRoutes(Javalin app) {
        app.get("/", ctx -> getHome(ctx, null));
        app.get("/logout_success", ctx -> getHome(ctx, new Message("success", "You have been successfully logged out.")));
        app.get("/access_denied", ctx -> getHome(ctx, new Message("danger", "You attempted to load a page you did not have access to or attempted to access a page while not logged in.")));
        app.get("/*", ctx -> getHome(ctx, new Message("danger", "The page you attempted to access does not exist.")));

        app.get("/protected/waiting", StartPageJavalinRoutes::getWaiting);
        app.get("/protected/welcome", ctx -> getWelcome(ctx, null));
        app.get("/protected/*", ctx -> getWelcome(ctx, new Message("danger", "The page you attempted to access does not exist.")));
    }
    
}
