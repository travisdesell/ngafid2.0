package org.ngafid.www.routes;

import io.javalin.http.Context;
import org.ngafid.core.Database;
import org.ngafid.core.accounts.User;
import org.ngafid.core.flights.DoubleTimeSeries;
import org.ngafid.core.flights.Flight;
import org.ngafid.core.flights.export.*;
import org.ngafid.core.uploads.Upload;
import org.ngafid.www.ErrorResponse;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

import static org.ngafid.core.Config.NGAFID_ARCHIVE_DIR;
import static org.ngafid.core.flights.export.XPlaneParameters.FDR_FILE_EXTENSION;

public class DataJavalinRoutes {
    private static final Logger LOG = Logger.getLogger(DataJavalinRoutes.class.getName());

    private static void getCSV(Context ctx) {
        final String flightIdStr = Objects.requireNonNull(ctx.queryParam("flight_id"));
        final boolean generated = Boolean.parseBoolean(ctx.queryParam("generated"));
        final int flightId = Integer.parseInt(flightIdStr);
        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        final int fleetId = user.getFleetId();

        // check to see if the user has upload access for this fleet.
        if (!user.hasViewAccess(fleetId)) {
            LOG.severe("INVALID ACCESS: user did not have view access this fleet.");
            ctx.status(401);
            ctx.result("User did not have access to view acces for this fleet.");
            return;
        }

        try (Connection connection = Database.getConnection()) {
            ctx.header("Content-Disposition", "attachment; filename=flight_" + flightId + ".csv");
            ctx.contentType("application/force-download");

            final Flight flight = Flight.getFlight(connection, flightId);
            if (flight == null) {
                ctx.status(404);
                ctx.result("Flight not found");
                return;
            } else if (flight.getFleetId() != fleetId) {
                LOG.severe("INVALID ACCESS: user did not have access to view this flight.");
                ctx.status(401);
                ctx.result("User did not have access to view this flight.");
                return;
            }

            final int uploaderId = flight.getUploaderId();
            boolean isAirSync = false;
            String zipRoot;
            if (uploaderId == 284) {
                // TODO: need to look up user from uploaderId and check if it is the airsync
                // user

                zipRoot = NGAFID_ARCHIVE_DIR + "/AirSyncUploader/";

                Upload upload = Upload.getUploadById(connection, flight.getUploadId());
                if (upload == null) {
                    ctx.status(404);
                    ctx.result("Upload not found");
                    return;
                } else if (upload.getFleetId() != fleetId) {
                    LOG.severe("INVALID ACCESS: user did not have access to view this upload.");
                    ctx.status(401);
                    ctx.result("User did not have access to view this upload.");
                    return;
                }

                String startTime = upload.getStartTime();
                String year = startTime.substring(0, 4);
                int month = Integer.parseInt(startTime.substring(5, 7)); // convert to int to strip leading 0
                zipRoot += year + "/" + month + "/";

                isAirSync = true;
            } else {
                zipRoot = NGAFID_ARCHIVE_DIR + "/" + fleetId + "/" + uploaderId + "/";
            }

            CSVWriter csvWriter;

            if (generated) {
                csvWriter = new GeneratedCSVWriter(flight, Optional.empty(), DoubleTimeSeries.getAllDoubleTimeSeries(connection, flight.getId()));
            } else {
                csvWriter = new CachedCSVWriter(zipRoot, flight, Optional.empty(), isAirSync);
            }

            LOG.info("Got file path for flight #" + flightId);
            LOG.info(csvWriter.toString());

            ctx.result(csvWriter.getFileContents());
        } catch (Exception e) {
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    private static void getKML(Context ctx) {
        final String flightIdStr = Objects.requireNonNull(ctx.queryParam("flight_id"));
        final int flightId = Integer.parseInt(flightIdStr);
        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        final int fleetId = user.getFleetId();

        // check to see if the user has upload access for this fleet.
        if (!user.hasViewAccess(fleetId)) {
            LOG.severe("INVALID ACCESS: user did not have view access this fleet.");
            ctx.status(401);
            ctx.result("User did not have access to view access for this fleet.");
            return;
        }

        try (Connection connection = Database.getConnection()) {
            ctx.header("Content-Disposition", "attachment; filename=flight_" + flightId + ".kml");
            ctx.contentType("application/force-download");

            final Flight flight = Flight.getFlight(connection, flightId);
            if (flight == null || flight.getFleetId() != fleetId) {
                ctx.status(401);
                ctx.result("User did not have access to view this flight.");
                return;
            }

            final DoubleTimeSeries altMSL = Objects.requireNonNull(DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "AltMSL"));
            final DoubleTimeSeries latitude = Objects.requireNonNull(DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "Latitude"));
            final DoubleTimeSeries longitude = Objects.requireNonNull(DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "Longitude"));

            // LOG.info(gson.toJson(flights));

            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < altMSL.size(); i++) {
                sb.append(longitude.get(i)).append(",").append(latitude.get(i)).append(",").append(altMSL.get(i)).append("\n");
            }

            final String templateFile = "template.kml";
            Map<String, Object> scopes = new HashMap<String, Object>();
            scopes.put("coords", sb.toString());
            scopes.put("description", "Flight " + flightId);

            ctx.render(templateFile, scopes);
        } catch (Exception e) {
            ctx.json(new ErrorResponse(e)).status(500);
        }
    }

    private static void getXPlane(Context ctx) {
        final String flightIdStr = Objects.requireNonNull(ctx.queryParam("flight_id"));
        final String aircraftPath = Objects.requireNonNull(ctx.queryParam("acft_path"));
        int version = Integer.parseInt(Objects.requireNonNull(ctx.queryParam("version")));
        boolean useMSL = Boolean.parseBoolean(Objects.requireNonNull(ctx.queryParam("use_msl")));

        LOG.info("MSL will be used: " + useMSL);
        LOG.info("Generating an X-Plane " + version + " FDR file for flight #" + flightIdStr + " with path for .acf: " + aircraftPath);

        int flightId = Integer.parseInt(flightIdStr);

        final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
        final int fleetId = user.getFleetId();

        //check to see if the user has upload access for this fleet.
        if (!user.hasViewAccess(fleetId)) {
            LOG.severe("INVALID ACCESS: user did not have view access this fleet.");
            ctx.status(401);
            ctx.result("User did not have access to view acces for this fleet.");
            return;
        }

        XPlaneExport export = (version == 10) ? new XPlane10Export(flightId, aircraftPath, useMSL) : new XPlane11Export(flightId, aircraftPath, useMSL);

        ctx.contentType("application/force-download");
        ctx.header("Content-Disposition", "attachment; filename=flight_" + flightId + "_xp" + version + FDR_FILE_EXTENSION);
        ctx.result(export.toFdrFile());
    }

    public static void bindRoutes(io.javalin.Javalin app) {
        app.get("/protected/get_csv", DataJavalinRoutes::getCSV);
        app.get("/protected/get_kml", DataJavalinRoutes::getKML);
        app.get("/protected/get_xplane", DataJavalinRoutes::getXPlane);
    }
}
