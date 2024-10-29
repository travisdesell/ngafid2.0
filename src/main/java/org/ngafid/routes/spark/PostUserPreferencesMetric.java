package org.ngafid.routes.spark;

import java.util.logging.Logger;
import java.sql.Connection;

import com.google.gson.Gson;

import org.ngafid.routes.ErrorResponse;
import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;

import org.ngafid.Database;
import org.ngafid.accounts.User;

public class PostUserPreferencesMetric implements Route {
    private static final Logger LOG = Logger.getLogger(PostUserPreferencesMetric.class.getName());
    private Gson gson;

    public PostUserPreferencesMetric(Gson gson) {
        this.gson = gson;

        LOG.info("post loci metrics route initialized.");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling post user prefs route!");

        final Session session = request.session();
        User user = session.attribute("user");
        int userId = user.getId();

        String metric = request.queryParams("metricName");
        String type = request.queryParams("modificationType");

        try (Connection connection = Database.getConnection()) {
            LOG.info("Modifiying " + metric + " (" + type + ") for user: " + user.toString());

            if (type.equals("addition")) {
                User.addUserPreferenceMetric(connection, userId, metric);
            } else {
                User.removeUserPreferenceMetric(connection, userId, metric);
            }

            return gson.toJson(User.getUserPreferences(connection, userId).getFlightMetrics());
        } catch (Exception e) {
            e.printStackTrace();
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
