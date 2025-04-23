package org.ngafid.www.routes;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.ngafid.core.Database;
import org.ngafid.core.accounts.User;
import org.ngafid.core.event.Event;
import org.ngafid.core.flights.DoubleTimeSeries;
import org.ngafid.core.flights.Flight;
import org.ngafid.core.flights.StringTimeSeries;
import org.ngafid.routes.ErrorResponse;
import org.ngafid.www.WebServer;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.ngafid.www.WebServer.gson;

public class CesiumDataJavalinRoutes {

    private static final Logger LOG = Logger.getLogger(CesiumDataJavalinRoutes.class.getName());
    private static final String CESIUM_DATA = "cesium_data";

    public static void bindRoutes(Javalin app) {
        app.get("/protected/ngafid_cesium", CesiumDataJavalinRoutes::handleGetNgafidCesium);
        app.post("/protected/cesium_data", CesiumDataJavalinRoutes::handlePostCesiumData);
    }

    private static void handleGetNgafidCesium(Context ctx) {
        LOG.info("Handling /protected/ngafid_cesium route");

        String flightIdStr = ctx.queryParam("flight_id");
        LOG.info("Getting information for flight ID: " + flightIdStr);
        int flightId = Integer.parseInt(flightIdStr);

        String otherFlightId = ctx.queryParam("other_flight_id");
        LOG.info("URL flight ID is: " + flightId);
        LOG.info("URL other flight ID is: " + otherFlightId);

        User user = ctx.sessionAttribute("user");
        if (user == null) {
            LOG.severe("INVALID ACCESS: user was not logged in.");
            ctx.status(401);
            return;
        }
        int fleetId = user.getFleetId();

        if (!user.hasViewAccess(fleetId)) {
            LOG.severe("INVALID ACCESS: User did not have view access for this fleet.");
            ctx.status(401).result("User did not have access to view this fleet.");
            return;
        }

        try (Connection connection = Database.getConnection()) {
            Flight flight = Flight.getFlight(connection, flightId);

            String[] flightIdsAll = ctx.queryParams("flight_id").toArray(new String[0]);
            LOG.info("Flight IDs are: " + Arrays.toString(flightIdsAll));

            if (flight.getFleetId() != fleetId) {
                LOG.severe("INVALID ACCESS: User did not have access to flight ID: " + flightId);
                return;
            }

            HashMap<String, Object> scopes = new HashMap<String, Object>();
            Map<String, Object> flights = new HashMap<>();

            String cesiumData = "";
            for (String flightIdNew : flightIdsAll) {
                Flight incomingFlight = Flight.getFlight(connection, Integer.parseInt(flightIdNew));
                int flightIdNewInteger = Integer.parseInt(flightIdNew);

                String airframeType = incomingFlight.getAirframeType();

                DoubleTimeSeries altMsl = DoubleTimeSeries.getDoubleTimeSeries(connection, flightIdNewInteger,
                        "AltMSL");
                DoubleTimeSeries latitude = DoubleTimeSeries.getDoubleTimeSeries(connection, flightIdNewInteger,
                        "Latitude");
                DoubleTimeSeries longitude = DoubleTimeSeries.getDoubleTimeSeries(connection, flightIdNewInteger,
                        "Longitude");
                DoubleTimeSeries altAgl = DoubleTimeSeries.getDoubleTimeSeries(connection, flightIdNewInteger,
                        "AltAGL");
                DoubleTimeSeries rpm = DoubleTimeSeries.getDoubleTimeSeries(connection, flightIdNewInteger, "E1 RPM");
                DoubleTimeSeries groundSpeed = DoubleTimeSeries.getDoubleTimeSeries(connection, flightIdNewInteger,
                        "GndSpd");

                StringTimeSeries date = StringTimeSeries.getStringTimeSeries(connection, flightIdNewInteger,
                        "Lcl Date");
                StringTimeSeries time = StringTimeSeries.getStringTimeSeries(connection, flightIdNewInteger,
                        "Lcl Time");

                ArrayList<Double> flightGeoAglTaxiing = new ArrayList<>();
                ArrayList<Double> flightGeoAglTakeOff = new ArrayList<>();
                ArrayList<Double> flightGeoAglClimb = new ArrayList<>();
                ArrayList<Double> flightGeoAglCruise = new ArrayList<>();
                ArrayList<Double> flightGeoInfoAgl = new ArrayList<>();

                ArrayList<String> flightTaxiingTimes = new ArrayList<>();
                ArrayList<String> flightTakeOffTimes = new ArrayList<>();
                ArrayList<String> flightClimbTimes = new ArrayList<>();
                ArrayList<String> flightCruiseTimes = new ArrayList<>();
                ArrayList<String> flightAglTimes = new ArrayList<>();

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
                    LOG.severe("INVALID ACCESS: user did not have access to flight id: " + flightId
                            + ", it belonged to fleet: " + flight.getFleetId() + " and the user's fleet id was: "
                            + fleetId);
                    ctx.status(401).result("User did not have access to view this fleet.");
                }

                CesiumResponse cr = new CesiumResponse(flightGeoAglTaxiing, flightGeoAglTakeOff, flightGeoAglClimb,
                        flightGeoAglCruise, flightGeoInfoAgl, flightTaxiingTimes, flightTakeOffTimes, flightClimbTimes,
                        flightCruiseTimes, flightAglTimes, airframeType);
                cesiumData = "var cesium_data_new = " + ctx.json(cr) + ";\n";
                flights.put(flightIdNew, cr);

            }

            scopes.put(CESIUM_DATA, gson.toJson(flights));
            scopes.put("cesium_data_js", gson.toJson(cesiumData));

            // This is for webpage section
            String resultString = "";
            String templateFile = WebServer.MUSTACHE_TEMPLATE_DIR + "ngafid_cesium.html";
            LOG.severe("template file: '" + templateFile + "'");

            MustacheFactory mf = new DefaultMustacheFactory();
            Mustache mustache = mf.compile(templateFile);

            StringWriter stringOut = new StringWriter();
            mustache.execute(new PrintWriter(stringOut), scopes).flush();

            ctx.json(stringOut.toString());
        } catch (Exception e) {
            LOG.severe("Database error: " + e.getMessage());
            ctx.status(500).json(new ErrorResponse(e));
        }
    }


    private static void handlePostCesiumData(Context ctx) {
        LOG.info("Handling /protected/cesium_data route");

        User user = ctx.sessionAttribute("user");
        int fleetId = user.getFleetId();

        if (!user.hasViewAccess(fleetId)) {
            LOG.severe("INVALID ACCESS: User did not have view access for this fleet.");
            ctx.status(401).result("User did not have access to view this fleet.");
            return;
        }

        try (Connection connection = Database.getConnection()) {
            int flightId = Integer.parseInt(ctx.formParam("flightId"));
            Flight flight = Flight.getFlight(connection, flightId);

            Map<Integer, Object> flights = new HashMap<>();
            String airframeType = flight.getAirframeType();

            DoubleTimeSeries altMsl = flight.getDoubleTimeSeries(connection, "AltMSL");
            DoubleTimeSeries latitude = flight.getDoubleTimeSeries(connection, "Latitude");
            DoubleTimeSeries longitude = flight.getDoubleTimeSeries(connection, "Longitude");
            DoubleTimeSeries altAgl = flight.getDoubleTimeSeries(connection, "AltAGL");
            DoubleTimeSeries rpm = flight.getDoubleTimeSeries(connection, "E1 RPM");
            DoubleTimeSeries groundSpeed = flight.getDoubleTimeSeries(connection, "GndSpd");

            StringTimeSeries date = flight.getStringTimeSeries(connection, "Lcl Date");
            StringTimeSeries time = flight.getStringTimeSeries(connection, "Lcl Time");

            ArrayList<Double> flightGeoAglTaxiing = new ArrayList<>();
            ArrayList<Double> flightGeoAglTakeOff = new ArrayList<>();
            ArrayList<Double> flightGeoAglClimb = new ArrayList<>();
            ArrayList<Double> flightGeoAglCruise = new ArrayList<>();
            ArrayList<Double> flightGeoInfoAgl = new ArrayList<>();

            ArrayList<String> flightTaxiingTimes = new ArrayList<>();
            ArrayList<String> flightTakeOffTimes = new ArrayList<>();
            ArrayList<String> flightClimbTimes = new ArrayList<>();
            ArrayList<String> flightCruiseTimes = new ArrayList<>();
            ArrayList<String> flightAglTimes = new ArrayList<>();

            int initCounter = 0;
            int takeoffCounter = 0;
            int countPostTakeoff = 0;
            int sizePreClimb = 0;
            int countPostCruise = 0;
            int dateSize = date.size();


            // Calculate the taxiing phase
            for (int i = 0; i < altAgl.size(); i++) {

                if (!Double.isNaN(longitude.get(i)) && !Double.isNaN(latitude.get(i)) && !Double.isNaN(altAgl.get(i)) && (i < dateSize)) {
                    initCounter++;
                    flightGeoAglTaxiing.add(longitude.get(i));
                    flightGeoAglTaxiing.add(latitude.get(i));
                    flightGeoAglTaxiing.add(altAgl.get(i));
                    flightTaxiingTimes.add(date.get(i) + "T" + time.get(i).trim() + "Z");

                    if ((rpm != null && rpm.get(i) >= 2100) && groundSpeed.get(i) > 14.5 && groundSpeed.get(i) < 80) {
                        break;
                    }
                }
            }

            // Calculate the takeoff-init phase
            for (int i = 0; i < altAgl.size(); i++) {

                if (!Double.isNaN(longitude.get(i)) && !Double.isNaN(latitude.get(i)) && !Double.isNaN(altAgl.get(i)) && (i < dateSize)) {
                    if ((rpm != null && rpm.get(i) >= 2100) && groundSpeed.get(i) > 14.5 && groundSpeed.get(i) < 80) {

                        if (takeoffCounter <= 15) {
                            flightGeoAglTakeOff.add(longitude.get(i));
                            flightGeoAglTakeOff.add(latitude.get(i));
                            flightGeoAglTakeOff.add(altAgl.get(i));
                            flightTakeOffTimes.add(date.get(i) + "T" + time.get(i).trim() + "Z");

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

                if (!Double.isNaN(longitude.get(i)) && !Double.isNaN(latitude.get(i)) && !Double.isNaN(altAgl.get(i)) && (i < dateSize)) {
                    if ((rpm != null && rpm.get(i) >= 2100) && groundSpeed.get(i) > 14.5 && groundSpeed.get(i) <= 80) {

                        if (countPostTakeoff >= 15) {
                            flightGeoAglClimb.add(longitude.get(i));
                            flightGeoAglClimb.add(latitude.get(i));
                            flightGeoAglClimb.add(altAgl.get(i));
                            flightClimbTimes.add(date.get(i) + "T" + time.get(i).trim() + "Z");

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
                if (!Double.isNaN(longitude.get(i)) && !Double.isNaN(latitude.get(i)) && !Double.isNaN(altAgl.get(i)) && (i < dateSize)) {

                    if (countPostCruise >= sizePreClimb) {
                        flightGeoAglCruise.add(longitude.get(i));
                        flightGeoAglCruise.add(latitude.get(i));
                        flightGeoAglCruise.add(altAgl.get(i));
                        flightCruiseTimes.add(date.get(i) + "T" + time.get(i).trim() + "Z");
                    }
                    countPostCruise++;
                }
            }

            // Calculate the full phase
            // I am avoiding NaN here as well!
            for (int i = 0; i < altAgl.size(); i++) {
                if (!Double.isNaN(longitude.get(i)) && !Double.isNaN(latitude.get(i)) && !Double.isNaN(altAgl.get(i)) && (i < dateSize)) {
                    flightGeoInfoAgl.add(longitude.get(i));
                    flightGeoInfoAgl.add(latitude.get(i));
                    flightGeoInfoAgl.add(altAgl.get(i));
                    flightAglTimes.add(date.get(i) + "T" + time.get(i).trim() + "Z");
                }
            }

            CesiumResponse cr = new CesiumResponse(flightGeoAglTaxiing, flightGeoAglTakeOff, flightGeoAglClimb, flightGeoAglCruise, flightGeoInfoAgl, flightTaxiingTimes, flightTakeOffTimes, flightClimbTimes, flightCruiseTimes, flightAglTimes, airframeType);
            flights.put(flightId, cr);

            ctx.json(flights);
        } catch (Exception e) {
            LOG.severe("Database error: " + e.getMessage());
            ctx.status(500).json(new ErrorResponse(e));
        }
    }

    private static class CesiumResponse {
        ArrayList<Double> flightGeoAglTaxiing;
        ArrayList<Double> flightGeoAglTakeOff;
        ArrayList<Double> flightGeoAglClimb;
        ArrayList<Double> flightGeoAglCruise;
        ArrayList<Double> flightGeoInfoAgl;
        ArrayList<Event> events;

        ArrayList<String> flightTaxiingTimes;
        ArrayList<String> flightTakeOffTimes;
        ArrayList<String> flightClimbTimes;
        ArrayList<String> flightCruiseTimes;
        ArrayList<String> flightAglTimes;

        String startTime;
        String endTime;
        String airframeType;

        public CesiumResponse(ArrayList<Double> flightGeoAglTaxiing, ArrayList<Double> flightGeoAglTakeOff,
                              ArrayList<Double> flightGeoAglClimb, ArrayList<Double> flightGeoAglCruise,
                              ArrayList<Double> flightGeoInfoAgl, ArrayList<String> flightTaxiingTimes,
                              ArrayList<String> flightTakeOffTimes, ArrayList<String> flightClimbTimes,
                              ArrayList<String> flightCruiseTimes, ArrayList<String> flightAglTimes,
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
//            this.events = events;
        }
    }
}
