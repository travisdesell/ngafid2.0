package org.ngafid.www.routes;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.javalin.Javalin;
import io.javalin.http.Context;

import org.ngafid.airsync.AirSyncFleet;
import org.ngafid.core.Database;
import org.ngafid.core.accounts.*;
import org.ngafid.core.util.SendEmail;
import org.ngafid.www.ErrorResponse;
import org.ngafid.www.MustacheHandler;
import org.ngafid.www.Navbar;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

import org.apache.kafka.streams.processor.ConnectedStoreProvider;
import org.ngafid.routes.ErrorResponse;
import static org.ngafid.www.WebServer.gson;

public class AccountJavalinRoutes {
    private static final Logger LOG = Logger.getLogger(AccountJavalinRoutes.class.getName());

    private static class LoginResponse {
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

        public LoginResponse(boolean loggedOut, boolean waiting, boolean denied, boolean loggedIn, String message, User user) {
            this.loggedOut = loggedOut;
            this.waiting = waiting;
            this.loggedIn = loggedIn;
            this.denied = denied;
            this.message = message;
            this.user = user;
        }
    }

    private static class LogoutResponse {
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
    }

    private static class ForgotPasswordResponse {
        @JsonProperty
        String message;
        @JsonProperty
        boolean registeredEmail;

        public ForgotPasswordResponse(String message, boolean registeredEmail) {
            this.message = message;
            this.registeredEmail = registeredEmail;

        }
    }

    private static class CreatedAccount {
        @JsonProperty
        private final String accountType;
        @JsonProperty
        private final User user;

        public CreatedAccount(String accountType, User user) {
            this.accountType = accountType;
            this.user = user;
        }
    }

    private static class ResetSuccessResponse {
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

        public ResetSuccessResponse(boolean loggedOut, boolean waiting, boolean denied, boolean loggedIn, String message, User user) {
            this.loggedOut = loggedOut;
            this.waiting = waiting;
            this.loggedIn = loggedIn;
            this.denied = denied;
            this.message = message;
            this.user = user;
        }
    }

    private static class Profile {
        @JsonProperty
        private final User user;

        public Profile(User user) {
            this.user = user;
        }
    }

    private static void postLogin(Context ctx) {
        final String email = ctx.formParam("email");
        final String password = ctx.formParam("password");

        LOG.info("email: '" + email + "'");
        // don't print the password to the log!

        try (Connection connection = Database.getConnection()) {
            User user = User.get(connection, email, password);

            if (user == null) {
                LOG.info("Could not get user, get returned null.");

                ctx.json(new LoginResponse(true, false, false, false, "Invalid email or password.", null));
            } else {
                LOG.info("User authentication successful.");

                user.updateLastLoginTimeStamp(connection);

                ctx.sessionAttribute("user", user);
                if (user.getFleetAccessType().equals(FleetAccess.DENIED)) {
                    ctx.json(new LoginResponse(false, false, true, false, "Waiting!", user));
                } else if (user.getFleetAccessType().equals(FleetAccess.WAITING)) {
                    ctx.json(new LoginResponse(false, true, false, false, "Waiting!", user));
                } else {
                    ctx.json(new LoginResponse(false, false, false, true, "Success!", user));
                }
            }

        } catch (SQLException e) {
            LOG.severe(e.toString());
            e.printStackTrace();
            ctx.json(new ErrorResponse(e)).status(500);
        } catch (AccountException e) {
            ctx.json(new LoginResponse(true, false, false, false, "Incorrect email or password.", null));
        }
    }

    private static void postLogout(Context ctx) {
        User user = ctx.sessionAttribute("user");

        // Set the session attribute for this user so it will be considered logged in.
        ctx.sessionAttribute("user", null);
        ctx.req().getSession().invalidate();
        LOG.info("removed user '" + user.getEmail() + "' from the session.");

        ctx.json(new LogoutResponse(true, false, false, "Successfully logged out.", null));
    }

    private static void getCreateAccount(Context ctx) {
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

    private static void getForgotPassword(Context ctx) {
        final String templateFile = "forgot_password.html";
        Map<String, Object> scopes = new HashMap<String, Object>();

        LOG.info("template file: '" + templateFile + "'");

        ctx.header("Content-Type", "text/html; charset=UTF-8");
        ctx.render(templateFile, scopes);
    }

    private static void getResetPassword(Context ctx) {
        final String templateFile = "reset_password.html";
        Map<String, Object> scopes = new HashMap<String, Object>();

        LOG.info("template file: '" + templateFile + "'");

        ctx.header("Content-Type", "text/html; charset=UTF-8");
        ctx.render(templateFile, scopes);
    }

    private static void getUpdatePassword(Context ctx) {
        final String templateFile = "update_password.html";
        Map<String, Object> scopes = new HashMap<String, Object>();
        User user = ctx.sessionAttribute("user");

        LOG.info("template file: '" + templateFile + "'");

        scopes.put("navbar_js", Navbar.getJavascript(ctx));
        scopes.put("user_js", "var user = JSON.parse('" + gson.toJson(user) + "');");

        ctx.header("Content-Type", "text/html; charset=UTF-8");
        ctx.render(templateFile, scopes);
    }

    private static void getUpdateProfile(Context ctx) {
        final String templateFile = "update_profile.html";
        Map<String, Object> scopes = new HashMap<>();

        scopes.put("navbar_js", Navbar.getJavascript(ctx));

        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        scopes.put("user_js", "var user = JSON.parse('" + gson.toJson(user) + "');");

        ctx.header("Content-Type", "text/html; charset=UTF-8");
        ctx.render(templateFile, scopes);
    }

    private static void postForgotPassword(Context ctx) {
        try (Connection connection = Database.getConnection()) {
            final String email = ctx.formParam("email");
            if (User.exists(connection, email)) {
                LOG.info("User exists. Sending reset password email.");
                User.sendPasswordResetEmail(connection, email);
                ctx.json(new ForgotPasswordResponse("A password reset link has been sent to your registered email address. Please click on it to reset your password.", true));
            } else {
                LOG.info("User with email : " + email + " doesn't exist.");
                ctx.json(new ForgotPasswordResponse("User doesn't exist in database", false));
            }
        } catch (Exception e) {
            LOG.severe(e.toString());
            ctx.json(new ForgotPasswordResponse(e.toString(), false));
        }
    }


    private static void postCreateAccount(Context ctx) {
        final String email = ctx.formParam("email");
        final String password = ctx.formParam("password");
        final String firstName = ctx.formParam("firstName");
        final String lastName = ctx.formParam("lastName");
        final String country = ctx.formParam("country");
        final String state = ctx.formParam("state");
        final String city = ctx.formParam("city");
        final String address = ctx.formParam("address");
        final String phoneNumber = ctx.formParam("phoneNumber");
        final String zipCode = ctx.formParam("zipCode");
        final String accountType = ctx.formParam("accountType");

        try (Connection connection = Database.getConnection()) {
            if (accountType != null) {
                ctx.json(new ErrorResponse("Invalid Account Type", "A request was made to create an account with an unknown account type '" + accountType + "'."));
            }

            if (accountType != null && accountType.equals("gaard")) {
                ctx.json(new ErrorResponse("Gaard Account Creation Disabled", "We apologize but Gaard account creation is currently disabled as we transition to the beta version of the NGAFID 2.0."));
            } else if (accountType.equals("newFleet")) {
                final String fleetName = ctx.formParam("fleetName");
                User user = User.createNewFleetUser(connection, email, password, firstName, lastName, country, state, city, address, phoneNumber, zipCode, fleetName);
                ctx.sessionAttribute("user", user);

                ctx.json(new CreatedAccount(accountType, user));
            } else if (accountType.equals("existingFleet")) {
                final String fleetName = ctx.formParam("fleetName");
                final User user = User.createExistingFleetUser(connection, email, password, firstName, lastName, country, state, city, address, phoneNumber, zipCode, fleetName);
                ctx.sessionAttribute("user", user);

                ctx.json(new CreatedAccount(accountType, user));
            } else {
                ctx.json(new ErrorResponse("Invalid Account Type", "A request was made to create an account with an unknown account type '" + accountType + "'."));
            }
        } catch (SQLException e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e)).status(500);
        } catch (AccountException e) {
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    private static void getUserEmailPreferences(Context ctx) {
        final String handleFetchType = Objects.requireNonNull(ctx.pathParam("handleFetchType"));
        final User sessionUser = Objects.requireNonNull(ctx.sessionAttribute("user"));
        int fleetUserID = -1;

        if (handleFetchType.equals("HANDLE_FETCH_USER")) { // Fetching Session User...
            fleetUserID = sessionUser.getId();
        } else if (handleFetchType.equals("HANDLE_FETCH_MANAGER")) { // Fetching a Manager's Fleet User...

            fleetUserID = Integer.parseInt(Objects.requireNonNull(ctx.pathParam("fleetUserID")));
            int fleetID = Integer.parseInt(Objects.requireNonNull(ctx.pathParam("fleetID")));

            if (!sessionUser.managesFleet(fleetID)) {
                ctx.status(401);
                ctx.result("User did not have access to fetch user email preferences on this fleet.");
                return;
            }
        }

        try (Connection connection = Database.getConnection()) {
            ctx.json(User.getUserEmailPreferences(connection, fleetUserID));
        } catch (Exception se) {
            LOG.severe("Error in GetUserEmailPreferences.java");
            ctx.status(500);
            ctx.json(new ErrorResponse(se));
        }
    }

    private static void getUserPreferences(Context ctx) {
        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));

        try (Connection connection = Database.getConnection()) {
            ctx.json(User.getUserPreferences(connection, user.getId()));
        } catch (Exception se) {
            ctx.json(new ErrorResponse(se));
        }
    }

    private static void getUserPreferencesPage(Context ctx) {
        final String templateFile = "preferences_page.html";
        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));

        try (Connection connection = Database.getConnection()) {
            Fleet fleet = Objects.requireNonNull(Fleet.get(connection, user.getFleetId()));
            UserPreferences userPreferences = User.getUserPreferences(connection, user.getId());
            Map<String, Object> scopes = new HashMap<>();

            scopes.put("navbar_js", Navbar.getJavascript(ctx));
            scopes.put("user_name", "var userName = JSON.parse('" + gson.toJson(user.getFullName()) + "');\n");
            scopes.put("is_admin", "var isAdmin = JSON.parse('" + gson.toJson(user.isAdmin()) + "');\n");
            scopes.put("user_prefs_json", "var userPreferences = JSON.parse('" + gson.toJson(userPreferences) + "');\n");

            if (fleet.hasAirsync(connection)) {
                String timeout = AirSyncFleet.getTimeout(connection, fleet.getId());
                scopes.put("airsync", "var airsync_timeout = JSON.parse('" + gson.toJson(timeout) + "');\n");
            } else {
                scopes.put("airsync", "var airsync_timeout = -1;\n");
            }

            ctx.header("Content-Type", "text/html; charset=UTF-8");
            ctx.render(templateFile, scopes);
        } catch (Exception se) {
            se.printStackTrace();
        }
    }

    private static void postUserPreferences(Context ctx) {
        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        final int decimalPrecision = Integer.parseInt(Objects.requireNonNull(ctx.formParam("decimal_precision")));

        try (Connection connection = Database.getConnection()) {
            ctx.json(User.updateUserPreferencesPrecision(connection, user.getId(), decimalPrecision));
        } catch (Exception e) {
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    private static void postUserPreferencesMetric(Context ctx) {
        LOG.info("handling post user prefs route!");

        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        final int userId = user.getId();
        final String metric = Objects.requireNonNull(ctx.formParam("metricName"));
        final String type = Objects.requireNonNull(ctx.formParam("modificationType"));

        try (Connection connection = Database.getConnection()) {
            LOG.info("Modifying " + metric + " (" + type + ") for user: " + user);

            if (type.equals("addition")) {
                User.addUserPreferenceMetric(connection, userId, metric);
            } else {
                User.removeUserPreferenceMetric(connection, userId, metric);
            }

            ctx.json(User.getUserPreferences(connection, userId).getFlightMetrics());
        } catch (Exception e) {
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    private static void postResetPassword(Context ctx) {
        final String emailAddress = Objects.requireNonNull(ctx.formParam("emailAddress"));
        final String passphrase = Objects.requireNonNull(ctx.formParam("passphrase"));
        final String newPassword = Objects.requireNonNull(ctx.formParam("newPassword"));
        final String confirmPassword = Objects.requireNonNull(ctx.formParam("confirmPassword"));

        try (Connection connection = Database.getConnection()) {
            // 1. make sure the new password and confirm password are the same
            if (!newPassword.equals(confirmPassword)) {
                ctx.json(new ErrorResponse("Could not reset password.", "The server received different new and confirmation passwords."));
                return;
            }

            // 2. make sure the passphrase is valid
            if (!User.validatePassphrase(connection, emailAddress, passphrase)) {
                ctx.json(new ErrorResponse("Could not reset password.", "The passphrase provided was not correct."));
                return;
            }

            User.updatePassword(connection, emailAddress, newPassword);
            User user = User.get(connection, emailAddress, newPassword);

            ctx.sessionAttribute("user", user);
            ctx.json(new ResetSuccessResponse(false, false, false, true, "Success!", user));
        } catch (SQLException e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e)).status(500);
        } catch (AccountException e) {
            ctx.json(new ResetSuccessResponse(true, false, false, false, "Incorrect email or password.", null));
        }

    }

    private static void postSendUserInvite(Context ctx) {
        class InvitationSent {
            final String message = "Invitation Sent.";
        }

        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        final int fleetId = Integer.parseInt(Objects.requireNonNull(ctx.formParam("fleetId")));
        final String fleetName = Objects.requireNonNull(ctx.formParam("fleetName"));
        final String inviteEmail = Objects.requireNonNull(ctx.formParam("email"));

        //check to see if the logged-in user can invite users to this fleet
        if (!user.managesFleet(fleetId)) {
            LOG.severe("INVALID ACCESS: user did not have access to invite other users.");
            ctx.status(401);
            ctx.result("User did not have access to invite other users.");
        } else {
            List<String> recipient = new ArrayList<>();
            recipient.add(inviteEmail);

            final String encodedFleetName = URLEncoder.encode(fleetName, StandardCharsets.UTF_8);
            final String formattedInviteLink = "https://ngafid.org/create_account?fleet_name=" + encodedFleetName + "&email=" + inviteEmail;
            final String body = "<html><body>" + "<p>Hi,<p><br>" + "<p>A account creation invitation was sent to your account for fleet: " + fleetName + "<p>" + "<p>Please click the link below to create an account.<p>" + "<p> <a href=" + formattedInviteLink + ">Create Account</a></p><br>" + "</body></html>";

            List<String> bccRecipients = new ArrayList<>();
            try {
                SendEmail.sendEmail(recipient, bccRecipients, "NGAFID Account Creation Invite", body, EmailType.ACCOUNT_CREATION_INVITE);
            } catch (SQLException e) {
                LOG.severe(e.toString());
                ctx.json(new ErrorResponse(e)).status(500);
            }

            ctx.json(new InvitationSent());
        }
    }

    private static void postUpdatePassword(Context ctx) {
        final String currentPassword = Objects.requireNonNull(ctx.formParam("currentPassword"));
        final String newPassword = Objects.requireNonNull(ctx.formParam("newPassword"));
        final String confirmPassword = Objects.requireNonNull(ctx.formParam("confirmPassword"));

        User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        // 1. make sure currentPassword authenticates against what's in the database
        try (Connection connection = Database.getConnection()) {
            if (!user.validate(connection, currentPassword)) {
                ctx.json(new ErrorResponse("Could not update password.", "The current password was not correct."));
            }

            // 2. make sure the new password and confirm password are the same
            if (!newPassword.equals(confirmPassword)) {
                ctx.json(new ErrorResponse("Could not update password.", "The server received different new and confirmation passwords."));
            }

            // 3. make sure the new password is different from the old password
            if (currentPassword.equals(newPassword)) {
                ctx.json(new ErrorResponse("Could not update password.", "The current password was the same as the new password."));
            }

            user.updatePassword(connection, newPassword);

            ctx.json(new Profile(user));
        } catch (SQLException e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e)).status(500);
        }

    }

    private static void postUpdateProfile(Context ctx) {
        final String firstName = Objects.requireNonNull(ctx.formParam("firstName"));
        final String lastName = Objects.requireNonNull(ctx.formParam("lastName"));
        final String country = Objects.requireNonNull(ctx.formParam("country"));
        final String state = Objects.requireNonNull(ctx.formParam("state"));
        final String city = Objects.requireNonNull(ctx.formParam("city"));
        final String address = Objects.requireNonNull(ctx.formParam("address"));
        final String phoneNumber = Objects.requireNonNull(ctx.formParam("phoneNumber"));
        final String zipCode = Objects.requireNonNull(ctx.formParam("zipCode"));

        try (Connection connection = Database.getConnection()) {
            User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
            user.updateProfile(connection, firstName, lastName, country, state, city, address, phoneNumber, zipCode);
            ctx.json(new Profile(user));
        } catch (SQLException e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    private static void postUpdateUserAccess(Context ctx) {
        class UpdateUserAccess {
            final String message = "Success.";
        }

        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        final int fleetUserId = Integer.parseInt(Objects.requireNonNull(ctx.formParam("fleetUserId")));
        final int fleetId = Integer.parseInt(Objects.requireNonNull(ctx.formParam("fleetId")));
        final String accessType = Objects.requireNonNull(ctx.formParam("accessType"));

        // check to see if the logged-in user can update access to this fleet
        if (!user.managesFleet(fleetId)) {
            LOG.severe("INVALID ACCESS: user did not have access to modify user access rights on this fleet.");
            ctx.status(401);
            ctx.result("User did not have access to modify user access rights on this fleet.");
        } else {
            try (Connection connection = Database.getConnection()) {
                FleetAccess.update(connection, fleetUserId, fleetId, accessType);
                user.updateFleet(connection);
                ctx.json(new UpdateUserAccess());
            } catch (SQLException | AccountException e) {
                ctx.json(new ErrorResponse(e)).status(500);
            }
        }

    }

    @SuppressWarnings("ConvertToStringSwitch")
    private static void postUpdateUserEmailPreferences(Context ctx) {

        final User sessionUser = Objects.requireNonNull(ctx.sessionAttribute("user"));

        //Add user to / update the UserEmailPreferences map
        try (Connection connection = Database.getConnection()) {

            UserEmailPreferences.addUser(connection, sessionUser);

        } catch (SQLException e) {

            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e)).status(500);

        }

        //Log the raw handleUpdateType value
        final String handleUpdateType = Objects.requireNonNull(ctx.formParam("handleUpdateType"));

        //User Update...
        if (handleUpdateType.equals("HANDLE_UPDATE_USER")) {

            //Unpack Submission Data
            final int userID = sessionUser.getId();

            final HashSet<String> KEYS_EXCLUDE = new HashSet<>(Arrays.asList(
                "handleUpdateType"
            ));

            Map<String, Boolean> emailTypesUser = new HashMap<>();
            for (String emailKey : ctx.formParamMap().keySet()) {

                //Skip excluded keys
                if (KEYS_EXCLUDE.contains(emailKey))
                    continue;

                emailTypesUser.put(emailKey, Boolean.valueOf(ctx.formParam(emailKey)));

            }

            try (Connection connection = Database.getConnection()) {
                ctx.json(User.updateUserEmailPreferences(connection, userID, emailTypesUser));
            } catch (Exception e) {
                LOG.severe("Error in postUpdateUserEmailPreferences.java (User): " + e.toString());
                ctx.json(new ErrorResponse(e)).status(500);
            }

        //Manager Update...
        } else if (handleUpdateType.equals("HANDLE_UPDATE_MANAGER")) {

            //Unpack Submission Data
            int fleetUserID = Integer.parseInt(Objects.requireNonNull(ctx.formParam("fleetUserID")));
            int fleetID = Integer.parseInt(Objects.requireNonNull(ctx.formParam("fleetID")));

            final HashSet<String> KEYS_EXCLUDE = new HashSet<>(Arrays.asList(
                "fleetUserID",
                "fleetID",
                "handleUpdateType"
            ));

            HashMap<String, Boolean> emailTypesUser = new HashMap<>();
            for (String emailKey : ctx.formParamMap().keySet()) {

                //Skip excluded keys
                if (KEYS_EXCLUDE.contains(emailKey))
                    continue;

                emailTypesUser.put(emailKey, Boolean.valueOf(ctx.formParam(emailKey)));
                
            }

            //Check to see if the logged-in user can update access to this fleet
            if (!sessionUser.managesFleet(fleetID)) {

                LOG.severe("INVALID ACCESS: user did not have access to modify user email preferences on this fleet.");
                ctx.status(401);
                ctx.result("User did not have access to modify user email preferences on this fleet.");
                return;

            }

            try (Connection connection = Database.getConnection()) {
                ctx.json(User.updateUserEmailPreferences(connection, fleetUserID, emailTypesUser));
            } catch (Exception e) {
                LOG.severe("Error in postUpdateUserEmailPreferences.java (Manager): " + e.toString());
                ctx.json(new ErrorResponse(e)).status(500);
            }

        //Unknown Update...
        } else {

            //ERROR -- Unknown Update!
            LOG.severe("INVALID ACCESS: handleUpdateType not specified.");
            ctx.status(401);
            ctx.result("handleUpdateType not specified.");

        }

    }

    private static void getEmailUnsubscribe(Context ctx) {
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
        app.post("/login", AccountJavalinRoutes::postLogin);
        app.post("/logout", AccountJavalinRoutes::postLogout);

        app.get("/create_account", AccountJavalinRoutes::getCreateAccount);
        app.post("/create_account", AccountJavalinRoutes::postCreateAccount);

        app.get("/forgot_password", AccountJavalinRoutes::getForgotPassword);
        app.post("/forgot_password", AccountJavalinRoutes::postForgotPassword);

        app.get("/reset_password", AccountJavalinRoutes::getResetPassword);
        app.post("/reset_password", AccountJavalinRoutes::postResetPassword);

        app.get("/protected/update_password", AccountJavalinRoutes::getUpdatePassword);
        app.post("/protected/update_password", AccountJavalinRoutes::postUpdatePassword);

        app.get("/protected/update_profile", AccountJavalinRoutes::getUpdateProfile);
        app.post("/protected/update_profile", AccountJavalinRoutes::postUpdateProfile);

        app.post("/protected/send_user_invite", AccountJavalinRoutes::postSendUserInvite);
        app.post("/protected/update_user_access", AccountJavalinRoutes::postUpdateUserAccess);
        app.get("/protected/user_preference", AccountJavalinRoutes::getUserPreferences);

        app.get("/protected/email_preferences/{handleFetchType}", AccountJavalinRoutes::getUserEmailPreferences);
        app.get("/protected/email_preferences/{handleFetchType}/{fleetUserID}/{fleetID}",
                AccountJavalinRoutes::getUserEmailPreferences);
        app.get("/email_unsubscribe", AccountJavalinRoutes::getEmailUnsubscribe);
        app.after("/email_unsubscribe", ctx -> ctx.redirect("/"));

        app.get("/protected/preferences", AccountJavalinRoutes::getUserPreferencesPage);
        app.post("/protected/preferences", AccountJavalinRoutes::postUserPreferences);
        app.post("/protected/preferences_metric", AccountJavalinRoutes::postUserPreferencesMetric);
        app.post("/protected/update_email_preferences", AccountJavalinRoutes::postUpdateUserEmailPreferences);
    }
}
