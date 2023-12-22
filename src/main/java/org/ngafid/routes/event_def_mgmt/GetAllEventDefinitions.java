package org.ngafid.routes.event_def_mgmt;

import com.google.gson.Gson;
import org.ngafid.Database;
import org.ngafid.routes.ErrorResponse;
import spark.Request;
import spark.Response;
import spark.Route;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

public class GetAllEventDefinitions implements Route {
    private static final Logger LOG = Logger.getLogger(GetAllEventDefinitions.class.getName());
    private Gson gson;

    public GetAllEventDefinitions(Gson gson) {
        this.gson = gson;
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        LOG.info("Handling " + this.getClass().getName() + " route");

        Connection connection = Database.getConnection();

        String query = "SELECT * FROM event_definitions";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            ResultSet results = statement.executeQuery();
            LOG.info(statement.toString());

            return gson.toJson(results);
        } catch (SQLException e) {
            e.printStackTrace();
            return gson.toJson(new ErrorResponse(e));
        }
    }
}