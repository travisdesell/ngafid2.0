package org.ngafid.routes.javalin;

import io.javalin.http.Context;
import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.routes.ErrorResponse;
import org.ngafid.routes.MustacheHandler;
import org.ngafid.routes.Navbar;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.ngafid.WebServer.gson;

public class FleetJavalinRoutes {
    private static final Logger LOG = Logger.getLogger(FleetJavalinRoutes.class.getName());

    public static void getManageFleet(Context ctx) {
        final String templateFile = "manage_fleet.html";

        try  {
            Map<String, Object> scopes = new HashMap<String, Object>();

            scopes.put("navbar_js", Navbar.getJavascript(ctx));

            final User user = ctx.sessionAttribute("user");
            scopes.put("user_js", "var user = JSON.parse('" + gson.toJson(user)  + "');");

            ctx.result(MustacheHandler.handle(templateFile, scopes));
        } catch (IOException e) {
            LOG.severe(e.toString());
        }
    }

    public static void postFleetNames(Context ctx) {
        try (Connection connection = Database.getConnection()) {
            List<String> names = new ArrayList<String>();

            try (PreparedStatement query = connection
                    .prepareStatement("SELECT fleet_name FROM fleet ORDER BY fleet_name");
                 ResultSet resultSet = query.executeQuery()) {
                while (resultSet.next()) {
                    names.add(resultSet.getString(1));
                }
                ctx.json(names);
            }
        } catch (SQLException e) {
            ctx.json(new ErrorResponse(e));
        }

    }
}
