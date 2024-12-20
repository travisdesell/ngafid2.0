package org.ngafid.routes.javalin;

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.events.EventDefinition;
import org.ngafid.filters.Filter;
import org.ngafid.flights.*;
import org.ngafid.routes.ErrorResponse;
import org.ngafid.routes.MustacheHandler;
import org.ngafid.routes.Navbar;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

import static org.ngafid.WebServer.gson;

public class FlightsJavalinRoutes {
    private static Logger LOG = Logger.getLogger(FlightsJavalinRoutes.class.getName());

    private static class FlightsResponse {
        public List<Flight> flights;
        public int numberPages;

        public FlightsResponse(List<Flight> flights, int numberPages) {
            this.flights = flights;
            this.numberPages = numberPages;
        }
    }

    private static void getFlight(Context ctx) {
        final String templateFile = "flight.html";

        try (Connection connection = Database.getConnection()) {
            Map<String, Object> scopes = new HashMap<String, Object>();

            scopes.put("navbar_js", Navbar.getJavascript(ctx));

            User user = ctx.sessionAttribute("user");
            if (user == null) {
                ctx.json(new ErrorResponse("error", "User not logged in."));
                return;
            }
            int fleetId = user.getFleetId();

            List<String> flightIds = ctx.formParams("flight_id");
            List<Flight> flights = new ArrayList<>();

            for (String flightId : flightIds) {
                Flight flight = Flight.getFlight(connection, Integer.parseInt(flightId));

                if (flight != null && flight.getFleetId() != fleetId) {
                    LOG.severe("INVALID ACCESS: user did not have access to flight id: " + flightId
                            + ", it belonged to fleet: " + flight.getFleetId() + " and the user's fleet id was: "
                            + fleetId);
                    ctx.status(401);
                    ctx.json("User did not have access to this flight.");
                }

                flights.add(flight);
            }

            boolean first = true;
            StringBuilder sb = new StringBuilder();

            sb.append("var flights = [");
            for (Flight flight : flights) {
                if (!first)
                    sb.append(", ");
                first = false;
                sb.append(gson.toJson(flight));
            }
            sb.append("];");

            scopes.put("flight_js", sb.toString());

            ctx.header("Content-Type", "text/html; charset=UTF-8");
            ctx.render(templateFile, scopes);
        } catch (Exception e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e));
        }
    }
    private static void getFlights(Context ctx) {
        final String templateFile = "flights.html";

        try (Connection connection = Database.getConnection()) {
            Map<String, Object> scopes = new HashMap<String, Object>();

            scopes.put("navbar_js", Navbar.getJavascript(ctx));

            final User user = ctx.sessionAttribute("user");
            if (user == null) {
                ctx.json(new ErrorResponse("error", "User not logged in."));
                return;
            }
            final int fleetId = user.getFleetId();

            long startTime;
            long endTime;

            StringBuilder sb = new StringBuilder();

            sb.append("var airframes = JSON.parse('");
            startTime = System.currentTimeMillis();
            sb.append(gson.toJson(Airframes.getAll(connection, fleetId)));
            endTime = System.currentTimeMillis();
            LOG.info("get all airframes took: " + ((endTime - startTime) / 1000.0) + " seconds");
            sb.append("');\n");

            sb.append("var tagNames = JSON.parse('");
            startTime = System.currentTimeMillis();
            sb.append(gson.toJson(Flight.getAllFleetTagNames(connection, fleetId)));
            endTime = System.currentTimeMillis();
            LOG.info("get all tag names took: " + ((endTime - startTime) / 1000.0) + " seconds");
            sb.append("');\n");

            sb.append("var tailNumbers = JSON.parse('");
            startTime = System.currentTimeMillis();
            sb.append(gson.toJson(Tails.getAllTails(connection, fleetId)));
            endTime = System.currentTimeMillis();
            LOG.info("get all tails names took: " + ((endTime - startTime) / 1000.0) + " seconds");
            sb.append("');\n");

            sb.append("var systemIds = JSON.parse('");
            startTime = System.currentTimeMillis();
            sb.append(gson.toJson(Tails.getAllSystemIds(connection, fleetId)));
            endTime = System.currentTimeMillis();
            LOG.info("get all system ids names took: " + ((endTime - startTime) / 1000.0) + " seconds");
            sb.append("');\n");

            sb.append("var doubleTimeSeriesNames = JSON.parse('");
            startTime = System.currentTimeMillis();
            sb.append(gson.toJson(DoubleTimeSeries.getAllNames(connection, fleetId)));
            endTime = System.currentTimeMillis();
            LOG.info("get all double time series names took: " + ((endTime - startTime) / 1000.0) + " seconds");
            sb.append("');\n");

            sb.append("var visitedAirports = JSON.parse('");
            startTime = System.currentTimeMillis();
            ArrayList<String> airports = Itinerary.getAllAirports(connection, fleetId);
            sb.append(gson.toJson(airports));
            endTime = System.currentTimeMillis();
            LOG.info("get all airports names took: " + ((endTime - startTime) / 1000.0) + " seconds");
            sb.append("');\n");

            sb.append("var visitedRunways = JSON.parse('");
            startTime = System.currentTimeMillis();
            sb.append(gson.toJson(Itinerary.getAllAirportRunways(connection, fleetId)));
            endTime = System.currentTimeMillis();
            LOG.info("get all runways names took: " + ((endTime - startTime) / 1000.0) + " seconds");
            sb.append("');\n");

            sb.append("var eventNames = JSON.parse('");
            startTime = System.currentTimeMillis();
            sb.append(gson.toJson(EventDefinition.getAllNames(connection, fleetId)));
            endTime = System.currentTimeMillis();
            LOG.info("get all event definition names took: " + ((endTime - startTime) / 1000.0) + " seconds");
            sb.append("');\n");

            sb.append("var flights = [];");

            scopes.put("flights_js", sb.toString());


            StringWriter stringOut = new StringWriter();
            startTime = System.currentTimeMillis();

            ctx.header("Content-Type", "text/html; charset=UTF-8");
            ctx.render(templateFile, scopes);

            endTime = System.currentTimeMillis();
            LOG.info("mustache write took: " + ((endTime - startTime) / 1000.0) + " seconds");
        } catch (Exception e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e));
        }
    }

    private static void getFlightDisplay(Context ctx) {
        final String templateFile = "flight_display.html";

        try {
            Map<String, Object> scopes = new HashMap<>();
            scopes.put("navbar_js", Navbar.getJavascript(ctx));

            ctx.header("Content-Type", "text/html; charset=UTF-8");
            ctx.render(templateFile, scopes);
        } catch (Exception e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e));
        }
    }

    private static void postFlights(Context ctx) {
        final String filterJSON = ctx.formParam("filterQuery");
        final Filter filter = gson.fromJson(filterJSON, Filter.class);
        final User user = ctx.attribute("user");
        if (user == null) {
            ctx.json(new ErrorResponse("error", "User not logged in."));
            return;
        }
        final int fleetId = user.getFleetId();


        // check to see if the user has upload access for this fleet.
        if (!user.hasViewAccess(fleetId)) {
            LOG.severe("INVALID ACCESS: user did not have access view imports for this fleet.");
            ctx.status(401);
            ctx.result("User did not have access to view imports for this fleet.");
            return;
        }

        try (Connection connection = Database.getConnection()) {
            final int currentPage = Integer.parseInt(Objects.requireNonNull(ctx.formParam("currentPage")));
            final int pageSize = Integer.parseInt(Objects.requireNonNull(ctx.formParam("pageSize")));
            final String orderingColumnn = Objects.requireNonNull(ctx.formParam("sortingColumn"));

            final boolean isAscending = Objects.equals(ctx.formParam("sortingOrder"), "Ascending");

            final int totalFlights = Flight.getNumFlights(connection, fleetId, filter);
            final int numberPages = (int) Math.ceil((double) totalFlights / pageSize);

            LOG.info("Ordered by: " + orderingColumnn);
            LOG.info("Filter: " + filter.toString());


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
            List<Flight> flights = Flight.getFlightsSorted(connection, fleetId, filter, currentPage, pageSize, orderingColumnn, isAscending);

            if (flights.isEmpty()) {
                ctx.json("NO_RESULTS");
            } else {
                ctx.json(new FlightsResponse(flights, numberPages));
            }

        } catch (SQLException e) {
            ctx.json(new ErrorResponse(e));
        }
    }

    public static void bindRoutes(Javalin app) {
        app.get("/protected/flight", FlightsJavalinRoutes::getFlight);
        app.get("/protected/flights", FlightsJavalinRoutes::getFlights);
        app.post("/protected/get_flights", FlightsJavalinRoutes::postFlights);
        app.get("/protected/flights/flight_display", FlightsJavalinRoutes::getFlightDisplay);
    }
}
