/**
 * Gets event annotations
 * @author <a href=mailto:apl1341@cs.rit.edu>Aidan LaBella</a>
 */
package org.ngafid.routes;

import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;

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

        final Session session = request.session();
        User user = session.attribute("user");
        int fleetId = user.getFleetId();

        int eventId = Integer.parseInt(request.queryParams("eventId"));

        if (!user.hasViewAccess(fleetId)) {
            LOG.severe("INVALID ACCESS: user did not have view access this fleet.");
            Spark.halt(401, "User did not have access to view acces for this fleet.");
            return null;
        }

        LOG.info("Testing -------------------------");

        try {
            List<Annotation> annotations = EventAnnotation.getAnnotationsByEvent(eventId, user.getId());
            LOG.info("Annotations: " + annotations);

            return gson.toJson(annotations);
        } catch(SQLException e) {
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
