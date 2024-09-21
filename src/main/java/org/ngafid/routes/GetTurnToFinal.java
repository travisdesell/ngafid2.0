package org.ngafid.routes;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.accounts.User;
import org.ngafid.events.Event;
import org.ngafid.events.EventDefinition;
import org.ngafid.flights.*;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Session;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

public class GetTurnToFinal implements Route {
    private static final Logger LOG = Logger.getLogger(GetTurnToFinal.class.getName());
    private Gson gson;

    private List<Event> events = null;

    public GetTurnToFinal() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        LOG.info("get " + this.getClass().getName() + " initalized");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        String resultString = "";
        String templateFile = WebServer.MUSTACHE_TEMPLATE_DIR + "ttf.html";
        LOG.severe("template file: '" + templateFile + "'");

        try (Connection connection = Database.getConnection()) {
            MustacheFactory mf = new DefaultMustacheFactory();
            Mustache mustache = mf.compile(templateFile);

            HashMap<String, Object> scopes = new HashMap<>();

            if (events != null) {
                scopes.put("events", events);
            }

            scopes.put("navbar_js", Navbar.getJavascript(request));

            final Session session = request.session();
            User user = session.attribute("user");
            int fleetId = user.getFleetId();

            scopes.put("ttf_js",
                    "var airports = " + gson.toJson(Itinerary.getAllAirports(connection, fleetId)) + ";\n" +
                            "var runways = " + gson.toJson(Itinerary.getAllRunwaysWithCoordinates(connection, fleetId))
                            + ";\n");

            StringWriter stringOut = new StringWriter();
            mustache.execute(new PrintWriter(stringOut), scopes).flush();
            resultString = stringOut.toString();
        } catch (SQLException e) {
            LOG.severe(e.toString());
            return gson.toJson(new ErrorResponse(e));

        } catch (IOException e) {
            LOG.severe(e.toString());
        }

        return resultString;
    }
}
