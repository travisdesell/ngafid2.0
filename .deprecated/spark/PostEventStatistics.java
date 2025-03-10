package org.ngafid.routes.spark;

import com.google.gson.Gson;
import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.events.EventStatistics;
import org.ngafid.routes.ErrorResponse;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Session;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

public class PostEventStatistics implements Route {
    private static final Logger LOG = Logger.getLogger(PostEventStatistics.class.getName());
    private Gson gson;

    public PostEventStatistics(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        LOG.info("handling " + this.getClass().getName());

        final Session session = request.session();
        User user = session.attribute("user");

        int fleetId = user.getFleetId();
        int airframeNameId = Integer.parseInt(request.formParams("airframeNameId"));
        String airframeName = request.formParams("airframeName");

        try (Connection connection = Database.getConnection()) {
            return gson.toJson(new EventStatistics(connection, airframeNameId, airframeName, fleetId));
        } catch (SQLException e) {
            LOG.severe(e.toString());
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
