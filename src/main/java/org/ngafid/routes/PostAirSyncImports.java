package org.ngafid.routes;

import com.google.gson.Gson;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;
import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.common.*;
import org.ngafid.flights.AirSyncImport;
import org.ngafid.flights.AirSyncImportResponse;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Session;
import spark.Spark;

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

            int totalImports = AirSyncImport.getNumImports(connection, fleetId, null);
            int numberPages = totalImports / pageSize;

            List<AirSyncImportResponse> imports = AirSyncImport.getImports(connection, fleetId, " LIMIT " + (currentPage * pageSize) + "," + pageSize);

            return gson.toJson(new PaginationResponse<AirSyncImportResponse>(imports, numberPages));
        } catch (SQLException e) {
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
