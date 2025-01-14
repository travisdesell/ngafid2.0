package org.ngafid.routes;

import static org.ngafid.flights.calculations.Parameters.*;

import com.google.gson.Gson;
import java.sql.Connection;
import java.util.logging.Logger;
import org.ngafid.Database;
import org.ngafid.accounts.User;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Session;

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
        LOG.info("handling post user prefs route!");

        final Session session = request.session();
        User user = session.attribute("user");

        int decimalPrecision = Integer.parseInt(request.queryParams("decimal_precision"));
        try {
            return gson.toJson(User.updateUserPreferencesPrecision(connection, user.getId(), decimalPrecision));
        } catch (Exception e) {
            e.printStackTrace();
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
