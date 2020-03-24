package org.ngafid.routes;

import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

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
import org.ngafid.common.FlightPaginator;

import org.ngafid.filters.Filter;

public class PostFlights implements Route {
    private static final Logger LOG = Logger.getLogger(PostFlights.class.getName());
    private Gson gson;
    private FlightPaginator paginator;
    private Filter filter;

    public PostFlights(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        LOG.info("test: " + request.queryParams("test"));

        LOG.info("filter JSON:");

        String filterJSON = request.queryParams("filterQuery");
        System.out.println(request.queryParams("pageIndex"));
        int pageNumber = Integer.parseInt(request.queryParams("pageIndex"));

        LOG.info(filterJSON);
        LOG.info("Jumping to page " + pageNumber);

        Filter filter = gson.fromJson(filterJSON, Filter.class);
        LOG.info("received request for flights with filter: " + filter);


        final Session session = request.session();
        User user = session.attribute("user");
        int fleetId = user.getFleetId();


        //check to see if the user has upload access for this fleet.
        if (!user.hasViewAccess(fleetId)) {
            LOG.severe("INVALID ACCESS: user did not have access view imports for this fleet.");
            Spark.halt(401, "User did not have access to view imports for this fleet.");
            return null;
        }

        try {
            System.out.println(pageNumber);
            if(this.filter == null || !this.filter.equals(filter)){
                this.filter = filter;
                List<Flight> flights = Flight.getFlights(Database.getConnection(), fleetId, this.filter, 50);
                this.paginator = new FlightPaginator(flights);
                //always start on page 0
                pageNumber = 0;
            }


            //LOG.info(gson.toJson(flights));
            this.paginator.jumpToPage(pageNumber);
            return gson.toJson(this.paginator.currentPage());
        } catch (SQLException e) {
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
