package org.ngafid.routes;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.DecimalFormat;

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
public class PostMaintenanceStatistics implements Route {
    private static final Logger LOG = Logger.getLogger(PostMaintenanceStatistics.class.getName());
    private Gson gson;
    private static Connection connection = Database.getConnection();

    private class MaintenanceResponse {
        private String maintenanceProbability, maintenanceRating;

        public MaintenanceResponse(int rating, double probability) {
            if (rating >= 0 && rating <= 10) {
                this.maintenanceRating = "" + rating;
            } else if (probability == -1) {
                this.maintenanceRating = "NS/NR";
            } else {
                this.maintenanceRating = "NR";
            }

            if (probability == -1) {
                this.maintenanceProbability = "No Stats";
            } else if (probability == -2) {
                this.maintenanceRating = "No Stats (csv format err.)";
            } else {
                this.maintenanceProbability = new DecimalFormat("#.##").format(probability);
            }
        }
    }

    public PostMaintenanceStatistics(Gson gson) {
        this.gson = gson;

        LOG.info("post loci metrics route initialized.");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling supplementary loci metrics route!");

        final Session session = request.session();
        int flightId = Integer.parseInt(request.queryParams("flightId"));

        User user = session.attribute("user");

        try {
            //check to see if the user has access to this data
            if (!user.hasFlightAccess(Database.getConnection(), flightId)) {
                LOG.severe("INVALID ACCESS: user did not have access to this flight.");
                Spark.halt(401, "User did not have access to this flight.");
            }

            double maintProb = Flight.getMaintenanceStatistics(connection, flightId);
            int maintRating = Flight.getMaintenanceRating(connection, flightId);

            return gson.toJson(new MaintenanceResponse(maintRating, maintProb));
        } catch (SQLException e) {
            e.printStackTrace();
            return gson.toJson(new ErrorResponse(e));
        }
    }
}

