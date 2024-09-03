package org.ngafid.routes;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;


import java.util.Arrays;
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
import spark.Spark;


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


public class GetFlight implements Route {
    private static final Logger LOG = Logger.getLogger(GetFlight.class.getName());
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

    public GetFlight(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initialized");
    }

    public GetFlight(Gson gson, String messageType, String messageText) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initialized");

        messages = new ArrayList<Message>();
        messages.add(new Message(messageType, messageText));
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        String resultString = "";
        String templateFile = WebServer.MUSTACHE_TEMPLATE_DIR + "flight.html";
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

            String[] flightIds = request.queryParamsValues("flight_id");
            LOG.info("Flight id(s) are: " + Arrays.toString(flightIds));

            long startTime, endTime;

            ArrayList<Flight> flights = new ArrayList<Flight>();

            for (String flightId : flightIds) {
                Flight flight = Flight.getFlight(Database.getConnection(), Integer.parseInt(flightId));

                if (flight.getFleetId() != fleetId) {
                    LOG.severe("INVALID ACCESS: user did not have access to flight id: " + flightId + ", it belonged to fleet: " + flight.getFleetId() + " and the user's fleet id was: " + fleetId);
                    Spark.halt(401, "User did not have access to this flight.");
                }

                flights.add(flight);
            }

            StringBuilder sb = new StringBuilder();
            sb.append("var flights = [");

            boolean first = true;
            for (Flight flight : flights) {
                if (!first) sb.append(", ");
                first = false;
                sb.append(gson.toJson(flight));
            }
            sb.append("];");

            scopes.put("flight_js", sb.toString());

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

        } catch (NumberFormatException e) {
            LOG.severe(e.toString());
            return gson.toJson(new ErrorResponse(e));
        }

        return resultString;
    }
}
