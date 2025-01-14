package org.ngafid.routes;

import com.google.gson.Gson;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;
import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.filters.StoredFilter;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Session;

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
