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
import org.ngafid.flights.*;
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

    private static class CesiumResponse {
        List<Double> flightGeoAglTaxiing;
        List<Double> flightGeoAglTakeOff;
        List<Double> flightGeoAglClimb;
        List<Double> flightGeoAglCruise;
        List<Double> flightGeoInfoAgl;

        List<String> flightTaxiingTimes;
        List<String> flightTakeOffTimes;
        List<String> flightClimbTimes;
        List<String> flightCruiseTimes;
        List<String> flightAglTimes;
        String startTime;
        String endTime;
        String airframeType;

        public CesiumResponse(List<Double> flightGeoAglTaxiing, List<Double> flightGeoAglTakeOff,
                              List<Double> flightGeoAglClimb, List<Double> flightGeoAglCruise,
                              List<Double> flightGeoInfoAgl, List<String> flightTaxiingTimes,
                              List<String> flightTakeOffTimes, List<String> flightClimbTimes,
                              List<String> flightCruiseTimes, List<String> flightAglTimes,
                              String airframeType) {

            this.flightGeoAglTaxiing = flightGeoAglTaxiing;
            this.flightGeoAglTakeOff = flightGeoAglTakeOff;
            this.flightGeoAglClimb = flightGeoAglClimb;
            this.flightGeoAglCruise = flightGeoAglCruise;
            this.flightGeoInfoAgl = flightGeoInfoAgl;

            this.flightTaxiingTimes = flightTaxiingTimes;
            this.flightTakeOffTimes = flightTakeOffTimes;
            this.flightClimbTimes = flightClimbTimes;
            this.flightCruiseTimes = flightCruiseTimes;
            this.flightAglTimes = flightAglTimes;

            this.startTime = flightAglTimes.get(0);
            this.endTime = flightAglTimes.get(flightAglTimes.size() - 1);
            this.airframeType = airframeType;
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

    public static void getCesium(Context ctx) {
        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        final String flightIdStr = Objects.requireNonNull(ctx.queryParam("flight_id"));
        final String otherFlightId = Objects.requireNonNull(ctx.queryParam("other_flight_id"));
        final int flightId = Integer.parseInt(flightIdStr);
        final int fleetId = user.getFleetId();

        // check to see if the user has upload access for this fleet.
        if (!user.hasViewAccess(fleetId)) {
            ctx.status(401);
            ctx.result("User did not have access to view acces for this fleet.");
            return;
        }

        try (Connection connection = Database.getConnection()) {
            final List<String> flightIdsAll = Objects.requireNonNull(ctx.queryParams("flight_id"));
            final Flight flight = Objects.requireNonNull(Flight.getFlight(connection, flightId));
            final Flight otherFlight = Objects.requireNonNull(Flight.getFlight(connection, Integer.parseInt(otherFlightId)));

            if (flight.getFleetId() != fleetId || otherFlight.getFleetId() != fleetId) {
                ctx.status(401);
                ctx.result("User did not have access to this flight.");
                return;
            }

            Map<String, Object> scopes = new HashMap<String, Object>();
            Map<String, Object> flights = new HashMap<String, Object>();

            for (String flightIdNew : flightIdsAll) {
                final Flight incomingFlight = Objects.requireNonNull(Flight.getFlight(connection, Integer.parseInt(flightIdNew)));
                final int flightIdNewInteger = Integer.parseInt(flightIdNew);

                final String airframeType = incomingFlight.getAirframeType();

                final DoubleTimeSeries altMsl = DoubleTimeSeries.getDoubleTimeSeries(connection, flightIdNewInteger,
                        "AltMSL");
                final DoubleTimeSeries latitude = DoubleTimeSeries.getDoubleTimeSeries(connection, flightIdNewInteger,
                        "Latitude");
                final DoubleTimeSeries longitude = DoubleTimeSeries.getDoubleTimeSeries(connection, flightIdNewInteger,
                        "Longitude");
                final DoubleTimeSeries altAgl = DoubleTimeSeries.getDoubleTimeSeries(connection, flightIdNewInteger,
                        "AltAGL");
                final DoubleTimeSeries rpm = DoubleTimeSeries.getDoubleTimeSeries(connection, flightIdNewInteger, "E1 RPM");
                final DoubleTimeSeries groundSpeed = DoubleTimeSeries.getDoubleTimeSeries(connection, flightIdNewInteger,
                        "GndSpd");

                final StringTimeSeries date = StringTimeSeries.getStringTimeSeries(connection, flightIdNewInteger,
                        "Lcl Date");
                final StringTimeSeries time = StringTimeSeries.getStringTimeSeries(connection, flightIdNewInteger,
                        "Lcl Time");

                final List<Double> flightGeoAglTaxiing = new ArrayList<>();
                final List<Double> flightGeoAglTakeOff = new ArrayList<>();
                final List<Double> flightGeoAglClimb = new ArrayList<>();
                final List<Double> flightGeoAglCruise = new ArrayList<>();
                final List<Double> flightGeoInfoAgl = new ArrayList<>();

                final List<String> flightTaxiingTimes = new ArrayList<>();
                final List<String> flightTakeOffTimes = new ArrayList<>();
                final List<String> flightClimbTimes = new ArrayList<>();
                final List<String> flightCruiseTimes = new ArrayList<>();
                final List<String> flightAglTimes = new ArrayList<>();

                int initCounter = 0;
                int takeoffCounter = 0;
                int countPostTakeoff = 0;
                int sizePreClimb = 0;
                int countPostCruise = 0;
                int dateSize = date.size();

                // Calculate the taxiing phase
                for (int i = 0; i < altAgl.size(); i++) {

                    if (!Double.isNaN(longitude.get(i)) && !Double.isNaN(latitude.get(i))
                            && !Double.isNaN(altAgl.get(i)) && (i < dateSize)) {
                        initCounter++;
                        flightGeoAglTaxiing.add(longitude.get(i));
                        flightGeoAglTaxiing.add(latitude.get(i));
                        flightGeoAglTaxiing.add(altAgl.get(i));
                        flightTaxiingTimes.add(date.get(i) + "T" + time.get(i) + "Z");

                        if ((rpm != null && rpm.get(i) >= 2100) && groundSpeed.get(i) > 14.5
                                && groundSpeed.get(i) < 80) {
                            break;
                        }
                    }
                }

                // Calculate the takeoff-init phase
                for (int i = 0; i < altAgl.size(); i++) {

                    if (!Double.isNaN(longitude.get(i)) && !Double.isNaN(latitude.get(i))
                            && !Double.isNaN(altAgl.get(i)) && (i < dateSize)) {
                        if ((rpm != null && rpm.get(i) >= 2100) && groundSpeed.get(i) > 14.5
                                && groundSpeed.get(i) < 80) {

                            if (takeoffCounter <= 15) {
                                flightGeoAglTakeOff.add(longitude.get(i));
                                flightGeoAglTakeOff.add(latitude.get(i));
                                flightGeoAglTakeOff.add(altAgl.get(i));
                                flightTakeOffTimes.add(date.get(i) + "T" + time.get(i) + "Z");

                                initCounter++;
                            } else if (takeoffCounter > 15) {
                                break;
                            }
                            takeoffCounter++;
                        } else {
                            takeoffCounter = 0;
                        }
                    }
                }

                // Calculate the climb phase
                for (int i = 0; i < altAgl.size(); i++) {

                    if (!Double.isNaN(longitude.get(i)) && !Double.isNaN(latitude.get(i))
                            && !Double.isNaN(altAgl.get(i)) && (i < dateSize)) {
                        if ((rpm != null && rpm.get(i) >= 2100) && groundSpeed.get(i) > 14.5
                                && groundSpeed.get(i) <= 80) {

                            if (countPostTakeoff >= 15) {
                                flightGeoAglClimb.add(longitude.get(i));
                                flightGeoAglClimb.add(latitude.get(i));
                                flightGeoAglClimb.add(altAgl.get(i));
                                flightClimbTimes.add(date.get(i) + "T" + time.get(i) + "Z");

                                initCounter++;
                            }
                            if (altAgl.get(i) >= 500) {
                                break;
                            }
                            countPostTakeoff++;
                        }
                    }
                }

                // Calculate the cruise to final phase
                int preClimb = (flightGeoAglTaxiing.size() + flightGeoAglTakeOff.size() + flightGeoAglClimb.size()) - 9;
                sizePreClimb = preClimb / 3;

                for (int i = 0; i < altAgl.size(); i++) {
                    if (!Double.isNaN(longitude.get(i)) && !Double.isNaN(latitude.get(i))
                            && !Double.isNaN(altAgl.get(i)) && (i < dateSize)) {

                        if (countPostCruise >= sizePreClimb) {
                            flightGeoAglCruise.add(longitude.get(i));
                            flightGeoAglCruise.add(latitude.get(i));
                            flightGeoAglCruise.add(altAgl.get(i));
                            flightCruiseTimes.add(date.get(i) + "T" + time.get(i) + "Z");
                        }
                        countPostCruise++;
                    }
                }

                // Calculate the full phase
                // I am avoiding NaN here as well!
                for (int i = 0; i < altAgl.size(); i++) {
                    if (!Double.isNaN(longitude.get(i)) && !Double.isNaN(latitude.get(i))
                            && !Double.isNaN(altAgl.get(i)) && (i < dateSize)) {
                        flightGeoInfoAgl.add(longitude.get(i));
                        flightGeoInfoAgl.add(latitude.get(i));
                        flightGeoInfoAgl.add(altAgl.get(i));
                        flightAglTimes.add(date.get(i) + "T" + time.get(i) + "Z");
                    }
                }

                if (incomingFlight.getFleetId() != fleetId) {
                    ctx.status(401);
                    ctx.result("User did not have access to this flight.");
                }

                CesiumResponse cr = new CesiumResponse(flightGeoAglTaxiing, flightGeoAglTakeOff, flightGeoAglClimb,
                        flightGeoAglCruise, flightGeoInfoAgl, flightTaxiingTimes, flightTakeOffTimes, flightClimbTimes,
                        flightCruiseTimes, flightAglTimes, airframeType);

                flights.put(flightIdNew, cr);

            }

            scopes.put("cesium_data", gson.toJson(flights));

            // This is for webpage section
            final String templateFile = "ngafid_cesium.html";
            ctx.contentType("text/html");
            ctx.result(MustacheHandler.handle(templateFile, scopes));

        } catch (Exception e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e));
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
