package org.ngafid.routes;

import com.google.gson.Gson;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;
import org.ngafid.Database;
import org.ngafid.accounts.FleetAccess;
import org.ngafid.accounts.User;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Session;
import spark.Spark;

public class PostUpdateUserAccess implements Route {
    private static final Logger LOG = Logger.getLogger(PostUpdateUserAccess.class.getName());
    private Gson gson;

    public PostUpdateUserAccess(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    private class UpdateUserAccess {
        String message = "Success.";
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName());

        final Session session = request.session();
        User user = session.attribute("user");

        int fleetUserId = Integer.parseInt(request.queryParams("fleetUserId"));
        int fleetId = Integer.parseInt(request.queryParams("fleetId"));
        String accessType = request.queryParams("accessType");

        //check to see if the logged in user can update access to this fleet
        if (!user.managesFleet(fleetId)) {
            LOG.severe("INVALID ACCESS: user did not have access to modify user access rights on this fleet.");
            Spark.halt(401, "User did not have access to modify user access rights on this fleet.");
            return null;
        } else {
            try {
                Connection connection = Database.getConnection();
                FleetAccess.update(connection, fleetUserId, fleetId, accessType);
                user.updateFleet(connection);
                return gson.toJson(new UpdateUserAccess());
            } catch (SQLException e) {
                e.printStackTrace();
                return gson.toJson(new ErrorResponse(e));
            }
        }
    }
}
