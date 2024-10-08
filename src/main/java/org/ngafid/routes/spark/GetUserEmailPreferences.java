package org.ngafid.routes.spark;

import java.util.logging.Logger;

import java.sql.Connection;

import com.google.gson.Gson;

import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;
import spark.Spark;

import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.accounts.UserEmailPreferences;

public class GetUserEmailPreferences implements Route {

    /*
     * This route's 'handle' method requires one or
     * two string keys supplied alongside the to
     * operate correctly!
     * 
     * Should either be...
     * 1. "HANDLE_FETCH_USER" (for user data)
     * 2. "HANDLE_FETCH_MANAGER" (for a user's data in a manager's fleet)
     */

    private static final Logger LOG = Logger.getLogger(GetUserEmailPreferences.class.getName());
    private Gson gson;

    public GetUserEmailPreferences(Gson gson) {
        this.gson = gson;
        LOG.info("get " + this.getClass().getName() + " initalized");
    }

    @Override
    public Object handle(Request request, Response response) {

        LOG.info("handling " + this.getClass().getName() + " route");

        // Unpack Fetching Data
        String handleFetchType = request.queryParams("handleFetchType");

        final Session session = request.session();
        User sessionUser = session.attribute("user");

        int fleetUserID = -1;

        if (handleFetchType.equals("HANDLE_FETCH_USER")) { // Fetching Session User...
            fleetUserID = sessionUser.getId();
        } else if (handleFetchType.equals("HANDLE_FETCH_MANAGER")) { // Fetching a Manager's Fleet User...

            fleetUserID = Integer.parseInt(request.queryParams("fleetUserID"));
            int fleetID = Integer.parseInt(request.queryParams("fleetID"));

            // Check to see if the logged in user can update access to this fleet
            if (!sessionUser.managesFleet(fleetID)) {
                LOG.severe("INVALID ACCESS: user did not have access to fetch user email preferences on this fleet.");
                Spark.halt(401, "User did not have access to fetch user email preferences on this fleet.");
                return null;
            }

        }

        try (Connection connection = Database.getConnection()) {
            UserEmailPreferences userPreferences = User.getUserEmailPreferences(Database.getConnection(), fleetUserID);
            return gson.toJson(userPreferences);
        } catch (Exception se) {
            LOG.severe("Error in GetUserEmailPreferences.java");
            se.printStackTrace();
            response.status(500);
            return gson.toJson(new ErrorResponse(se));
        }

    }

}
