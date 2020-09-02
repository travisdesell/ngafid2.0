package org.ngafid.routes;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

// A route to fetch all saved queries for a given group (site, user, or fleet specific)
public class GetQueries implements Route {
    private static final Logger LOG = Logger.getLogger(GetFlights.class.getName());
    private Gson gson;

    public GetQueries(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        try {
            final Session session = request.session();
            User user = session.attribute("user");
            int fleetID = Integer.parseInt(request.queryParams("fleetID"));                    // get fleet ID from submissionData
            Connection connection = Database.getConnection();
            // prepare query
            // check if querying by userID
            PreparedStatement query;
            if (fleetID == -1) {
                query = connection.prepareStatement("SELECT * FROM saved_queries WHERE fleet_id = ? AND user_id = ?");
                query.setInt(1, fleetID);
                query.setInt(2, user.getId());
            } else {
                query = connection.prepareStatement("SELECT * FROM saved_queries WHERE fleet_id = ?");
                query.setInt(1, fleetID);
            }
            ResultSet results = query.executeQuery();

            // process results
            String resultsString = "";

            while (results.next()) {
                if (resultsString != ""){
                    resultsString += "--";
                }

                String queryName = results.getString(5);
                String queryText = results.getString(6);
                String queryInfo = results.getString(4);

                resultsString += queryName + "++" + queryText + "++" + queryInfo;
            }

            return resultsString;

        } catch (SQLException e) {
            LOG.severe(e.toString());
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
