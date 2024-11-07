package org.ngafid.routes.javalin;

import com.google.gson.JsonElement;
import io.javalin.http.Context;
import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.accounts.UserPreferences;
import org.ngafid.airports.Airport;
import org.ngafid.airports.Airports;
import org.ngafid.events.Event;
import org.ngafid.events.EventDefinition;
import org.ngafid.events.RateOfClosure;
import org.ngafid.flights.Airframes;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.Flight;
import org.ngafid.flights.Itinerary;
import org.ngafid.flights.calculations.TurnToFinal;
import org.ngafid.routes.ErrorResponse;
import org.ngafid.routes.MustacheHandler;
import org.ngafid.routes.Navbar;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.ngafid.WebServer.gson;

public class AnalysisJavalinRoutes {
    private static final Logger LOG = Logger.getLogger(AnalysisJavalinRoutes.class.getName());

    private static class Coordinates {
        int nanOffset = -1;
        List<double[]> coordinates = new ArrayList<>();

        public Coordinates(Connection connection, int flightId, String name) throws Exception {
            final DoubleTimeSeries latitudes = Objects.requireNonNull(DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "Latitude"));
            final DoubleTimeSeries longitudes = Objects.requireNonNull(DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "Longitude"));

            for (int i = 0; i < latitudes.size(); i++) {
                double longitude = longitudes.get(i);
                double latitude = latitudes.get(i);

                if (!Double.isNaN(longitude) && !Double.isNaN(latitude) && latitude != 0.0 && longitude != 0.0) {
                    if (nanOffset < 0) nanOffset = i;
                    coordinates.add(new double[]{longitude, latitude});
                }
            }
        }
    }

    private static class RateOfClosurePlotData {

        int[] x;
        double[] y;

        public RateOfClosurePlotData(RateOfClosure rateOfClosure) {
            this.x = new int[rateOfClosure.getSize()];
            this.y = rateOfClosure.getRateOfClosureArray();
            for (int i = -5; i < rateOfClosure.getSize() - 5; i++) {
                x[i + 5] = i;
            }
        }
    }

    private static class FlightMetric {
        String value;
        String name;

        public FlightMetric(double value, String name) {
            // json does not like NaN so we must make it a null string
            this.value = Double.isNaN(value) ? "null" : String.valueOf(value);
            this.name = name;
        }

        public FlightMetric(String name) {
            this(Double.NaN, name);
        }

        @Override
        public String toString() {
            return name + ": " + value;
        }
    }

    private static class FlightMetricResponse {
        List<FlightMetric> values;
        int precision;

        public FlightMetricResponse(List<FlightMetric> values, int precision) {
            this.values = values;
            this.precision = precision;
        }

        @Override
        public String toString() {
            return values.toString() + " with " + precision + " significant figures";
        }
    }

    public static void getSeverities(Context ctx) {
        final String templateFile = "severities.html";
        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        final int fleetId = user.getFleetId();

        try (Connection connection = Database.getConnection()) {
            Map<String, Object> scopes = new HashMap<>();
            final String fleetInfo = "var airframes = " + gson.toJson(Airframes.getAll(connection, fleetId)) + ";\n" + "var eventNames = " + gson.toJson(EventDefinition.getUniqueNames(connection, fleetId)) + ";\n" + "var tagNames = " + gson.toJson(Flight.getAllFleetTagNames(connection, fleetId)) + ";\n";
            scopes.put("navbar_js", Navbar.getJavascript(ctx));
            scopes.put("fleet_info_js", fleetInfo);

            ctx.contentType("text/html");
            ctx.result(MustacheHandler.handle(templateFile, scopes));

        } catch (Exception e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e));
        }
    }

    public static void postSeverities(Context ctx) {
        final String startDate = Objects.requireNonNull(ctx.queryParam("startDate"));
        final String endDate = Objects.requireNonNull(ctx.queryParam("endDate"));
        final String eventName = Objects.requireNonNull(ctx.queryParam("eventName"));
        final String tagName = Objects.requireNonNull(ctx.queryParam("tagName"));
        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        final int fleetId = user.getFleetId();

        // check to see if the user has upload access for this fleet.
        if (!user.hasViewAccess(fleetId)) {
            ctx.status(401);
            ctx.result("User did not have access to view imports for this fleet.");
            return;
        }

        try (Connection connection = Database.getConnection()) {
            Map<String, ArrayList<Event>> eventMap = Event.getEvents(connection, fleetId, eventName, LocalDate.parse(startDate), LocalDate.parse(endDate), tagName);
            ctx.json(eventMap);
        } catch (SQLException e) {
            ctx.json(new ErrorResponse(e));
        }
    }

    public static void getTurnToFinal(Context ctx) {
        final String templateFile = "ttf.html";
        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        final int fleetId = user.getFleetId();

        try (Connection connection = Database.getConnection()) {
            Map<String, Object> scopes = new HashMap<>();

            scopes.put("navbar_js", Navbar.getJavascript(ctx));
            scopes.put("ttf_js", "var airports = " + gson.toJson(Itinerary.getAllAirports(connection, fleetId)) + ";\n" + "var runways = " + gson.toJson(Itinerary.getAllRunwaysWithCoordinates(connection, fleetId)) + ";\n");

            ctx.contentType("text/html");
            ctx.result(MustacheHandler.handle(templateFile, scopes));
        } catch (SQLException e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e));

        } catch (IOException e) {
            LOG.severe(e.toString());
        }
    }

    public static void postTurnToFinal(Context ctx) {
        String startDate = ctx.queryParam("startDate");
        String endDate = ctx.queryParam("endDate");
        String airportIataCode = ctx.queryParam("airport");
        System.out.println(startDate);
        System.out.println(endDate);


        List<JsonElement> _ttfs = new ArrayList<>();
        Set<String> iataCodes = new HashSet<>();
        try (Connection connection = Database.getConnection()) {

            // TODO: Update this limit when caching of TTF objects is implemented.
            List<Flight> flights = Flight.getFlightsWithinDateRangeFromAirport(connection, startDate, endDate, airportIataCode, 10000);

            for (Flight flight : flights) {
                for (TurnToFinal ttf : TurnToFinal.getTurnToFinal(connection, flight, airportIataCode)) {
                    JsonElement jsonElement = ttf.jsonify();
                    if (jsonElement != null) {
                        _ttfs.add(jsonElement);
                        iataCodes.add(ttf.airportIataCode);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            ctx.json(new ErrorResponse(e));
        }

        List<String> iataCodesList = new ArrayList<>(iataCodes.size());
        iataCodesList.addAll(iataCodes);

        for (Airport ap : Airports.getAirports(iataCodesList).values()) {
            System.out.println("long = " + ap.longitude + ", lat = " + ap.latitude);
        }

        Map<String, JsonElement> airports = Airports.getAirports(iataCodesList).entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().jsonify(gson)));

        ctx.status(200);
        ctx.json(Map.of("airports", airports, "ttfs", _ttfs));
    }

    public static void getTrends(Context ctx) {
        final String templateFile = "trends.html";
        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        final int fleetId = user.getFleetId();

        try (Connection connection = Database.getConnection()) {
            Map<String, Object> scopes = new HashMap<>();
            final String fleetInfo = "var airframes = " + gson.toJson(Airframes.getAll(connection, fleetId)) + ";\n" + "var eventNames = " + gson.toJson(EventDefinition.getUniqueNames(connection, fleetId)) + ";\n" + "var tagNames = " + gson.toJson(Flight.getAllTagNames(connection)) + ";\n";

            scopes.put("navbar_js", Navbar.getJavascript(ctx));
            scopes.put("fleet_info_js", fleetInfo);
            ctx.contentType("text/html");
            ctx.result(MustacheHandler.handle(templateFile, scopes));
        } catch (SQLException e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e));
        } catch (IOException e) {
            LOG.severe(e.toString());
        }
    }

    public static void postRateOfClosure(Context ctx) {
        final int eventId = Integer.parseInt(Objects.requireNonNull(ctx.queryParam("eventId")));
        try (Connection connection = Database.getConnection()) {
            RateOfClosure rateOfClosure = RateOfClosure.getRateOfClosureOfEvent(connection, eventId);
            if (rateOfClosure != null) {
                ctx.json(gson.toJson(new RateOfClosurePlotData(rateOfClosure)));
            }
        } catch (Exception e) {
            ctx.json(new ErrorResponse(e));
        }
    }

    public static void postLociMetrics(Context ctx) {
        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        final int flightId = Integer.parseInt(Objects.requireNonNull(ctx.queryParam("flight_id")));
        final int timeIndex = Integer.parseInt(Objects.requireNonNull(ctx.queryParam("time_index")));

        try (Connection connection = Database.getConnection()) {
            // check to see if the user has access to this data
            if (!user.hasFlightAccess(connection, flightId)) {
                ctx.status(401);
                ctx.result("User did not have access to this flight.");
            }

            LOG.info("getting metrics for flight #" + flightId + " at index " + timeIndex);

            final UserPreferences userPreferences = User.getUserPreferences(connection, user.getId());
            List<String> metrics = userPreferences.getFlightMetrics();
            List<FlightMetric> flightMetrics = new ArrayList<>();

            for (String seriesName : metrics) {
                DoubleTimeSeries series = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, seriesName);

                if (series == null) {
                    flightMetrics.add(new FlightMetric(seriesName));
                } else {
                    flightMetrics.add(new FlightMetric(series.get(timeIndex), seriesName));
                }
            }

            ctx.json(new FlightMetricResponse(flightMetrics, userPreferences.getDecimalPrecision()));
        } catch (Exception e) {
            e.printStackTrace();
            ctx.json(new ErrorResponse(e));
        }
    }

    public static void postCoordinates(Context ctx) {
        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        final String name = Objects.requireNonNull(ctx.queryParam("seriesName"));
        final int flightId = Integer.parseInt(Objects.requireNonNull(ctx.queryParam("flightId")));

        try (Connection connection = Database.getConnection()) {
            // check to see if the user has access to this data
            if (!user.hasFlightAccess(connection, flightId)) {
                ctx.status(401);
                ctx.result("User did not have access to this flight.");
            }

            final Coordinates coordinates = new Coordinates(connection, flightId, name);
            final String output = gson.toJson(coordinates).replaceAll("NaN", "null");

            ctx.json(output);
        } catch (Exception e) {
            e.printStackTrace();
            ctx.json(new ErrorResponse(e));
        }
    }
}
