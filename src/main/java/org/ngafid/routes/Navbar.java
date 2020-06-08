package org.ngafid.routes;

import java.sql.Connection;
import java.sql.SQLException;

import org.ngafid.Database;

import org.ngafid.accounts.FleetAccess;
import org.ngafid.accounts.User;

import spark.Request;

public class Navbar {
    public static String getJavascript(Request request) {
        User user = (User)request.session().attribute("user");

        boolean fleetManager = false;
        int waitingUserCount = 0;
        boolean modifyTailsAccess = false;
        int unconfirmedTailsCount = 0;

        if (user != null && user.getFleetAccessType().equals(FleetAccess.MANAGER)) {
            fleetManager = true;
            waitingUserCount = user.getWaitingUserCount();
        }

        try {
            Connection connection = Database.getConnection();

            if (user != null && (user.getFleetAccessType().equals(FleetAccess.MANAGER) || user.getFleetAccessType().equals(FleetAccess.UPLOAD))) {
                modifyTailsAccess = true;
                unconfirmedTailsCount = user.getUnconfirmedTailsCount(connection);
            }
        } catch (SQLException e) {
            //don't do anything so the navbar still displays even if there is an issue with the database
        }

        return "var fleetManager = " + fleetManager + "; var waitingUserCount = " + waitingUserCount + "; var modifyTailsAccess = " + modifyTailsAccess + "; var unconfirmedTailsCount = " + unconfirmedTailsCount + ";";
    }
}
