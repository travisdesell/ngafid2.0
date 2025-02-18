package org.ngafid.routes.javalin;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.ngafid.common.Database;
import org.ngafid.accounts.User;
import org.ngafid.common.FlightTag;
import org.ngafid.common.filters.StoredFilter;
import org.ngafid.flights.Flight;
import org.ngafid.routes.ErrorResponse;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public class TagFilterJavalinRoutes {
    private static final Logger LOG = Logger.getLogger(TagFilterJavalinRoutes.class.getName());

    private static class RemoveTagResponse {
        @JsonProperty
        private final boolean allTagsCleared;
        @JsonProperty
        private final FlightTag tag;

        public RemoveTagResponse() {
            this.tag = null;
            this.allTagsCleared = true;
        }

        public RemoveTagResponse(FlightTag tag) {
            this.tag = tag;
            this.allTagsCleared = false;
        }
    }

    private static void postCreateTag(Context ctx) {
        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        final String name = Objects.requireNonNull(ctx.formParam("name"));
        final String description = Objects.requireNonNull(ctx.formParam("description"));
        final String color = Objects.requireNonNull(ctx.formParam("color"));
        final int flightId = Integer.parseInt(Objects.requireNonNull(ctx.formParam("id")));
        final int fleetId = user.getFleetId();

        try (Connection connection = Database.getConnection()) {

            if (Flight.tagExists(connection, fleetId, name)) {
                ctx.json("ALREADY_EXISTS");
            }

            ctx.json(Flight.createTag(fleetId, flightId, name, description, color, connection));
        } catch (SQLException e) {
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    private static void postEditTag(Context ctx) {
        final String name = Objects.requireNonNull(ctx.formParam("name"));
        final String description = Objects.requireNonNull(ctx.formParam("description"));
        final String color = Objects.requireNonNull(ctx.formParam("color"));
        final int tagId = Integer.parseInt(Objects.requireNonNull(ctx.formParam("tag_id")));
        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));


        try (Connection connection = Database.getConnection()) {
            FlightTag flightTag = new FlightTag(tagId, user.getFleetId(), name, description, color);

            FlightTag currentTag = Flight.getTag(connection, tagId);

            LOG.info("currentTag: " + currentTag + " edited tag: " + flightTag);

            if (flightTag.equals(currentTag)) {
                LOG.info("No change detected in the tag.");
                ctx.json("NOCHANGE");
            }

            ctx.json(Objects.requireNonNull(Flight.editTag(connection, flightTag)));
        } catch (SQLException e) {
            System.err.println("Error in SQL ");
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    private static void postRemoveTag(Context ctx) {
        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        final int flightId = Integer.parseInt(Objects.requireNonNull(ctx.formParam("flight_id")));
        final int tagId = Integer.parseInt(Objects.requireNonNull(ctx.formParam("tag_id")));
        boolean isPermanent = Boolean.parseBoolean(ctx.formParam("permanent"));
        boolean allTags = Boolean.parseBoolean(ctx.formParam("all"));

        try (Connection connection = Database.getConnection()) {
            FlightTag tag = Flight.getTag(connection, tagId);
            if (isPermanent) {
                LOG.info("Permanently deleting tag: " + tag);
                Flight.deleteTag(tagId, connection);
            } else if (allTags) {
                LOG.info("Clearing all tags from flight " + flightId);
                Flight.disassociateAllTags(flightId, connection);

                ctx.json(new RemoveTagResponse());
            } else {
                Flight.disassociateTags(tagId, connection, flightId);
            }

            // check to see if the user has access to this data
            if (!user.hasFlightAccess(connection, flightId)) {
                LOG.severe("INVALID ACCESS: user did not have access to this flight.");
                ctx.status(401);
                ctx.result("User did not have access to this flight.");
            }

            ctx.json(new RemoveTagResponse(tag));
        } catch (SQLException e) {
            System.err.println("Error in SQL ");

            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    private static void postTags(Context ctx) {
        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        final int flightId = Integer.parseInt(Objects.requireNonNull(ctx.formParam("flightId")));
        System.out.println("TAGGED FLTID: " + flightId);

        try (Connection connection = Database.getConnection()) {
            // check to see if the user has access to this data
            if (!user.hasFlightAccess(connection, flightId)) {
                LOG.severe("INVALID ACCESS: user did not have access to this flight.");
                ctx.status(401);
                ctx.result("User did not have access to this flight.");
            }

            List<FlightTag> tags = Flight.getTags(connection, flightId);
            ctx.json(Objects.requireNonNullElseGet(tags, () -> new ErrorResponse("error", "No tags found for flight.")));
        } catch (SQLException e) {
            System.err.println("Error in SQL ");

            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    private static void postUnassociatedTags(Context ctx) {
        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        final int fleetId = user.getFleetId();
        final int flightId = Integer.parseInt(Objects.requireNonNull(ctx.formParam("id")));

        try (Connection connection = Database.getConnection()) {

            List<FlightTag> tags = Flight.getUnassociatedTags(connection, flightId, fleetId);

            // check to see if the user has access to this data
            if (!user.hasFlightAccess(connection, flightId)) {
                LOG.severe("INVALID ACCESS: user did not have access to this flight.");
                ctx.status(401);
                ctx.result("User did not have access to this flight.");
            }

            ctx.json(tags);
        } catch (SQLException e) {

            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    private static void postAssociateTag(Context ctx) {
        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        final int flightId = Integer.parseInt(Objects.requireNonNull(ctx.formParam("id")));
        final int tagId = Integer.parseInt(Objects.requireNonNull(ctx.formParam("tag_id")));

        try (Connection connection = Database.getConnection()) {
            if (!user.hasFlightAccess(connection, flightId)) {
                ctx.status(401);
                ctx.result("User did not have access to this flight.");
            }

            ctx.json(Objects.requireNonNull(Flight.getTag(connection, tagId)));
        } catch (SQLException e) {

            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    private static void getStoredFilters(Context ctx) {
        try (Connection connection = Database.getConnection()) {
            final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
            final List<StoredFilter> filters = StoredFilter.getStoredFilters(connection, user.getFleetId());
            ctx.json(filters);
        } catch (SQLException e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    private static void postStoreFilter(Context ctx) {
        try (Connection connection = Database.getConnection()) {
            final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
            final String name = Objects.requireNonNull(ctx.formParam("name"));
            final String filterJSON = Objects.requireNonNull(ctx.formParam("filterJSON"));
            final String color = Objects.requireNonNull(ctx.formParam("color"));
            final int fleetId = user.getFleetId();

            StoredFilter.storeFilter(connection, fleetId, filterJSON, name, color);
            ctx.json("SUCCESS");
        } catch (SQLIntegrityConstraintViolationException se) {
            LOG.info("DUPLICATE_PK detected: " + se);
            ctx.json("DUPLICATE_PK");
        } catch (SQLException e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    private static void postModifyFilter(Context ctx) {
        final String currentName = Objects.requireNonNull(ctx.formParam("currentName"));
        final String newName = Objects.requireNonNull(ctx.formParam("newName"));
        final String filterJSON = Objects.requireNonNull(ctx.formParam("filterJSON"));
        final String color = Objects.requireNonNull(ctx.formParam("color"));
        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        final int fleetId = user.getFleetId();

        try (Connection connection = Database.getConnection()) {
            StoredFilter.modifyFilter(connection, fleetId, filterJSON, currentName, newName, color);
            ctx.json("SUCCESS");
        } catch (SQLIntegrityConstraintViolationException se) {
            LOG.info("DUPLICATE_PK detected: " + se);
            ctx.json("DUPLICATE_PK");
        } catch (SQLException e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }


    private static void postRemoveFilter(Context ctx) {
        try (Connection connection = Database.getConnection()) {
            final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
            final String name = Objects.requireNonNull(ctx.formParam("name"));
            final int fleetId = user.getFleetId();

            StoredFilter.removeFilter(connection, fleetId, name);

            ctx.json(StoredFilter.getStoredFilters(connection, fleetId));
        } catch (SQLException e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    public static void bindRoutes(Javalin app) {
        app.post("/protected/flight_tags", TagFilterJavalinRoutes::postTags);
        app.post("/protected/create_tag", TagFilterJavalinRoutes::postCreateTag);
        app.post("/protected/get_unassociated_tags", TagFilterJavalinRoutes::postUnassociatedTags);
        app.post("/protected/associate_tag", TagFilterJavalinRoutes::postAssociateTag);
        app.post("/protected/remove_tag", TagFilterJavalinRoutes::postRemoveTag);
        app.post("/protected/edit_tag", TagFilterJavalinRoutes::postEditTag);

        app.get("/protected/stored_filters", TagFilterJavalinRoutes::getStoredFilters);
        app.post("/protected/store_filter", TagFilterJavalinRoutes::postStoreFilter);
        app.post("/protected/remove_filter", TagFilterJavalinRoutes::postRemoveFilter);
        app.post("/protected/modify_filter", TagFilterJavalinRoutes::postModifyFilter);
    }
}
