package org.ngafid.routes.spark;

import com.google.gson.Gson;
import org.ngafid.Database;
import spark.Request;
import spark.Response;
import spark.Route;

import java.sql.Connection;
import java.util.logging.Logger;

import static org.ngafid.events.EventStatistics.updateMonthlyTotalFlights;

public class UpdateMonthlyFlightsCache implements Route {
    private static final Logger LOG = Logger.getLogger(UpdateMonthlyFlightsCache.class.getName());
    private Gson gson;

    public UpdateMonthlyFlightsCache(Gson gson) {
        this.gson = gson;
        LOG.info("put " + this.getClass().getName() + " initialized");
    }

    @Override
    public Object handle(Request request, Response response) {
        try (Connection connection = Database.getConnection()) {
            updateMonthlyTotalFlights(connection, Integer.parseInt(request.queryParams("fleetId")));
            response.status(200);
            return gson.toJson(request.queryParams("fleetId"));
        } catch (Exception e) {
            e.printStackTrace();
            response.status(500);
            return gson.toJson(new ErrorResponse(e));
        }

    }
}
