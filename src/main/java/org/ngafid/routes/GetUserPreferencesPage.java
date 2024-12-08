
package org.ngafid.routes;

import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.logging.Logger;
import java.util.HashMap;

import java.sql.Connection;

import com.google.gson.Gson;

import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;

import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.accounts.AirSyncFleet;
import org.ngafid.accounts.Fleet;
import org.ngafid.accounts.User;
import org.ngafid.accounts.UserPreferences;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

public class GetUserPreferencesPage implements Route {
    private static final Logger LOG = Logger.getLogger(GetUserPreferencesPage.class.getName());
    private Gson gson;

    public GetUserPreferencesPage(Gson gson) {
        this.gson = gson;

        LOG.info("get " + this.getClass().getName() + " initalized");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        String resultString = "";
        String templateFile = WebServer.MUSTACHE_TEMPLATE_DIR + "preferences_page.html";
        LOG.severe("template file: '" + templateFile + "'");

        final Session session = request.session();
        User user = session.attribute("user");

        try (Connection connection = Database.getConnection()) {
            Fleet fleet = Fleet.get(connection, user.getFleetId());
            UserPreferences userPreferences = User.getUserPreferences(connection, user.getId());

            MustacheFactory mf = new DefaultMustacheFactory();
            Mustache mustache = mf.compile(templateFile);

            HashMap<String, Object> scopes = new HashMap<String, Object>();

            scopes.put("navbar_js", Navbar.getJavascript(request));
            scopes.put("user_name", "var userName = JSON.parse('" + gson.toJson(user.getFullName()) + "');\n");
            scopes.put("is_admin", "var isAdmin = JSON.parse('" + gson.toJson(user.isAdmin()) + "');\n");
            scopes.put("user_prefs_json",
                    "var userPreferences = JSON.parse('" + gson.toJson(userPreferences) + "');\n");

            if (fleet.hasAirsync(connection)) {
                String timeout = AirSyncFleet.getTimeout(connection, fleet.getId());
                scopes.put("airsync", "var airsync_timeout = JSON.parse('" + gson.toJson(timeout) + "');\n");
            } else {
                scopes.put("airsync", "var airsync_timeout = -1;\n");
            }

            StringWriter stringOut = new StringWriter();
            mustache.execute(new PrintWriter(stringOut), scopes).flush();
            resultString = stringOut.toString();

        } catch (Exception se) {
            se.printStackTrace();
        }

        return resultString;
    }
}
