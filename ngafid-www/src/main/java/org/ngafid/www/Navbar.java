package org.ngafid.www;

import io.javalin.http.Context;
import org.ngafid.core.Database;
import org.ngafid.core.accounts.FleetAccess;
import org.ngafid.core.accounts.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Navbar {

    public static String getJavascript(Context ctx) {

        User user = ctx.sessionAttribute("user");

        boolean fleetManager = false;
        boolean airSyncEnabled = false;
        int waitingUserCount = 0;
        boolean modifyTailsAccess = false;
        boolean hasUploadAccess = false;
        int unconfirmedTailsCount = 0;

        try (Connection connection = Database.getConnection()) {

            //User is a fleet manager...
            if (user != null && user.getFleetAccessType().equals(FleetAccess.MANAGER)) {

                fleetManager = true;
                waitingUserCount = user.getWaitingUserCount(connection);

            }

            final int FLEET_ID_DEFAULT = -1;
            int fleetId = FLEET_ID_DEFAULT;

            //Found user Fleet ID...
            if ((user != null) && (fleetId = user.getFleetId()) > 0) {

                String sql = "SELECT EXISTS(SELECT fleet_id FROM airsync_fleet_info WHERE fleet_id = ?)";
                try (PreparedStatement query = connection.prepareStatement(sql)) {

                    query.setInt(1, fleetId);

                    try (ResultSet resultSet = query.executeQuery()) {

                        if (resultSet.next())
                            airSyncEnabled = resultSet.getBoolean(1);

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

        return "var admin = " + isAdmin + ";"
                + "var aggregateView = " + hasAggregateView + ";"
                + "var hasStatusView = " + hasStatusView + ";"
                + "var fleetManager = " + fleetManager + ";"
                + "var waitingUserCount = " + waitingUserCount + ";"
                + "var modifyTailsAccess = " + modifyTailsAccess + ";"
                + "var unconfirmedTailsCount = " + unconfirmedTailsCount + ";"
                + "var airSyncEnabled = " + airSyncEnabled + ";"
                + "var isUploader = " + hasUploadAccess + ";";

    }

}
