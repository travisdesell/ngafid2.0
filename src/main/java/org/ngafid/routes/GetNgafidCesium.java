package org.ngafid.routes;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.ArrayList;
import java.util.HashMap;

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

            if (flight.getFleetId() != fleetId) {
                LOG.severe("INVALID ACCESS: user did not have access to view this flight.");
                Spark.halt(401, "User did not have access to view this fleet.");
                return null;
            }

            DoubleTimeSeries altMSL = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "AltMSL");
            DoubleTimeSeries latitude = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "Latitude");
            DoubleTimeSeries longitude = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "Longitude");
            DoubleTimeSeries AltAgl = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "AltAGL");

            ArrayList<String> flightGeoInfoAGL = new ArrayList<>();
            // I am avoiding NaN here!
            for (int i = 0; i < AltAgl.size(); i++) {
                if (!Double.isNaN(longitude.get(i)) && !Double.isNaN(latitude.get(i)) && !Double.isNaN(AltAgl.get(i))) {
                    flightGeoInfoAGL.add(longitude.get(i) + "," + latitude.get(i) + "," + AltAgl.get(i));
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
            scopes.put("flightGeoInfoAGL", flightGeoInfoAGL);

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

