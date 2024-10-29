package org.ngafid.routes.javalin;

import io.javalin.http.Context;
import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.events.EventDefinition;
import org.ngafid.routes.ErrorResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;
import java.util.logging.Logger;

import static org.ngafid.WebServer.gson;

public class EventJavalinRoutes {
    private static final Logger LOG = Logger.getLogger(EventJavalinRoutes.class.getName());

    public static void getAllEventDefinitions(Context ctx) throws IOException {
        try (Connection connection = Database.getConnection()) {
            ctx.json(EventDefinition.getAll(connection));
        } catch (SQLException e) {
            LOG.severe(e.toString());
            ctx.status(500);
            ctx.result(e.toString());
        }
    }

    public static void putEventDefinitions(Context ctx) throws IOException {
        EventDefinition updatedEvent = gson.fromJson(ctx.body(), EventDefinition.class);

        try (Connection connection = Database.getConnection()) {
            updatedEvent.updateSelf(connection);
        } catch (SQLException e) {
            ctx.status(500);
            ctx.json(new ErrorResponse(e));
        }
    }

    public static void deleteEventDefinitions(Context ctx) throws IOException {
        User user = ctx.sessionAttribute("user");
        if (user == null) {
            LOG.severe("INVALID ACCESS: user was not logged in.");
            ctx.status(401);
            ctx.json(new ErrorResponse("Not Logged In", "No user logged in."));
            return;
        }

        if (!user.isAdmin()) {
            LOG.severe("INVALID ACCESS: user did not have admin access.");
            ctx.json(new ErrorResponse("Not Admin", "No permissions to delete event definitions."));
        }

        String query = "DELETE FROM event_definitions WHERE id=?";
        try (Connection connection = Database.getConnection();
            PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, Integer.parseInt(Objects.requireNonNull(ctx.queryParam("eventDefinitionID"))));
            statement.executeUpdate();
            ctx.json("Successfully deleted event definition.");
        } catch (SQLException e) {
            ctx.status(500);
            ctx.json(new ErrorResponse(e));
        }
    }

    public static void getEventCreator(Context ctx) throws IOException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

}
