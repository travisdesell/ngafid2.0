
package org.ngafid.routes;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;


import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.HashMap;

import java.sql.Connection;
import java.sql.SQLException;

import com.google.gson.Gson;

import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;

import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.accounts.User;
import org.ngafid.accounts.UserPreferences;
import org.ngafid.flights.Tail;
import org.ngafid.flights.Tails;

public class GetUserPreferences implements Route {
    private static final Logger LOG = Logger.getLogger(GetUserPreferences.class.getName());
    private static Connection connection = Database.getConnection();
    private Gson gson;

    public GetUserPreferences(Gson gson) {
        this.gson = gson;

        LOG.info("get " + this.getClass().getName() + " initalized");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        final Session session = request.session();
        User user = session.attribute("user");

        try {
            UserPreferences userPreferences = User.getUserPreferences(connection, user.getId(), gson);

            return gson.toJson(userPreferences);
        } catch (Exception se) {
            se.printStackTrace();
            return gson.toJson(new ErrorResponse(se));
        }
    }
}
