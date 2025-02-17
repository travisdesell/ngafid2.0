package org.ngafid.routes.spark;

import java.time.LocalDate;

import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.HashMap;

import java.sql.Connection;
import java.sql.SQLException;

import com.google.gson.Gson;

import org.ngafid.routes.ErrorResponse;
import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;
import spark.Spark;

import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.events.Event;

public class PostSeverities implements Route {
    private static final Logger LOG = Logger.getLogger(PostSeverities.class.getName());
    private Gson gson;

    public PostSeverities(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        String startDate = request.formParams("startDate");
        String endDate = request.formParams("endDate");
        String eventName = request.formParams("eventName");
        String tagName = request.formParams("tagName");
        final Session session = request.session();
        User user = session.attribute("user");
        int fleetId = user.getFleetId();

        // check to see if the user has upload access for this fleet.
        if (!user.hasViewAccess(fleetId)) {
            LOG.severe("INVALID ACCESS: user did not have access view imports for this fleet.");
            Spark.halt(401, "User did not have access to view imports for this fleet.");
            return null;
        }

        try (Connection connection = Database.getConnection()) {
            HashMap<String, ArrayList<Event>> eventMap = Event.getEvents(connection, fleetId, eventName,
                    LocalDate.parse(startDate), LocalDate.parse(endDate), tagName);
            return gson.toJson(eventMap);

        } catch (SQLException e) {
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
