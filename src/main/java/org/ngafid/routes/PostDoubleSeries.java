package org.ngafid.routes;

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

public class PostDoubleSeries implements Route {
    private static final Logger LOG = Logger.getLogger(PostDoubleSeries.class.getName());
    private Gson gson;

    public PostDoubleSeries(Gson gson) {
        this.gson = gson;

        System.out.println("post main content route initalized");
        LOG.info("post main content route initialized.");
    }

    private class DoubleSeries {
        int[] x;
        double[] y;

        public DoubleSeries(int flightId, String name) throws SQLException {
            Connection connection = Database.getConnection();
            DoubleTimeSeries doubleTimeSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, name);

            x = new int[doubleTimeSeries.size()];
            y = new double[doubleTimeSeries.size()];
            for (int i = 0; i < doubleTimeSeries.size(); i++) {
                x[i] = i;
                y[i] = doubleTimeSeries.get(i);
            }
       }
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling double series route!");

        final Session session = request.session();
        User user = session.attribute("user");

        int flightId = Integer.parseInt(request.queryParams("flightId"));
        String name = request.queryParams("seriesName");

        try {
            //check to see if the user has access to this data
            if (!user.hasFlightAccess(Database.getConnection(), flightId)) {
                LOG.severe("INVALID ACCESS: user did not have access to this flight.");
                Spark.halt(401, "User did not have access to this flight.");
            }

            DoubleSeries doubleSeries = new DoubleSeries(flightId, name);

            //System.out.println(gson.toJson(uploadDetails));
            String output = gson.toJson(doubleSeries);
            //need to convert NaNs to null so they can be parsed by JSON
            output = output.replaceAll("NaN","null");

            //LOG.info(output);

            return output;
        } catch (SQLException e) {
            e.printStackTrace();
            return gson.toJson(new ErrorResponse(e));
        }
    }
}


