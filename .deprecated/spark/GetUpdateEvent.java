package org.ngafid.routes.spark;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.logging.Logger;
import java.util.HashMap;

import java.sql.Connection;
import java.sql.SQLException;

import com.google.gson.Gson;

import org.ngafid.routes.ErrorResponse;
import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;
import spark.Spark;

import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.accounts.User;
import org.ngafid.events.EventDefinition;
import org.ngafid.flights.Airframes;
import org.ngafid.flights.DoubleTimeSeries;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

public class GetUpdateEvent implements Route {
    private static final Logger LOG = Logger.getLogger(GetUpdateEvent.class.getName());
    private Gson gson;

    public GetUpdateEvent(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    public GetUpdateEvent(Gson gson, String messageType, String messageText) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        String resultString = "";
        String templateFile = WebServer.MUSTACHE_TEMPLATE_DIR + "update_event.html";
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

            scopes.put("update_event_js",
                    "var airframes = JSON.parse('" + gson.toJson(Airframes.getAll(connection)) + "');\n" +
                            "var doubleTimeSeriesNames = JSON.parse('"
                            + gson.toJson(DoubleTimeSeries.getAllNames(connection, fleetId)) + "');\n" +
                            "var eventDefinitions = JSON.parse('" + gson.toJson(EventDefinition.getAll(connection))
                            + "');\n" +
                            "var airframeMap = JSON.parse('" + gson.toJson(Airframes.getIdToNameMap(connection))
                            + "');\n");

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
