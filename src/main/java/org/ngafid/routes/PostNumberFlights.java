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
import org.ngafid.common.FlightTag;
import org.ngafid.accounts.User;
import org.ngafid.events.Event;
import org.ngafid.events.EventDefinition;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.Flight;

public class PostNumberFlights implements Route {
    private static final Logger LOG = Logger.getLogger(PostNumberFlights.class.getName());
    private Gson gson;

    public PostNumberFlights(Gson gson) {
        this.gson = gson;

        LOG.info("initialized " + this.getClass().getName() + " route!");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route!");
        return "";
        // final Session session = request.session();
        // User user = session.attribute("user");

        // int airframeId = Integer.parseInt(request.queryParams("airframe_id"));
        // int tagId = Integer.parseInt(request.queryParams("tag_id"));

        // try {
        //     Connection connection = Database.getConnection();

        //     if (!user.hasFlightAccess(connection, flightId)) {
        //         LOG.severe("INVALID ACCESS: user did not have access to this flight.");
        //         Spark.halt(401, "User did not have access to this flight.");
        //     }

        //     Flight.associateTag(flightId, tagId, connection);

        //     FlightTag ft = Flight.getTag(connection, tagId);

        //     return gson.toJson(ft);
        // } catch (SQLException e) {
        //     System.err.println("Error in SQL ");
        //     e.printStackTrace();
        //     return gson.toJson(new ErrorResponse(e));
        // }
    }
}
