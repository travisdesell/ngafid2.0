package org.ngafid.routes.v2;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;


import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.HashMap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.ngafid.routes.ErrorResponse;


import com.google.gson.Gson;

import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;

import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.accounts.User;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;


public class GetFleetId implements Route {
    private static final Logger LOG = Logger.getLogger(GetFleetId.class.getName());
    private Gson gson;

    public GetFleetId(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");
        
        ArrayList<String> names = new ArrayList<String>();
        
        try  {

            Connection connection = Database.getConnection();

            PreparedStatement query = connection.prepareStatement("SELECT fleet_name FROM fleet ORDER BY fleet_name");
            ResultSet resultSet = query.executeQuery();

            boolean first = true;
            while (resultSet.next()) {
                
                    names.add(resultSet.getString(1));
                    
            }

            } catch (SQLException e) {
                return gson.toJson(new ErrorResponse(e));
            }

        return gson.toJson(names);
    }
}
