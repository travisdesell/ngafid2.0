package org.ngafid.routes;

import com.google.gson.Gson;
import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.events.EventDefinition;
import org.ngafid.events.EventStatistics;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Session;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
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
        int airframeNameId = Integer.parseInt(request.queryParams("airframeNameId"));
        String airframeName = request.queryParams("airframeName");

        try {
            Connection connection = Database.getConnection();
            return gson.toJson(new EventStatistics(connection, airframeNameId, airframeName, fleetId));
        } catch (SQLException e) {
            LOG.severe(e.toString());
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
