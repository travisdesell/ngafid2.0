package org.ngafid.routes;

import com.google.gson.Gson;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;
import org.ngafid.Database;
import org.ngafid.accounts.AccountException;
import org.ngafid.accounts.User;
import spark.Request;
import spark.Response;
import spark.Route;

public class PostResetPassword implements Route {
    private static final Logger LOG = Logger.getLogger(PostResetPassword.class.getName());
    private Gson gson;

    public PostResetPassword(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    private class ResetSuccessResponse {
        private boolean loggedOut;
        private boolean waiting;
        private boolean denied;
        private boolean loggedIn;
        private String message;
        private User user;

        public ResetSuccessResponse(boolean loggedOut, boolean waiting, boolean denied, boolean loggedIn, String message, User user) {
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

        String emailAddress = request.queryParams("emailAddress");
        String passphrase = request.queryParams("passphrase");
        String newPassword = request.queryParams("newPassword");
        String confirmPassword = request.queryParams("confirmPassword");

        LOG.info("emailAddress: '" + emailAddress + "'");
        //don't print the password to the log!
        //LOG.info("password: '" + password + "'");

        try {
            Connection connection = Database.getConnection();

            //1. make sure the new password and confirm password are the same
            if (!newPassword.equals(confirmPassword)) {
                return gson.toJson(new ErrorResponse("Could not reset password.", "The server received different new and confirmation passwords."));
            }

            //2. make sure the passphrase is valid
            if (!User.validatePassphrase(connection, emailAddress, passphrase)) {
                return gson.toJson(new ErrorResponse("Could not reset password.", "The passphrase provided was not correct."));
            }

            User.updatePassword(connection, emailAddress, newPassword);
            User user = User.get(connection, emailAddress, newPassword);

            //set the session attribute for this user so
            //it will be considered logged in.
            request.session().attribute("user", user);

            return gson.toJson(new ResetSuccessResponse(false, false, false, true, "Success!", user));

        } catch (SQLException e) {
            LOG.severe(e.toString());
            e.printStackTrace();
            return gson.toJson(new ErrorResponse(e));
        } catch (AccountException e) {
            return gson.toJson(new ResetSuccessResponse(true, false, false, false, "Incorrect email or password.", null));
        }
    }
}
