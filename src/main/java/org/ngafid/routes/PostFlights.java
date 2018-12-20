package org.ngafid.routes;

import java.util.ArrayList;
import java.util.logging.Logger;

import java.sql.SQLException;

import com.google.gson.Gson;

import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;

import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.flights.Flight;

public class PostFlights implements Route {
    private static final Logger LOG = Logger.getLogger(PostFlights.class.getName());
    private Gson gson;

    public PostFlights(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        final Session session = request.session();
        User user = session.attribute("user");

        try {
            ArrayList<Flight> flights = Flight.getFlights(Database.getConnection(), user.getFleetId());

            //LOG.info(gson.toJson(flights));

            return gson.toJson(flights);
        } catch (SQLException e) {
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
