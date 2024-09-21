package org.ngafid.routes;

/**
 * Route for getting the page listing all events and their definitions
 */

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.gson.Gson;
import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.accounts.User;
import org.ngafid.events.EventDefinition;
import org.ngafid.flights.Airframes;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Session;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.logging.Logger;

public class GetEventDefinitions implements Route {
    private static final Logger LOG = Logger.getLogger(GetEventDefinitions.class.getName());
    private Gson gson;

    public GetEventDefinitions(Gson gson) {
        this.gson = gson;
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        LOG.info("Handling " + this.getClass().getName() + " route");

        String resultString = "";
        String templateFile = WebServer.MUSTACHE_TEMPLATE_DIR + "event_definitions_display.html";
        LOG.severe("Template File: '" + templateFile + "'");

        try (Connection connection = Database.getConnection()) {
            MustacheFactory mf = new DefaultMustacheFactory();
            Mustache mustache = mf.compile(templateFile);

            final Session session = request.session();
            User user = session.attribute("user");
            int fleetId = user.getFleetId();

            HashMap<String, Object> scopes = new HashMap<>();

            scopes.put("navbar_js", Navbar.getJavascript(request));

            long startTime = System.currentTimeMillis();

            scopes.put("events_js",
                    // "var eventStats = JSON.parse('" + gson.toJson(eventStatistics) + "');\n"
                    "var eventNames = JSON.parse('" + gson.toJson(EventDefinition.getUniqueNames(connection, fleetId))
                            + "');\n");

            long endTime = System.currentTimeMillis();

            LOG.info("Event Definitions to JSON took " + (endTime - startTime) + "ms.");
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
