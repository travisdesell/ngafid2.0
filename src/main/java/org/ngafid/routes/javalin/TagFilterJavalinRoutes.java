package org.ngafid.routes.javalin;

import io.javalin.http.Context;
import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.common.FlightTag;
import org.ngafid.filters.StoredFilter;
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

    public static class RemoveTagResponse {
        private final boolean allTagsCleared;
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

    public static void postCreateTag(Context ctx) {
        User user = ctx.sessionAttribute("user");
        if (user == null) {
            ctx.json(new ErrorResponse("error", "User not logged in."));
            return;
        }

        String name = ctx.queryParam("name");
        String description = ctx.queryParam("description");
        String color = ctx.queryParam("color");
        int flightId = Integer.parseInt(Objects.requireNonNull(ctx.queryParam("id")));
        int fleetId = user.getFleetId();

        try (Connection connection = Database.getConnection()) {

            if (Flight.tagExists(connection, fleetId, name)) {
                ctx.json("ALREADY_EXISTS");
            }

            ctx.json(Flight.createTag(fleetId, flightId, name, description, color, connection));
        } catch (SQLException e) {
            ctx.json(new ErrorResponse(e));
        }
    }

    public static void postEditTag(Context ctx) {
        String name = ctx.queryParam("name");
        String description = ctx.queryParam("description");
        String color = ctx.queryParam("color");
        int tagId = Integer.parseInt(Objects.requireNonNull(ctx.queryParam("tag_id")));
        User user = ctx.sessionAttribute("user");
        if (user == null) {
            ctx.json(new ErrorResponse("error", "User not logged in."));
            return;
        }

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
            ctx.json(new ErrorResponse(e));
        }
    }

    public static void postRemoveTag(Context ctx) {
        User user = ctx.sessionAttribute("user");
        if (user == null) {
            ctx.json(new ErrorResponse("error", "User not logged in."));
            return;
        }

        int flightId = Integer.parseInt(Objects.requireNonNull(ctx.queryParam("flight_id")));
        int tagId = Integer.parseInt(Objects.requireNonNull(ctx.queryParam("tag_id")));
        boolean isPermanent = Boolean.parseBoolean(ctx.queryParam("permanent"));
        boolean allTags = Boolean.parseBoolean(ctx.queryParam("all"));

        try (Connection connection = Database.getConnection()) {
            FlightTag tag = Flight.getTag(connection, tagId);
            if (isPermanent) {
                LOG.info("Permanently deleting tag: " + tag.toString());
                Flight.deleteTag(tagId, connection);
            } else if (allTags) {
                LOG.info("Clearing all tags from flight " + flightId);
                Flight.unassociateAllTags(flightId, connection);

                ctx.json(new RemoveTagResponse());
            } else {
                Flight.unassociateTags(tagId, connection, flightId);
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

            ctx.json(new ErrorResponse(e));
        }
    }

    public static void postTags(Context ctx) {
        final User user = ctx.sessionAttribute("user");
        if (user == null) {
            ctx.json(new ErrorResponse("error", "User not logged in."));
            return;
        }

        final int flightId = Integer.parseInt(Objects.requireNonNull(ctx.queryParam("flightId")));
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

            ctx.json(new ErrorResponse(e));
        }
    }

    public static void postUnassociatedTags(Context ctx) {
        User user = ctx.sessionAttribute("user");
        if (user == null) {
            ctx.json(new ErrorResponse("error", "User not logged in."));
            return;
        }

        int fleetId = user.getFleetId();
        int flightId = Integer.parseInt(Objects.requireNonNull(ctx.queryParam("id")));

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

            ctx.json(new ErrorResponse(e));
        }
    }

    public static void postAssociateTag(Context ctx) {
        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        final int flightId = Integer.parseInt(Objects.requireNonNull(ctx.queryParam("id")));
        final int tagId = Integer.parseInt(Objects.requireNonNull(ctx.queryParam("tag_id")));

        try (Connection connection = Database.getConnection()) {
            if (!user.hasFlightAccess(connection, flightId)) {
                ctx.status(401);
                ctx.result("User did not have access to this flight.");
            }

            ctx.json(Objects.requireNonNull(Flight.getTag(connection, tagId)));
        } catch (SQLException e) {

            ctx.json(new ErrorResponse(e));
        }
    }

    public static void getStoredFilters(Context ctx) {
        try (Connection connection = Database.getConnection()) {
            final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
            final List<StoredFilter> filters = StoredFilter.getStoredFilters(connection, fleetId);
            ctx.json(filters);
        } catch (SQLException e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e));
        }
    }

    public static void postStoreFilter(Context ctx) {
        try (Connection connection = Database.getConnection()) {
            final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
            final String name = Objects.requireNonNull(ctx.queryParam("name"));
            final String filterJSON = Objects.requireNonNull(ctx.queryParam("filterJSON"));
            final String color = Objects.requireNonNull(ctx.queryParam("color"));
            final int fleetId = user.getFleetId();

            StoredFilter.storeFilter(connection, fleetId, filterJSON, name, color);
            ctx.json("SUCCESS");
        } catch (SQLIntegrityConstraintViolationException se) {
            LOG.info("DUPLICATE_PK detected: " + se);
            ctx.json("DUPLICATE_PK");
        } catch (SQLException e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e));
        }
    }

    public static void postModifyFilter(Context ctx) {
        try (Connection connection = Database.getConnection()) {
            final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
            final int fleetId = user.getFleetId();
            final String currentName = Objects.requireNonNull(ctx.queryParam("currentName"));
            final String newName = Objects.requireNonNull(ctx.queryParam("newName"));
            final String filterJSON = Objects.requireNonNull(ctx.queryParam("filterJSON"));
            final String color = Objects.requireNonNull(ctx.queryParam("color"));

            StoredFilter.modifyFilter(connection, fleetId, filterJSON, currentName, newName, color);
            ctx.json("SUCCESS");
        } catch (SQLIntegrityConstraintViolationException se) {
            LOG.info("DUPLICATE_PK detected: " + se);
            ctx.json("DUPLICATE_PK");
        } catch (SQLException e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e));
        }
    }


    public static void postRemoveFilter(Context ctx) {
        try (Connection connection = Database.getConnection()) {
            final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
            final String name = Objects.requireNonNull(ctx.queryParam("name"));
            final int fleetId = user.getFleetId();

            StoredFilter.removeFilter(connection, fleetId, name);

            ctx.json(StoredFilter.getStoredFilters(connection, fleetId));
        } catch (SQLException e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e));
        }
    }
}
