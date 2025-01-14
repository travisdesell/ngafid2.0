package org.ngafid.routes;

import com.google.gson.Gson;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;
import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.common.*;
import org.ngafid.flights.Flight;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Session;
import spark.Spark;

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
        Connection connection = Database.getConnection();

        final Session session = request.session();
        User user = session.attribute("user");

        int flightId = Integer.parseInt(request.queryParams("flight_id"));
        int tagId = Integer.parseInt(request.queryParams("tag_id"));
        boolean isPermanent = Boolean.parseBoolean(request.queryParams("permanent"));
        boolean allTags = Boolean.parseBoolean(request.queryParams("all"));


        try {
            FlightTag tag = Flight.getTag(connection, tagId);
            if(isPermanent) {
                LOG.info("Permanently deleting tag: " + tag.toString());
                Flight.deleteTag(tagId, connection);
            } else if(allTags) {
                LOG.info("Clearing all tags from flight " + flightId);
                Flight.unassociateAllTags(flightId, connection);

                return gson.toJson(new RemoveTagResponse());
            } else {
                Flight.unassociateTags(tagId, connection, flightId);
            }

            //check to see if the user has access to this data
            if (!user.hasFlightAccess(Database.getConnection(), flightId)) {
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
