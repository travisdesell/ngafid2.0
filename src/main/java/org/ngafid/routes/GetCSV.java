package org.ngafid.routes;

import java.io.IOException;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.File;

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.Enumeration;

import java.util.logging.Logger;

import java.sql.Connection;
import java.sql.SQLException;

import com.google.gson.Gson;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;
import spark.Spark;

import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.accounts.User;
import org.ngafid.flights.Flight;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.common.CSVWriter;

import org.ngafid.filters.Filter;

public class GetCSV implements Route {
    private static final Logger LOG = Logger.getLogger(GetKML.class.getName());
    private Gson gson;

    public GetCSV(Gson gson) {
        this.gson = gson;

        LOG.info("get " + this.getClass().getName() + " initalized");
    }


	/**
	* {inheritDoc}
	*/
    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        String flightIdStr = request.queryParams("flight_id");

        LOG.info("getting csv for flight id: " + flightIdStr);

        int flightId = Integer.parseInt(flightIdStr);

        final Session session = request.session();
        User user = session.attribute("user");
        int fleetId = user.getFleetId();


        //check to see if the user has upload access for this fleet.
        if (!user.hasViewAccess(fleetId)) {
            LOG.severe("INVALID ACCESS: user did not have view access this fleet.");
            Spark.halt(401, "User did not have access to view acces for this fleet.");
            return null;
        }


		try {
            response.header("Content-Disposition", "attachment; filename=flight_" + flightId + ".csv");
            response.type("application/force-download");

			Flight flight = Flight.getFlight(Database.getConnection(), flightId);

			int uploaderId = flight.getUploaderId(); 

			String zipRoot = WebServer.NGAFID_ARCHIVE_DIR + "/" + fleetId + "/" +
				uploaderId + "/";
			
			CSVWriter csvWriter = new CSVWriter(zipRoot, flight);

			LOG.info("Got file path for flight #"+flightId);
			return csvWriter.write();

		} catch (SQLException e) {
			return gson.toJson(new ErrorResponse(e));
		} catch (IOException e) {
			LOG.severe(e.toString());
		}
        return "";
    }
}
