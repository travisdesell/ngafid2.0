package org.ngafid.routes;

import java.time.LocalDate;
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
import org.ngafid.accounts.User;
import org.ngafid.events.EventStatistics;

public class PostMonthlyEventCounts implements Route {
    private static final Logger LOG = Logger.getLogger(PostMonthlyEventCounts.class.getName());
    private Gson gson;

    public PostMonthlyEventCounts(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initialized");
    }


    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        String startDate = request.queryParams("startDate");
        String endDate = request.queryParams("endDate");
        String eventName = request.queryParams("eventName");
        boolean aggregateTrendsPage = Boolean.parseBoolean(request.queryParams("aggregatePage"));

        final Session session = request.session();
        User user = session.attribute("user");

        try {
            Connection connection = Database.getConnection();
            Map<String, EventStatistics.MonthlyEventCounts> eventCountsMap;
            if (aggregateTrendsPage) {
                LOG.warning("ACCESSING AGGREGATE PAGE");

                if (!user.hasAggregateView()) {
                    LOG.severe("INVALID ACCESS: user did not have aggregate access to view aggregate trends page.");
                    Spark.halt(401, "User did not have aggregate access to view aggregate trends page.");
                    return null;
                }
                eventCountsMap = EventStatistics.getMonthlyEventCounts(connection, eventName, LocalDate.parse(startDate), LocalDate.parse(endDate));
            }
            else {
                int fleetId = user.getFleetId();
                //check to see if the user has upload access for this fleet.
                if (!user.hasViewAccess(fleetId)) {
                    LOG.severe("INVALID ACCESS: user did not have access view imports for this fleet.");
                    Spark.halt(401, "User did not have access to view imports for this fleet.");
                    return null;
                }
                LOG.warning("NOT ACCESSING AGGREGATE PAGE");
                eventCountsMap = EventStatistics.getMonthlyEventCounts(connection, fleetId, eventName, LocalDate.parse(startDate), LocalDate.parse(endDate));
            }

            return gson.toJson(eventCountsMap);
        } catch (SQLException e) {
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
