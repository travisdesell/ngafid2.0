package org.ngafid.routes.v2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import org.ngafid.Database;

import org.ngafid.routes.ErrorResponse;
import java.util.logging.Logger;

import com.google.gson.Gson;

import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;

import org.ngafid.accounts.FleetAccess;
import org.ngafid.accounts.User;


public class GetAccount implements Route {
    private static final Logger LOG = Logger.getLogger(GetAccount.class.getName());
    private Gson gson;


    public GetAccount(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    

    @Override
    public Object handle(Request request, Response response) {
        
        LOG.info("handling " + this.getClass().getName() + " route");

        User user = request.session().attribute("user");

        boolean fleetManager = false;
        boolean airSyncEnabled = false;
        int waitingUserCount = 0;
        boolean modifyTailsAccess = false;
        int unconfirmedTailsCount = 0;
        HashMap<String, Object> scopes = new HashMap<String, Object>();

        if (user != null && user.getFleetAccessType().equals(FleetAccess.MANAGER)) {
            fleetManager = true;
            waitingUserCount = user.getWaitingUserCount();
        }


        try {
            int fleetId = -1;

            if ((fleetId = user.getFleetId()) > 0) {
                Connection connection = Database.getConnection();

                String sql = "SELECT EXISTS(SELECT fleet_id FROM airsync_fleet_info WHERE fleet_id = ?)";
                PreparedStatement query = connection.prepareStatement(sql);

                query.setInt(1, fleetId);

                ResultSet resultSet = query.executeQuery();

                if (resultSet.next()) {
                    airSyncEnabled = resultSet.getBoolean(1);
                }

                if (user != null && (user.getFleetAccessType().equals(FleetAccess.MANAGER) || user.getFleetAccessType().equals(FleetAccess.UPLOAD))) {
                    modifyTailsAccess = true;
                    unconfirmedTailsCount = user.getUnconfirmedTailsCount(connection);
                }
            }
            scopes.put("admin", user.isAdmin());
            scopes.put("aggregateView", user.hasAggregateView());
            scopes.put("fleetManager", fleetManager);
            scopes.put("waitingUserCount", waitingUserCount);
            scopes.put("modifyTailsAccess", modifyTailsAccess);
            scopes.put("unconfirmedTailsCount", unconfirmedTailsCount);
            scopes.put("airSyncEnabled", airSyncEnabled);

            return gson.toJson(scopes);
        } catch (SQLException e) {
            //don't do anything so the navbar still displays even if there is an issue with the database
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
