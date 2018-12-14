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

import org.ngafid.Database;
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
        int flightId = Integer.parseInt(request.queryParams("flightId"));
        String name = request.queryParams("seriesName");

        try {
            DoubleSeries doubleSeries = new DoubleSeries(flightId, name);

            //System.out.println(gson.toJson(uploadDetails));
            String output = gson.toJson(doubleSeries);
            //need to convert NaNs to null so they can be parsed by JSON
            output = output.replaceAll("NaN","null");

            //LOG.info(output);

            return output;
        } catch (SQLException e) {
            return gson.toJson(new ErrorResponse(e));
        }
    }
}


