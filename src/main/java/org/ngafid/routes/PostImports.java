package org.ngafid.routes;

import java.util.ArrayList;
import java.util.logging.Logger;

import java.sql.SQLException;

import com.google.gson.Gson;

import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;
import spark.Spark;

import org.ngafid.common.*;
import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.accounts.User;
import org.ngafid.flights.Upload;

public class PostImports implements Route {
    private static final Logger LOG = Logger.getLogger(PostImports.class.getName());
    private Gson gson;
    private Paginator paginator;
    private int buffSize;

    public PostImports(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        final Session session = request.session();
        User user = session.attribute("user");

        int fleetId = user.getFleetId();

        //check to see if the user has upload access for this fleet.
        if (!user.hasUploadAccess(fleetId)) {
            LOG.severe("INVALID ACCESS: user did not have access view imports for this fleet.");
            Spark.halt(401, "User did not have access to view imports for this fleet.");
            return null;
        }


        try {
            int index = Integer.parseInt(request.queryParams("index"));
            int buffSize = Integer.parseInt(request.queryParams("buffSize"));
            if(this.paginator == null || this.buffSize != buffSize){
                this.paginator = new ImportPaginator(buffSize, fleetId);
                this.buffSize = buffSize;
                index = 0;
            }
            this.paginator.jumpToPage(index);
            Page<Upload> imports = paginator.currentPage();
            //LOG.info(gson.toJson(imports));

            return gson.toJson(imports);
        } catch (SQLException e) {
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
