package org.ngafid.routes.spark;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

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

public class GetKML implements Route {
    private static final Logger LOG = Logger.getLogger(GetKML.class.getName());
    private Gson gson;

    public GetKML(Gson gson) {
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

        try (Connection connection = Database.getConnection()) {
            response.header("Content-Disposition", "attachment; filename=flight_" + flightId + ".kml");
            response.type("application/force-download");

            Flight flight = Flight.getFlight(connection, flightId);

            if (flight.getFleetId() != fleetId) {
                LOG.severe("INVALID ACCESS: user did not have access to view this flight.");
                Spark.halt(401, "User did not have access to view this fleet.");
                return null;
            }

            DoubleTimeSeries altMSL = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "AltMSL");
            DoubleTimeSeries latitude = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "Latitude");
            DoubleTimeSeries longitude = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "Longitude");

            // LOG.info(gson.toJson(flights));

            StringBuffer sb = new StringBuffer();

            for (int i = 0; i < altMSL.size(); i++) {
                sb.append(longitude.get(i) + "," + latitude.get(i) + "," + altMSL.get(i) + "\n");
            }

            String templateFile = WebServer.MUSTACHE_TEMPLATE_DIR + "template.kml";
            LOG.info("template file: '" + templateFile + "'");

            MustacheFactory mf = new DefaultMustacheFactory();
            Mustache mustache = mf.compile(templateFile);

            HashMap<String, Object> scopes = new HashMap<String, Object>();
            scopes.put("coords", sb.toString());
            scopes.put("description", "Flight " + flightId);

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
