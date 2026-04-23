package org.ngafid.www.routes;

import static org.ngafid.www.WebServer.GSON;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;
import org.ngafid.core.Config;
import org.ngafid.core.Database;
import org.ngafid.core.accounts.User;
import org.ngafid.core.flights.*;
import org.ngafid.core.util.filters.Filter;
import org.ngafid.www.ErrorResponse;
import org.ngafid.www.Navbar;

public class FlightsJavalinRoutes {
    private static final Logger LOG = Logger.getLogger(FlightsJavalinRoutes.class.getName());

    private FlightsJavalinRoutes() {
        // Utility class
    }

    private static String getChartTileBaseUrlScript() {
        try {
            String chartBase = Config.getProperty("ngafid.chart.tile.base.url");
            if (chartBase != null && !chartBase.trim().isEmpty())
                chartBase = chartBase.replaceAll("/+$", "");
            else
                chartBase = "http://localhost:8187";
            return "var chartTileBaseUrl = '" + chartBase + "';\n";
        } catch (RuntimeException e) {
            return "var chartTileBaseUrl = 'http://localhost:8187';\n";
        }
    }

    private static class FlightsResponse {
        @JsonProperty
        private final List<Flight> flights;

        @JsonProperty
        private final int numberPages;
        @JsonProperty
        private final int totalFlights;

        FlightsResponse(List<Flight> flights, int numberPages, int totalFlights) {
            this.flights = flights;
            this.numberPages = numberPages;
            this.totalFlights = totalFlights;
        }

        public List<Flight> getFlights() {
            return flights;
        }

        public int getNumberPages() {
            return numberPages;
        }
        public int getTotalFlights() {
            return totalFlights;
        }

    }

    private static void getFlight(Context ctx) {
        final String templateFile = "flight.html";
        final Map<String, Object> scopes = new HashMap<String, Object>();
        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        final int fleetId = user.getFleetId();
        final List<String> flightIds = Objects.requireNonNull(ctx.formParams("flight_id"));
        final List<Flight> flights = new ArrayList<>();

        try (Connection connection = Database.getConnection()) {
            scopes.put("navbar_js", Navbar.getJavascript(ctx));
            scopes.put("chart_tile_base_url", getChartTileBaseUrlScript());
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
                if (!first) sb.append(", ");
                first = false;
                sb.append(GSON.toJson(flight));
            }
            sb.append("];");

            scopes.put("flight_js", sb.toString());

            ctx.header("Content-Type", "text/html; charset=UTF-8");
            ctx.render(templateFile, scopes);
        } catch (Exception e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    private static void getFlightDisplay(Context ctx) {
        final String templateFile = "flight_display.html";
        final Map<String, Object> scopes = new HashMap<>();

        try {
            scopes.put("navbar_js", Navbar.getJavascript(ctx));

            ctx.header("Content-Type", "text/html; charset=UTF-8");
            ctx.render(templateFile, scopes);
        } catch (Exception e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    public static void getFlights(Context ctx) {
        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        final String filterJSON = Objects.requireNonNull(ctx.queryParam("filterQuery"));

        // Handle the case where the filter is null or empty
        if (filterJSON == null || filterJSON.isEmpty()) {
            LOG.severe("INVALID REQUEST: filter query parameter is missing or empty.");
            ctx.status(400);
            ctx.result("Filter query parameter is required and cannot be empty.");
            return;
        }

        try {

            final Filter filter = Filter.fromUiJson(filterJSON);
            final int fleetId = user.getFleetId();

            // check to see if the user has upload access for this fleet.
            if (!user.hasViewAccess(fleetId)) {
                LOG.severe("INVALID ACCESS: user did not have access view imports for this fleet.");
                ctx.status(401);
                ctx.result("User did not have access to view imports for this fleet.");
                return;
            }

            try (Connection connection = Database.getConnection()) {
                final int currentPage = Integer.parseInt(Objects.requireNonNull(ctx.queryParam("currentPage")));
                final int pageSize = Integer.parseInt(Objects.requireNonNull(ctx.queryParam("pageSize")));
                final String orderingColumnn = Objects.requireNonNull(ctx.queryParam("sortingColumn"));

                final boolean isAscending = Objects.equals(ctx.queryParam("sortingOrder"), "Ascending");

                final int totalFlights = Flight.getNumFlights(connection, fleetId, filter);
                final int numberPages = (int) Math.ceil((double) totalFlights / pageSize);

                LOG.info(() -> "Ordered by: " + orderingColumnn);
                LOG.info(() -> "Filter: " + filter.toString());

                /*
                    - Valid Column Names:

                    - Flight Number
                    - Flight Length (valid data points)
                    - Start Date and Time
                    - End Date and Time
                    - Number Airports Visited
                    - Number of tags associated
                    - Total Event Count
                    - System ID
                    - Tail Number
                    - Airframe
                    - Number Takeoffs/Landings
                    - Flight ID
                */
                List<Flight> flights = Flight.getFlightsSorted(
                    connection, fleetId, filter, currentPage, pageSize, orderingColumnn, isAscending
                );

                // Populate event counts for each flight
                if (!flights.isEmpty()) {

                    List<Integer> ids = new ArrayList<>(flights.size());
                    for (Flight f : flights) ids.add(f.getId());

                    Map<Integer, Integer> counts = Flight.getEventCounts(connection, ids);
                    for (Flight f : flights)
                        f.setEventCount(counts.getOrDefault(f.getId(), 0));

                }

                if (flights.isEmpty()) {
                    ctx.status(204);
                } else {
                    ctx.json(new FlightsResponse(flights, numberPages, totalFlights)).status(200);
                }

            } catch (SQLException e) {
                ctx.json(new ErrorResponse(e)).status(500);
            }

        } catch (IllegalArgumentException e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e)).status(400);
        }

    }

    public static void bindRoutes(Javalin app) {
        app.get("/protected/flight", FlightsJavalinRoutes::getFlight);
        app.get("/protected/flights", FlightsJavalinRoutes::getFlights);
        // app.post("/protected/get_flights", FlightsJavalinRoutes::postFlights);
        app.get("/protected/flights/flight_display", FlightsJavalinRoutes::getFlightDisplay);
    }
}
