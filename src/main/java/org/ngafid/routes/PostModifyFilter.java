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
public class PostModifyFilter implements Route {
    private static final Logger LOG = Logger.getLogger(PostModifyFilter.class.getName());
    private Gson gson;

    public PostModifyFilter(Gson gson) {
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

            String currentName = request.queryParams("currentName");
            String newName = request.queryParams("newName");
            String filterJSON = request.queryParams("filterJSON");
            String color = request.queryParams("color");

            LOG.info("Modifying filter: " + currentName);
            LOG.info(filterJSON);
            StoredFilter.modifyFilter(connection, fleetId, filterJSON, currentName, newName, color);
            LOG.info("New filter: name: " + newName + "; filterJSON: " + filterJSON + ";");

            return gson.toJson(StoredFilter.getStoredFilters(connection, fleetId));
        } catch (SQLException e) {
            LOG.severe(e.toString());
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
