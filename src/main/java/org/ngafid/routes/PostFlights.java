package org.ngafid.routes;

import com.google.gson.Gson;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Logger;
import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.common.*;
import org.ngafid.filters.Filter;
import org.ngafid.flights.Flight;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Session;
import spark.Spark;

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
            String orderingColumnn = request.queryParams("sortingColumn");

            boolean isAscending = (request.queryParams("sortingOrder").equals("Ascending") ? true : false);

            Connection connection = Database.getConnection();

            int totalFlights = Flight.getNumFlights(connection, fleetId, filter);
            int numberPages = (int) Math.ceil((double) totalFlights / pageSize);

            LOG.info("Ordered by: " + orderingColumnn);
            LOG.info("Filter: " + filter.toString());

            ArrayList<Flight> flights = null;

            /**
             * Valid Column Names:
             *
             * Flight Number
             * Flight Length (valid data points)
             * Start Date and Time
             * End Date and Time
             * Number Airports Visited
             * Number of tags associated
             * Total Event Count
             * System ID
             * Tail Number
             * Airframe
             * Number Takeoffs/Landings
             * Flight ID
             **/

            flights = Flight.getFlightsSorted(connection, fleetId, filter, currentPage, pageSize, orderingColumnn, isAscending);

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
