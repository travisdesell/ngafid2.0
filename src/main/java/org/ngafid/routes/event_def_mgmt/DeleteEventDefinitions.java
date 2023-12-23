package org.ngafid.routes.event_def_mgmt;

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

public class DeleteEventDefinitions implements Route {
    private static final Logger LOG = Logger.getLogger(DeleteEventDefinitions.class.getName());
    private Gson gson;

    public DeleteEventDefinitions(Gson gson) {
        this.gson = gson;

        LOG.info("delete " + this.getClass().getName() + " initialized");
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        LOG.info("Handling " + this.getClass().getName() + " route");

        Connection connection = Database.getConnection();

        String query = "DELETE FROM event_definitions WHERE id=?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, Integer.parseInt(request.queryParams("id")));
            LOG.info(statement.toString());

            statement.executeUpdate();
            return gson.toJson("Successfully deleted event definition.");
        } catch (SQLException e) {
            e.printStackTrace();
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
