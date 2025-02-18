package org.ngafid.routes.spark;

import java.util.HashMap;
import java.util.logging.Logger;
import java.sql.Connection;

import com.google.gson.Gson;

import org.ngafid.routes.ErrorResponse;
import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;
import spark.Spark;

import org.ngafid.Database;
import org.ngafid.accounts.User;

public class PostUpdateUserEmailPreferences implements Route {

    /*
     * This route's 'handle' method requires one or
     * two string keys supplied alongside the update
     * data to operate correctly!
     * 
     * Should either be...
     * 1. "HANDLE_UPDATE_USER" (for user updates)
     * 2. "HANDLE_UPDATE_MANAGER" (for manager updates)
     */

    private static final Logger LOG = Logger.getLogger(PostUpdateUserEmailPreferences.class.getName());
    private Gson gson;

    public PostUpdateUserEmailPreferences(Gson gson) {
        this.gson = gson;
        LOG.info("post email prefs route initialized.");
    }

    @Override
    public Object handle(Request request, Response response) {

        final Session session = request.session();
        User sessionUser = session.attribute("user");

        // Log the raw handleUpdateType value
        String handleUpdateType = request.formParams("handleUpdateType");

        if (handleUpdateType.equals("HANDLE_UPDATE_USER")) { // User Update...
            return handleUserUpdate(request, response, sessionUser);
        } else if (handleUpdateType.equals("HANDLE_UPDATE_MANAGER")) { // Manager Update...
            return handleManagerUpdate(request, response, sessionUser);
        }

        // ERROR -- Unknown Update!
        LOG.severe("INVALID ACCESS: handleUpdateType not specified.");
        Spark.halt(401, "handleUpdateType not specified.");
        return null;

    }

    // Handle User Update
    public Object handleUserUpdate(Request request, Response response, User sessionUser) {

        // Unpack Submission Data
        int userID = sessionUser.getId();

        HashMap<String, Boolean> emailTypesUser = new HashMap<String, Boolean>();
        for (String emailKey : request.formParams()) {

            if (emailKey.equals("handleUpdateType")) {
                continue;
            }

            emailTypesUser.put(emailKey, Boolean.parseBoolean(request.formParams(emailKey)));

        }

        try (Connection connection = Database.getConnection()) {
            return gson.toJson(User.updateUserEmailPreferences(connection, userID, emailTypesUser));
        } catch (Exception e) {
            e.printStackTrace();
            return gson.toJson(new ErrorResponse(e));
        }

    }

    // Handle Manager Update
    public Object handleManagerUpdate(Request request, Response response, User sessionUser) {

        // Unpack Submission Data
        int fleetUserID = Integer.parseInt(request.formParams("fleetUserID"));
        int fleetID = Integer.parseInt(request.formParams("fleetID"));

        HashMap<String, Boolean> emailTypesUser = new HashMap<String, Boolean>();
        for (String emailKey : request.formParams()) {

            if (emailKey.equals("fleetUserID") || emailKey.equals("fleetID") || emailKey.equals("handleUpdateType")) {
                continue;
            }

            emailTypesUser.put(emailKey, Boolean.parseBoolean(request.formParams(emailKey)));

        }

        // Check to see if the logged in user can update access to this fleet
        if (!sessionUser.managesFleet(fleetID)) {
            LOG.severe("INVALID ACCESS: user did not have access to modify user email preferences on this fleet.");
            Spark.halt(401, "User did not have access to modify user email preferences on this fleet.");
            return null;
        }

        try (Connection connection = Database.getConnection()) {
            return gson.toJson(User.updateUserEmailPreferences(connection, fleetUserID, emailTypesUser));
        } catch (Exception e) {
            e.printStackTrace();
            return gson.toJson(new ErrorResponse(e));
        }

    }

}
