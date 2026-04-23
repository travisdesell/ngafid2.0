package org.ngafid.www.routes;

import static org.ngafid.www.WebServer.GSON;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;
import org.ngafid.airsync.AirSyncFleet;
import org.ngafid.core.Database;
import org.ngafid.core.accounts.EmailType;
import org.ngafid.core.accounts.Fleet;
import org.ngafid.core.accounts.User;
import org.ngafid.core.accounts.UserPreferences;
import org.ngafid.www.ErrorResponse;
import org.ngafid.www.Navbar;

public class AccountJavalinRoutes {
    public static final Logger LOG = Logger.getLogger(AccountJavalinRoutes.class.getName());

    private AccountJavalinRoutes() {
        // Utility class
    }

    public static class LoginResponse {
        @JsonProperty
        private final boolean loggedOut;

        @JsonProperty
        private final boolean waiting;

        @JsonProperty
        private final boolean denied;

        @JsonProperty
        private final boolean loggedIn;

        @JsonProperty
        private final String message;

        @JsonProperty
        private final User user;

        public LoginResponse(
                boolean loggedOut, boolean waiting, boolean denied, boolean loggedIn, String message, User user) {
            this.loggedOut = loggedOut;
            this.waiting = waiting;
            this.loggedIn = loggedIn;
            this.denied = denied;
            this.message = message;
            this.user = user;
        }

        public boolean isLoggedOut() {
            return loggedOut;
        }

        public boolean isWaiting() {
            return waiting;
        }

        public boolean isDenied() {
            return denied;
        }

        public boolean isLoggedIn() {
            return loggedIn;
        }

        public String getMessage() {
            return message;
        }

        public User getUser() {
            return user;
        }
    }

    public static class LogoutResponse {
        @JsonProperty
        private final boolean loggedOut;

        @JsonProperty
        private final boolean waiting;

        @JsonProperty
        private final boolean loggedIn;

        @JsonProperty
        private final String message;

        @JsonProperty
        private final User user;

        public LogoutResponse(boolean loggedOut, boolean waiting, boolean loggedIn, String message, User user) {
            this.loggedOut = loggedOut;
            this.waiting = waiting;
            this.loggedIn = loggedIn;
            this.message = message;
            this.user = user;
        }

        public boolean isLoggedOut() {
            return loggedOut;
        }

        public boolean isWaiting() {
            return waiting;
        }

        public boolean isLoggedIn() {
            return loggedIn;
        }

        public String getMessage() {
            return message;
        }

        public User getUser() {
            return user;
        }
    }

    public static class ForgotPasswordResponse {
        @JsonProperty
        private final String message;

        @JsonProperty
        private final boolean registeredEmail;

        public ForgotPasswordResponse(String message, boolean registeredEmail) {
            this.message = message;
            this.registeredEmail = registeredEmail;
        }

        public String getMessage() {
            return message;
        }

        public boolean isRegisteredEmail() {
            return registeredEmail;
        }
    }

    public static class CreatedAccount {
        @JsonProperty
        private final String accountType;

        @JsonProperty
        private final User user;

        public CreatedAccount(String accountType, User user) {
            this.accountType = accountType;
            this.user = user;
        }

        public String getAccountType() {
            return accountType;
        }

        public User getUser() {
            return user;
        }
    }

    public static class ResetSuccessResponse {
        @JsonProperty
        private final boolean loggedOut;

        @JsonProperty
        private final boolean waiting;

        @JsonProperty
        private final boolean denied;

        @JsonProperty
        private final boolean loggedIn;

        @JsonProperty
        private final String message;

        @JsonProperty
        private final User user;

        public ResetSuccessResponse(
                boolean loggedOut, boolean waiting, boolean denied, boolean loggedIn, String message, User user) {
            this.loggedOut = loggedOut;
            this.waiting = waiting;
            this.loggedIn = loggedIn;
            this.denied = denied;
            this.message = message;
            this.user = user;
        }

        public boolean isLoggedOut() {
            return loggedOut;
        }

        public boolean isWaiting() {
            return waiting;
        }

        public boolean isDenied() {
            return denied;
        }

        public boolean isLoggedIn() {
            return loggedIn;
        }

        public String getMessage() {
            return message;
        }

        public User getUser() {
            return user;
        }
    }

    public static class Profile {
        @JsonProperty
        private final User user;

        public Profile(User user) {
            this.user = user;
        }

        public User getUser() {
            return user;
        }
    }

    public static void getEmailUnsubscribe(Context ctx) {
        final int id = Integer.parseInt(Objects.requireNonNull(ctx.formParam("id")));
        final String token = ctx.formParam("token");

        // Check if the token is valid
        try (Connection connection = Database.getConnection()) {
            try (PreparedStatement query =
                    connection.prepareStatement("SELECT * FROM email_unsubscribe_tokens WHERE token=? AND user_id=?")) {
                query.setString(1, token);
                query.setInt(2, id);
                try (ResultSet resultSet = query.executeQuery()) {
                    if (!resultSet.next()) {
                        String exceptionMessage = "Provided token/id pairing was not found: (" + token + ", " + id
                                + "), may have already expired or been used";
                        LOG.severe(exceptionMessage);
                        throw new Exception(exceptionMessage);
                    }
                }
            }

            // Remove the token from the database
            try (PreparedStatement queryTokenRemoval =
                    connection.prepareStatement("DELETE FROM email_unsubscribe_tokens WHERE token=? AND user_id=?")) {
                queryTokenRemoval.setString(1, token);
                queryTokenRemoval.setInt(2, id);
                queryTokenRemoval.executeUpdate();
            }

            // Set all non-forced email preferences to 0 in the database
            try (PreparedStatement queryClearPreferences =
                    connection.prepareStatement("SELECT * FROM email_preferences WHERE user_id=?")) {
                queryClearPreferences.setInt(1, id);
                try (ResultSet resultSet = queryClearPreferences.executeQuery()) {
                    while (resultSet.next()) {
                        String emailType = resultSet.getString("email_type");
                        if (EmailType.isForced(emailType)) {
                            continue;
                        }

                        try (PreparedStatement update = connection.prepareStatement(
                                "UPDATE email_preferences SET enabled=0 WHERE user_id=? AND email_type=?")) {
                            update.setInt(1, id);
                            update.setString(2, emailType);
                            update.executeUpdate();
                        }
                    }
                }
            }

            ctx.result("Successfully unsubscribed from emails...");
        } catch (Exception e) {
            e.printStackTrace();
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    public static void bindRoutes(Javalin app) {
        app.get("/email_unsubscribe", AccountJavalinRoutes::getEmailUnsubscribe);
        app.after("/email_unsubscribe", ctx -> ctx.redirect("/"));
    }
}
