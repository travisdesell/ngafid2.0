package org.ngafid.routes.spark;

import java.util.List;
import java.util.logging.Logger;

import java.sql.Connection;

import com.google.gson.Gson;

import org.ngafid.routes.ErrorResponse;
import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;

import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.flights.Flight;

public class PostSimAircraft implements Route {
    private static final Logger LOG = Logger.getLogger(PostSimAircraft.class.getName());

    private final static String CACHE = "cache";
    private final static String RMCACHE = "rmcache";

    private Gson gson;

    public PostSimAircraft(Gson gson) {
        this.gson = gson;
        LOG.info("initialized " + this.getClass().getName() + " route!");
    }

    /**
     * {inheritDoc}
     */
    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route!");

        final Session session = request.session();
        User user = session.attribute("user");

        String type = request.queryParams("type");
        String path = request.queryParams("path");

        LOG.info("performing " + type + " on " + path);

        int fleetId = user.getFleetId();

        try (Connection connection = Database.getConnection()) {
            switch (type) {
                case CACHE:
                    List<String> currPaths = Flight.getSimAircraft(connection, fleetId);
                    if (!currPaths.contains(path)) {
                        Flight.addSimAircraft(connection, fleetId, path);
                        return gson.toJson("SUCCESS");
                    } else {
                        return gson.toJson("FAILURE");
                    }

                case RMCACHE:
                    Flight.removeSimAircraft(connection, fleetId, path);
                    return gson.toJson(Flight.getSimAircraft(connection, fleetId));

                default:
                    return gson.toJson("FAILURE");
            }

        } catch (Exception e) {
            System.err.println("Error in SQL ");
            e.printStackTrace();
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
