package org.ngafid.routes.EventDefManagement;

import com.google.gson.Gson;
import org.ngafid.Database;
import org.ngafid.routes.ErrorResponse;
import spark.Request;
import spark.Response;
import spark.Route;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Logger;

public class PostEventDefinitions implements Route {
    private static final Logger LOG = Logger.getLogger(PostEventDefinitions.class.getName());
    private Gson gson;

    public PostEventDefinitions(Gson gson) {
        this.gson = gson;
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        LOG.info("Handling " + this.getClass().getName() + " route");

        Connection connection = Database.getConnection();

        String query = "INSERT INTO event_definitions (fleet_id, airframe_id, name, start_buffer, stop_buffer, column_names, condition_json, severity_column_names, severity_type, color) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, Integer.parseInt(request.queryParams("fleetId")));
            statement.setInt(2, Integer.parseInt(request.queryParams("airframeId")));
            statement.setString(3, request.queryParams("name"));
            statement.setInt(4, Integer.parseInt(request.queryParams("startBuffer")));
            statement.setInt(5, Integer.parseInt(request.queryParams("stopBuffer")));
            statement.setString(6, request.queryParams("columnNames"));
            statement.setString(7, request.queryParams("conditionJson"));
            statement.setString(8, request.queryParams("severityColumnNames"));
            statement.setString(9, request.queryParams("severityType"));
            statement.setString(10, request.queryParams("color"));


            LOG.info(statement.toString());

            statement.executeUpdate();
            return gson.toJson("Successfully updated event definition.");
        } catch (SQLException e) {
            e.printStackTrace();
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
