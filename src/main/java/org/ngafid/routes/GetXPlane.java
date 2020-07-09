/**
 * Generates flies for xplane animations
 * @author <a href=mailto:apl1341@cs.rit.edu>Aidan LaBella</a>
 */
package org.ngafid.routes;

import java.util.logging.Logger;
import com.google.gson.Gson;

import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;
import spark.Spark;

import org.ngafid.accounts.User;
import org.ngafid.flights.XPlaneExport;
import org.ngafid.flights.XPlane10Export;
import org.ngafid.flights.XPlane11Export;

//Parameters that have to do with fdr file format
import static org.ngafid.flights.XPlaneParameters.*;

public class GetXPlane implements Route {
    private static final Logger LOG = Logger.getLogger(GetXPlane.class.getName());
    private Gson gson;

    /**
     * Constructor
     * @param gson the gson object for JSON conversions
     */
    public GetXPlane(Gson gson) {
        this.gson = gson;

        LOG.info("GET " + this.getClass().getName() + " initalized");
    }

    /**
     * {inheritDoc}
     */
    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        String flightIdStr = request.queryParams("flight_id");
        int version = Integer.parseInt(request.queryParams("version"));


		LOG.info("Generating an X-Plane "+version+" FDR file for flight #"+flightIdStr);
		//TODO: differentiate between xp10 and 11 here

        int flightId = Integer.parseInt(flightIdStr);

        final Session session = request.session();
        User user = session.attribute("user");
        int fleetId = user.getFleetId();

		XPlaneExport export;


        //check to see if the user has upload access for this fleet.
        if (!user.hasViewAccess(fleetId)) {
            LOG.severe("INVALID ACCESS: user did not have view access this fleet.");
            Spark.halt(401, "User did not have access to view acces for this fleet.");
            return null;
        }


        response.header("Content-Disposition", "attachment; filename=flight_" + flightId + FDR_FILE_EXTENSION);
		response.type("application/force-download");


		export = (version == 10) ? new XPlane10Export(flightId) : new XPlane11Export(flightId);

        return export.toFdrFile();
    }
}
