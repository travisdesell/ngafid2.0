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
import org.ngafid.flights.Flight;
import org.ngafid.common.*;

public class PostRemoveTag implements Route {
    private static final Logger LOG = Logger.getLogger(PostRemoveTag.class.getName());
    private Gson gson;

    public PostRemoveTag(Gson gson) {
        this.gson = gson;
        LOG.info("initialized " + this.getClass().getName() + " route!");
    }

    public class RemoveTagResponse {
        private boolean allTagsCleared;
        private FlightTag tag;

        public RemoveTagResponse() {
            this.tag = null;
            this.allTagsCleared = true;
        }

        public RemoveTagResponse(FlightTag tag) {
            this.tag = tag;
            this.allTagsCleared = false;
        }
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route!");

        final Session session = request.session();
        User user = session.attribute("user");

        int flightId = Integer.parseInt(request.formParams("flight_id"));
        int tagId = Integer.parseInt(request.formParams("tag_id"));
        boolean isPermanent = Boolean.parseBoolean(request.formParams("permanent"));
        boolean allTags = Boolean.parseBoolean(request.formParams("all"));

        try (Connection connection = Database.getConnection()) {

            FlightTag tag = Flight.getTag(connection, tagId);
            if (isPermanent) {
                LOG.info("Permanently deleting tag: " + tag.toString());
                Flight.deleteTag(tagId, connection);
            } else if (allTags) {
                LOG.info("Clearing all tags from flight " + flightId);
                Flight.disassociateAllTags(flightId, connection);

                return gson.toJson(new RemoveTagResponse());
            } else {
                Flight.disassociateTags(tagId, connection, flightId);
            }

            // check to see if the user has access to this data
            if (!user.hasFlightAccess(connection, flightId)) {
                LOG.severe("INVALID ACCESS: user did not have access to this flight.");
                Spark.halt(401, "User did not have access to this flight.");
            }

            return gson.toJson(new RemoveTagResponse(tag));
        } catch (SQLException e) {
            System.err.println("Error in SQL ");
            e.printStackTrace();
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
