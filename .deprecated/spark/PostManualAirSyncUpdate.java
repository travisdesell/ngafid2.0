package org.ngafid.routes.spark;

import java.util.logging.Logger;

import java.sql.Connection;

import com.google.gson.Gson;

import org.ngafid.routes.ErrorResponse;
import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;
import spark.Spark;

import org.ngafid.Database;
import org.ngafid.accounts.AirSyncFleet;
import org.ngafid.accounts.User;

public class PostManualAirSyncUpdate implements Route {
    private static final Logger LOG = Logger.getLogger(PostManualAirSyncUpdate.class.getName());
    private Gson gson;

    public PostManualAirSyncUpdate(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        final Session session = request.session();

        User user = session.attribute("user");
        int fleetId = user.getFleetId();

        // check to see if the user has upload access for this fleet.
        if (!user.hasUploadAccess(fleetId)) {
            LOG.severe("INVALID ACCESS: user did not have access to upload flights for this fleet.");
            Spark.halt(401, "User did not have access to upload flights for this fleet.");
            return null;
        }

        try (Connection connection = Database.getConnection()) {
            AirSyncFleet fleet = AirSyncFleet.getAirSyncFleet(connection, fleetId);

            LOG.info("Beginning AirSync update process!");
            String status = fleet.update(connection);
            LOG.info("AirSync update process complete! Status: " + status);

            fleet.setOverride(connection, true);

            return gson.toJson("OK");
        } catch (Exception e) {
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
