
package org.ngafid.routes;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

import com.google.gson.Gson;

import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;
import spark.Spark;

import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.events.EventAnnotation;
import org.ngafid.accounts.UserPreferences;
import org.ngafid.flights.DoubleTimeSeries;

/**
 * This class posts the LOCI metrics data for the map flight metric viewer.
 *
 * @author <a href=mailto:apl1341@cs.rit.edu>Aidan LaBella @ RIT CS</a>
 */
public class PostEventAnnotation implements Route {
    private static final Logger LOG = Logger.getLogger(PostEventAnnotation.class.getName());
    private Gson gson;
    private static Connection connection = Database.getConnection();

    public static String OK_RESPONSE = "OK";
    public static String ALREADY_EXISTS = "ALREADY_EXISTS";
    public static String INVALID_PERMISSION = "INVALID_PERMISSION";

    public PostEventAnnotation(Gson gson) {
        this.gson = gson;

        LOG.info("POST " + this.getClass().getName() + " initalized");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling event annotation");

        final Session session = request.session();
        User user = session.attribute("user");

        String className = request.queryParams("className");
        String notes = request.queryParams("notes");
        int eventId = Integer.parseInt(request.queryParams("eventId"));

        //used to indicate that the user wants to change their annotation after being prompted with a message modal
        boolean override = Boolean.parseBoolean(request.queryParams("override"));

        try {
            EventAnnotation annotation = new EventAnnotation(eventId, className, user, LocalDateTime.now(), notes);

            if (!annotation.userIsAdmin()) {
                return gson.toJson(INVALID_PERMISSION);
            }

            if (annotation.alreadyExists()) {
                if (!override) {
                    return gson.toJson(ALREADY_EXISTS);
                } else {
                    annotation.changeAnnotation(LocalDateTime.now());
                    return gson.toJson(OK_RESPONSE);
                }
            } 

            if (annotation.updateDatabase()) {
                return gson.toJson(OK_RESPONSE);
            }

            return gson.toJson(ALREADY_EXISTS);
        } catch (Exception e) {
            e.printStackTrace();
            return gson.toJson(new ErrorResponse(e));
        }
    }
}

