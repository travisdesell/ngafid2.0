
package org.ngafid.routes;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.sql.Connection;
import java.sql.SQLException;

import com.google.gson.Gson;

import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;
import spark.Spark;

import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.accounts.UserPreferences;
import org.ngafid.flights.DoubleTimeSeries;

/**
 * This class posts the LOCI metrics data for the map flight metric viewer.
 *
 * @author <a href=mailto:apl1341@cs.rit.edu>Aidan LaBella @ RIT CS</a>
 */
public class PostLOCIMetrics implements Route {
    private static final Logger LOG = Logger.getLogger(PostLOCIMetrics.class.getName());
    private Gson gson;

    public PostLOCIMetrics(Gson gson) {
        this.gson = gson;

        LOG.info("post loci metrics route initialized.");
    }

    protected class FlightMetric {
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

    protected class FlightMetricResponse {
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

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling supplementary loci metrics route!");

        final Session session = request.session();
        User user = session.attribute("user");
        int flightId = Integer.parseInt(request.queryParams("flight_id"));
        int timeIndex = Integer.parseInt(request.queryParams("time_index"));

        try {
            Connection connection = Database.getConnection();

            // check to see if the user has access to this data
            if (!user.hasFlightAccess(connection, flightId)) {
                LOG.severe("INVALID ACCESS: user did not have access to this flight.");
                Spark.halt(401, "User did not have access to this flight.");
            }

            LOG.info("getting metrics for flight #" + flightId + " at index " + timeIndex);

            UserPreferences userPreferences = User.getUserPreferences(connection, user.getId());
            List<String> metrics = userPreferences.getFlightMetrics();
            List<FlightMetric> flightMetrics = new ArrayList<>();

            for (String seriesName : metrics) {
                System.out.println(seriesName);
                DoubleTimeSeries series = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, seriesName);

                if (series == null) {
                    flightMetrics.add(new FlightMetric(seriesName));
                } else {
                    FlightMetric flightMetric = new FlightMetric(series.get(timeIndex), seriesName);
                    flightMetrics.add(flightMetric);
                }
            }

            LOG.info("Posting " + flightMetrics.size() + " metrics to user");

            return gson.toJson(new FlightMetricResponse(flightMetrics, userPreferences.getDecimalPrecision()));
        } catch (Exception e) {
            e.printStackTrace();
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
