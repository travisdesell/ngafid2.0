package org.ngafid.routes.spark;

import java.util.logging.Logger;

import java.sql.Connection;
import java.sql.SQLException;

import com.google.gson.Gson;

import org.ngafid.routes.ErrorResponse;
import spark.Route;
import spark.Request;
import spark.Response;

import org.ngafid.Database;

import org.ngafid.events.EventDefinition;

public class PostUpdateEvent implements Route {
    private static final Logger LOG = Logger.getLogger(PostUpdateEvent.class.getName());
    private Gson gson;

    public PostUpdateEvent(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        int fleetId = 0; // all events work on all fleets for now
        int eventId = Integer.parseInt(request.formParams("eventId"));
        String eventName = request.formParams("eventName");
        int startBuffer = Integer.parseInt(request.formParams("startBuffer"));
        int stopBuffer = Integer.parseInt(request.formParams("stopBuffer"));
        String airframe = request.formParams("airframe");
        String filterJSON = request.formParams("filterQuery");
        String severityColumnNamesJSON = request.formParams("severityColumnNames");
        String severityType = request.formParams("severityType");

        LOG.info("eventId: " + eventId);
        LOG.info("eventName: " + eventName);
        LOG.info("startBuffer: " + startBuffer);
        LOG.info("stopBuffer: " + stopBuffer);
        LOG.info("airframe: " + airframe);
        LOG.info("filter JSON:");
        LOG.info(filterJSON);
        LOG.info("severityColumnNames JSON:");
        LOG.info(severityColumnNamesJSON);
        LOG.info("severity type: " + severityType);

        try (Connection connection = Database.getConnection()) {
            EventDefinition.update(connection, fleetId, eventId, eventName, startBuffer, stopBuffer, airframe,
                    filterJSON, severityColumnNamesJSON, severityType);

            return "{}";
        } catch (SQLException e) {
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
