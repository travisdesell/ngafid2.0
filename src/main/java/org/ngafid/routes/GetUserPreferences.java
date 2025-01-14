
package org.ngafid.routes;



import com.google.gson.Gson;
import java.sql.Connection;
import java.util.logging.Logger;
import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.accounts.UserPreferences;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Session;

public class GetUserPreferences implements Route {
    private static final Logger LOG = Logger.getLogger(GetUserPreferences.class.getName());
    private static Connection connection = Database.getConnection();
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
            UserPreferences userPreferences = User.getUserPreferences(connection, user.getId());

            return gson.toJson(userPreferences);
        } catch (Exception se) {
            se.printStackTrace();
            return gson.toJson(new ErrorResponse(se));
        }
    }
}
