package org.ngafid.routes;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.logging.Logger;

import java.sql.Connection;
import java.sql.SQLException;

import com.google.gson.Gson;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;
import spark.Spark;

import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.accounts.User;
import org.ngafid.flights.Flight;
import org.ngafid.flights.DoubleTimeSeries;

import org.ngafid.filters.Filter;

public class GetNgafidCesium implements Route {
    private static final Logger LOG = Logger.getLogger(GetNgafidCesium.class.getName());
    private Gson gson;

    private class LOCIInfo {
        int flightId;
        DoubleTimeSeries stallIndex, lociIndex;

        public LOCIInfo(Flight flight) {
            this.flightId = flight.getId();
            Connection connection = Database.getConnection();
            try {
                this.stallIndex = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "Stall Index");
                this.lociIndex = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "LOC-I Index");
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public GetNgafidCesium(Gson gson) {
        this.gson = gson;
        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        String flightIdStr = request.queryParams("flight_id");
        LOG.info("getting kml for flight id: " + flightIdStr);
        int flightId = Integer.parseInt(flightIdStr);

        String otherFlightId = request.queryParams("other_flight_id");
        LOG.info("URL flight id is: " + flightId);
        LOG.info("URL other flight id is: " + otherFlightId);

 
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
            Flight flight = Flight.getFlight(connection, flightId);
            Flight otherFlight = null;
            if (otherFlightId != null) {
                otherFlight = Flight.getFlight(Database.getConnection(), Integer.parseInt(otherFlightId));

            }

            if (flight.getFleetId() != fleetId || (otherFlight != null && otherFlight.getFleetId() != fleetId)) {
                LOG.severe("INVALID ACCESS: user did not have access to this flight.");
                Spark.halt(401, "User did not have access to this flight.");
                return null;
            }


            DoubleTimeSeries altMsl = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "AltMSL");
            DoubleTimeSeries latitude = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "Latitude");
            DoubleTimeSeries longitude = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "Longitude");
            DoubleTimeSeries altAgl = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "AltAGL");
            DoubleTimeSeries rpm = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "E1 RPM");
            DoubleTimeSeries groundSpeed = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "GndSpd");

            //New LOCI Gradients
            Map<Flight, LOCIInfo> loci = new HashMap<Flight, LOCIInfo>();
            loci.put(otherFlight, new LOCIInfo(otherFlight));

            ArrayList<String> flightGeoAglTaxiing = new ArrayList<>();
            ArrayList<String> flightGeoAglTakeOff = new ArrayList<>();
            ArrayList<String> flightGeoAglClimb = new ArrayList<>();
            ArrayList<String> flightGeoAglCruise = new ArrayList<>();
            ArrayList<String> flightGeoInfoAgl = new ArrayList<>();

            int initCounter = 0;
            int takeoffCounter = 0;
            int countPostTakeoff = 0;
            int sizePreClimb = 0;
            int countPostCruise = 0;

            // Calculate the taxiing phase
            for (int i = 0; i < altAgl.size(); i++) {

                if (!Double.isNaN(longitude.get(i)) && !Double.isNaN(latitude.get(i)) && !Double.isNaN(altAgl.get(i))) {
                    initCounter++;
                    flightGeoAglTaxiing.add(longitude.get(i) + "," + latitude.get(i) + "," + altAgl.get(i));

                    if (rpm.get(i) >= 2100 && groundSpeed.get(i) > 14.5 && groundSpeed.get(i) < 80) {
                        break;
                    }
                }
            }

            // Calculate the takeoff-init phase
            for (int i = 0; i < altAgl.size(); i++) {

                if (!Double.isNaN(longitude.get(i)) && !Double.isNaN(latitude.get(i)) && !Double.isNaN(altAgl.get(i))) {
                    if (rpm.get(i) >= 2100 && groundSpeed.get(i) > 14.5 && groundSpeed.get(i) < 80) {

                        if (takeoffCounter <= 15) {
                            flightGeoAglTakeOff.add(longitude.get(i) + "," + latitude.get(i) + "," + altAgl.get(i));
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

                if (!Double.isNaN(longitude.get(i)) && !Double.isNaN(latitude.get(i)) && !Double.isNaN(altAgl.get(i))) {
                    if (rpm.get(i) >= 2100 && groundSpeed.get(i) > 14.5 && groundSpeed.get(i) <= 80) {

                        if (countPostTakeoff >= 15) {
                            flightGeoAglClimb.add(longitude.get(i) + "," + latitude.get(i) + "," + altAgl.get(i));
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
            //
            sizePreClimb = (flightGeoAglTaxiing.size() + flightGeoAglTakeOff.size() + flightGeoAglClimb.size()) - 3;

            for (int i = 0; i < altAgl.size(); i++) {
                if (!Double.isNaN(longitude.get(i)) && !Double.isNaN(latitude.get(i)) && !Double.isNaN(altAgl.get(i))) {

                    if (countPostCruise >= sizePreClimb) {
                        flightGeoAglCruise.add(longitude.get(i) + "," + latitude.get(i) + "," + altAgl.get(i));
                    }
                    countPostCruise++;
                }
            }

            // Calculate the full phase
            // I am avoiding NaN here as well!
            for (int i = 0; i < altAgl.size(); i++) {
                if (!Double.isNaN(longitude.get(i)) && !Double.isNaN(latitude.get(i)) && !Double.isNaN(altAgl.get(i))) {
                    flightGeoInfoAgl.add(longitude.get(i) + "," + latitude.get(i) + "," + altAgl.get(i));
                }
            }

            // This is for webpage section
            String resultString = "";
            String templateFile = WebServer.MUSTACHE_TEMPLATE_DIR + "ngafid_cesium.html";
            LOG.severe("template file: '" + templateFile + "'");

            MustacheFactory mf = new DefaultMustacheFactory();
            Mustache mustache = mf.compile(templateFile);

            HashMap<String, Object> scopes = new HashMap<String, Object>();
            // scopes.put("description", "Flight " + flightId);
            scopes.put("flightId", flightId);
            scopes.put("flightGeoInfoAgl", flightGeoInfoAgl);
            scopes.put("flightGeoAglTaxiing", flightGeoAglTaxiing);
            scopes.put("flightGeoAglTakeOff", flightGeoAglTakeOff);
            scopes.put("flightGeoAglClimb", flightGeoAglClimb);
            scopes.put("flightGeoAglCruise", flightGeoAglCruise);

            StringWriter stringOut = new StringWriter();
            mustache.execute(new PrintWriter(stringOut), scopes).flush();
            return stringOut.toString();

        } catch (SQLException e) {
            return gson.toJson(new ErrorResponse(e));
        } catch (IOException e) {
            LOG.severe(e.toString());
        }

        return "";
    }
}
