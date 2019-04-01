package org.ngafid.routes;

import org.ngafid.accounts.FleetAccess;
import org.ngafid.accounts.User;

import spark.Request;

public class Navbar {
    public static String getJavascript(Request request) {
        User user = (User)request.session().attribute("user");

        boolean fleetManager = false;
        int waitingUserCount = 0;
        if (user != null && user.getFleetAccessType().equals(FleetAccess.MANAGER)) {
            fleetManager = true;
            waitingUserCount = user.getWaitingUserCount();
        }

        return "var fleetManager = " + fleetManager + "; var waitingUserCount = " + waitingUserCount + ";";
    }
}
