package org.ngafid.routes.spark;

import java.util.logging.Logger;

import java.sql.Connection;
import java.sql.SQLException;

import com.google.gson.Gson;

import org.ngafid.routes.ErrorResponse;
import spark.Route;
import spark.Request;
import spark.Response;

import org.ngafid.Database;
import org.ngafid.accounts.User;

public class PostUpdatePassword implements Route {
    private static final Logger LOG = Logger.getLogger(PostUpdatePassword.class.getName());
    private Gson gson;

    public PostUpdatePassword(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    private class UpdatedPassword {
        private User user;

        public UpdatedPassword(User user) {
            this.user = user;
        }
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName());

        String currentPassword = request.formParams("currentPassword");
        String newPassword = request.formParams("newPassword");
        String confirmPassword = request.formParams("confirmPassword");

        LOG.info("currentPassword: '" + currentPassword + "'");
        LOG.info("newPassword: '" + newPassword + "'");
        LOG.info("confirmPassword: '" + confirmPassword + "'");

        User user = (User) request.session().attribute("user");
        // 1. make sure currentPassword authenticates against what's in the database
        try (Connection connection = Database.getConnection()) {
            if (!user.validate(connection, currentPassword)) {
                return gson.toJson(
                        new ErrorResponse("Could not update password.", "The current password was not correct."));
            }

            // 2. make sure the new password and confirm password are the same
            if (!newPassword.equals(confirmPassword)) {
                return gson.toJson(new ErrorResponse("Could not update password.",
                        "The server received different new and confirmation passwords."));
            }

            // 3. make sure the new password is different from the old password
            if (currentPassword.equals(newPassword)) {
                return gson.toJson(new ErrorResponse("Could not update password.",
                        "The current password was the same as the new password."));
            }

            user.updatePassword(connection, newPassword);

            return gson.toJson(new UpdatedPassword(user));
        } catch (SQLException e) {
            LOG.severe(e.toString());
            e.printStackTrace();
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
