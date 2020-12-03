package org.ngafid.routes;

import java.util.List;
import java.util.ArrayList;
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
import org.ngafid.WebServer;
import org.ngafid.accounts.User;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.Flight;
import org.ngafid.common.*;

import org.ngafid.filters.Filter;

public class PostFlights implements Route {
    private static final Logger LOG = Logger.getLogger(PostFlights.class.getName());
    private Gson gson;

    public PostFlights(Gson gson) {
        this.gson = gson;
        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    public static class FlightsResponse {
        public ArrayList<Flight> flights;
        public int numberPages;

        public FlightsResponse(ArrayList<Flight> flights, int numberPages) {
            this.flights = flights;
            this.numberPages = numberPages;
        }
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        LOG.info("test: " + request.queryParams("test"));

        LOG.info("filter JSON:");

        String filterJSON = request.queryParams("filterQuery");


        System.err.println(request.queryParams("currentPage"));
        System.err.println(request.queryParams("numPerPage"));


        LOG.info(filterJSON);


        Filter filter = gson.fromJson(filterJSON, Filter.class);
        LOG.info("received request for flights with filter: " + filter);

        final Session session = request.session();
        User user = session.attribute("user");
        int fleetId = user.getFleetId();

        LOG.info("USER: " + user.getId() + ", '" + user.getFullName() + "', fleetId: " + fleetId);


        //check to see if the user has upload access for this fleet.
        if (!user.hasViewAccess(fleetId)) {
            LOG.severe("INVALID ACCESS: user did not have access view imports for this fleet.");
            Spark.halt(401, "User did not have access to view imports for this fleet.");
            return null;
        }

        try {
            int currentPage = Integer.parseInt(request.queryParams("currentPage"));
            int pageSize = Integer.parseInt(request.queryParams("pageSize"));

            Connection connection = Database.getConnection();

            int totalFlights = Flight.getNumFlights(connection, fleetId, filter);
            int numberPages = totalFlights / pageSize;
            ArrayList<Flight> flights = Flight.getFlights(connection, fleetId, filter, " LIMIT "+ (currentPage * pageSize) + "," + pageSize);

            //List<String> fltNums = new ArrayList<>();
            //for (Flight flight : flights) {
                //DoubleTimeSeries aoa = DoubleTimeSeries.getDoubleTimeSeries(connection, flight.getId(), "AOASimple");
                //fltNums.add(flight.getId() + "\t\t\t" + aoa.getMax() + "\n");
            //}

            //for (String s : fltNums) {
                //System.out.print(s);
            //}

            if (flights.size() == 0) {
                return gson.toJson("NO_RESULTS");
            } else {
                return gson.toJson(new FlightsResponse(flights, numberPages));
            }

        } catch (SQLException e) {
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
