package org.ngafid.routes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.ngafid.accounts.UserPreferences;
import org.ngafid.flights.DoubleTimeSeries;

import static org.ngafid.flights.LossOfControlParameters.*;

public class PostUserPreferences implements Route {
    private static final Logger LOG = Logger.getLogger(PostUserPreferences.class.getName());
    private Gson gson;
    private static Connection connection = Database.getConnection();

    public PostUserPreferences(Gson gson) {
        this.gson = gson;

        LOG.info("post loci metrics route initialized.");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling supplementary loci metrics route!");

        final Session session = request.session();
        User user = session.attribute("user");

        String metrics = request.queryParams("flight_metrics");
        int decimalPrecision = Integer.parseInt(request.queryParams("decimal_precision"));

        try {
            UserPreferences currentPreferences = User.getUserPreferences(connection, user.getId());
            currentPreferences.update(decimalPrecision, metrics);

            return gson.toJson(currentPreferences);
        } catch (SQLException e) {
            e.printStackTrace();
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
