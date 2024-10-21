package org.ngafid.routes;

import java.io.IOException;
import java.util.ArrayList;
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

public class PostCoordinates implements Route {
    private static final Logger LOG = Logger.getLogger(PostCoordinates.class.getName());
    private Gson gson;

    public PostCoordinates(Gson gson) {
        this.gson = gson;

        LOG.info("post coordinates route initialized.");
    }

    private class Coordinates {
        int nanOffset = -1;
        ArrayList<double[]> coordinates = new ArrayList<double[]>();

        public Coordinates(Connection connection, int flightId, String name) throws SQLException, IOException {
            DoubleTimeSeries latitudes = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "Latitude");
            DoubleTimeSeries longitudes = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "Longitude");

            for (int i = 0; i < latitudes.size(); i++) {
                double longitude = longitudes.get(i);
                double latitude = latitudes.get(i);

                if (Double.isNaN(longitude) || Double.isNaN(latitude) || latitude == 0.0 || longitude == 0.0) {
                } else {
                    if (nanOffset < 0)
                        nanOffset = i;
                    coordinates.add(new double[] { longitude, latitude });
                }
            }
        }
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling coordinates route!");

        final Session session = request.session();
        User user = session.attribute("user");

        int flightId = Integer.parseInt(request.queryParams("flightId"));
        String name = request.queryParams("seriesName");

        try (Connection connection = Database.getConnection()) {
            // check to see if the user has access to this data
            if (!user.hasFlightAccess(connection, flightId)) {
                LOG.severe("INVALID ACCESS: user did not have access to this flight.");
                Spark.halt(401, "User did not have access to this flight.");
            }

            Coordinates coordinates = new Coordinates(connection, flightId, name);

            // System.out.println(gson.toJson(uploadDetails));
            String output = gson.toJson(coordinates);
            // need to convert NaNs to null so they can be parsed by JSON
            output = output.replaceAll("NaN", "null");

            // LOG.info(output);

            return output;
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
