package org.ngafid.www;

import io.javalin.http.Context;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.ngafid.core.Database;
import org.ngafid.core.accounts.FleetAccess;
import org.ngafid.core.accounts.User;

public class Navbar {

    private Navbar() {
        // Utility class
    }

    /**
     * Builds the JavaScript snippet that initializes the navigation bar for the current page. It inspects the
     * session user and queries the database to set the client-side flags the navbar needs -- whether the user is a
     * fleet manager, the count of users awaiting approval, whether AirSync is enabled for the fleet, tail-modify and
     * upload access, and the count of unconfirmed tails.
     *
     * @param ctx the Javalin request context supplying the session user
     * @return a JavaScript source string defining the navbar state variables
     */
    public static String getJavascript(Context ctx) {

        User user = ctx.sessionAttribute("user");

        boolean fleetManager = false;
        boolean airSyncEnabled = false;
        int waitingUserCount = 0;
        boolean modifyTailsAccess = false;
        boolean hasUploadAccess = false;
        int unconfirmedTailsCount = 0;

        try (Connection connection = Database.getConnection()) {

            // User is a fleet manager...
            if (user != null && user.getFleetAccessType().equals(FleetAccess.MANAGER)) {

                fleetManager = true;
                waitingUserCount = user.getWaitingUserCount(connection);
            }

            final int fleetIdDefault = -1;
            int fleetId = fleetIdDefault;

            // Found user Fleet ID...
            if ((user != null) && (fleetId = user.getFleetId()) > 0) {

                String sql = "SELECT EXISTS(SELECT fleet_id FROM airsync_fleet_info WHERE fleet_id = ?)";
                try (PreparedStatement query = connection.prepareStatement(sql)) {

                    query.setInt(1, fleetId);

                    try (ResultSet resultSet = query.executeQuery()) {

                        if (resultSet.next()) airSyncEnabled = resultSet.getBoolean(1);

                        modifyTailsAccess = user.hasUploadAccess(fleetId);
                        hasUploadAccess = user.hasUploadAccess(fleetId);
                        unconfirmedTailsCount = user.getUnconfirmedTailsCount(connection);
                    }
                }
            }

        } catch (SQLException e) {

            /*
                Do nothing so the navbar will display even
                when there is an issue with the database
            */

        }

        final boolean isAdmin = (user != null && user.isAdmin());
        final boolean hasAggregateView = (user != null && user.hasAggregateView());
        final boolean hasStatusView = true;
        final boolean rotorcraftSpecsView = (user != null && user.hasRotorcraftSpecsView());
        final boolean rotorcraftSpecsEdit = (user != null && user.hasRotorcraftSpecsEdit());

        return "var admin = " + isAdmin + ";"
                + "var aggregateView = " + hasAggregateView + ";"
                + "var hasStatusView = " + hasStatusView + ";"
                + "var fleetManager = " + fleetManager + ";"
                + "var waitingUserCount = " + waitingUserCount + ";"
                + "var modifyTailsAccess = " + modifyTailsAccess + ";"
                + "var unconfirmedTailsCount = " + unconfirmedTailsCount + ";"
                + "var airSyncEnabled = " + airSyncEnabled + ";"
                + "var isUploader = " + hasUploadAccess + ";"
                + "var rotorcraftSpecsView = " + rotorcraftSpecsView + ";"
                + "var rotorcraftSpecsEdit = " + rotorcraftSpecsEdit + ";";
    }
}
