package org.ngafid.routes;

import com.google.gson.Gson;
import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.filters.StoredFilter;

import spark.Request;
import spark.Response;
import spark.Route;
import spark.Session;

import java.sql.*;
import java.util.logging.Logger;

// A route to save a user's query to the saved_queries table in the database
public class PostStoreFilter implements Route {
    private static final Logger LOG = Logger.getLogger(PostStoreFilter.class.getName());
    private Gson gson;

    public PostStoreFilter(Gson gson) {
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
            String filterJSON = request.queryParams("filterJSON");
            String color = request.queryParams("color");

            LOG.info("Storing filter: " + name);
            LOG.info(filterJSON);
            StoredFilter.storeFilter(connection, fleetId, filterJSON, name, color);

            return gson.toJson(StoredFilter.getStoredFilters(connection, fleetId));
        } catch (SQLException e) {
            LOG.severe(e.toString());
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
