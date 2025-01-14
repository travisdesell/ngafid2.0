package org.ngafid.routes;

import com.google.gson.Gson;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;
import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.common.*;
import org.ngafid.events.Event;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Session;
import spark.Spark;

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

        String startDate = request.queryParams("startDate");
        String endDate = request.queryParams("endDate");
        String eventName = request.queryParams("eventName");
        String tagName = request.queryParams("tagName");
        final Session session = request.session();
        User user = session.attribute("user");
        int fleetId = user.getFleetId();

        //check to see if the user has upload access for this fleet.
        if (!user.hasViewAccess(fleetId)) {
            LOG.severe("INVALID ACCESS: user did not have access view imports for this fleet.");
            Spark.halt(401, "User did not have access to view imports for this fleet.");
            return null;
        }

        try {
            Connection connection = Database.getConnection();

            HashMap<String, ArrayList<Event>> eventMap = Event.getEvents(connection, fleetId, eventName, LocalDate.parse(startDate), LocalDate.parse(endDate), tagName);
            return gson.toJson(eventMap);

        } catch (SQLException e) {
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
