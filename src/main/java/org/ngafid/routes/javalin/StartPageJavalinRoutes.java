package org.ngafid.routes.javalin;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.gson.Gson;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.accounts.User;
import org.ngafid.events.EventStatistics;
import org.ngafid.flights.Airframes;
import org.ngafid.routes.ErrorResponse;
import org.ngafid.routes.MustacheHandler;
import org.ngafid.routes.Navbar;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
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

    private static void getWelcome(Context ctx, List<Message> messages) {
        final String templateFile = "welcome.html";
        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        final int fleetId = user.getFleetId();
        System.out.println("Fleet ID: " + fleetId);

        try (Connection connection = Database.getConnection()) {
            Map<String, Object> scopes = new HashMap<>();
            List<String> airframes = Airframes.getAll(connection, fleetId);
            for (String airframe : airframes) {
                LOG.info("Airframe: " + airframe);
            }

            LOG.info(gson.toJson(airframes));

            scopes.put("navbar_js", Navbar.getJavascript(ctx));
            scopes.put("fleet_info_js", "var airframes = " + gson.toJson(Airframes.getAll(connection, fleetId)) + ";\n");
            if (!messages.isEmpty()) {
                scopes.put("messages", messages);
            }

            ctx.html(MustacheHandler.handle(templateFile, scopes));
        } catch (Exception e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e));
        }
    }
    
    
    public static void bindRoutes(Javalin app) {
        app.get("/", ctx -> getHome(ctx, null));
        app.get("/logout_success", ctx -> getHome(ctx, new Message("success", "You have been successfully logged out.")));
        app.get("/access_denied", ctx -> getHome(ctx, new Message("danger", "You attempted to load a page you did not have access to or attempted to access a page while not logged in.")));
//        app.get("/*", ctx -> getHome(ctx, new Message("danger", "The page you attempted to access does not exist.")));

        app.get("/protected/waiting", StartPageJavalinRoutes::getWaiting);
        app.get("/protected/welcome", ctx -> getWelcome(ctx, new ArrayList<>()));
        app.get("/protected/*", ctx -> getWelcome(ctx, List.of(new Message("danger", "The page you attempted to access does not exist."))));
    }
    
}
