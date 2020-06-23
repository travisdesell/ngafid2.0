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

import org.ngafid.filters.Filter;

import org.ngafid.flights.Flight;
import org.ngafid.flights.FlightError;
import org.ngafid.flights.FlightWarning;
import org.ngafid.flights.Tail;
import org.ngafid.flights.Tails;
import org.ngafid.flights.Upload;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;


import org.ngafid.events.EventStatistics;

public class GetWelcome implements Route {
    private static final Logger LOG = Logger.getLogger(GetWelcome.class.getName());
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

    public GetWelcome(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    public GetWelcome(Gson gson, String messageType, String messageText) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");

        messages = new ArrayList<Message>();
        messages.add(new Message(messageType, messageText));
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        String resultString = "";
        String templateFile = WebServer.MUSTACHE_TEMPLATE_DIR + "welcome.html";
        LOG.severe("template file: '" + templateFile + "'");

        final Session session = request.session();
        User user = session.attribute("user");
        int fleetId = user.getFleetId();

        try  {
            MustacheFactory mf = new DefaultMustacheFactory();
            Mustache mustache = mf.compile(templateFile);

            Connection connection = Database.getConnection();

            HashMap<String, Object> scopes = new HashMap<String, Object>();

            if (messages != null) {
                scopes.put("messages", messages);
            }

            scopes.put("navbar_js", Navbar.getJavascript(request));

            LocalDate firstOfMonth = LocalDate.now().with( TemporalAdjusters.firstDayOfMonth() );
            LocalDate firstOfYear = LocalDate.now().with( TemporalAdjusters.firstDayOfYear() );

            HashMap<String, EventStatistics.EventCounts> eventCountsMap = EventStatistics.getEventCounts(connection, fleetId, null, null);

            long startTime = System.currentTimeMillis();
            String fleetInfo =
                "var numberFlights = " + Flight.getNumFlights(connection, fleetId, null) + ";\n" +
                "var flightHours = " + Flight.getTotalFlightHours(connection, fleetId, null) + ";\n" +
                "var numberAircraft = " + Tails.getNumberTails(connection, fleetId) + ";\n" +
                "var totalEvents = " + EventStatistics.getEventCount(connection, fleetId, null, null) + ";\n" +
                "var yearEvents = " + EventStatistics.getEventCount(connection, fleetId, firstOfYear, null) + ";\n" +
                "var monthEvents = " + EventStatistics.getEventCount(connection, fleetId, firstOfMonth, null) + ";\n" +
                "var uploadsNotImported = " + Upload.getNumUploads(connection, fleetId, " AND status = 'UPLOADED'") + ";\n" +
                "var uploadsWithError = " + Upload.getNumUploads(connection, fleetId, " AND status = 'ERROR'") + ";\n" +
                "var flightsWithWarning = " + FlightWarning.getCount(connection, fleetId) + ";\n" +
                "var flightsWithError = " + FlightError.getCount(connection, fleetId) + ";\n" +
                "var eventCounts = " + gson.toJson(eventCountsMap) + ";";
                //"var eventCounts = JSON.parse('" + gson.toJson(eventCountsMap) + "');";

            scopes.put("fleet_info_js", fleetInfo);
            long endTime = System.currentTimeMillis();
            LOG.info("getting fleet info took " + (endTime-startTime) + "ms.");

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
