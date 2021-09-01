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
import org.ngafid.accounts.User;
import org.ngafid.flights.Flight;

/**
 * This class posts the Maintenance Statistics to the user
 *
 * @author <a href=mailto:apl1341@cs.rit.edu>Aidan LaBella @ RIT CS</a>
 */
public class PostMaintenanceRating implements Route {
    private static final Logger LOG = Logger.getLogger(PostMaintenanceRating.class.getName());
    private Gson gson;
    private static Connection connection = Database.getConnection();

    public PostMaintenanceRating(Gson gson) {
        this.gson = gson;

        LOG.info("post loci metrics route initialized.");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling supplementary loci metrics route!");

        final Session session = request.session();
        int flightId = Integer.parseInt(request.queryParams("flightId"));
        int rating = Integer.parseInt(request.queryParams("rating"));

        User user = session.attribute("user");

        try {
            //check to see if the user has access to this data
            if (!user.hasFlightAccess(Database.getConnection(), flightId)) {
                LOG.severe("INVALID ACCESS: user did not have access to this flight.");
                Spark.halt(401, "User did not have access to this flight.");
            }

            Flight.setMaintenanceRating(connection, rating, flightId);

            return gson.toJson("SUCCESS");
        } catch (SQLException e) {
            e.printStackTrace();
            return gson.toJson(new ErrorResponse(e));
        }
    }
}

