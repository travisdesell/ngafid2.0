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
import org.ngafid.common.*;
import org.ngafid.WebServer;
import org.ngafid.accounts.AirSyncFleet;
import org.ngafid.accounts.User;
import org.ngafid.flights.AirSyncImport;
import org.ngafid.flights.Upload;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;


public class GetAirSyncUploads implements Route {
    private static final Logger LOG = Logger.getLogger(GetAirSyncUploads.class.getName());
    private Gson gson;

    public GetAirSyncUploads(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        String resultString = "";
        String templateFile = WebServer.MUSTACHE_TEMPLATE_DIR + "airsync_uploads.html";
        LOG.severe("template file: '" + templateFile + "'");

        try  {
            Connection connection = Database.getConnection();

            MustacheFactory mf = new DefaultMustacheFactory();
            Mustache mustache = mf.compile(templateFile);

            HashMap<String, Object> scopes = new HashMap<String, Object>();

            scopes.put("navbar_js", Navbar.getJavascript(request));

            final Session session = request.session();
            User user = session.attribute("user");
            AirSyncFleet fleet = AirSyncFleet.getAirSyncFleet(connection, user.getFleetId());

            //default page values
            int currentPage = 0;
            int pageSize = 10;

            String timestamp = fleet.getLastUpdateTime(connection);
            int totalUploads = AirSyncImport.getNumUploads(connection, fleet.getId(), null);
            List<Upload> uploads = AirSyncImport.getUploads(connection, fleet.getId(), " LIMIT "+ (currentPage * pageSize) + "," + pageSize);
            int numberPages = totalUploads / pageSize;


            scopes.put("numPages_js", "var numberPages = " + numberPages + ";");
            scopes.put("index_js", "var currentPage = 0;");
            scopes.put("lastUpdateTime_js", "var lastUpdateTime = " + gson.toJson(timestamp) + ";");
            scopes.put("uploads_js", "var uploads = JSON.parse('" + gson.toJson(uploads) + "');");

            StringWriter stringOut = new StringWriter();
            mustache.execute(new PrintWriter(stringOut), scopes).flush();
            resultString = stringOut.toString();
        } catch (SQLException e) {
            return gson.toJson(new ErrorResponse(e));

        } catch (Exception e) {
            LOG.severe(e.toString());
        }

        return resultString;
    }
}
