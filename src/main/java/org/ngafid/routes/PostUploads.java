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

import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.accounts.User;
import org.ngafid.flights.Upload;
import org.ngafid.common.*;

public class PostUploads implements Route {
    private static final Logger LOG = Logger.getLogger(PostUploads.class.getName());
    private Gson gson;
    private Paginator paginator;

    public PostUploads(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        final Session session = request.session();
        User user = session.attribute("user");

        int fleetId = user.getFleetId();
	try{
	    this.paginator = new UploadPaginator(bufferSize, fleetId);
	}catch(SQLException e){
	    LOG.severe("Uploads in DB error: "+e);//change this
	}

			
        //check to see if the user has upload access for this fleet.
        if (!user.hasUploadAccess(fleetId)) {
            LOG.severe("INVALID ACCESS: user did not have access to upload flights for this fleet.");
            Spark.halt(401, "User did not have access to upload flights for this fleet.");
            return null;
        }

        try {
            Page<Upload> currPage = this.paginator.currentPage();

            //LOG.info(gson.toJson(uploads));

            return gson.toJson(currPage);
        } catch (SQLException e) {
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
