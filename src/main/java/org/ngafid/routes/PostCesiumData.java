package org.ngafid.routes;

import com.google.gson.Gson;
import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.events.Event;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.Flight;
import org.ngafid.flights.StringTimeSeries;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Session;
import spark.Spark;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class PostCesiumData implements Route {

    private static final Logger LOG = Logger.getLogger(PostCesiumData.class.getName());
    private Gson gson;

    public PostCesiumData(Gson gson) {
        this.gson = gson;

        LOG.info("get " + this.getClass().getName() + " initalized");
    }

    public static class CesiumResponse {
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
                              String airframeType, ArrayList<Event> events) {

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
            this.events = events;
        }
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {

        LOG.info("handling " + this.getClass().getName() + " route");


        /* String flightIdStr = request.queryParams("flight_id");
        LOG.info("getting information for flight id: " + flightIdStr);
        int flightId = Integer.parseInt(flightIdStr);

        String otherFlightId = request.queryParams("other_flight_id");
        LOG.info("URL flight id is: " + flightId);
        LOG.info("URL other flight id is: " + otherFlightId);
 */
        final Session session = request.session();
        User user = session.attribute("user");
        int fleetId = user.getFleetId();

        // check to see if the user has upload access for this fleet.
        if (!user.hasViewAccess(fleetId)) {
            LOG.severe("INVALID ACCESS: user did not have view access this fleet.");
            Spark.halt(401, "User did not have access to view acces for this fleet.");
            return null;
        }

        try {

            Connection connection = Database.getConnection();
            // Flight flight = Flight.getFlight(connection, flightId);

            String[] flightIdsAll = request.queryParamsValues("allFlightIds");
            LOG.info("Flight id(s) are: " + Arrays.toString(flightIdsAll));

            /* Flight otherFlight = null;
            if (otherFlightId != null) {
                otherFlight = flight.getFlight(Database.getConnection(), Integer.parseInt(otherFlightId));

            }

            if (flight.getFleetId() != fleetId || (otherFlight != null && otherFlight.getFleetId() != fleetId)) {
                LOG.severe("INVALID ACCESS: user did not have access to this flight.");
                Spark.halt(401, "User did not have access to this flight.");
                return null;
            } */

            HashMap<String, Object> scopes = new HashMap<String, Object>();
            Map<String, Object> flights = new HashMap<String, Object>();
            for (String flightIdNew : flightIdsAll) {
                Flight incomingFlight = Flight.getFlight(Database.getConnection(), Integer.parseInt(flightIdNew));
                int flightIdNewInteger = Integer.parseInt(flightIdNew);

                String airframeType = incomingFlight.getAirframeType();

                DoubleTimeSeries altMsl = DoubleTimeSeries.getDoubleTimeSeries(connection, flightIdNewInteger, "AltMSL");
                DoubleTimeSeries latitude = DoubleTimeSeries.getDoubleTimeSeries(connection, flightIdNewInteger, "Latitude");
                DoubleTimeSeries longitude = DoubleTimeSeries.getDoubleTimeSeries(connection, flightIdNewInteger, "Longitude");
                DoubleTimeSeries altAgl = DoubleTimeSeries.getDoubleTimeSeries(connection, flightIdNewInteger, "AltAGL");
                DoubleTimeSeries rpm = DoubleTimeSeries.getDoubleTimeSeries(connection, flightIdNewInteger, "E1 RPM");
                DoubleTimeSeries groundSpeed = DoubleTimeSeries.getDoubleTimeSeries(connection, flightIdNewInteger, "GndSpd");

                StringTimeSeries date = StringTimeSeries.getStringTimeSeries(connection, flightIdNewInteger, "Lcl Date");
                StringTimeSeries time = StringTimeSeries.getStringTimeSeries(connection, flightIdNewInteger, "Lcl Time");

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
                        flightTaxiingTimes.add(date.get(i) + "T" + time.get(i) + "Z");

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

                    if (!Double.isNaN(longitude.get(i)) && !Double.isNaN(latitude.get(i)) && !Double.isNaN(altAgl.get(i)) && (i < dateSize)) {
                        if ((rpm != null && rpm.get(i) >= 2100) && groundSpeed.get(i) > 14.5 && groundSpeed.get(i) <= 80) {

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
                int preClimb = (flightGeoAglTaxiing.size() + flightGeoAglTakeOff.size() + flightGeoAglClimb.size()) -9;
                sizePreClimb = preClimb / 3;

                for (int i = 0; i < altAgl.size(); i++) {
                    if (!Double.isNaN(longitude.get(i)) && !Double.isNaN(latitude.get(i)) && !Double.isNaN(altAgl.get(i)) && (i < dateSize)) {

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
                    if (!Double.isNaN(longitude.get(i)) && !Double.isNaN(latitude.get(i)) && !Double.isNaN(altAgl.get(i)) && (i < dateSize)) {
                        flightGeoInfoAgl.add(longitude.get(i));
                        flightGeoInfoAgl.add(latitude.get(i));
                        flightGeoInfoAgl.add(altAgl.get(i));
                        flightAglTimes.add(date.get(i) + "T" + time.get(i) + "Z");
                    }
                }

                GetNgafidCesium.CesiumResponse cr = new GetNgafidCesium.CesiumResponse(flightGeoAglTaxiing, flightGeoAglTakeOff, flightGeoAglClimb, flightGeoAglCruise, flightGeoInfoAgl, flightTaxiingTimes, flightTakeOffTimes, flightClimbTimes, flightCruiseTimes, flightAglTimes, airframeType);
                flights.put(flightIdNew, cr);
            }

            return gson.toJson(flights);

        } catch (SQLException e) {
            return gson.toJson(new ErrorResponse(e));
        }

    }
}
