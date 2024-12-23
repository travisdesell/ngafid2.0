package org.ngafid.routes.spark;

import java.util.ArrayList;
import java.util.logging.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.google.gson.Gson;

import org.ngafid.routes.ErrorResponse;
import spark.Route;
import spark.Request;
import spark.Response;

import org.ngafid.Database;

public class PostFleetNames implements Route {
    private static final Logger LOG = Logger.getLogger(PostFleetNames.class.getName());
    private Gson gson;

    public PostFleetNames(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName());

        try (Connection connection = Database.getConnection()) {
            ArrayList<String> names = new ArrayList<String>();

            try (PreparedStatement query = connection
                    .prepareStatement("SELECT fleet_name FROM fleet ORDER BY fleet_name");
                    ResultSet resultSet = query.executeQuery()) {
                while (resultSet.next()) {
                    names.add(resultSet.getString(1));
                }
                return gson.toJson(names);
            }
        } catch (SQLException e) {
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
