package org.ngafid.www.routes;

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.ngafid.core.Database;
import org.ngafid.core.accounts.User;
import org.ngafid.core.flights.DoubleTimeSeries;
import org.ngafid.routes.ErrorResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import static org.ngafid.www.WebServer.gson;

public class DoubleSeriesJavalinRoutes {
    private static final Logger LOG = Logger.getLogger(DoubleSeriesJavalinRoutes.class.getName());

    private static class AllDoubleSeriesNames {
        List<String> names = new ArrayList<String>();

        public AllDoubleSeriesNames(Connection connection) throws SQLException {
            try (PreparedStatement query = connection.prepareStatement("SELECT name FROM double_series_names ORDER BY name")) {
                try (ResultSet resultSet = query.executeQuery()) {
                    while (resultSet.next()) {
                        names.add(resultSet.getString(1));
                    }
                }
            }
        }
    }

    private static class DoubleSeries {
        String[] x;
        double[] y;

        public DoubleSeries(Connection connection, int flightId, String name) throws SQLException, IOException {
            DoubleTimeSeries doubleTimeSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, name);
            LOG.info("POST double series getting double time series for flight id: " + flightId + " and name: '" + name + "'");

            int size = 0;
            if (doubleTimeSeries != null) {
                size = doubleTimeSeries.size();
            }

            x = new String[size];
            y = new double[size];

            for (int i = 0; i < size; i++) {
                x[i] = String.valueOf(i);
                y[i] = doubleTimeSeries.get(i);
            }
        }
    }

    private static class DoubleSeriesNames {
        List<String> names = new ArrayList<String>();

        public DoubleSeriesNames(Connection connection, int flightId) throws SQLException {
            try (PreparedStatement query = connection.prepareStatement("SELECT dsn.name FROM double_series AS ds INNER JOIN double_series_names AS dsn ON ds.name_id = dsn.id WHERE ds.flight_id = ? ORDER BY dsn.name")) {
                query.setInt(1, flightId);

                try (ResultSet resultSet = query.executeQuery()) {
                    while (resultSet.next()) {
                        names.add(resultSet.getString(1));
                    }
                }
            }
        }
    }


    private static void getAllDoubleSeriesNames(Context ctx) throws IOException {
        try (Connection connection = Database.getConnection()) {
            ctx.json(new AllDoubleSeriesNames(connection));
        } catch (SQLException e) {
            e.printStackTrace();
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    private static void postDoubleSeries(Context ctx) {
        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        final int flightId = Integer.parseInt(Objects.requireNonNull(ctx.formParam("flightId")));
        final String name = Objects.requireNonNull(ctx.formParam("seriesName"));

        try (Connection connection = Database.getConnection()) {
            // check to see if the user has access to this data
            if (!user.hasFlightAccess(connection, flightId)) {
                LOG.severe("INVALID ACCESS: user did not have access to this flight.");
                ctx.status(401);
                ctx.result("User did not have access to this flight.");
                return;
            }

            DoubleSeries doubleSeries = new DoubleSeries(connection, flightId, name);

            // System.out.println(gson.toJson(uploadDetails));
            String output = gson.toJson(doubleSeries);
            // need to convert NaNs to null so they can be parsed by JSON
            output = output.replaceAll("NaN", "null");

            ctx.contentType("application/json");
            ctx.result(output);
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    private static void postDoubleSeriesNames(Context ctx) {
        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        final int flightId = Integer.parseInt(Objects.requireNonNull(ctx.formParam("flightId")));

        try (Connection connection = Database.getConnection()) {
            // check to see if the user has access to this data
            if (!user.hasFlightAccess(connection, flightId)) {
                LOG.severe("INVALID ACCESS: user did not have access to this flight.");
                ctx.status(401);
                ctx.result("User did not have access to this flight.");
                return;
            }

            ctx.json(new DoubleSeriesNames(connection, flightId));
        } catch (SQLException e) {
            e.printStackTrace();
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    public static void bindRoutes(Javalin app) {
        app.get("/protected/all_double_series_names", DoubleSeriesJavalinRoutes::getAllDoubleSeriesNames);
        app.post("/protected/double_series", DoubleSeriesJavalinRoutes::postDoubleSeries);
        app.post("/protected/double_series_names", DoubleSeriesJavalinRoutes::postDoubleSeriesNames);

    }
}
