package org.ngafid.routes;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.Optional;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.google.gson.Gson;

import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;
import spark.Spark;

import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.common.FlightTag;
import org.ngafid.accounts.User;
import org.ngafid.events.Event;
import org.ngafid.events.EventDefinition;
import org.ngafid.flights.DoubleTimeSeries;
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
