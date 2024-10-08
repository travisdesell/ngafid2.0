/**
 * Gets the cached aircrafts for xplane in the database
 * @author <a href=mailto:apl1341@cs.rit.edu>Aidan LaBella</a>
 */
package org.ngafid.routes.spark;

import java.util.logging.Logger;
import java.util.List;
import com.google.gson.Gson;

import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;
import spark.Spark;

import java.sql.Connection;
import java.sql.SQLException;

import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.flights.Flight;

public class GetSimAircraft implements Route {
    private static final Logger LOG = Logger.getLogger(GetSimAircraft.class.getName());
    private Gson gson;

    /**
     * Constructor
     * 
     * @param gson the gson object for JSON conversions
     */
    public GetSimAircraft(Gson gson) {
        this.gson = gson;

        LOG.info("GET " + this.getClass().getName() + " initalized");
    }

    /**
     * {inheritDoc}
     */
    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        final Session session = request.session();
        User user = session.attribute("user");
        int fleetId = user.getFleetId();

        if (!user.hasViewAccess(fleetId)) {
            LOG.severe("INVALID ACCESS: user did not have view access this fleet.");
            Spark.halt(401, "User did not have access to view acces for this fleet.");
            return null;
        }

        try (Connection connection = Database.getConnection()) {
            List<String> paths = Flight.getSimAircraft(connection, fleetId);
            System.out.println("paths: " + paths.toString());
            return gson.toJson(paths);
        } catch (SQLException e) {
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
