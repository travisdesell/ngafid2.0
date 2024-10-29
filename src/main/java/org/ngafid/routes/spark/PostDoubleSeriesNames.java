package org.ngafid.routes.spark;

import java.util.ArrayList;
import java.util.logging.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.google.gson.Gson;

import org.ngafid.routes.ErrorResponse;
import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;
import spark.Spark;

import org.ngafid.Database;
import org.ngafid.accounts.User;

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

        public DoubleSeriesNames(Connection connection, int flightId) throws SQLException {
            PreparedStatement query = connection.prepareStatement(
                    "SELECT dsn.name FROM double_series AS ds INNER JOIN double_series_names AS dsn ON ds.name_id = dsn.id WHERE ds.flight_id = ? ORDER BY dsn.name");
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

        final Session session = request.session();
        User user = session.attribute("user");

        int flightId = Integer.parseInt(request.queryParams("flightId"));

        try (Connection connection = Database.getConnection()) {
            // check to see if the user has access to this data
            if (!user.hasFlightAccess(connection, flightId)) {
                LOG.severe("INVALID ACCESS: user did not have access to this flight.");
                Spark.halt(401, "User did not have access to this flight.");
            }

            DoubleSeriesNames doubleSeriesNames = new DoubleSeriesNames(connection, flightId);

            // System.out.println(gson.toJson(doubleSeriesNames));
            // LOG.info(gson.toJson(doubleSeriesNames));

            return gson.toJson(doubleSeriesNames);
        } catch (SQLException e) {
            e.printStackTrace();
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
