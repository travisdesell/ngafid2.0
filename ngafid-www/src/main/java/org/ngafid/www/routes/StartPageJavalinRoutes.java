package org.ngafid.www.routes;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import org.ngafid.core.Database;
import org.ngafid.core.accounts.User;
import org.ngafid.core.flights.Airframes;
import org.ngafid.www.ErrorResponse;
import org.ngafid.www.Navbar;
import static org.ngafid.www.WebServer.gson;

import io.javalin.Javalin;
import io.javalin.http.Context;

public class StartPageJavalinRoutes {
    private static final Logger LOG = Logger.getLogger(StartPageJavalinRoutes.class.getName());

    private static class Message {
        String type;
        String message;

        Message(String type, String message) {
            this.type = type;
            this.message = message;
        }
    }

    private static void getHome(Context ctx, Message message) {
        final String homeTemplateFileName = "home.html";
        Map<String, Object> scopes = new HashMap<>();

        if (message != null) {
            scopes.put("messages", new Message[]{message});
        }

        ctx.header("Content-Type", "text/html; charset=UTF-8");
        ctx.render(homeTemplateFileName, scopes);
    }

    private static void getWaiting(Context ctx) {
        final String templateFile = "waiting.html";
        ctx.header("Content-Type", "text/html; charset=UTF-8");
        ctx.render(templateFile);
    }

    private static void getWelcome(Context ctx, List<Message> messages) {
        final String templateFile = "welcome.html";
        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        final int fleetId = user.getFleetId();

        try (Connection connection = Database.getConnection()) {
            Map<String, Object> scopes = new HashMap<>();
            List<String> airframes = Airframes.getAll(connection, fleetId);
            scopes.put("navbar_js", Navbar.getJavascript(ctx));
            scopes.put("fleet_info_js", "var airframes = " + gson.toJson(airframes) + ";\n");
            LOG.info("var airframes = " + airframes + ";\n");
            if (!messages.isEmpty()) {
                scopes.put("messages", messages);
            }
            ctx.header("Content-Type", "text/html; charset=UTF-8");
            ctx.render(templateFile, scopes);
        } catch (Exception e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }


    public static void bindRoutes(Javalin app) {
        app.get("/", ctx -> getHome(ctx, null));
        app.get("/logout_success", ctx -> getHome(ctx, new Message("success", "You have been successfully logged out.")));
        app.get("/access_denied", ctx -> getHome(ctx, new Message("danger", "You attempted to load a page you did not have access to or attempted to access a page while not logged in.")));
//        app.get("/*", ctx -> getHome(ctx, new Message("danger", "The page you attempted to access does not exist.")));

        app.get("/protected/waiting", StartPageJavalinRoutes::getWaiting);
        app.get("/protected/welcome", ctx -> getWelcome(ctx, new ArrayList<>()));
//        app.get("/protected/*", ctx -> getWelcome(ctx, List.of(new Message("danger", "The page you attempted to access does not exist."))));
    }

}
