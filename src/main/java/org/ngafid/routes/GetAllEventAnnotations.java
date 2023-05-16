/**
 * Gets all of the event annotations
 *
 * @author <a href=mailto:apl1341@cs.rit.edu>Aidan LaBella</a>
 */
package org.ngafid.routes;

import java.util.logging.Logger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.gson.Gson;

import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;
import spark.Spark;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;

import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.accounts.User;
import org.ngafid.events.EventAnnotation;

public class GetAllEventAnnotations implements Route {
    private static final Logger LOG = Logger.getLogger(GetAllEventAnnotations.class.getName());
    private static Connection connection = Database.getConnection();
    private Gson gson;

    /**
     * Constructor
     *
     * @param gson the gson object for JSON conversions
     */
    public GetAllEventAnnotations(Gson gson) {
        this.gson = gson;

        LOG.info("GET " + this.getClass().getName() + " initalized");
    }

    /**
     * {inheritDoc}
     */
    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        String resultString = "";

        final Session session = request.session();
        User user = session.attribute("user");
        int fleetId = user.getFleetId();

        File templateFile = new File(WebServer.MUSTACHE_TEMPLATE_DIR + "all_event_annotations.html");

        if (!user.hasViewAccess(fleetId)) {
            LOG.severe("INVALID ACCESS: user did not have view access this fleet.");
            Spark.halt(401, "User did not have access to view acces for this fleet.");
            return null;
        }

        try {
            MustacheFactory mf = new DefaultMustacheFactory();
            Mustache mustache = mf.compile(templateFile.getPath());

            Map<String, Object> scopes = new HashMap<>();
            List<EventAnnotation> annotations = EventAnnotation.getDisplayedGroupAnnotations(user);

            scopes.put("navbar_js", Navbar.getJavascript(request));
            scopes.put("all_annotations", "var annotations = JSON.parse('" + gson.toJson(annotations) + "');\n");

            StringWriter stringOut = new StringWriter();
            mustache.execute(new PrintWriter(stringOut), scopes).flush();
            resultString = stringOut.toString();
        } catch (Exception e) {
            return gson.toJson(new ErrorResponse(e));
        }

        return resultString;
    }
}
