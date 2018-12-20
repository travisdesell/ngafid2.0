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

public class PostDoubleSeriesNames implements Route {
    private static final Logger LOG = Logger.getLogger(PostDoubleSeriesNames.class.getName());
    private Gson gson;

    public PostDoubleSeriesNames(Gson gson) {
        this.gson = gson;

        System.out.println("post main content route initalized");
        LOG.info("post main content route initialized.");
    }

    private class DoubleSeriesNames {
        ArrayList<String> names = new ArrayList<String>();

        public DoubleSeriesNames(int flightId) throws SQLException {
            Connection connection = Database.getConnection();

            PreparedStatement query = connection.prepareStatement("SELECT name FROM double_series WHERE flight_id = ? ORDER BY name");
            query.setInt(1, flightId);
            ResultSet resultSet = query.executeQuery();

            while (resultSet.next()) {
                names.add(resultSet.getString(1));
            }
        }
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling double series names route!");
        int flightId = Integer.parseInt(request.queryParams("flightId"));

        try {
            DoubleSeriesNames doubleSeriesNames = new DoubleSeriesNames(flightId);

            //System.out.println(gson.toJson(doubleSeriesNames));
            //LOG.info(gson.toJson(doubleSeriesNames));

            return gson.toJson(doubleSeriesNames);
        } catch (SQLException e) {
            return gson.toJson(new ErrorResponse(e));
        }
    }
}

