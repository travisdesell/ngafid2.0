package org.ngafid.routes;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.ngafid.Database;

import org.ngafid.accounts.FleetAccess;
import org.ngafid.accounts.User;

import spark.Request;

public class Navbar {
    public static String getJavascript(Request request) {
        User user = request.session().attribute("user");

        boolean fleetManager = false;
        boolean airSyncEnabled = false;
        int waitingUserCount = 0;
        boolean modifyTailsAccess = false;
        boolean hasUploadAccess = false;
        int unconfirmedTailsCount = 0;

        if (user != null && user.getFleetAccessType().equals(FleetAccess.MANAGER)) {
            fleetManager = true;
            waitingUserCount = user.getWaitingUserCount();
        }

        try (Connection connection = Database.getConnection()) {
            int fleetId = -1;

            if ((fleetId = user.getFleetId()) > 0) {
                String sql = "SELECT EXISTS(SELECT fleet_id FROM airsync_fleet_info WHERE fleet_id = ?)";
                try (PreparedStatement query = connection.prepareStatement(sql)) {

                    query.setInt(1, fleetId);

                    try (ResultSet resultSet = query.executeQuery()) {

                        if (resultSet.next()) {
                            airSyncEnabled = resultSet.getBoolean(1);
                        }

                        if (user != null) {
                            modifyTailsAccess = user.hasUploadAccess(fleetId);
                            hasUploadAccess = user.hasUploadAccess(fleetId);
                            unconfirmedTailsCount = user.getUnconfirmedTailsCount(connection);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            // don't do anything so the navbar still displays even if there is an issue with
            // the database
        }

        return "var admin = " + user.isAdmin() + ";"
                + "var aggregateView = " + user.hasAggregateView() + ";"
                + "var fleetManager = " + fleetManager + ";"
                + "var waitingUserCount = " + waitingUserCount + ";"
                + "var modifyTailsAccess = " + modifyTailsAccess + ";"
                + "var unconfirmedTailsCount = " + unconfirmedTailsCount + ";"
                + "var airSyncEnabled = " + airSyncEnabled + ";"
                + "var uploader = " + hasUploadAccess + ";";
    }
}
