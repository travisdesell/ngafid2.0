package org.ngafid.routes;

import java.util.ArrayList;
import java.util.logging.Logger;

import java.sql.SQLException;

import com.google.gson.Gson;

import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;

import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.accounts.User;
import org.ngafid.flights.Upload;

public class PostUploads implements Route {
    private static final Logger LOG = Logger.getLogger(PostUploads.class.getName());
    private Gson gson;

    public PostUploads(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        final Session session = request.session();
        User user = session.attribute("user");

        int fleetId = user.getFleetId();

        try {
            ArrayList<Upload> uploads = Upload.getUploads(Database.getConnection(), fleetId);

            //LOG.info(gson.toJson(uploads));

            return gson.toJson(uploads);
        } catch (SQLException e) {
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
