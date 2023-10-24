package org.ngafid.routes;

import java.time.LocalDate;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
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
import org.ngafid.events.EventStatistics;
import org.ngafid.common.*;

public class PostAllEventCounts implements Route {
    private static final Logger LOG = Logger.getLogger(PostAllEventCounts.class.getName());
    private Gson gson;

    public PostAllEventCounts(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }


    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        String startDate = request.queryParams("startDate");
        String endDate = request.queryParams("endDate");

        final Session session = request.session();
        User user = session.attribute("user");
        int fleetId = user.getFleetId();

        //check to see if the user has access to view aggregate information
        if (!user.hasAggregateView()) {
            LOG.severe("INVALID ACCESS: user did not have aggregate access to view all event counts.");
            Spark.halt(401, "User did not have aggregate access to view all event counts.");
            return null;
        }

        try {
            Connection connection = Database.getConnection();

            Map<String, EventStatistics.EventCounts> eventCountsMap = EventStatistics.getEventCounts(connection, null, LocalDate.parse(startDate), LocalDate.parse(endDate));
            return gson.toJson(eventCountsMap);

        } catch (SQLException e) {
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
