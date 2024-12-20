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

        String startDate = request.formParams("startDate");
        String endDate = request.formParams("endDate");
        String eventName = request.formParams("eventName");
        boolean aggregateTrendsPage = Boolean.parseBoolean(request.formParams("aggregatePage"));

        final Session session = request.session();
        User user = session.attribute("user");

        try (Connection connection = Database.getConnection()) {
            Map<String, EventStatistics.MonthlyEventCounts> eventCountsMap;
            Map<String, Map<String, EventStatistics.MonthlyEventCounts>> map;

            if (aggregateTrendsPage) {
                if (!user.hasAggregateView()) {
                    LOG.severe("INVALID ACCESS: user did not have aggregate access to view aggregate trends page.");
                    Spark.halt(401, "User did not have aggregate access to view aggregate trends page.");
                    return null;
                }

                map = EventStatistics.getMonthlyEventCounts(connection, -1, LocalDate.parse(startDate),
                        LocalDate.parse(endDate));
            } else {

                int fleetId = user.getFleetId();
                // check to see if the user has upload access for this fleet.
                if (!user.hasViewAccess(fleetId)) {
                    LOG.severe("INVALID ACCESS: user did not have access view imports for this fleet.");
                    Spark.halt(401, "User did not have access to view imports for this fleet.");
                    return null;
                }

                map = EventStatistics.getMonthlyEventCounts(connection, fleetId, LocalDate.parse(startDate),
                        LocalDate.parse(endDate));
            }

            if (eventName == null) {
                return gson.toJson(map);
            } else {
                return gson.toJson(map.get(eventName));
            }
        } catch (SQLException e) {
            LOG.severe(e.toString());
            e.printStackTrace();
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
