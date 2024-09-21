
package org.ngafid.routes;

import java.util.logging.Logger;
import com.google.gson.Gson;

import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;

import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.accounts.UserPreferences;

public class GetUserPreferences implements Route {
    private static final Logger LOG = Logger.getLogger(GetUserPreferences.class.getName());
    private Gson gson;

    public GetUserPreferences(Gson gson) {
        this.gson = gson;

        LOG.info("get " + this.getClass().getName() + " initalized");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        final Session session = request.session();
        User user = session.attribute("user");

        try {
            UserPreferences userPreferences = User.getUserPreferences(Database.getConnection(), user.getId());

            return gson.toJson(userPreferences);
        } catch (Exception se) {
            se.printStackTrace();
            return gson.toJson(new ErrorResponse(se));
        }
    }
}
