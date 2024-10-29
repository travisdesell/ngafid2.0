package org.ngafid.routes.javalin;

import io.javalin.http.Context;
import org.ngafid.Database;
import org.ngafid.accounts.AccountException;
import org.ngafid.accounts.FleetAccess;
import org.ngafid.accounts.User;
import org.ngafid.routes.ErrorResponse;
import org.ngafid.routes.MustacheHandler;
import org.ngafid.routes.Navbar;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.ngafid.WebServer.gson;

public class AccountJavalinRoutes {
    private static final Logger LOG = Logger.getLogger(AccountJavalinRoutes.class.getName());

    private static class LoginResponse {
        private boolean loggedOut;
        private boolean waiting;
        private boolean denied;
        private boolean loggedIn;
        private String message;
        private User user;

        public LoginResponse(boolean loggedOut, boolean waiting, boolean denied, boolean loggedIn, String message,
                             User user) {
            this.loggedOut = loggedOut;
            this.waiting = waiting;
            this.loggedIn = loggedIn;
            this.denied = denied;
            this.message = message;
            this.user = user;
        }
    }

    private static class LogoutResponse {
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

    public static void postLogin(Context ctx) {
        final String email = ctx.queryParam("email");
        final String password = ctx.queryParam("password");

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
            ctx.json(new ErrorResponse(e));
        } catch (AccountException e) {
            ctx.json(new LoginResponse(true, false, false, false, "Incorrect email or password.", null));
        }
    }

    public static void postLogout(Context ctx) {
        User user = ctx.sessionAttribute("user");

        // Set the session attribute for this user so it will be considered logged in.
        ctx.sessionAttribute("user", null);
        LOG.info("removed user '" + user.getEmail() + "' from the session.");

        ctx.json(new LogoutResponse(true, false, false, "Successfully logged out.", null));
    }

    public static void getCreateAccount(Context ctx) throws IOException {
        final String templateFile = "create_account.html";
        HashMap<String, Object> scopes = new HashMap<String, Object>();

        LOG.severe("template file: '" + templateFile + "'");

        try {
            StringBuilder fleetnamesJavascript = new StringBuilder("var fleetNames = [");
            try (Connection connection = Database.getConnection()) {
                List<String> names = new ArrayList<String>();

                try (PreparedStatement query = connection.prepareStatement("SELECT fleet_name FROM fleet ORDER BY fleet_name");
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
                ctx.json(new ErrorResponse(e));
            }

            fleetnamesJavascript.append("];");

            scopes.put("fleetnames_js", fleetnamesJavascript);
            MustacheHandler.handle(templateFile, scopes);

            ctx.contentType("text/html");
            ctx.result(MustacheHandler.handle(templateFile, scopes));
        } catch (IOException e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e));
        }
    }

    public static void getForgotPassword(Context ctx) throws IOException {
        final String templateFile = "forgot_password.html";
        Map<String, Object> scopes = new HashMap<String, Object>();

        LOG.info("template file: '" + templateFile + "'");

        ctx.contentType("text/html");
        ctx.result(MustacheHandler.handle(templateFile, scopes));
    }

    public static void getResetPassword(Context ctx) throws IOException {
        final String templateFile = "reset_password.html";
        Map<String, Object> scopes = new HashMap<String, Object>();

        LOG.info("template file: '" + templateFile + "'");

        ctx.contentType("text/html");
        ctx.result(MustacheHandler.handle(templateFile, scopes));
    }

    public static void getUpdatePassword(Context ctx) throws IOException {
        final String templateFile = "update_password.html";
        Map<String, Object> scopes = new HashMap<String, Object>();
        User user = ctx.sessionAttribute("user");

        LOG.info("template file: '" + templateFile + "'");

        scopes.put("navbar_js", Navbar.getJavascript(ctx));
        scopes.put("user_js", "var user = JSON.parse('" + gson.toJson(user)  + "');");

        ctx.contentType("text/html");
        ctx.result(MustacheHandler.handle(templateFile, scopes));
    }
}
