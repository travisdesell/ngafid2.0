package org.ngafid.routes;

import java.util.ArrayList;
import java.util.List;
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
import org.ngafid.common.FlightTag;
import org.ngafid.accounts.User;
import org.ngafid.events.Event;
import org.ngafid.events.EventDefinition;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.Flight;

public class PostTags implements Route {
    private static final Logger LOG = Logger.getLogger(PostTags.class.getName());
    private Gson gson;

    public PostTags(Gson gson) {
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

        int flightId = 1;
        String name = request.queryParams("seriesName");

        boolean eventDefinitionsLoaded = Boolean.parseBoolean(request.queryParams("eventDefinitionsLoaded"));

        try {
            Connection connection = Database.getConnection();

            //check to see if the user has access to this data
            if (!user.hasFlightAccess(Database.getConnection(), flightId)) {
                LOG.severe("INVALID ACCESS: user did not have access to this flight.");
                Spark.halt(401, "User did not have access to this flight.");
            }

            List<FlightTag> fts = Flight.getTags(connection, flightId);


            return null;
        } catch (SQLException e) {
            e.printStackTrace();
            return gson.toJson(new ErrorResponse(e));
        }
    }
}

