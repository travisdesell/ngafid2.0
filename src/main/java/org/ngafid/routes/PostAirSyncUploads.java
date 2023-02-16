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
import org.ngafid.WebServer;
import org.ngafid.accounts.User;
import org.ngafid.flights.AirSyncImport;
import org.ngafid.flights.Upload;
import org.ngafid.common.*;
import org.ngafid.routes.PostUploads.UploadsResponse;


public class PostAirSyncUploads implements Route {
    private static final Logger LOG = Logger.getLogger(PostAirSyncUploads.class.getName());
    private Gson gson;

    public PostAirSyncUploads(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }


    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        final Session session = request.session();
        User user = session.attribute("user");

        int fleetId = user.getFleetId();

        //check to see if the user has upload access for this fleet.
        if (!user.hasUploadAccess(fleetId)) {
            LOG.severe("INVALID ACCESS: user did not have access to upload flights for this fleet.");
            Spark.halt(401, "User did not have access to upload flights for this fleet.");
            return null;
        }

        try {
            int currentPage = Integer.parseInt(request.queryParams("currentPage"));
            int pageSize = Integer.parseInt(request.queryParams("pageSize"));

            Connection connection = Database.getConnection();

            int totalUploads = AirSyncImport.getNumUploads(connection, fleetId, null);
            int numberPages = totalUploads / pageSize;

            List<Upload> uploads = AirSyncImport.getUploads(connection, fleetId, " LIMIT " + (currentPage * pageSize) + "," + pageSize);

            return gson.toJson(new PaginationResponse<Upload>(uploads, numberPages));
        } catch (SQLException e) {
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
