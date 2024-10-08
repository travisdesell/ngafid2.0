package org.ngafid.routes.spark;

import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.logging.Logger;
import java.util.HashMap;

import java.sql.Connection;

import com.google.gson.Gson;

import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;
import spark.Spark;

import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.accounts.User;
import org.ngafid.flights.Airframes;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.events.EventDefinition;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

public class GetEventManager implements Route {
    private static final Logger LOG = Logger.getLogger(GetEventManager.class.getName());
    private Gson gson;

    public GetEventManager(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    public GetEventManager(Gson gson, String messageType, String messageText) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initialized");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        String resultString = "";
        String templateFile = WebServer.MUSTACHE_TEMPLATE_DIR + "manage_events.html";
        LOG.severe("template file: '" + templateFile + "'");

        try (Connection connection = Database.getConnection()) {
            MustacheFactory mf = new DefaultMustacheFactory();
            Mustache mustache = mf.compile(templateFile);

            HashMap<String, Object> scopes = new HashMap<String, Object>();

            scopes.put("navbar_js", Navbar.getJavascript(request));

            final Session session = request.session();
            User user = session.attribute("user");
            int fleetId = user.getFleetId();

            if (!user.isAdmin()) {
                LOG.severe("INVALID ACCESS: user does not have access to this page.");
                Spark.halt(401, "User did not have access to this page.");
                return null;
            }

            scopes.put("event_manager_js",
                    "var eventDefinitions = JSON.parse('" + gson.toJson(EventDefinition.getAll(connection)) + "');\n" +
                            "var airframes = JSON.parse('" + gson.toJson(Airframes.getAll(connection)) + "');\n" +
                            "var doubleTimeSeriesNames = JSON.parse('"
                            + gson.toJson(DoubleTimeSeries.getAllNames(connection, fleetId)) + "');\n" +
                            "var airframeMap = JSON.parse('" + gson.toJson(Airframes.getIdToNameMap(connection))
                            + "');\n");

            StringWriter stringOut = new StringWriter();
            mustache.execute(new PrintWriter(stringOut), scopes).flush();
            resultString = stringOut.toString();
        } catch (Exception e) {
            LOG.severe(e.toString());
            return gson.toJson(new ErrorResponse(e));

        }

        return resultString;
    }
}
