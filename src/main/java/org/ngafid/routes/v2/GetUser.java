package org.ngafid.routes.v2;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;


import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.HashMap;
import org.ngafid.routes.ErrorResponse;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


import com.google.gson.Gson;

import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;

import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.accounts.User;



public class GetUser implements Route {
    private static final Logger LOG = Logger.getLogger(GetUser.class.getName());
    private Gson gson;

    public GetUser(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }
    private class LoginResponse {
        private boolean loggedOut;
        private boolean waiting;
        private boolean denied;
        private boolean loggedIn;
        private String message;
        private User user;

        public LoginResponse(boolean loggedOut, boolean waiting, boolean denied, boolean loggedIn, String message, User user) {
            this.loggedOut = loggedOut;
            this.waiting = waiting;
            this.loggedIn = loggedIn;
            this.denied = denied;
            this.message = message;
            this.user = user;
        }
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");
        LOG.info("Request " + request);
        final Session session = request.session();
        
        try  {

            User user = session.attribute("user");
            
            LOG.info("User data " + user);
            if(user != null){
                return gson.toJson(new LoginResponse(false, false, false, true, "Success!", user));
            }

            } catch (Exception e) {
                return gson.toJson(new ErrorResponse(e));
            }

        // Return an empty JSON if there is no user data
        return gson.toJson(new HashMap<>());
    }
}
