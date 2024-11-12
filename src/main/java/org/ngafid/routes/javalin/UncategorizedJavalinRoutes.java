package org.ngafid.routes.javalin;

import io.javalin.http.Context;
import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.flights.Flight;
import org.ngafid.flights.Upload;
import org.ngafid.routes.ErrorResponse;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import static org.ngafid.WebServer.gson;
import static org.ngafid.events.EventStatistics.updateMonthlyTotalFlights;

public class UncategorizedJavalinRoutes {
    private static class MainContent {
        List<Flight> flights;
        List<Upload> uploads;
        List<Upload> imports;

        public MainContent(Connection connection, int fleetId) throws SQLException {
            flights = Flight.getFlights(connection, fleetId);
            uploads = Upload.getUploads(connection, fleetId);
            imports = Upload.getUploads(connection, fleetId, new String[]{"IMPORTED", "ERROR"});
        }
    }

    private static void putUpdateMonthlyFlightsCache(Context ctx) {
        try (Connection connection = Database.getConnection()) {
            updateMonthlyTotalFlights(connection, Integer.parseInt(Objects.requireNonNull(ctx.queryParam("fleetId"))));
            ctx.status(200);
            ctx.json(Objects.requireNonNull(ctx.queryParam("fleetId")));
        } catch (Exception e) {
            e.printStackTrace();
            ctx.status(500);
            ctx.json(new ErrorResponse(e));
        }
    }

    private static void postMainContent(Context ctx) {
        final User user = ctx.sessionAttribute("user");

        try (Connection connection = Database.getConnection()) {
            ctx.json(new MainContent(connection, user.getFleetId()));
        } catch (SQLException e) {
            ctx.json(new ErrorResponse(e));
        }
    }

    public static void bindRoutes(io.javalin.Javalin app) {
        app.get("/update_monthly_flights", UncategorizedJavalinRoutes::putUpdateMonthlyFlightsCache);
    }
}
