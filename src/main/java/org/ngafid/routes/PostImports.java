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
import org.ngafid.flights.Upload;

public class PostImports implements Route {
    private static final Logger LOG = Logger.getLogger(PostImports.class.getName());
    private Gson gson;

    public PostImports(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        final Session session = request.session();
        String userSession = session.attribute("user");
        LOG.info("user session (after generate): " + userSession);
        LOG.info("session id: " + session.id());

        int fleetId = 1;

        try {
            ArrayList<Upload> imports = Upload.getUploads(Database.getConnection(), fleetId, new String[]{"IMPORTED", "ERROR"});

            //LOG.info(gson.toJson(imports));

            return gson.toJson(imports);
        } catch (SQLException e) {
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
