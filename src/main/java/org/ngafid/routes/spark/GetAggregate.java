package org.ngafid.routes.spark;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.HashMap;

import java.sql.Connection;
import java.sql.SQLException;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

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

import org.ngafid.flights.Airframes;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import org.ngafid.events.EventStatistics;

public class GetAggregate implements Route {
    private static final Logger LOG = Logger.getLogger(GetAggregate.class.getName());
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

    public GetAggregate(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    public GetAggregate(Gson gson, String messageType, String messageText) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");

        messages = new ArrayList<Message>();
        messages.add(new Message(messageType, messageText));
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        String resultString = "";
        String templateFile = WebServer.MUSTACHE_TEMPLATE_DIR + "aggregate.html";
        LOG.severe("template file: '" + templateFile + "'");

        final Session session = request.session();
        User user = session.attribute("user");
        int fleetId = user.getFleetId();

        // check to see if the user has access to view aggregate information
        if (!user.hasAggregateView()) {
            LOG.severe("INVALID ACCESS: user did not have aggregate access to view aggregate dashboard.");
            Spark.halt(401, "User did not have aggregate access to view aggregate dashboard.");
            return null;
        }

        try (Connection connection = Database.getConnection()) {
            MustacheFactory mf = new DefaultMustacheFactory();
            Mustache mustache = mf.compile(templateFile);

            HashMap<String, Object> scopes = new HashMap<String, Object>();

            if (messages != null) {
                scopes.put("messages", messages);
            }

            scopes.put("navbar_js", Navbar.getJavascript(request));

            LocalDate firstOfMonth = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth());
            LocalDate firstOfYear = LocalDate.now().with(TemporalAdjusters.firstDayOfYear());
            LocalDate lastThirtyDays = LocalDate.now().minusDays(30);

            Map<String, EventStatistics.EventCounts> eventCountsMap = EventStatistics.getEventCounts(connection, null,
                    null);

            // create a filter to grab things
            String lastThirtyDaysQuery = "start_time >= '" + lastThirtyDays.toString() + "'";
            String yearQuery = "start_time >= '" + firstOfYear.toString() + "'";

            System.out.println("monthly flight query: " + lastThirtyDaysQuery);

            long startTime = System.currentTimeMillis();
            // String fleetInfo =
            // // Move to async summary
            // "var numberFlights = " + Flight.getNumFlights(connection) + ";\n" +
            // "var flightHours = " + Flight.getTotalFlightHours(connection) + ";\n" +
            // "var numberAircraft = " + Tails.getNumberTails(connection) + ";\n" +
            // "var yearNumberFlights = " + Flight.getNumFlights(connection, yearQuery) +
            // ";\n" +
            // "var yearFlightHours = " + Flight.getTotalFlightHours(connection, yearQuery)
            // + ";\n" +
            // "var monthNumberFlights = " + Flight.getNumFlights(connection,
            // lastThirtyDaysQuery) + ";\n" +
            // "var monthFlightHours = " + Flight.getTotalFlightHours(connection,
            // lastThirtyDaysQuery) + ";\n" +
            // "var totalEvents = " + EventStatistics.getEventCount(connection, null, null)
            // + ";\n" +
            // "var yearEvents = " + EventStatistics.getEventCount(connection, firstOfYear,
            // null) + ";\n" +
            // "var monthEvents = " + EventStatistics.getEventCount(connection,
            // firstOfMonth, null) + ";\n" +
            // "var numberFleets = " + Fleet.getNumberFleets(connection) + ";" +
            // "var numberUsers = " + User.getNumberUsers(connection) + ";" +
            //
            // // async event_counts
            // "var eventCounts = " + gson.toJson(eventCountsMap) + ";";

            // scopes.put("fleet_info_js", fleetInfo);
            scopes.put("fleet_info_js", "var airframes = " + gson.toJson(Airframes.getAll(connection)) + ";\n");

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
