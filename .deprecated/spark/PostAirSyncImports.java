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
import org.ngafid.flights.AirSyncImport;
import org.ngafid.flights.AirSyncImportResponse;

public class PostAirSyncImports implements Route {
    private static final Logger LOG = Logger.getLogger(PostAirSyncImports.class.getName());
    private Gson gson;

    public PostAirSyncImports(Gson gson) {
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
            int currentPage = Integer.parseInt(request.formParams("currentPage"));
            int pageSize = Integer.parseInt(request.formParams("pageSize"));

            int totalImports = AirSyncImport.getNumImports(connection, fleetId, null);
            int numberPages = totalImports / pageSize;

            List<AirSyncImportResponse> imports = AirSyncImport.getImports(connection, fleetId,
                    " LIMIT " + (currentPage * pageSize) + "," + pageSize);

            return gson.toJson(new PaginationResponse<AirSyncImportResponse>(imports, numberPages));
        } catch (SQLException e) {
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
