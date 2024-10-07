package org.ngafid.routes.event_def_mgmt;

import com.google.gson.Gson;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Logger;

import com.google.gson.JsonObject;
import org.ngafid.Database;
import org.ngafid.events.EventDefinition;
import org.ngafid.flights.Airframes;
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
    public Object handle(Request request, Response response) {
        LOG.info("Handling " + this.getClass().getName() + " route");

        // JSON parse
        LOG.info("request.body(): " + request.body());
        EventDefinition updatedEvent = gson.fromJson(request.body(), EventDefinition.class);
        LOG.info(updatedEvent.toString());

        try (Connection connection = Database.getConnection()) {
            updatedEvent.updateSelf(connection);
        } catch (SQLException e) {
            response.status(500);
            return gson.toJson(new ErrorResponse(e));
        }

        return gson.toJson("Successfully updated event definition.");
    }
}
