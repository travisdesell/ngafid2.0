package org.ngafid.routes.spark;

import com.google.gson.Gson;
import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.filters.StoredFilter;

import org.ngafid.routes.ErrorResponse;
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

        try (Connection connection = Database.getConnection()) {
            final Session session = request.session();

            User user = session.attribute("user");
            int fleetId = user.getFleetId();

            String currentName = request.formParams("currentName");
            String newName = request.formParams("newName");
            String filterJSON = request.formParams("filterJSON");
            String color = request.formParams("color");

            LOG.info("Modifying filter: " + currentName + " to: " + newName);
            LOG.info(filterJSON);

            StoredFilter.modifyFilter(connection, fleetId, filterJSON, currentName, newName, color);
            LOG.info("New filter: name: " + newName + "; filterJSON: " + filterJSON + ";");
            return gson.toJson("SUCCESS");
        } catch (SQLIntegrityConstraintViolationException se) {
            LOG.info("DUPLICATE_PK detected: " + se.toString());
            return gson.toJson("DUPLICATE_PK");
        } catch (SQLException e) {
            LOG.severe(e.toString());
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
