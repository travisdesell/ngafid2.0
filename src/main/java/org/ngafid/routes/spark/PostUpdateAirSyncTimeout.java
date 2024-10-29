package org.ngafid.routes.spark;

import java.util.logging.Logger;
import java.sql.Connection;

import com.google.gson.Gson;

import org.ngafid.routes.ErrorResponse;
import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;
import org.ngafid.Database;
import org.ngafid.accounts.AirSyncFleet;
import org.ngafid.accounts.User;

public class PostUpdateAirSyncTimeout implements Route {
    private static final Logger LOG = Logger.getLogger(PostUpdateAirSyncTimeout.class.getName());
    private Gson gson;

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

        try (Connection connection = Database.getConnection()) {
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
