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
import org.ngafid.flights.Flight;

import org.ngafid.events.EventDefinition;

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

        try {
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

            long startTime, endTime;

            StringBuilder sb = new StringBuilder();

            sb.append("var airframes = JSON.parse('");
            startTime = System.currentTimeMillis();
            sb.append(gson.toJson(Airframes.getAll(connection, fleetId)));
            endTime = System.currentTimeMillis();
            LOG.info("get all airframes took: " + ((endTime - startTime) / 1000.0) + " seconds");
            sb.append("');\n");

            sb.append("var tagNames = JSON.parse('");
            startTime = System.currentTimeMillis();
            sb.append(gson.toJson(Flight.getAllFleetTagNames(connection, fleetId)));
            endTime = System.currentTimeMillis();
            LOG.info("get all tag names took: " + ((endTime - startTime) / 1000.0) + " seconds");
            sb.append("');\n");

            sb.append("var tailNumbers = JSON.parse('");
            startTime = System.currentTimeMillis();
            sb.append(gson.toJson(Tails.getAllTails(connection, fleetId)));
            endTime = System.currentTimeMillis();
            LOG.info("get all tails names took: " + ((endTime - startTime) / 1000.0) + " seconds");
            sb.append("');\n");

            sb.append("var systemIds = JSON.parse('");
            startTime = System.currentTimeMillis();
            sb.append(gson.toJson(Tails.getAllSystemIds(connection, fleetId)));
            endTime = System.currentTimeMillis();
            LOG.info("get all system ids names took: " + ((endTime - startTime) / 1000.0) + " seconds");
            sb.append("');\n");

            sb.append("var doubleTimeSeriesNames = JSON.parse('");
            startTime = System.currentTimeMillis();
            sb.append(gson.toJson(DoubleTimeSeries.getAllNames(connection, fleetId)));
            endTime = System.currentTimeMillis();
            LOG.info("get all double time series names took: " + ((endTime - startTime) / 1000.0) + " seconds");
            sb.append("');\n");

            sb.append("var visitedAirports = JSON.parse('");
            startTime = System.currentTimeMillis();
            ArrayList<String> airports = Itinerary.getAllAirports(connection, fleetId);
            sb.append(gson.toJson(airports));
            endTime = System.currentTimeMillis();
            LOG.info("get all airports names took: " + ((endTime - startTime) / 1000.0) + " seconds");
            sb.append("');\n");

            sb.append("var visitedRunways = JSON.parse('");
            startTime = System.currentTimeMillis();
            sb.append(gson.toJson(Itinerary.getAllAirportRunways(connection, fleetId)));
            endTime = System.currentTimeMillis();
            LOG.info("get all runways names took: " + ((endTime - startTime) / 1000.0) + " seconds");
            sb.append("');\n");

            sb.append("var eventNames = JSON.parse('");
            startTime = System.currentTimeMillis();
            sb.append(gson.toJson(EventDefinition.getAllNames(connection, fleetId)));
            endTime = System.currentTimeMillis();
            LOG.info("get all event definition names took: " + ((endTime - startTime) / 1000.0) + " seconds");
            sb.append("');\n");

            sb.append("var flights = [];");


            scopes.put("flights_js", sb.toString());
            /*
            try {
                //scopes.put("flights_js", "var flights = JSON.parse('" + gson.toJson(Upload.getUploads(Database.getConnection(), fleetId, new String[]{"IMPORTED", "ERROR"})) + "');");

            } catch (SQLException e) {
                return gson.toJson(new ErrorResponse(e));
            }
            */

            StringWriter stringOut = new StringWriter();
            startTime = System.currentTimeMillis();
            mustache.execute(new PrintWriter(stringOut), scopes).flush();
            endTime = System.currentTimeMillis();
            LOG.info("mustache write took: " + ((endTime - startTime) / 1000.0) + " seconds");

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
