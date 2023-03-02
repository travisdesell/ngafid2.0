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
public class PostEventAnnotationNotes implements Route {
    private static final Logger LOG = Logger.getLogger(PostEventAnnotationNotes.class.getName());
    private Gson gson;
    private static Connection connection = Database.getConnection();

    private static final String ERROR = "ERROR";
    private static final String SUCCESS = "SUCCESS";

    public PostEventAnnotationNotes(Gson gson) {
        this.gson = gson;

        LOG.info("POST " + this.getClass().getName() + " initalized");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling event annotation");

        final Session session = request.session();
        User user = session.attribute("user");

        String notes = request.queryParams("notes");
        int eventId = Integer.parseInt(request.queryParams("eventId"));

        try {
            EventAnnotation eventAnnotation = EventAnnotation.getEventAnnotation(user, eventId);
            eventAnnotation.updateNotes(notes);
        } catch (SQLException se) {
            return gson.toJson(ERROR);
        }

        return gson.toJson(SUCCESS);
    }
}

