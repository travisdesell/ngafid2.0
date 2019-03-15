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


public class GetFlights implements Route {
    private static final Logger LOG = Logger.getLogger(GetFlights.class.getName());
    private Gson gson;

    private static class Message {
        String type;
        String message;

        Message(String type, String message) {
            this.type = type;
            this.message = message;
        }
    }

    private List<Message> messages = null;

    public GetFlights(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    public GetFlights(Gson gson, String messageType, String messageText) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");

        messages = new ArrayList<Message>();
        messages.add(new Message(messageType, messageText));
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        String resultString = "";
        String templateFile = WebServer.MUSTACHE_TEMPLATE_DIR + "flights.html";
        LOG.severe("template file: '" + templateFile + "'");

        try  {
            MustacheFactory mf = new DefaultMustacheFactory();
            Mustache mustache = mf.compile(templateFile);

            HashMap<String, Object> scopes = new HashMap<String, Object>();

            if (messages != null) {
                scopes.put("messages", messages);
            }

            scopes.put("navbar_js", Navbar.getJavascript(request));

            final Session session = request.session();
            User user = session.attribute("user");
            int fleetId = user.getFleetId();

            Connection connection = Database.getConnection();

            scopes.put("flights_js",
                    "var airframes = JSON.parse('" + gson.toJson(Airframes.getAll(connection, fleetId)) + "');\n" +
                    "var tailNumbers = JSON.parse('" + gson.toJson(Tails.getAll(connection, fleetId)) + "');\n" +
                    "var doubleTimeSeriesNames = JSON.parse('" + gson.toJson(DoubleTimeSeries.getAllNames(connection, fleetId)) + "');\n" +
                    "var visitedAirports = JSON.parse('" + gson.toJson(Itinerary.getAllAirports(connection, fleetId)) + "');\n" +
                    "var visitedRunways = JSON.parse('" + gson.toJson(Itinerary.getAllAirportRunways(connection, fleetId)) + "');\n" +
                    "var flights = [];"
                    );
            /*
            try {
                //scopes.put("flights_js", "var flights = JSON.parse('" + gson.toJson(Upload.getUploads(Database.getConnection(), fleetId, new String[]{"IMPORTED", "ERROR"})) + "');");

            } catch (SQLException e) {
                return gson.toJson(new ErrorResponse(e));
            }
            */

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
