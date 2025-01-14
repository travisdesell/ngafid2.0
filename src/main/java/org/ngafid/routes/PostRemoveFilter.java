package org.ngafid.routes;

import com.google.gson.Gson;
import java.sql.*;
import java.util.logging.Logger;
import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.filters.StoredFilter;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Session;

// A route to save a user's query to the saved_queries table in the database
public class PostRemoveFilter implements Route {
    private static final Logger LOG = Logger.getLogger(PostRemoveFilter.class.getName());
    private Gson gson;

    public PostRemoveFilter(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        try {
            final Session session = request.session();
            Connection connection = Database.getConnection();

            User user = session.attribute("user");
            int fleetId = user.getFleetId();

            String name = request.queryParams("name");

            LOG.info("Removing filter: " + name);
            StoredFilter.removeFilter(connection, fleetId, name);

            return gson.toJson(StoredFilter.getStoredFilters(connection, fleetId));
        } catch (SQLException e) {
            LOG.severe(e.toString());
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
