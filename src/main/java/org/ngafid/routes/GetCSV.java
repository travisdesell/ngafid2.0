package org.ngafid.routes;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.FileFilter;
import java.io.File;

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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

import org.ngafid.filters.Filter;

public class GetCSV implements Route {
    private static final Logger LOG = Logger.getLogger(GetKML.class.getName());
    private Gson gson;

    public GetCSV(Gson gson) {
        this.gson = gson;

        LOG.info("get " + this.getClass().getName() + " initalized");
    }

	private File getZipFile(String directoryRoot, int uploadId){
		File root = new File(directoryRoot);
		File[] dirs = root.listFiles();

		System.out.println("target id: "+uploadId);
		for (File archive : dirs) {
			String archPath = archive.toString();
			String [] archDirs = archPath.split("/");
			String archName = archDirs[archDirs.length - 1];
			if(archName.contains(Integer.toString(uploadId))){
				return archive;
			}
		}
		
		return null;

	}

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
			Flight flight = Flight.getFlight(Database.getConnection(), flightId);

			int uploadId = flight.getUploadId();
			int uploaderId = flight.getUploaderId(); 

			String zipRoot = WebServer.NGAFID_ARCHIVE_DIR + "/" + fleetId + "/" +
				uploaderId + "/";

			ZipFile zipArchive = new ZipFile(getZipFile(zipRoot, uploadId));

			LOG.info("Got file path for flight #"+flightId+": "+zipArchive.toString());

			String filename = flight.getFilename();

			System.out.println("filename: "+filename);

			Enumeration<? extends ZipEntry> entries = zipArchive.entries();

			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				String name = entry.getName();

				System.err.println("PROCESSING: " + name);

				if (entry.getName().equals(filename)) {
					LOG.info("found file: "+entry.toString());	
					return Spark.entry;
				} 
			} 
			zipArchive.close();

			return "";
		} catch (SQLException e) {
			return gson.toJson(new ErrorResponse(e));
		} catch (IOException e) {
			//LOG.severe(e.toString());
		}

        return "";
    }
}
