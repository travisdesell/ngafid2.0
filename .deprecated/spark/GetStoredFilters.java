package org.ngafid.routes.spark;

import java.util.List;
import java.util.logging.Logger;

import java.sql.Connection;
import java.sql.SQLException;

import com.google.gson.Gson;

import org.ngafid.routes.ErrorResponse;
import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;

import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.filters.StoredFilter;

// A route to fetch all saved queries for a given group (site, user, or fleet specific)
public class GetStoredFilters implements Route {
    private static final Logger LOG = Logger.getLogger(GetStoredFilters.class.getName());
    private Gson gson;

    public GetStoredFilters(Gson gson) {
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

            List<StoredFilter> filters = StoredFilter.getStoredFilters(connection, fleetId);
            LOG.info("Pushing " + filters.size() + " filters to the user");

            return gson.toJson(filters);
        } catch (SQLException e) {
            LOG.severe(e.toString());
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
