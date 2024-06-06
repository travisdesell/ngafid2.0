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
import org.ngafid.EmailType;
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

            if (fleet.lock(connection)) {
                LOG.info("Beginning AirSync update process!");
                String status = fleet.update(connection);
                LOG.info("AirSync update process complete! Status: " + status);

                fleet.unlock(connection);

                StringBuilder sb = new StringBuilder("<h1>NGAFID Manual AirSync Update Report</h1>");
                sb.append("<br><p>");
                sb.append(status);
                sb.append("</p>");

                //Send email
                ArrayList<String> emailList = new ArrayList<>();
                emailList.add(user.getEmail());

                SendEmail.sendEmail(emailList, new ArrayList<String>(), "NGAFID AirSync Update Report", sb.toString(), EmailType.AIRSYNC_UPDATE_REPORT);
                String lastUpdateTime = fleet.getLastUpdateTime(connection);

                return gson.toJson(lastUpdateTime);
            } else {
                LOG.info("Unable to enter critical section!");
                return gson.toJson("UNABLE_MUTEX");
            }
        } catch (Exception e) {
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
