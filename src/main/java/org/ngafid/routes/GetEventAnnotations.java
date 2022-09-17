/**
 * Gets event annotations
 *
 * @author <a href=mailto:apl1341@cs.rit.edu>Aidan LaBella</a>
 */
package org.ngafid.routes;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.gson.Gson;

import org.ngafid.WebServer;
import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;
import spark.Spark;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.events.Annotation;
import org.ngafid.events.EventAnnotation;
import org.ngafid.flights.Flight;

public class GetEventAnnotations implements Route {
    private static final Logger LOG = Logger.getLogger(GetEventAnnotations.class.getName());
    private static Connection connection = Database.getConnection();
    private Gson gson;

    /**
     * Constructor
     * @param gson the gson object for JSON conversions
     */
    public GetEventAnnotations(Gson gson) {
        this.gson = gson;

        LOG.info("GET " + this.getClass().getName() + " initalized");
    }

    /**
     * {inheritDoc}
     */
    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");
        String templateFile = WebServer.MUSTACHE_TEMPLATE_DIR + "all_event_annotations.html";
        LOG.severe("Template Dir: " + WebServer.MUSTACHE_TEMPLATE_DIR);
        LOG.severe("Template File: '" + templateFile + "'");

        final Session session = request.session();
        User user = session.attribute("user");
        int fleetId = user.getFleetId();

        int eventId = Integer.parseInt(request.queryParams("eventId"));

        if (!user.hasViewAccess(fleetId)) {
            LOG.severe("INVALID ACCESS: user did not have view access this fleet.");
            Spark.halt(401, "User did not have access to view acces for this fleet.");
            return null;
        }

        try {
            MustacheFactory mf = new DefaultMustacheFactory();
            Mustache mustache = mf.compile(templateFile);

            HashMap<String, Object> scopes = new HashMap<>();

            scopes.put("navbar_js", Navbar.getJavascript(request));

            List<Annotation> annotations = EventAnnotation.getAnnotationsByEvent(eventId, user.getId());
            LOG.info("Annotations: " + annotations);

            StringWriter stringOut = new StringWriter();
            mustache.execute(new PrintWriter(stringOut), scopes).flush();

            return gson.toJson(annotations);
        } catch (SQLException | IOException e) {
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
