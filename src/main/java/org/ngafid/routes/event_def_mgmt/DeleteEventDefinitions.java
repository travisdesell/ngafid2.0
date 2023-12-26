package org.ngafid.routes.event_def_mgmt;

import com.google.gson.Gson;
import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.routes.ErrorResponse;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Session;

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

        User user = request.session().attribute("user");
        if (!user.isAdmin()) {
            LOG.severe("INVALID ACCESS: user did not have admin access.");
            return gson.toJson(new ErrorResponse("Not Admin", "No permissions to delete event definitions."));
        }


        Connection connection = Database.getConnection();

        String query = "DELETE FROM event_definitions WHERE id=?";
        System.out.println("query: " + request.body());
        for (String params : request.queryParams()) {
            LOG.info("PARAM: " + params + " = " + request.queryParams(params));
        }
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, Integer.parseInt(request.queryParams("eventDefinitionID")));
            LOG.info(statement.toString());

            statement.executeUpdate();
            return gson.toJson("Successfully deleted event definition.");
        } catch (SQLException e) {
            e.printStackTrace();
            response.status(500);
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
