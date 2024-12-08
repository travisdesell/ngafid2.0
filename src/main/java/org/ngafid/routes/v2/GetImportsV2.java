package org.ngafid.routes.v2;


import java.util.List;
import java.util.logging.Logger;
import java.util.HashMap;

import java.sql.Connection;
import java.sql.SQLException;
import org.ngafid.routes.ErrorResponse;

import com.google.gson.Gson;

import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;

import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.flights.Upload;



public class GetImportsV2 implements Route {
    private static final Logger LOG = Logger.getLogger(GetImportsV2.class.getName());
    private Gson gson;


    public GetImportsV2(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    

    @Override
    public Object handle(Request request, Response response) {
        
        LOG.info("handling " + this.getClass().getName() + " route");

        try (Connection connection = Database.getConnection()) {

            final Session session = request.session();
            User user = session.attribute("user");
            int fleetId = user.getFleetId();
            HashMap<String, Object> data = new HashMap<String, Object>();
            //default page values
            int currentPage = 0;
            int pageSize = 10;

            int totalImports = Upload.getNumUploads(connection, fleetId, null);
            int numberPages = totalImports / pageSize;
            List<Upload> imports = Upload.getUploads(connection, fleetId, new String[]{"IMPORTED", "ERROR"}, " LIMIT "+ (currentPage * pageSize) + "," + pageSize);


            data.put("numberPages", numberPages);
            data.put("currentPage", 0);
            data.put("imports",imports);

            return gson.toJson(data);

        } catch (SQLException e) {
            return gson.toJson(new ErrorResponse(e));
        }
    }
}