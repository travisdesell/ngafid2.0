package org.ngafid.routes.v2;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.HashMap;

import java.sql.Connection;
import java.sql.SQLException;
import org.ngafid.routes.ErrorResponse;

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


import org.ngafid.events.EventStatistics;

public class GetWelcomeV2 implements Route {
    private static final Logger LOG = Logger.getLogger(GetWelcomeV2.class.getName());
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

    public GetWelcomeV2(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initialized");
    }

    public GetWelcomeV2(Gson gson, String messageType, String messageText) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initialized");

        messages = new ArrayList<Message>();
        messages.add(new Message(messageType, messageText));
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");
        LOG.info("cookie from request cookie " + request.cookies());
        LOG.info("headers from request " + request.headers());
        LOG.info("SessionID from request session " + request.session().id());
        final Session session = request.session();
        LOG.info("all current attribute "+request.session().attributes());
        User user = request.session().attribute("user");
        int fleetId = user.getFleetId();

        try  {

            HashMap<String, Object> scopes = new HashMap<String, Object>();
            HashMap<String, Object> fleetInfo = new HashMap<String, Object>();
            Connection connection = Database.getConnection();

            if (messages != null) {
                scopes.put("messages", messages);
            }

            LocalDate firstOfMonth = LocalDate.now().with( TemporalAdjusters.firstDayOfMonth() );
            LocalDate firstOfYear = LocalDate.now().with( TemporalAdjusters.firstDayOfYear() );
            LocalDate lastThirtyDays = LocalDate.now().minusDays(30);

            String lastThirtyDaysQuery = "start_time >= '" + lastThirtyDays.toString() + "'";
            String yearQuery = "start_time >= '" + firstOfYear.toString() + "'";

            long startTime = System.currentTimeMillis();
            Map<String, EventStatistics.EventCounts> eventCountsMap = EventStatistics.getEventCounts(connection, fleetId, null, null);
            LOG.info("getting event counts took " + (System.currentTimeMillis() - startTime) + "ms.");

            startTime = System.currentTimeMillis();

            fleetInfo.put("numberFlights", Flight.getNumFlights(connection, fleetId, null));
            fleetInfo.put("flightHours", Flight.getTotalFlightTime(connection, fleetId, null));
            fleetInfo.put("numberAircraft", Tails.getNumberTails(connection, fleetId));
            fleetInfo.put("totalEvents", EventStatistics.getEventCount(connection, fleetId, null, null));
            fleetInfo.put("yearEvents", EventStatistics.getEventCount(connection, fleetId, firstOfYear, null));
            fleetInfo.put("monthEvents", EventStatistics.getEventCount(connection, fleetId, firstOfMonth, null));
            fleetInfo.put("yearNumberAircraft", Flight.getNumFlights(connection, yearQuery));
            fleetInfo.put("yearFlightHours", Flight.getTotalFlightTime(connection, yearQuery,-1));
            fleetInfo.put("monthNumberAircraft", Flight.getNumFlights(connection, lastThirtyDaysQuery));
            fleetInfo.put("monthFlightHours", Flight.getTotalFlightTime(connection, lastThirtyDaysQuery,-1));
            fleetInfo.put("uploadsNotImported", Upload.getNumUploads(connection, fleetId, " AND status = 'UPLOADED'"));
            fleetInfo.put("uploadsWithError", Upload.getNumUploads(connection, fleetId, " AND status = 'ERROR'"));
            fleetInfo.put("flightsWithWarning", FlightWarning.getCount(connection, fleetId));
            fleetInfo.put("flightsWithError", FlightError.getCount(connection, fleetId));
            fleetInfo.put("airframes", Airframes.getAll(connection, fleetId));
            fleetInfo.put("eventCounts", eventCountsMap);

            scopes.put("fleetInfo", fleetInfo);
            long endTime = System.currentTimeMillis();
            LOG.info("getting fleet info took " + (endTime-startTime) + "ms.");

            return gson.toJson(scopes);
        } catch (SQLException e) {
            LOG.severe(e.toString());
            return gson.toJson(new ErrorResponse(e));

        } catch (Exception e) {
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
