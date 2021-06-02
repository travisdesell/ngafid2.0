package org.ngafid.routes;

import java.util.ArrayList;
import java.util.logging.Logger;

import java.sql.Connection;
import java.sql.SQLException;

import com.google.gson.Gson;

import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;
import spark.Spark;

import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.accounts.User;

import org.ngafid.events.EventDefinition;
import org.ngafid.flights.Flight;
import org.ngafid.filters.Filter;

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

        int fleetId = 0; //all events work on all fleets for now
        int eventId = Integer.parseInt(request.queryParams("eventId"));
        String eventName = request.queryParams("eventName");
        int startBuffer = Integer.parseInt(request.queryParams("startBuffer"));
        int stopBuffer = Integer.parseInt(request.queryParams("stopBuffer"));
        String airframe = request.queryParams("airframe");
        String filterJSON = request.queryParams("filterQuery");
        String severityColumnNamesJSON = request.queryParams("severityColumnNames");
        String severityType = request.queryParams("severityType");

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

        try {
            Connection connection = Database.getConnection();

            EventDefinition.update(connection, fleetId, eventId, eventName, startBuffer, stopBuffer, airframe, filterJSON, severityColumnNamesJSON, severityType);

            return "{}";
        } catch (SQLException e) {
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
