package org.ngafid.routes;

import java.io.IOException;
import java.util.Optional;
import java.util.logging.Logger;
import java.sql.Connection;
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
import org.ngafid.flights.Flight;
import org.ngafid.flights.GeneratedCSVWriter;
import org.ngafid.flights.CSVWriter;
import org.ngafid.flights.CachedCSVWriter;
import org.ngafid.flights.DoubleTimeSeries;

public class GetCSV implements Route {
    private static final Logger LOG = Logger.getLogger(GetCSV.class.getName());
    private static final Connection connection = Database.getConnection();

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
        boolean generated = Boolean.parseBoolean(request.queryParams("generated"));

        LOG.info("getting csv for flight id: " + flightIdStr + ", generating: " + generated);

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
            
            CSVWriter csvWriter;

            if (generated) {
                csvWriter = new GeneratedCSVWriter(flight, Optional.empty(), DoubleTimeSeries.getAllDoubleTimeSeries(connection, flight.getId()));
            } else {
                // TODO: Change to original file writer?
                csvWriter = new CachedCSVWriter(zipRoot, flight, Optional.empty());
            }

            LOG.info("Got file path for flight #" + flightId);
            LOG.info(csvWriter.toString());

            return csvWriter.getFileContents();
        } catch (Exception e) {
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
