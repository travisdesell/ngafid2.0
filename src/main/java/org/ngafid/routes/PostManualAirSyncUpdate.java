package org.ngafid.routes;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import java.sql.Connection;
import java.sql.SQLException;

import com.google.gson.Gson;

import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;
import spark.Spark;

import org.ngafid.Database;
import org.ngafid.SendEmail;
import org.ngafid.accounts.EmailType;
import org.ngafid.WebServer;
import org.ngafid.accounts.AirSyncFleet;
import org.ngafid.accounts.User;
import org.ngafid.flights.AirSyncImport;
import org.ngafid.flights.Upload;
import org.ngafid.common.*;
import org.ngafid.routes.PostUploads.UploadsResponse;


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
        Connection connection = Database.getConnection();

        User user = session.attribute("user");
        int fleetId = user.getFleetId();

        //check to see if the user has upload access for this fleet.
        if (!user.hasUploadAccess(fleetId)) {
            LOG.severe("INVALID ACCESS: user did not have access to upload flights for this fleet.");
            Spark.halt(401, "User did not have access to upload flights for this fleet.");
            return null;
        }

        try {
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
