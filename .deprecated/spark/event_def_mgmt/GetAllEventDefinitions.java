package org.ngafid.routes.spark.event_def_mgmt;

import com.google.gson.Gson;
import org.ngafid.Database;
import org.ngafid.events.EventDefinition;
import spark.Request;
import spark.Response;
import spark.Route;

import java.sql.Connection;
import java.util.logging.Logger;

public class GetAllEventDefinitions implements Route {
    private static final Logger LOG = Logger.getLogger(GetAllEventDefinitions.class.getName());
    private Gson gson;

    public GetAllEventDefinitions(Gson gson) {
        this.gson = gson;

        LOG.info("get " + this.getClass().getName() + " initialized");
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        LOG.info("Handling " + this.getClass().getName() + " route");
        try (Connection connection = Database.getConnection()) {
            return gson.toJson(EventDefinition.getAll(connection));
        }
    }
}
