package org.ngafid.routes.event_def_mgmt;

import com.google.gson.Gson;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Logger;

import org.ngafid.Database;
import org.ngafid.events.EventDefinition;
import org.ngafid.routes.ErrorResponse;
import spark.Request;
import spark.Response;
import spark.Route;

public class PutEventDefinitions implements Route {
    private static final Logger LOG = Logger.getLogger(PutEventDefinitions.class.getName());
    private Gson gson;

    public PutEventDefinitions(Gson gson) {
        this.gson = gson;

        LOG.info("put " + this.getClass().getName() + " initialized");

    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        LOG.info("Handling " + this.getClass().getName() + " route");

        Connection connection = Database.getConnection();

        int eventId = Integer.parseInt(request.queryParams("id"));
        int fleetId = Integer.parseInt(request.queryParams("fleetId"));
        int startBuffer = Integer.parseInt(request.queryParams("startBuffer"));
        int stopBuffer = Integer.parseInt(request.queryParams("stopBuffer"));

        String name = request.queryParams("name");
        String airframeName = request.queryParams("airframeName");
        String columnNamesJson = request.queryParams("columnNamesJson");
        String filterJson = request.queryParams("filterJson");
        String severityType = request.queryParams("severityType");

        EventDefinition.update(connection, fleetId, eventId, name, startBuffer, stopBuffer, airframeName, filterJson, columnNamesJson, severityType);

        return gson.toJson("Successfully updated event definition.");
    }
}
