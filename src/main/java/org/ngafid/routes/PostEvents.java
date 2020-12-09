package org.ngafid.routes;

import java.util.ArrayList;
import java.util.logging.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.google.gson.Gson;

import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;
import spark.Spark;

import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.events.Event;
import org.ngafid.events.EventDefinition;
import org.ngafid.flights.DoubleTimeSeries;

public class PostEvents implements Route {
    private static final Logger LOG = Logger.getLogger(PostEvents.class.getName());
    private Gson gson;

    public PostEvents(Gson gson) {
        this.gson = gson;

        LOG.info("initialized " + this.getClass().getName() + " route!");
    }

    public static class EventInfo {
        private ArrayList<Event> events;
        private ArrayList<EventDefinition> definitions;

        public EventInfo(ArrayList<Event> events, ArrayList<EventDefinition> definitions) {
            this.events = events;
            this.definitions = definitions;
        }

    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route!");

        final Session session = request.session();
        User user = session.attribute("user");

        int flightId = Integer.parseInt(request.queryParams("flightId"));
        String name = request.queryParams("seriesName");

        boolean eventDefinitionsLoaded = Boolean.parseBoolean(request.queryParams("eventDefinitionsLoaded"));

        try {
            Connection connection = Database.getConnection();

            //check to see if the user has access to this data
            if (!user.hasFlightAccess(Database.getConnection(), flightId)) {
                LOG.severe("INVALID ACCESS: user did not have access to this flight.");
                Spark.halt(401, "User did not have access to this flight.");
            }

            ArrayList<Event> events = Event.getAll(connection, flightId);

            ArrayList<EventDefinition> definitions = null;
            if (!eventDefinitionsLoaded) {
                definitions = EventDefinition.getAll(connection);
            }

            EventInfo eventInfo = new EventInfo(events, definitions);

            //System.out.println(gson.toJson(uploadDetails));
            String output = gson.toJson(eventInfo);
            //need to convert NaNs to null so they can be parsed by JSON
            output = output.replaceAll("NaN","null");

            //LOG.info(output);

            return output;
        } catch (SQLException e) {
            e.printStackTrace();
            return gson.toJson(new ErrorResponse(e));
        }
    }
}


