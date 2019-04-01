package org.ngafid.routes;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;


import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.HashMap;

import java.sql.Connection;
import java.sql.SQLException;

import com.google.gson.Gson;

import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;


import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.accounts.User;
import org.ngafid.flights.Airframes;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.Upload;
import org.ngafid.flights.Itinerary;
import org.ngafid.flights.Tails;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;


public class GetCreateEvent implements Route {
    private static final Logger LOG = Logger.getLogger(GetCreateEvent.class.getName());
    private Gson gson;

    public GetCreateEvent(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    public GetCreateEvent(Gson gson, String messageType, String messageText) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        String resultString = "";
        String templateFile = WebServer.MUSTACHE_TEMPLATE_DIR + "create_event.html";
        LOG.severe("template file: '" + templateFile + "'");

        try  {
            MustacheFactory mf = new DefaultMustacheFactory();
            Mustache mustache = mf.compile(templateFile);

            HashMap<String, Object> scopes = new HashMap<String, Object>();

            scopes.put("navbar_js", Navbar.getJavascript(request));

            final Session session = request.session();
            User user = session.attribute("user");
            int fleetId = user.getFleetId();

            Connection connection = Database.getConnection();

            scopes.put("create_event_js",
                    "var airframes = JSON.parse('" + gson.toJson(Airframes.getAll(connection)) + "');\n" +
                    "var doubleTimeSeriesNames = JSON.parse('" + gson.toJson(DoubleTimeSeries.getAllNames(connection, fleetId)) + "');\n"
                    );

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
