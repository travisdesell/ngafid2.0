package org.ngafid.routes.spark;

import java.util.List;
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
import org.ngafid.flights.Upload;

public class PostUploads implements Route {
    private static final Logger LOG = Logger.getLogger(PostUploads.class.getName());
    private Gson gson;

    public PostUploads(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    public static class UploadsResponse {
        public List<Upload> uploads;
        public int numberPages;

        public UploadsResponse(List<Upload> uploads, int numberPages) {
            this.uploads = uploads;
            this.numberPages = numberPages;
        }
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        final Session session = request.session();
        User user = session.attribute("user");

        int fleetId = user.getFleetId();

        // check to see if the user has view access for this fleet.
        if (!user.hasViewAccess(fleetId)) {
            LOG.severe("INVALID ACCESS: user did not have access to view flights for this fleet.");
            Spark.halt(401, "User did not have access to view flights for this fleet.");
            return null;
        }

        try (Connection connection = Database.getConnection()) {
            int currentPage = Integer.parseInt(request.queryParams("currentPage"));
            int pageSize = Integer.parseInt(request.queryParams("pageSize"));

            int totalUploads = Upload.getNumUploads(connection, fleetId, null);
            int numberPages = totalUploads / pageSize;
            List<Upload> uploads = Upload.getUploads(connection, fleetId,
                    " LIMIT " + (currentPage * pageSize) + "," + pageSize);

            return gson.toJson(new UploadsResponse(uploads, numberPages));
        } catch (SQLException e) {
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
