package org.ngafid.routes;

import com.google.gson.Gson;
import com.mysql.jdbc.Statement;
import org.ngafid.Database;
import org.ngafid.accounts.Fleet;
import org.ngafid.accounts.User;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Session;

import java.sql.*;
import java.util.ArrayList;
import java.util.logging.Logger;

// A route to save a user's query to the saved_queries table in the database
public class PostSaveQuery implements Route {
    private static final Logger LOG = Logger.getLogger(GetFlights.class.getName());
    private Gson gson;

    public PostSaveQuery(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        try {
            final Session session = request.session();
            User user = session.attribute("user");
            String queryName = request.queryParams("queryName");
            String queryText = request.queryParams("queryText");
            String query = request.queryParams("query");
            Integer fleetID = Integer.parseInt(request.queryParams("fleetID"));

            Connection connection = Database.getConnection();

            // check if name is taken
            PreparedStatement check = connection.prepareStatement("SELECT COUNT(1) FROM saved_queries WHERE fleet_id = ? AND query_name = ?");
            check.setInt(1, fleetID);
            check.setString(2, queryName);
            ResultSet results = check.executeQuery();
            results.next();

            // unique to what... user, fleet, everything?
            // just fleet
            // need to get fleet_id / group id from ajax and not user session

            // if query name unique
            if (results.getInt(1) < 1) {
                // prepare query string

                PreparedStatement insert = connection.prepareStatement("INSERT INTO saved_queries SET user_id = ?, fleet_id = ?, query = ?, query_name = ?, query_text = ?");
                insert.setInt(1, user.getId());
                insert.setInt(2, fleetID);
                insert.setString(3, query);
                insert.setString(4, queryName);
                insert.setString(5, queryText);

                insert.executeUpdate();
                return gson.toJson("success");

            } else {
                //raise error
                return gson.toJson("'" + queryName + "' is already taken!");
            }

        } catch (SQLException e) {
            LOG.severe(e.toString());
            return gson.toJson(new ErrorResponse(e));
        }
    }
}