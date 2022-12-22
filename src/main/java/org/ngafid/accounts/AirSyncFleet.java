package org.ngafid.accounts;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AirSyncFleet extends Fleet {
    private AirSyncAuth authCreds;

    private static List<AirSyncFleet> fleets = null;

    public AirSyncFleet(int id, String name, AirSyncAuth airSyncAuth) {
        super(id, name);
        this.authCreds = airSyncAuth;
    }

    private AirSyncFleet(ResultSet resultSet) throws SQLException {
        this(resultSet.getInt(1), resultSet.getString(2), 
                new AirSyncAuth(resultSet.getString(3), resultSet.getString(4)));
    }

    public AirSyncAuth getAuth() {
        return this.authCreds;
    }

    public static List<AirSyncFleet> getAll(Connection connection) throws SQLException {
        if (fleets == null) {
            String sql = "SELECT fl.id, fl.fleet_name, sync.api_key, sync.api_secret FROM fleet AS fl INNER JOIN airsync_fleet_info AS sync ON sync.fleet_id = fl.id";
            PreparedStatement query = connection.prepareStatement(sql);

            ResultSet resultSet = query.executeQuery();

            fleets = new ArrayList<>();

            while (resultSet.next()) {
                fleets.add(new AirSyncFleet(resultSet));
            }
        }

        return fleets;
    }
}
