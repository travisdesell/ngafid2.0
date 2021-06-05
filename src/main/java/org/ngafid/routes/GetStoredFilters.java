package org.ngafid.routes;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import java.sql.Connection;
import java.sql.SQLException;

import com.google.gson.Gson;

import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;


import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.filters.Filter;
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

        try {
            final Session session = request.session();
            User user = session.attribute("user");
            Connection connection = Database.getConnection();

            List<StoredFilter> filters = StoredFilter.getStoredFilters(connection, user);
            System.out.println(filters.toString());

            return gson.toJson(filters);
        } catch (SQLException e) {
            LOG.severe(e.toString());
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
