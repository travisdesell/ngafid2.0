package org.ngafid.routes.spark;

import java.util.logging.Logger;

import java.sql.Connection;
import java.sql.SQLException;

import com.google.gson.Gson;

import org.ngafid.routes.ErrorResponse;
import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;
import spark.Spark;

import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.flights.Tails;

public class PostUpdateTail implements Route {
    private static final Logger LOG = Logger.getLogger(PostUpdateTail.class.getName());
    private Gson gson;

    public PostUpdateTail(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    private class UpdateTailResponse {
        private int fleetId;
        private String systemId;
        private String tail;
        private int confirmed;

        public UpdateTailResponse(int fleetId, String systemId, String tail, int confirmed) {
            this.fleetId = fleetId;
            this.systemId = systemId;
            this.tail = tail;
            this.confirmed = confirmed;
        }
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName());

        String systemId = request.queryParams("systemId");
        String tail = request.queryParams("tail");

        LOG.info("systemId: '" + systemId + "', tail: '" + tail + "'");

        try (Connection connection = Database.getConnection()) {
            final Session session = request.session();
            User user = session.attribute("user");
            int fleetId = user.getFleetId();

            // check to see if the user has upload access for this fleet.
            if (!user.hasUploadAccess(fleetId)) {
                LOG.severe("INVALID ACCESS: user did not have access view imports for this fleet.");
                Spark.halt(401, "User did not have access to view imports for this fleet.");
                return null;
            }

            Tails.updateTail(connection, fleetId, systemId, tail);

            return gson.toJson(new UpdateTailResponse(fleetId, systemId, tail, 1));

        } catch (SQLException e) {
            LOG.severe(e.toString());
            e.printStackTrace();
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
