package org.ngafid.www.routes;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import org.ngafid.core.Database;
import org.ngafid.core.accounts.User;
import org.ngafid.core.flights.DoubleTimeSeries;
import org.ngafid.www.ErrorResponse;

public class DoubleSeriesJavalinRoutes {
    public static final Logger LOG = Logger.getLogger(DoubleSeriesJavalinRoutes.class.getName());

    private DoubleSeriesJavalinRoutes() {
        // Utility class - prevent instantiation
    }

    public static class AllDoubleSeriesNames {
        @JsonProperty
        private final List<String> names = new ArrayList<String>();

        /**
         * Loads the catalog of all known double-series names (every row of {@code double_series_names}, ordered by
         * name) into this response.
         *
         * @param connection the database connection used to load the names
         * @throws SQLException if the query fails
         */
        public AllDoubleSeriesNames(Connection connection) throws SQLException {
            try (PreparedStatement query =
                    connection.prepareStatement("SELECT name FROM double_series_names ORDER BY name")) {
                try (ResultSet resultSet = query.executeQuery()) {
                    while (resultSet.next()) {
                        names.add(resultSet.getString(1));
                    }
                }
            }
        }

        public List<String> getNames() {
            return names;
        }
    }

    public static class DoubleSeries {
        @JsonProperty
        private final String[] x;

        @JsonProperty
        private final double[] y;

        /**
         * Loads one named double time series for a flight into parallel plot arrays: {@code y} holds the series
         * values and {@code x} holds their sample indices as strings. If the series is absent the arrays are empty.
         *
         * @param connection the database connection used to load the series
         * @param flightId the flight to load the series for
         * @param name the name of the double series to load
         * @throws SQLException if the query fails
         * @throws IOException if reading the series data fails
         */
        public DoubleSeries(Connection connection, int flightId, String name) throws SQLException, IOException {
            DoubleTimeSeries doubleTimeSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, name);
            LOG.info("POST double series getting double time series for flight id: " + flightId + " and name: '" + name
                    + "'");

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

        public String[] getX() {
            return x;
        }

        public double[] getY() {
            return y;
        }
    }

    public static class DoubleSeriesNames {
        @JsonProperty
        private final List<String> names = new ArrayList<String>();

        /**
         * Loads the names of the double series that are actually present for a specific flight (ordered by name)
         * into this response.
         *
         * @param connection the database connection used to load the names
         * @param flightId the flight whose available series names are loaded
         * @throws SQLException if the query fails
         */
        public DoubleSeriesNames(Connection connection, int flightId) throws SQLException {
            try (PreparedStatement query = connection.prepareStatement("SELECT dsn.name FROM double_series AS ds "
                    + "INNER JOIN double_series_names AS dsn ON ds.name_id = dsn.id "
                    + "WHERE ds.flight_id = ? ORDER BY dsn.name")) {
                query.setInt(1, flightId);

                try (ResultSet resultSet = query.executeQuery()) {
                    while (resultSet.next()) {
                        names.add(resultSet.getString(1));
                    }
                }
            }
        }

        public List<String> getNames() {
            return names;
        }
    }

    /**
     * Returns, as JSON, the catalog of all known double-series names. Responds 500 on a database error.
     *
     * @param ctx the Javalin request context, whose response is written as JSON
     */
    public static void getAllDoubleSeriesNames(Context ctx) {
        try (Connection connection = Database.getConnection()) {
            ctx.json(new AllDoubleSeriesNames(connection));
        } catch (SQLException e) {
            e.printStackTrace();
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    /**
     * Returns, as JSON, a single named double time series for a flight (identified by the {@code fid} and
     * {@code series} path parameters), after verifying the user has access to the flight. Responds 401 if access is
     * denied and 500 on a database or I/O error.
     *
     * @param ctx the Javalin request context supplying the session user and the {@code fid}/{@code series} path
     *     parameters
     */
    public static void postDoubleSeries(Context ctx) {
        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        final int flightId = Integer.parseInt(Objects.requireNonNull(ctx.pathParam("fid")));
        final String name = Objects.requireNonNull(ctx.pathParam("series"));

        try (Connection connection = Database.getConnection()) {
            // check to see if the user has access to this data
            if (!user.hasFlightAccess(connection, flightId)) {
                LOG.severe("INVALID ACCESS: user did not have access to this flight.");
                ctx.status(401);
                ctx.result("User did not have access to this flight.");
                return;
            }

            LOG.info("Fetching series with name: " + name);
            DoubleSeries doubleSeries = new DoubleSeries(connection, flightId, name);
            ctx.json(doubleSeries);
        } catch (SQLException | IOException e) {
            e.printStackTrace();
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    /**
     * Returns, as JSON, the names of the double series available for a flight (identified by the {@code fid} path
     * parameter), after verifying the user has access to the flight. Responds 401 if access is denied and 500 on a
     * database error.
     *
     * @param ctx the Javalin request context supplying the session user and the {@code fid} path parameter
     */
    public static void postDoubleSeriesNames(Context ctx) {
        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        final int flightId = Integer.parseInt(Objects.requireNonNull(ctx.pathParam("fid")));

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

    /**
     * Intended to register this class's double-series routes on the given Javalin app. Currently a no-op: the route
     * registrations are commented out, so no double-series routes are active.
     *
     * @param app the Javalin application the routes would be registered on
     */
    public static void bindRoutes(Javalin app) {
        // app.get("/protected/all_double_series_names", DoubleSeriesJavalinRoutes::getAllDoubleSeriesNames);
        // app.post("/protected/double_series", DoubleSeriesJavalinRoutes::postDoubleSeries);
        // app.post("/protected/double_series_names", DoubleSeriesJavalinRoutes::postDoubleSeriesNames);

    }
}
