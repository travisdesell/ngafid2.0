
package org.ngafid.routes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.google.gson.Gson;

import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;
import spark.Spark;

import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.flights.DoubleTimeSeries;

import static org.ngafid.flights.LossOfControlParameters.*;

public class PostLOCIMetrics implements Route {
    private static final Logger LOG = Logger.getLogger(PostLOCIMetrics.class.getName());
    private Gson gson;
	private static Connection connection = Database.getConnection();

    public PostLOCIMetrics(Gson gson) {
        this.gson = gson;

        LOG.info("post loci metrics route initialized.");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling supplementary loci metrics route!");

        final Session session = request.session();
        User user = session.attribute("user");
        int flightId = Integer.parseInt(request.queryParams("flight_id"));
		int timeIndex = Integer.parseInt(request.queryParams("time_index"));

        try {
            //check to see if the user has access to this data
            if (!user.hasFlightAccess(Database.getConnection(), flightId)) {
                LOG.severe("INVALID ACCESS: user did not have access to this flight.");
                Spark.halt(401, "User did not have access to this flight.");
            }

			LOG.info("getting metrics for flight #" + flightId + " at index " + timeIndex);

			Map<String, Double> metrics = new HashMap<>();

			for (String seriesName : uiMetrics) {
				System.out.println(seriesName);
				DoubleTimeSeries series = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, seriesName); 

				metrics.put(seriesName, series.get(timeIndex));
			}

            //System.out.println(gson.toJson(uploadDetails));
            //need to convert NaNs to null so they can be parsed by JSON

            //LOG.info(output);

            return gson.toJson(metrics);
        } catch (SQLException e) {
            e.printStackTrace();
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
