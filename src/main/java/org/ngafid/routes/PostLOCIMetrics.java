
package org.ngafid.routes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;
import spark.Spark;

import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.accounts.UserPreferences;
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

    protected class FlightMetric {
        String value;
        String name;

        public FlightMetric(double value, String name) {
            //json does not like NaN so we must make it a null string
            this.value = Double.isNaN(value) ? "null" : String.valueOf(value);
            this.name = name;
        }

        @Override
        public String toString() {
            return name + ": " + value;
        }
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling supplementary loci metrics route!");

        final Session session = request.session();
        User user = session.attribute("user");
        String rawMetrics = request.queryParams("flight_metrics");
        int flightId = Integer.parseInt(request.queryParams("flight_id"));
        int timeIndex = Integer.parseInt(request.queryParams("time_index"));

        ObjectMapper om = new ObjectMapper();

        try {
            //check to see if the user has access to this data
            if (!user.hasFlightAccess(Database.getConnection(), flightId)) {
                LOG.severe("INVALID ACCESS: user did not have access to this flight.");
                Spark.halt(401, "User did not have access to this flight.");
            }

            LOG.info("getting metrics for flight #" + flightId + " at index " + timeIndex);

            List<String> metrics = om.readValue(rawMetrics, List.class);
            List<FlightMetric> flightMetrics = new ArrayList<>();

            for (String seriesName : metrics) {
                System.out.println(seriesName);
                DoubleTimeSeries series = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, seriesName); 

                FlightMetric flightMetric = new FlightMetric(series.get(timeIndex), seriesName);  
                flightMetrics.add(flightMetric);
            }

            //System.out.println(gson.toJson(uploadDetails));
            //need to convert NaNs to null so they can be parsed by JSON

            //LOG.info(output);
            for (FlightMetric fm : flightMetrics) {
                System.out.println(fm.toString());
            }

            return gson.toJson(flightMetrics);
        } catch (Exception e) {
            e.printStackTrace();
            return gson.toJson(new ErrorResponse(e));
        }
    }
}

