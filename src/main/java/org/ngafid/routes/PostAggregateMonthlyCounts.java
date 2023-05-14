package org.ngafid.routes;

import com.google.gson.Gson;
import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.events.EventStatistics;
import spark.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.logging.Logger;

public class PostAggregateMonthlyCounts implements Route {


    private static final Logger LOG = Logger.getLogger(PostAggregateMonthlyCounts.class.getName());
    private Gson gson;

    public PostAggregateMonthlyCounts(Gson gson) {
        this.gson = gson;
        LOG.info("post " + this.getClass().getName() + " initalized");
    }
    @Override
    public Object handle(Request request, Response response) throws Exception {
        LOG.info("handling " + this.getClass().getName() + " route");

        String startDate = request.queryParams("startDate");
        String endDate = request.queryParams("endDate");
        String eventName = request.queryParams("eventName");

        final Session session = request.session();
        User user = session.attribute("user");

        //check to see if the user has upload access for this fleet.
        if (!user.hasAggregateView()) {
            LOG.severe("INVALID ACCESS: user did not have access view imports for this fleet.");
            Spark.halt(401, "User did not have access to view imports for this fleet.");
            return null;
        }

        try {
            Connection connection = Database.getConnection();

            HashMap<String, EventStatistics.MonthlyEventCounts> eventCountsMap = EventStatistics.getMonthlyEventCounts(connection, null, eventName, LocalDate.parse(startDate), LocalDate.parse(endDate));
            System.out.println("--------------------------------------------- Events Count Map ---------------------------------------------");
            System.out.println(gson.toJson(eventCountsMap));
            return gson.toJson(eventCountsMap);

        } catch (SQLException e) {
            return gson.toJson(new ErrorResponse(e));
        }
    }
}

