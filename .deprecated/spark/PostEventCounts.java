package org.ngafid.routes.spark;

import java.time.LocalDate;

import java.util.Map;
import java.util.logging.Logger;

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
import org.ngafid.events.EventStatistics;

public class PostEventCounts implements Route {
    private static final Logger LOG = Logger.getLogger(PostEventCounts.class.getName());
    private Gson gson;
    private final boolean aggregate;

    public PostEventCounts(Gson gson, boolean aggregate) {
        this.gson = gson;
        this.aggregate = aggregate;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        String startDate = request.formParams("startDate");
        String endDate = request.formParams("endDate");

        final Session session = request.session();
        User user = session.attribute("user");
        int fleetId = user.getFleetId();

        // check to see if the user has upload access for this fleet.
        if (!user.hasViewAccess(fleetId)) {
            LOG.severe("INVALID ACCESS: user did not have access to view events for this fleet.");
            Spark.halt(401, "User did not have access to view events for this fleet.");
            return null;
        }

        if (this.aggregate && !user.hasAggregateView()) {
            LOG.severe("INVALID ACCESS: user did not have aggregate access to view all event counts.");
            Spark.halt(401, "User did not have aggregate access to view all event counts.");
            return null;
        }

        try (Connection connection = Database.getConnection()) {
            if (aggregate)
                fleetId = -1;

            Map<String, EventStatistics.EventCounts> eventCountsMap = EventStatistics.getEventCounts(connection,
                    fleetId, LocalDate.parse(startDate), LocalDate.parse(endDate));
            return gson.toJson(eventCountsMap);

        } catch (SQLException e) {
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
