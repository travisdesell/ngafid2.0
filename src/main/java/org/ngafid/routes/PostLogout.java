package org.ngafid.routes;

import java.util.ArrayList;
import java.util.logging.Logger;

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
import org.ngafid.accounts.AccountException;
import org.ngafid.accounts.FleetAccess;
import org.ngafid.accounts.User;

public class PostLogout implements Route {
    private static final Logger LOG = Logger.getLogger(PostLogout.class.getName());
    private Gson gson;

    public PostLogout(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    private class LogoutResponse {
        private boolean loggedOut;
        private boolean waiting;
        private boolean loggedIn;
        private String message;
        private User user;

        public LogoutResponse(boolean loggedOut, boolean waiting, boolean loggedIn, String message, User user) {
            this.loggedOut = loggedOut;
            this.waiting = waiting;
            this.loggedIn = loggedIn;
            this.message = message;
            this.user = user;
        }
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName());

        final Session session = request.session();
        User user = session.attribute("user");

        //set the session attribute for this user so
        //it will be considered logged in.
        request.session().removeAttribute("user");
        LOG.info("removed user '" + user.getEmail() + "' from the session.");

        return gson.toJson(new LogoutResponse(true, false, false, "Successfully logged out.", null));
    }
}

