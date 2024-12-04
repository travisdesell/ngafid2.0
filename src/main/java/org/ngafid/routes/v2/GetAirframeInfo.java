package org.ngafid.routes.v2;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;


import java.util.ArrayList;
import java.util.Arrays;
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
import org.ngafid.flights.Airframes;
import org.ngafid.flights.Tails;
import org.ngafid.flights.Tail;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;


public class GetAirframeInfo implements Route {
    private static final Logger LOG = Logger.getLogger(GetAirframeInfo.class.getName());
    private Gson gson;

    public GetAirframeInfo(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");
        
        HashMap<Integer,String> airframeInfos = new HashMap<Integer,String>();
        // Retrieve the query parameter "systemId" as a list of strings
        //String systemId = request.queryParams("systemId");
        HashMap<String, Object> scopes = new HashMap<String, Object>();

        ArrayList<Tail> getTails = new ArrayList<Tail>();

        // Split the string into individual elements
        //String[] systemIds = systemId.split(",");

        //LOG.info("systemId" + systemIds);
        
        try  {

            Connection connection = Database.getConnection();
            
            airframeInfos = Airframes.getIdToNameMap(connection);

            scopes.put("airframes",airframeInfos);

            getTails = Tails.getAll(connection);

            scopes.put("tails",getTails);


        } catch (SQLException e) {
            return gson.toJson(new ErrorResponse(e));
        }

        return gson.toJson(scopes);
    }
}
