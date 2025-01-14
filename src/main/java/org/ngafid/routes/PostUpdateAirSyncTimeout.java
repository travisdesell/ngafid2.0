package org.ngafid.routes;

import com.google.gson.Gson;
import java.sql.Connection;
import java.util.logging.Logger;
import org.ngafid.Database;
import org.ngafid.accounts.AirSyncFleet;
import org.ngafid.accounts.User;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Session;

public class PostUpdateAirSyncTimeout implements Route {
    private static final Logger LOG = Logger.getLogger(PostUpdateAirSyncTimeout.class.getName());
    private Gson gson;
    private static Connection connection = Database.getConnection();

    public PostUpdateAirSyncTimeout(Gson gson) {
        this.gson = gson;
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling update airsync timeout route!");

        final Session session = request.session();
        User user = session.attribute("user");

        int fleetId = user.getFleetId();

        String newTimeout = request.queryParams("timeout");

        try {
            LOG.info("User set new timeout: " + newTimeout + ", requesting user: " + user.getFullName());
            AirSyncFleet fleet = AirSyncFleet.getAirSyncFleet(connection, fleetId);

            fleet.updateTimeout(connection, user, newTimeout);

            return gson.toJson(newTimeout);
        } catch (Exception e) {
            e.printStackTrace();
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
