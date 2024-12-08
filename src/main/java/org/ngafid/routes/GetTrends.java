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

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import com.google.gson.Gson;

import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;

import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.accounts.User;

import org.ngafid.events.EventDefinition;
import org.ngafid.events.EventStatistics;

import org.ngafid.filters.Filter;

import org.ngafid.flights.Airframes;
import org.ngafid.flights.Flight;
import org.ngafid.flights.FlightError;
import org.ngafid.flights.FlightWarning;
import org.ngafid.flights.Tail;
import org.ngafid.flights.Tails;
import org.ngafid.flights.Upload;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

public class GetTrends implements Route {
    private static final Logger LOG = Logger.getLogger(GetTrends.class.getName());
    private Gson gson;

    private static class Message {
        String type;
        String message;

        Message(String type, String message) {
            this.type = type;
            this.message = message;
        }
    }

    private List<Message> messages = new ArrayList<Message>();

    public GetTrends(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    public GetTrends(Gson gson, String messageType, String messageText) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");

        messages = new ArrayList<Message>();
        messages.add(new Message(messageType, messageText));
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        String resultString = "";
        String templateFile = WebServer.MUSTACHE_TEMPLATE_DIR + "trends.html";
        LOG.severe("template file: '" + templateFile + "'");

        final Session session = request.session();
        User user = session.attribute("user");
        int fleetId = user.getFleetId();

        try (Connection connection = Database.getConnection()) {
            MustacheFactory mf = new DefaultMustacheFactory();
            Mustache mustache = mf.compile(templateFile);

            HashMap<String, Object> scopes = new HashMap<String, Object>();

            if (messages != null) {
                scopes.put("messages", messages);
            }

            scopes.put("navbar_js", Navbar.getJavascript(request));

            long startTime = System.currentTimeMillis();
            String fleetInfo = "var airframes = " + gson.toJson(Airframes.getAll(connection, fleetId)) + ";\n" +
                    "var eventNames = " + gson.toJson(EventDefinition.getUniqueNames(connection, fleetId)) + ";\n" +
                    "var tagNames = " + gson.toJson(Flight.getAllTagNames(connection)) + ";\n";

            scopes.put("fleet_info_js", fleetInfo);
            long endTime = System.currentTimeMillis();
            LOG.info("getting fleet info took " + (endTime - startTime) + "ms.");

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
