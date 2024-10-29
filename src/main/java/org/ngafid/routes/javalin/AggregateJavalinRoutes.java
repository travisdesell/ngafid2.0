package org.ngafid.routes.javalin;

import io.javalin.http.Context;
import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.events.EventDefinition;
import org.ngafid.events.EventStatistics;
import org.ngafid.flights.Airframes;
import org.ngafid.flights.Flight;
import org.ngafid.routes.ErrorResponse;
import org.ngafid.routes.MustacheHandler;
import org.ngafid.routes.Navbar;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.ngafid.WebServer.gson;

public class AggregateJavalinRoutes {
    private static final Logger LOG = Logger.getLogger(AggregateJavalinRoutes.class.getName());

    public static void getAggregate(Context ctx) throws IOException {
        final String templateFile = "aggregate.html";

        User user = ctx.sessionAttribute("user");
        if (user == null) {
            LOG.severe("INVALID ACCESS: user was not logged in.");
            ctx.status(401);
            return;
        }

        // check to see if the user has access to view aggregate information
        if (!user.hasAggregateView()) {
            LOG.severe("INVALID ACCESS: user did not have aggregate access to view aggregate dashboard.");
            ctx.status(401);
            ctx.result("User did not have aggregate access to view aggregate dashboard.");
            return;
        }

        try (Connection connection = Database.getConnection()) {
            Map<String, Object> scopes = new HashMap<String, Object>();

            scopes.put("navbar_js", Navbar.getJavascript(ctx));

            long startTime = System.currentTimeMillis();
            scopes.put("fleet_info_js", "var airframes = " + gson.toJson(Airframes.getAll(connection)) + ";\n");
            long endTime = System.currentTimeMillis();

            LOG.info("getting fleet info took " + (endTime - startTime) + "ms.");

            ctx.contentType("text/html");
            ctx.result(MustacheHandler.handle(templateFile, scopes));
        } catch (SQLException e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e));

        } catch (IOException e) {
            LOG.severe(e.toString());
        }
    }

    public static void getAggregateTrends(Context ctx) throws IOException {
        final String templateFile = "aggregate_trends.html";

        User user = ctx.sessionAttribute("user");
        if (user == null) {
            LOG.severe("INVALID ACCESS: user was not logged in.");
            ctx.status(401);
            return;
        }

        // check to see if the user has access to view aggregate information
        if (!user.hasAggregateView()) {
            LOG.severe("INVALID ACCESS: user did not have aggregate access to view aggregate dashboard.");
            ctx.status(401);
            ctx.result("User did not have aggregate access to view aggregate dashboard.");
            return;
        }

        try (Connection connection = Database.getConnection()) {
            Map<String, Object> scopes = new HashMap<String, Object>();

            scopes.put("navbar_js", Navbar.getJavascript(ctx));

            long startTime = System.currentTimeMillis();
            String fleetInfo = "var airframes = " + gson.toJson(Airframes.getAll(connection)) + ";\n" +
                    "var eventNames = " + gson.toJson(EventDefinition.getUniqueNames(connection)) + ";\n" +
                    "var tagNames = " + gson.toJson(Flight.getAllTagNames(connection)) + ";\n";

            scopes.put("fleet_info_js", fleetInfo);
            long endTime = System.currentTimeMillis();
            LOG.info("getting aggreagte data info took " + (endTime - startTime) + "ms.");

            ctx.contentType("text/html");
            ctx.result(MustacheHandler.handle(templateFile, scopes));
        } catch (SQLException e) {
            LOG.severe(e.toString());
            ctx.json(new ErrorResponse(e));
        } catch (IOException e) {
            LOG.severe(e.toString());
        }
    }
}
