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

public class PostImports implements Route {
    private static final Logger LOG = Logger.getLogger(PostImports.class.getName());
    private Gson gson;

    public PostImports(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    public static class ImportsResponse {
        public List<Upload> imports;
        public int numberPages;

        public ImportsResponse(List<Upload> imports, int numberPages) {
            this.imports = imports;
            this.numberPages = numberPages;
        }
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        final Session session = request.session();
        User user = session.attribute("user");

        int fleetId = user.getFleetId();

        // check to see if the user has upload access for this fleet.
        if (!user.hasUploadAccess(fleetId)) {
            LOG.severe("INVALID ACCESS: user did not have access view imports for this fleet.");
            Spark.halt(401, "User did not have access to view imports for this fleet.");
            return null;
        }

        try (Connection connection = Database.getConnection()) {
            int currentPage = Integer.parseInt(request.queryParams("currentPage"));
            int pageSize = Integer.parseInt(request.queryParams("pageSize"));

            int totalImports = Upload.getNumUploads(connection, fleetId, null);
            int numberPages = totalImports / pageSize;
            List<Upload> imports = Upload.getUploads(connection, fleetId, new String[] { "IMPORTED", "ERROR" },
                    " LIMIT " + (currentPage * pageSize) + "," + pageSize);

            return gson.toJson(new ImportsResponse(imports, numberPages));
        } catch (SQLException e) {
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
