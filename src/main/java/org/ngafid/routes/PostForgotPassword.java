package org.ngafid.routes;

import com.google.gson.Gson;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;
import org.ngafid.Database;
import org.ngafid.accounts.User;
import spark.Request;
import spark.Response;
import spark.Route;

public class PostForgotPassword implements Route {

    private static final Logger LOG = Logger.getLogger(PostForgotPassword.class.getName());
    private Gson gson;

    public PostForgotPassword(Gson gson) {
        this.gson = gson;
        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    private class ForgotPasswordResponse {
        String message;
        boolean registeredEmail;

        public ForgotPasswordResponse(String message, boolean registeredEmail) {
            this.message = message;
            this.registeredEmail = registeredEmail;

        }
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {

        try {
            Connection connection = Database.getConnection();
            LOG.info("handling " + this.getClass().getName() + " route");
            String email = request.queryParams("email");
            if (User.exists(connection, email)) {
                LOG.info("User exists. Sending reset password email.");
                User.sendPasswordResetEmail(connection, email);
                return gson.toJson(new ForgotPasswordResponse("A password reset link has been sent to your registered email address. Please click on it to reset your password.", true));
            }
            else {
                LOG.info("User with email : "  + email +  " doesn't exist.");
                return gson.toJson(new ForgotPasswordResponse("User doesn't exist in database", false));
            }
        } catch (SQLException e) {
            LOG.severe(e.toString());
            e.printStackTrace();
            return gson.toJson(new ForgotPasswordResponse(e.toString(), false));
        }

    }
}
