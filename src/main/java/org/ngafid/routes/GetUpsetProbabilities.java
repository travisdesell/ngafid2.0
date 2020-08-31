
/**
 * Grabs upset probability data for flights
 * @author <a href=mailto:apl1341@cs.rit.edu>Aidan LaBella</a>
 */
package org.ngafid.routes;

import java.util.logging.Logger;
import com.google.gson.Gson;

import java.sql.SQLException;
import java.sql.Connection;

import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;
import spark.Spark;

import org.ngafid.accounts.User;
import org.ngafid.flights.DoubleTimeSeries;

import org.ngafid.Database;

public class GetUpsetProbabilities implements Route {
    private static final Logger LOG = Logger.getLogger(GetUpsetProbabilities.class.getName());
    private Gson gson;

    /**
     * Constructor
     * @param gson the gson object for JSON conversions
     */
    public GetUpsetProbabilities(Gson gson) {
        this.gson = gson;

        LOG.info("GET " + this.getClass().getName() + " initalized");
    }

    /**
     * {inheritDoc}
     */
    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");
		try{
			Connection connection = Database.getConnection();
			final Session session = request.session();

			int flightId = Integer.parseInt(request.queryParams("flight_id"));
			User user = session.attribute("user");
			int fleetId = user.getFleetId();

			//check to see if the user has upload access for this fleet.
			if (!user.hasViewAccess(fleetId)) {
				LOG.severe("INVALID ACCESS: user did not have view access this fleet.");
				Spark.halt(401, "User did not have access to view acces for this fleet.");
				return null;
			}

			DoubleTimeSeries dts = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "LOCI");

			return gson.toJson(dts);
		}catch (SQLException e) {
			return new ErrorResponse(e);
		}

    }
}
