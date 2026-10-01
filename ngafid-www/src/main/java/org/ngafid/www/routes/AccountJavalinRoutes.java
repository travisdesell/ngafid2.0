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
import org.ngafid.www.MustacheHandler;
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

        /**
         * Constructs the JSON response describing the outcome of a login attempt.
         *
         * @param loggedOut true if the session is now logged out
         * @param waiting true if the account is awaiting approval/activation
         * @param denied true if the login was denied (e.g. bad credentials or access refused)
         * @param loggedIn true if the login succeeded and the session is now authenticated
         * @param message a human-readable status message for the client
         * @param user the authenticated user when login succeeded, otherwise null
         */
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

        /**
         * Constructs the JSON response describing the outcome of a logout request.
         *
         * @param loggedOut true if the session was successfully logged out
         * @param waiting true if the account is awaiting approval/activation
         * @param loggedIn true if the session is still authenticated
         * @param message a human-readable status message for the client
         * @param user the user the session belonged to, or null
         */
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

        /**
         * Constructs the JSON response for a forgot-password request.
         *
         * @param message a human-readable status message for the client
         * @param registeredEmail true if the submitted email matched a registered account (and a reset email was
         *     therefore sent)
         */
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

        /**
         * Constructs the JSON response describing a newly created account.
         *
         * @param accountType the type of account that was created
         * @param user the newly created user
         */
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

        /**
         * Constructs the JSON response describing the outcome of a password reset.
         *
         * @param loggedOut true if the session is now logged out
         * @param waiting true if the account is awaiting approval/activation
         * @param denied true if the reset was denied (e.g. an invalid or expired token)
         * @param loggedIn true if the user is authenticated after the reset
         * @param message a human-readable status message for the client
         * @param user the affected user, or null
         */
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

        /**
         * Constructs a profile response wrapping the given user.
         *
         * @param user the user whose profile is returned
         */
        public Profile(User user) {
            this.user = user;
        }

        public User getUser() {
            return user;
        }
    }

    /**
     * Renders the account-creation page ({@code create_account.html}), including any invite information needed to
     * prefill the form.
     *
     * @param ctx the Javalin request context, whose response is rendered
     */
    public static void getCreateAccount(Context ctx) {
        final String templateFile = "create_account.html";
        HashMap<String, Object> scopes = new HashMap<String, Object>();

        LOG.severe("template file: '" + templateFile + "'");

        try {
            StringBuilder fleetnamesJavascript = new StringBuilder("var fleetNames = [");
            try (Connection connection = Database.getConnection()) {
                List<String> names = new ArrayList<String>();

                try (PreparedStatement query =
                                connection.prepareStatement("SELECT fleet_name FROM fleet ORDER BY fleet_name");
                        ResultSet resultSet = query.executeQuery()) {
                    boolean first = true;
                    while (resultSet.next()) {
                        if (first) {
                            first = false;
                            fleetnamesJavascript.append("\"");
                        } else {
                            fleetnamesJavascript.append(", \"");
                        }
                        fleetnamesJavascript.append(resultSet.getString(1));
                        fleetnamesJavascript.append("\"");
                    }
                }
            } catch (SQLException e) {
                ctx.json(new ErrorResponse(e)).status(500);
            }

            fleetnamesJavascript.append("];");

            scopes.put("fleetnames_js", fleetnamesJavascript);
            MustacheHandler.handle(templateFile, scopes);

            ctx.header("Content-Type", "text/html; charset=UTF-8");
            ctx.render(templateFile, scopes);
        } catch (IOException e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    /**
     * Renders the forgot-password page ({@code forgot_password.html}) where a user can request a reset email.
     *
     * @param ctx the Javalin request context, whose response is rendered
     */
    public static void getForgotPassword(Context ctx) {
        final String templateFile = "forgot_password.html";
        Map<String, Object> scopes = new HashMap<String, Object>();

        LOG.info("template file: '" + templateFile + "'");

        ctx.header("Content-Type", "text/html; charset=UTF-8");
        ctx.render(templateFile, scopes);
    }

    /**
     * Renders the password-reset page ({@code reset_password.html}) that a user reaches from a reset-email link.
     *
     * @param ctx the Javalin request context, whose response is rendered
     */
    public static void getResetPassword(Context ctx) {
        final String templateFile = "reset_password.html";
        Map<String, Object> scopes = new HashMap<String, Object>();

        LOG.info("template file: '" + templateFile + "'");

        ctx.header("Content-Type", "text/html; charset=UTF-8");
        ctx.render(templateFile, scopes);
    }

    /**
     * Renders the change-password page ({@code update_password.html}) for the logged-in user.
     *
     * @param ctx the Javalin request context, whose response is rendered
     */
    public static void getUpdatePassword(Context ctx) {
        final String templateFile = "update_password.html";
        Map<String, Object> scopes = new HashMap<String, Object>();
        User user = ctx.sessionAttribute("user");

        LOG.info("template file: '" + templateFile + "'");

        scopes.put("navbar_js", Navbar.getJavascript(ctx));
        scopes.put("user_js", "var user = JSON.parse('" + GSON.toJson(user) + "');");

        ctx.header("Content-Type", "text/html; charset=UTF-8");
        ctx.render(templateFile, scopes);
    }

    /**
     * Renders the edit-profile page ({@code update_profile.html}) for the logged-in user.
     *
     * @param ctx the Javalin request context, whose response is rendered
     */
    public static void getUpdateProfile(Context ctx) {
        final String templateFile = "update_profile.html";
        Map<String, Object> scopes = new HashMap<>();

        scopes.put("navbar_js", Navbar.getJavascript(ctx));

        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        scopes.put("user_js", "var user = JSON.parse('" + GSON.toJson(user) + "');");

        ctx.header("Content-Type", "text/html; charset=UTF-8");
        ctx.render(templateFile, scopes);
    }

    /**
     * Renders the two-factor-authentication settings page ({@code two_factor_settings.html}) for the logged-in
     * user.
     *
     * @param ctx the Javalin request context, whose response is rendered
     */
    public static void getTwoFactorSettings(Context ctx) {
        final String templateFile = "two_factor_settings.html";
        Map<String, Object> scopes = new HashMap<>();

        scopes.put("navbar_js", Navbar.getJavascript(ctx));

        // Try to get user from session, but don't require it
        final User user = ctx.sessionAttribute("user");
        if (user != null) {
            scopes.put("user_js", "var user = JSON.parse('" + GSON.toJson(user) + "');");
        } else {
            scopes.put("user_js", "var user = null;");
        }

        ctx.header("Content-Type", "text/html; charset=UTF-8");
        ctx.render(templateFile, scopes);
    }

    /**
     * Renders the user-preferences page ({@code preferences_page.html}) for the logged-in user, injecting their
     * current preferences for the client.
     *
     * @param ctx the Javalin request context, whose response is rendered
     */
    public static void getUserPreferencesPage(Context ctx) {
        final String templateFile = "preferences_page.html";
        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));

        try (Connection connection = Database.getConnection()) {
            Fleet fleet = Objects.requireNonNull(Fleet.get(connection, user.getFleetId()));
            UserPreferences userPreferences = User.getUserPreferences(connection, user.getId());
            Map<String, Object> scopes = new HashMap<>();

            scopes.put("navbar_js", Navbar.getJavascript(ctx));
            scopes.put("user_name", "var userName = JSON.parse('" + GSON.toJson(user.getFullName()) + "');\n");
            scopes.put(
                    "user_fleet_selected",
                    "var userFleetSelected = JSON.parse('" + GSON.toJson(user.getSelectedFleetId()) + "');\n");
            scopes.put("is_admin", "var isAdmin = JSON.parse('" + GSON.toJson(user.isAdmin()) + "');\n");
            scopes.put(
                    "user_prefs_json", "var userPreferences = JSON.parse('" + GSON.toJson(userPreferences) + "');\n");

            if (fleet.hasAirsync(connection)) {
                String timeout = AirSyncFleet.getTimeout(connection, fleet.getId());
                scopes.put("airsync", "var airsyncTimeout = JSON.parse('" + GSON.toJson(timeout) + "');\n");
            } else {
                scopes.put("airsync", "var airsyncTimeout = -1;\n");
            }

            ctx.header("Content-Type", "text/html; charset=UTF-8");
            ctx.render(templateFile, scopes);
        } catch (Exception se) {
            se.printStackTrace();
        }
    }

    /**
     * Processes an email-unsubscribe request submitted from an unsubscribe link, identified by the {@code id} form
     * parameter and validated against the accompanying {@code token}, updating that user's email preferences.
     *
     * @param ctx the Javalin request context supplying the {@code id} and {@code token} form parameters
     */
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

    /**
     * Registers this class's account routes (login/logout, account creation, password reset/update, profile and
     * preferences pages, two-factor settings, and email unsubscribe) on the given Javalin application.
     *
     * @param app the Javalin application to register the routes on
     */
    public static void bindRoutes(Javalin app) {
        app.get("/create_account", AccountJavalinRoutes::getCreateAccount);
        app.get("/forgot_password", AccountJavalinRoutes::getForgotPassword);
        app.get("/reset_password", AccountJavalinRoutes::getResetPassword);
        app.get("/protected/update_password", AccountJavalinRoutes::getUpdatePassword);
        app.get("/protected/update_profile", AccountJavalinRoutes::getUpdateProfile);
        app.get("/protected/preferences", AccountJavalinRoutes::getUserPreferencesPage);
        app.get("/two-factor-settings", AccountJavalinRoutes::getTwoFactorSettings);
        app.get("/email_unsubscribe", AccountJavalinRoutes::getEmailUnsubscribe);
        app.after("/email_unsubscribe", ctx -> ctx.redirect("/"));
    }
}
