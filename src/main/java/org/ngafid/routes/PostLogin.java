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

import org.ngafid.Database;
import org.ngafid.accounts.AccountException;
import org.ngafid.accounts.FleetAccess;
import org.ngafid.accounts.User;

public class PostLogin implements Route {
    private static final Logger LOG = Logger.getLogger(PostLogin.class.getName());
    private Gson gson;

    public PostLogin(Gson gson) {
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
        LOG.info("handling " + this.getClass().getName());

        String email = request.queryParams("email");
        String password = request.queryParams("password");

        LOG.info("email: '" + email + "'");
        //don't print the password to the log!
        //LOG.info("password: '" + password + "'");

        try {
            Connection connection = Database.getConnection();
            User user = User.get(connection, email, password);

            if (user == null) {
                LOG.info("Could not get user, get returned null.");
                return gson.toJson(new LoginResponse(true, false, false, false, "Invalid email or password.", null));
            } else {
                LOG.info("User authentication successful.");
                user.updateLastLoginTimeStamp(connection);
                //set the session attribute for this user so
                //it will be considered logged in.
                request.session().attribute("user", user);

                if (user.getFleetAccessType().equals(FleetAccess.DENIED)) {
                    return gson.toJson(new LoginResponse(false, false, true, false, "Waiting!", user));
                } else if (user.getFleetAccessType().equals(FleetAccess.WAITING)) {
                    return gson.toJson(new LoginResponse(false, true, false, false, "Waiting!", user));
                } else {
                    return gson.toJson(new LoginResponse(false, false, false, true, "Success!", user));
                }
            }

        } catch (SQLException e) {
            LOG.severe(e.toString());
            e.printStackTrace();
            return gson.toJson(new ErrorResponse(e));
        } catch (AccountException e) {
            return gson.toJson(new LoginResponse(true, false, false, false, "Incorrect email or password.", null));
        }
    }
}

