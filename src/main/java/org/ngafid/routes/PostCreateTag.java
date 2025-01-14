package org.ngafid.routes;

import com.google.gson.Gson;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;
import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.common.FlightTag;
import org.ngafid.flights.Flight;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Session;

public class PostCreateTag implements Route {
    private static final Logger LOG = Logger.getLogger(PostTags.class.getName());
    private Gson gson;

    static String ALREADY_EXISTS = "ALREADY_EXISTS";

    public PostCreateTag(Gson gson) {
        this.gson = gson;

        LOG.info("initialized " + this.getClass().getName() + " route!");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route!");

        final Session session = request.session();
        User user = session.attribute("user");

        String name = request.queryParams("name");
        String description = request.queryParams("description");
        String color = request.queryParams("color");
        int flightId = Integer.parseInt(request.queryParams("id"));

        int fleetId = user.getFleetId();

        try {
            Connection connection = Database.getConnection();

            if(Flight.tagExists(connection, fleetId, name)){
                return gson.toJson(ALREADY_EXISTS);
            }

            FlightTag nTag = Flight.createTag(fleetId, flightId, name, description, color, connection);


            // if (!user.hasFlightAccess(Database.getConnection(), flightId)) {
            //     LOG.severe("INVALID ACCESS: user did not have access to this flight.");
            //     Spark.halt(401, "User did not have access to this flight.");
            // }

            return gson.toJson(nTag);
        } catch (SQLException e) {
            System.err.println("Error in SQL ");
            e.printStackTrace();
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
