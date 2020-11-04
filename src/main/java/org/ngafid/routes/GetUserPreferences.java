
package org.ngafid.routes;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;


import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.HashMap;

import java.sql.Connection;
import java.sql.SQLException;

import com.google.gson.Gson;

import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;

import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.accounts.User;

import org.ngafid.flights.Tail;
import org.ngafid.flights.Tails;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;


import org.ngafid.events.EventStatistics;

public class GetUserPreferences implements Route {
    private static final Logger LOG = Logger.getLogger(GetSystemIds.class.getName());
    private Gson gson;

    public GetSystemIds(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        String resultString = "";
        String templateFile = WebServer.MUSTACHE_TEMPLATE_DIR + "preferences_page.html";
        LOG.severe("template file: '" + templateFile + "'");

        final Session session = request.session();
        User user = session.attribute("user");
        int fleetId = user.getFleetId();

        try  {
            MustacheFactory mf = new DefaultMustacheFactory();
            Mustache mustache = mf.compile(templateFile);

            Connection connection = Database.getConnection();


            scopes.put("navbar_js", Navbar.getJavascript(request));

            long startTime = System.currentTimeMillis();
            scopes.put("system_ids_js",
                    "var systemIds = JSON.parse('" + gson.toJson(tailInfo) + "');\n"
                    );
            long endTime = System.currentTimeMillis();
            LOG.info("converting event statistics to JSON took " + (endTime-startTime) + "ms.");

            StringWriter stringOut = new StringWriter();
            mustache.execute(new PrintWriter(stringOut), scopes).flush();
            resultString = stringOut.toString();

        } catch (SQLException e) {
            LOG.severe(e.toString());
            return gson.toJson(new ErrorResponse(e));

        } catch (IOException e) {
            LOG.severe(e.toString());
        }

        return resultString;
    }
}
