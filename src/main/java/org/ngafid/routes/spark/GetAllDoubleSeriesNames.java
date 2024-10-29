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

import org.ngafid.Database;
import org.ngafid.accounts.User;

/**
 * This class provides all the names of {@link DoubleTimeSeries} in the NGAFID
 *
 * @author <a href=mailto:apl1341@cs.rit.edu>Aidan LaBella @ RIT CS</a>
 */
public class GetAllDoubleSeriesNames implements Route {
    private static final Logger LOG = Logger.getLogger(GetAllDoubleSeriesNames.class.getName());
    private Gson gson;

    public GetAllDoubleSeriesNames(Gson gson) {
        this.gson = gson;

        System.out.println("post main content route initalized");
        LOG.info("post main content route initialized.");
    }

    private class AllDoubleSeriesNames {
        ArrayList<String> names = new ArrayList<String>();

        public AllDoubleSeriesNames(Connection connection) throws SQLException {
            PreparedStatement query = connection.prepareStatement("SELECT name FROM double_series_names ORDER BY name");
            LOG.info(query.toString());
            ResultSet resultSet = query.executeQuery();

            while (resultSet.next()) {
                names.add(resultSet.getString(1));
            }
        }
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling post all double series names route!");

        final Session session = request.session();
        User user = session.attribute("user");

        try (Connection connection = Database.getConnection()) {
            // check to see if the user has access to this data
            // if (!user.hasViewAccess(user.getFleetId())) {
            // LOG.severe("INVALID ACCESS: user did not have access to this fleet.");
            // Spark.halt(401, "User did not have access to this fleet.");
            // }

            AllDoubleSeriesNames doubleSeriesNames = new AllDoubleSeriesNames(connection);

            // System.out.println(gson.toJson(doubleSeriesNames));
            // LOG.info(gson.toJson(doubleSeriesNames));

            return gson.toJson(doubleSeriesNames);
        } catch (SQLException e) {
            e.printStackTrace();
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
