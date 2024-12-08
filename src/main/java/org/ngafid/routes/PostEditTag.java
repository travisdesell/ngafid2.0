package org.ngafid.routes;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.Optional;

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
import org.ngafid.WebServer;
import org.ngafid.common.FlightTag;
import org.ngafid.accounts.User;
import org.ngafid.events.Event;
import org.ngafid.events.EventDefinition;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.Flight;

public class PostEditTag implements Route {
    private static final Logger LOG = Logger.getLogger(PostEditTag.class.getName());
    private Gson gson;
    private static String NO_CHANGE = "NOCHANGE";

    public PostEditTag(Gson gson) {
        this.gson = gson;

        LOG.info("initialized " + this.getClass().getName() + " route!");
    }

    /**
     * {inheritDoc}
     */
    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route!");

        final Session session = request.session();
        User user = session.attribute("user");

        int tagId = Integer.parseInt(request.queryParams("tag_id"));
        String name = request.queryParams("name");
        String description = request.queryParams("description");
        String color = request.queryParams("color");

        try (Connection connection = Database.getConnection()) {
            FlightTag flightTag = new FlightTag(tagId, user.getFleetId(), name, description, color);

            FlightTag currentTag = Flight.getTag(connection, tagId);

            LOG.info("currentTag: " + currentTag + " edited tag: " + flightTag);

            if (flightTag.equals(currentTag)) {
                LOG.info("No change detected in the tag.");
                return gson.toJson(NO_CHANGE);
            }

            return gson.toJson(Flight.editTag(connection, flightTag));
        } catch (SQLException e) {
            System.err.println("Error in SQL ");
            e.printStackTrace();
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
