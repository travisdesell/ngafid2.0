package org.ngafid.routes;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;


import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.HashMap;

import java.sql.Connection;
import java.sql.SQLException;

import com.google.gson.Gson;

import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;

import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.accounts.User;
import org.ngafid.accounts.UserPreferences;
import org.ngafid.accounts.UserEmailPreferences;
import org.ngafid.flights.Tail;
import org.ngafid.flights.Tails;

public class GetUserEmailPreferences implements Route {


    /*
        This route's 'handle' method requires one or
        two string keys supplied alongside the to
        operate correctly!

        Should either be...
            1. "HANDLE_FETCH_USER" (for user data)
            2. "HANDLE_FETCH_MANAGER" (for a user's data in a manager's fleet)
    */

    private static final Logger LOG = Logger.getLogger(GetUserEmailPreferences.class.getName());
    private static Connection connection = Database.getConnection();
    private Gson gson;

    public GetUserEmailPreferences(Gson gson) {
        this.gson = gson;
        LOG.info("get " + this.getClass().getName() + " initalized");   
    }

    @Override
    public Object handle(Request request, Response response) {

        LOG.info("handling " + this.getClass().getName() + " route");

        //Unpack Fetching Data
        String handleFetchType = request.queryParams("handleFetchType");

        final Session session = request.session();
        User sessionUser = session.attribute("user");


        int fleetUserID = -1;
        
        //Fetching Session User...
        if (handleFetchType.equals("HANDLE_FETCH_USER")) {
            fleetUserID = sessionUser.getId();
        }

        //Fetching a Manager's Fleet User...
        else if (handleFetchType.equals("HANDLE_FETCH_MANAGER")) {
            fleetUserID = Integer.parseInt(request.queryParams("fleetUserID"));
        }


        try {
            UserEmailPreferences userPreferences = User.getUserEmailPreferences(connection, fleetUserID);
            return gson.toJson(userPreferences);
        } catch (Exception se) {
            LOG.severe("Error in GetUserEmailPreferences.java");
            se.printStackTrace();
            response.status(500);
            return gson.toJson(new ErrorResponse(se));
        }
    
    }

}