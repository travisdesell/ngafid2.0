/**
 * Generates flies for xplane animations
 * @author <a href=mailto:apl1341@cs.rit.edu>Aidan LaBella</a>
 */
package org.ngafid.routes;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.ArrayList;
import java.util.List;
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
import org.ngafid.flights.Itinerary;
import org.ngafid.flights.DoubleTimeSeries;

import org.ngafid.filters.Filter;

//Parameters that have to do with fdr file format
import static org.ngafid.routes.XPlaneParameters.*;

public class GetXPlane implements Route {
    private static final Logger LOG = Logger.getLogger(GetXPlane.class.getName());
    private Gson gson;

    /**
     * Constructor
     * @param gson the gson object for JSON conversions
     */
    public GetXPlane(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    /**
     * Returns a string full of 0's, follwed by a comma, to poplulate the extra as null in the FDR format.
     * If we start tracking other data, we can change this method to include such data
     * @param nZeros the number of 0's
     * @return a string in the format 0(0),0(1),...0(n),
     */
    private String getZeros(int nZeros){
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i<nZeros; i++){
            sb.append(NULL_DATA);
        }
        return sb.toString();
    }

    /**
     * {inheritDoc}
     */
    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        String flightIdStr = request.queryParams("flight_id");

        LOG.info("getting kml for flight id: " + flightIdStr);

        int flightId = Integer.parseInt(flightIdStr);

        final Session session = request.session();
        User user = session.attribute("user");
        int fleetId = user.getFleetId();


        //check to see if the user has upload access for this fleet.
        if (!user.hasViewAccess(fleetId)) {
            LOG.severe("INVALID ACCESS: user did not have view access this fleet.");
            Spark.halt(401, "User did not have access to view acces for this fleet.");
            return null;
        }


        try {
            response.header("Content-Disposition", "attachment; filename=flight_" + flightId + FDR_FILE_EXTENSION);
            response.type("application/force-download");

            Connection connection = Database.getConnection();

            Flight flight = Flight.getFlight(connection, flightId);
            String airframe = flight.getAirframeType();

            if (flight.getFleetId() != fleetId) {
                LOG.severe("INVALID ACCESS: user did not have access to view this flight.");
                Spark.halt(401, "User did not have access to view this fleet.");
                return null;
            }

            //flight info here

            HashMap<String, Object> scopes = new HashMap<String, Object>();

            List<String> allICAO = Itinerary.getAllAirports(connection, fleetId);
            List<Itinerary> allWaypoints = Itinerary.getItinerary(connection, fleetId);

            scopes.put(ACFT, ACFT.toUpperCase()+","+xplaneNames.get(airframe)+",");
            scopes.put(TAIL, TAIL.toUpperCase()+","+flight.getTailNumber()+",");


            DoubleTimeSeries altMSL = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "AltMSL");
            DoubleTimeSeries latitude = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "Latitude");
            DoubleTimeSeries longitude = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "Longitude");

            //LOG.info(gson.toJson(flights));

            StringBuffer sb = new StringBuffer();

            for (int i = 0; i < altMSL.size(); i++) {
                sb.append("DATA, " + i + "," + NULL_DATA + longitude.get(i) + "," + latitude.get(i) +
                    "," + altMSL.get(i) + "," + getZeros(NUM_NULL_PARAMS) + "\n");
            }

            String templateFile = WebServer.MUSTACHE_TEMPLATE_DIR + "template.fdr";
            LOG.info("template file: '" + templateFile + "'");

            MustacheFactory mf = new DefaultMustacheFactory();
            Mustache mustache = mf.compile(templateFile);


            scopes.put(DATA, sb.toString());
            scopes.put(COMM, COMM.toUpperCase()+",Flight " + flightId+",");

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
