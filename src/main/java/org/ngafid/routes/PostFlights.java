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
            String orderingColumnn = request.queryParams("sortingColumn");

            boolean isAscending = (request.queryParams("sortingOrder").equals("Ascending") ? true : false);

            Connection connection = Database.getConnection();

            int totalFlights = Flight.getNumFlights(connection, fleetId, filter);
            int numberPages = totalFlights / pageSize;

            LOG.info("Ordered by: " + orderingColumnn);
            ArrayList<Flight> flights = null;

            /**
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
             **/

            switch (orderingColumnn) {
                case "Airframe":
                    flights = Flight.getFlightsSorted(connection, fleetId, filter, currentPage, pageSize, "airframe_id", isAscending);
                    break;
                case "Flight Length (valid data points)":
                    flights = Flight.getFlightsSorted(connection, fleetId, filter, currentPage, pageSize, "number_rows", isAscending);
                    break;
                case "Tail Number":
                    flights = Flight.getFlightsSorted(connection, fleetId, filter, currentPage, pageSize, "tail_number", isAscending);
                    break;
                case "Start Date and Time":
                    flights = Flight.getFlightsSorted(connection, fleetId, filter, currentPage, pageSize, "start_time", isAscending);
                    break;
                case "End Date and Time":
                    flights = Flight.getFlightsSorted(connection, fleetId, filter, currentPage, pageSize, "end_time", isAscending);
                    break;
                case "System ID":
                    flights = Flight.getFlightsSorted(connection, fleetId, filter, currentPage, pageSize, "system_id", isAscending);
                    break;
                case "Number Airports Visited":
                    //flights = Flight.getFlightsSortedByAirportsVisited(connection, fleetId, filter, currentPage, pageSize, isAscending);
                    break;
                case "Number of tags associated":
                    flights = Flight.getFlightsSorted(connection, fleetId, filter, currentPage, pageSize, "flight_tags", isAscending);
                    break;
                case "Number Takeoffs/Landings":
                    flights = Flight.getFlightsSorted(connection, fleetId, filter, currentPage, pageSize, "itinerary", isAscending);
                    break;
                case "Total Event Count":
                    flights = Flight.getFlightsSorted(connection, fleetId, filter, currentPage, pageSize, "events", isAscending);
                    break;
                default:
                    flights = Flight.getFlightsSorted(connection, fleetId, filter, currentPage, pageSize, "id", isAscending);
                    break;
            }


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
