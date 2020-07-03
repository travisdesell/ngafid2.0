package org.ngafid.routes;

import java.time.LocalDate;

import java.util.List;
import java.util.ArrayList;
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

public class PostEventCounts implements Route {
    private static final Logger LOG = Logger.getLogger(PostEventCounts.class.getName());
    private Gson gson;

    public PostEventCounts(Gson gson) {
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

        //check to see if the user has upload access for this fleet.
        if (!user.hasViewAccess(fleetId)) {
            LOG.severe("INVALID ACCESS: user did not have access view imports for this fleet.");
            Spark.halt(401, "User did not have access to view imports for this fleet.");
            return null;
        }

        try {
            Connection connection = Database.getConnection();

            HashMap<String, EventStatistics.EventCounts> eventCountsMap = EventStatistics.getEventCounts(connection, fleetId, LocalDate.parse(startDate), LocalDate.parse(endDate));
            return gson.toJson(eventCountsMap);

        } catch (SQLException e) {
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
