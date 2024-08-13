package org.ngafid.routes.event_def_mgmt;

import com.google.gson.Gson;
import org.ngafid.Database;
import org.ngafid.events.EventDefinition;
import org.ngafid.routes.ErrorResponse;
import spark.Request;
import spark.Response;
import spark.Route;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
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
        Connection connection = Database.getConnection();
        return gson.toJson(EventDefinition.getAll(connection));
    }
}