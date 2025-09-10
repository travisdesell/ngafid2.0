package org.ngafid.www.routes;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.ngafid.airsync.AirSyncFleet;
import org.ngafid.core.Database;
import org.ngafid.core.accounts.EmailType;
import org.ngafid.core.accounts.Fleet;
import org.ngafid.core.accounts.User;
import org.ngafid.core.accounts.UserPreferences;
import org.ngafid.www.ErrorResponse;
import org.ngafid.www.MustacheHandler;
import org.ngafid.www.Navbar;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

import org.apache.kafka.streams.processor.ConnectedStoreProvider;

import static org.ngafid.www.WebServer.gson;

public class AccountJavalinRoutes {
    public static final Logger LOG = Logger.getLogger(AccountJavalinRoutes.class.getName());

    public static class LoginResponse {
        @JsonProperty
        public final boolean loggedOut;
        @JsonProperty
        public final boolean waiting;
        @JsonProperty
        public final boolean denied;
        @JsonProperty
        public final boolean loggedIn;
        @JsonProperty
        public final String message;
        @JsonProperty
        public final User user;

        public LoginResponse(boolean loggedOut, boolean waiting, boolean denied, boolean loggedIn, String message, User user) {
            this.loggedOut = loggedOut;
            this.waiting = waiting;
            this.loggedIn = loggedIn;
            this.denied = denied;
            this.message = message;
            this.user = user;
        }
    }

    public static class LogoutResponse {
        @JsonProperty
        public final boolean loggedOut;
        @JsonProperty
        public final boolean waiting;
        @JsonProperty
        public final boolean loggedIn;
        @JsonProperty
        public final String message;
        @JsonProperty
        public final User user;

        public LogoutResponse(boolean loggedOut, boolean waiting, boolean loggedIn, String message, User user) {
            this.loggedOut = loggedOut;
            this.waiting = waiting;
            this.loggedIn = loggedIn;
            this.message = message;
            this.user = user;
        }
    }

    public static class ForgotPasswordResponse {
        @JsonProperty
        String message;
        @JsonProperty
        boolean registeredEmail;

        public ForgotPasswordResponse(String message, boolean registeredEmail) {
            this.message = message;
            this.registeredEmail = registeredEmail;

        }
    }

    public static class CreatedAccount {
        @JsonProperty
        public final String accountType;
        @JsonProperty
        public final User user;

        public CreatedAccount(String accountType, User user) {
            this.accountType = accountType;
            this.user = user;
        }
    }

    public static class ResetSuccessResponse {
        @JsonProperty
        public final boolean loggedOut;
        @JsonProperty
        public final boolean waiting;
        @JsonProperty
        public final boolean denied;
        @JsonProperty
        public final boolean loggedIn;
        @JsonProperty
        public final String message;
        @JsonProperty
        public final User user;

        public ResetSuccessResponse(boolean loggedOut, boolean waiting, boolean denied, boolean loggedIn, String message, User user) {
            this.loggedOut = loggedOut;
            this.waiting = waiting;
            this.loggedIn = loggedIn;
            this.denied = denied;
            this.message = message;
            this.user = user;
        }
    }

    public static class Profile {
        @JsonProperty
        public final User user;

        public Profile(User user) {
            this.user = user;
        }
    }

    public static void getCreateAccount(Context ctx) {
        final String templateFile = "create_account.html";
        HashMap<String, Object> scopes = new HashMap<String, Object>();

        LOG.severe("template file: '" + templateFile + "'");

        try {
            StringBuilder fleetnamesJavascript = new StringBuilder("var fleetNames = [");
            try (Connection connection = Database.getConnection()) {
                List<String> names = new ArrayList<String>();

                try (PreparedStatement query = connection.prepareStatement("SELECT fleet_name FROM fleet ORDER BY fleet_name"); ResultSet resultSet = query.executeQuery()) {
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

    public static void getForgotPassword(Context ctx) {
        final String templateFile = "forgot_password.html";
        Map<String, Object> scopes = new HashMap<String, Object>();

        LOG.info("template file: '" + templateFile + "'");

        ctx.header("Content-Type", "text/html; charset=UTF-8");
        ctx.render(templateFile, scopes);
    }

    public static void getResetPassword(Context ctx) {
        final String templateFile = "reset_password.html";
        Map<String, Object> scopes = new HashMap<String, Object>();

        LOG.info("template file: '" + templateFile + "'");

        ctx.header("Content-Type", "text/html; charset=UTF-8");
        ctx.render(templateFile, scopes);
    }

    public static void getUpdatePassword(Context ctx) {
        final String templateFile = "update_password.html";
        Map<String, Object> scopes = new HashMap<String, Object>();
        User user = ctx.sessionAttribute("user");

        LOG.info("template file: '" + templateFile + "'");

        scopes.put("navbar_js", Navbar.getJavascript(ctx));
        scopes.put("user_js", "var user = JSON.parse('" + gson.toJson(user) + "');");

        ctx.header("Content-Type", "text/html; charset=UTF-8");
        ctx.render(templateFile, scopes);
    }

    public static void getUpdateProfile(Context ctx) {
        final String templateFile = "update_profile.html";
        Map<String, Object> scopes = new HashMap<>();

        scopes.put("navbar_js", Navbar.getJavascript(ctx));

        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        scopes.put("user_js", "var user = JSON.parse('" + gson.toJson(user) + "');");

        ctx.header("Content-Type", "text/html; charset=UTF-8");
        ctx.render(templateFile, scopes);
    }

    public static void getUserPreferencesPage(Context ctx) {
        final String templateFile = "preferences_page.html";
        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));

        try (Connection connection = Database.getConnection()) {
            Fleet fleet = Objects.requireNonNull(Fleet.get(connection, user.getFleetId()));
            UserPreferences userPreferences = User.getUserPreferences(connection, user.getId());
            Map<String, Object> scopes = new HashMap<>();

            scopes.put("navbar_js", Navbar.getJavascript(ctx));
            scopes.put("user_name", "var userName = JSON.parse('" + gson.toJson(user.getFullName()) + "');\n");
            scopes.put("user_fleet_selected", "var userFleetSelected = JSON.parse('" + gson.toJson(user.getSelectedFleetId()) + "');\n");
            scopes.put("is_admin", "var isAdmin = JSON.parse('" + gson.toJson(user.isAdmin()) + "');\n");
            scopes.put("user_prefs_json", "var userPreferences = JSON.parse('" + gson.toJson(userPreferences) + "');\n");

            if (fleet.hasAirsync(connection)) {
                String timeout = AirSyncFleet.getTimeout(connection, fleet.getId());
                scopes.put("airsync", "var airsyncTimeout = JSON.parse('" + gson.toJson(timeout) + "');\n");
            } else {
                scopes.put("airsync", "var airsyncTimeout = -1;\n");
            }

            ctx.header("Content-Type", "text/html; charset=UTF-8");
            ctx.render(templateFile, scopes);
        } catch (Exception se) {
            se.printStackTrace();
        }
    }

    public static void getEmailUnsubscribe(Context ctx) {
        final int id = Integer.parseInt(Objects.requireNonNull(ctx.formParam("id")));
        final String token = ctx.formParam("token");

        // Check if the token is valid
        try (Connection connection = Database.getConnection()) {
            try (PreparedStatement query = connection.prepareStatement("SELECT * FROM email_unsubscribe_tokens WHERE token=? AND user_id=?")) {
                query.setString(1, token);
                query.setInt(2, id);
                try (ResultSet resultSet = query.executeQuery()) {
                    if (!resultSet.next()) {
                        String exceptionMessage = "Provided token/id pairing was not found: (" + token + ", " + id + "), may have already expired or been used";
                        LOG.severe(exceptionMessage);
                        throw new Exception(exceptionMessage);
                    }
                }
            }

            // Remove the token from the database
            try (PreparedStatement queryTokenRemoval = connection.prepareStatement("DELETE FROM email_unsubscribe_tokens WHERE token=? AND user_id=?")) {
                queryTokenRemoval.setString(1, token);
                queryTokenRemoval.setInt(2, id);
                queryTokenRemoval.executeUpdate();
            }

            // Set all non-forced email preferences to 0 in the database
            try (PreparedStatement queryClearPreferences = connection.prepareStatement("SELECT * FROM email_preferences WHERE user_id=?")) {
                queryClearPreferences.setInt(1, id);
                try (ResultSet resultSet = queryClearPreferences.executeQuery()) {
                    while (resultSet.next()) {
                        String emailType = resultSet.getString("email_type");
                        if (EmailType.isForced(emailType)) {
                            continue;
                        }

                        try (PreparedStatement update = connection.prepareStatement("UPDATE email_preferences SET enabled=0 WHERE user_id=? AND email_type=?")) {
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
        app.get("/create_account", AccountJavalinRoutes::getCreateAccount);
        app.get("/forgot_password", AccountJavalinRoutes::getForgotPassword);
        app.get("/reset_password", AccountJavalinRoutes::getResetPassword);
        app.get("/protected/update_password", AccountJavalinRoutes::getUpdatePassword);
        app.get("/protected/update_profile", AccountJavalinRoutes::getUpdateProfile);
        app.get("/protected/preferences", AccountJavalinRoutes::getUserPreferencesPage);
        app.get("/email_unsubscribe", AccountJavalinRoutes::getEmailUnsubscribe);
        app.after("/email_unsubscribe", ctx -> ctx.redirect("/"));
    }
}
