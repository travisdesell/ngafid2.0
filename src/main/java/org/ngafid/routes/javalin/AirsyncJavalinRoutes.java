package org.ngafid.routes.javalin;

import io.javalin.http.Context;
import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.accounts.AirSyncFleet;
import org.ngafid.accounts.User;
import org.ngafid.flights.AirSyncImport;
import org.ngafid.flights.AirSyncImportResponse;
import org.ngafid.flights.Upload;
import org.ngafid.routes.ErrorResponse;
import org.ngafid.routes.MustacheHandler;
import org.ngafid.routes.Navbar;
import org.ngafid.routes.PaginationResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import static org.ngafid.WebServer.gson;
import static org.ngafid.flights.AirSyncImport.getImports;
import static org.ngafid.flights.AirSyncImport.getNumImports;

public class AirsyncJavalinRoutes {
    private static final Logger LOG = Logger.getLogger(AirsyncJavalinRoutes.class.getName());

    public static void getAirsyncImports(Context ctx) throws IOException {
        final String templateFile = "airsync_imports.html";

        try (Connection connection = Database.getConnection()) {
            Map<String, Object> scopes = new HashMap<String, Object>();
            User user = ctx.sessionAttribute("user");
            if (user == null) {
                LOG.severe("INVALID ACCESS: user was not logged in.");
                ctx.status(401);
                return;
            }


            int fleetId = user.getFleetId();
            // default page values
            int currentPage = 0;
            int pageSize = 10;
            int totalUploads = getNumImports(connection, fleetId, null);
            int numberPages = totalUploads / pageSize;
            List<AirSyncImportResponse> imports = getImports(connection, fleetId, " LIMIT " + (currentPage * pageSize) + "," + pageSize);

            scopes.put("navbar_js", Navbar.getJavascript(ctx));
            scopes.put("numPages_js", "var numberPages = " + numberPages + ";");
            scopes.put("index_js", "var currentPage = 0;");
            scopes.put("imports_js", "var imports = JSON.parse('" + gson.toJson(imports) + "');");

            ctx.contentType("text/html");
            ctx.result(MustacheHandler.handle(templateFile, scopes));
        } catch (SQLException e) {
            ctx.json(new ErrorResponse(e));
        } catch (Exception e) {
            LOG.severe(e.toString());
        }
    }

    public static void getAirsyncUploads(Context ctx) throws IOException {
        String templateFile = WebServer.MUSTACHE_TEMPLATE_DIR + "airsync_uploads.html";
        LOG.severe("template file: '" + templateFile + "'");

        try (Connection connection = Database.getConnection()) {
            Map<String, Object> scopes = new HashMap<String, Object>();
            // default page values
            int currentPage = 0;
            int pageSize = 10;

            User user = ctx.sessionAttribute("user");
            if (user == null) {
                LOG.severe("INVALID ACCESS: user was not logged in.");
                ctx.status(401);
                return;
            }

            AirSyncFleet fleet = AirSyncFleet.getAirSyncFleet(connection, user.getFleetId());
            String timestamp = fleet.getLastUpdateTime(connection);
            if (fleet.getOverride(connection)) timestamp = "Pending";
            int totalUploads = AirSyncImport.getNumUploads(connection, fleet.getId(), null);
            int numberPages = totalUploads / pageSize;
            List<Upload> uploads = AirSyncImport.getUploads(connection, fleet.getId(), " LIMIT " + (currentPage * pageSize) + "," + pageSize);

            scopes.put("navbar_js", Navbar.getJavascript(ctx));
            scopes.put("numPages_js", "var numberPages = " + numberPages + ";");
            scopes.put("index_js", "var currentPage = 0;");
            scopes.put("lastUpdateTime_js", "var lastUpdateTime = " + gson.toJson(timestamp) + ";");
            scopes.put("uploads_js", "var uploads = JSON.parse('" + gson.toJson(uploads) + "');");

            ctx.contentType("text/html");
            ctx.result(MustacheHandler.handle(templateFile, scopes));
        } catch (SQLException e) {
            ctx.json(new ErrorResponse(e));
        } catch (Exception e) {
            LOG.severe(e.toString());
        }
    }

    public static void postAirsyncImports(Context ctx) {
        User user = ctx.sessionAttribute("user");
        if (user == null) {
            LOG.severe("INVALID ACCESS: user was not logged in.");
            ctx.status(401);
            return;
        }

        int fleetId = user.getFleetId();

        // check to see if the user has upload access for this fleet.
        if (!user.hasUploadAccess(fleetId)) {
            LOG.severe("INVALID ACCESS: user did not have access to upload flights for this fleet.");
            ctx.status(401);
            ctx.result("User did not have access to upload flights for this fleet.");
            return;
        }

        try (Connection connection = Database.getConnection()) {
            int currentPage = Integer.parseInt(Objects.requireNonNull(ctx.queryParam("currentPage")));
            int pageSize = Integer.parseInt(Objects.requireNonNull(ctx.queryParam("pageSize")));
            int totalImports = AirSyncImport.getNumImports(connection, fleetId, null);
            int numberPages = totalImports / pageSize;

            List<AirSyncImportResponse> imports = AirSyncImport.getImports(connection, fleetId,
                    " LIMIT " + (currentPage * pageSize) + "," + pageSize);

            ctx.json(new PaginationResponse<>(imports, numberPages));
        } catch (SQLException e) {
            ctx.json(new ErrorResponse(e));
        }
    }

    public static void postAirsyncUploads(Context ctx) {
        User user = ctx.sessionAttribute("user");
        if (user == null) {
            LOG.severe("INVALID ACCESS: user was not logged in.");
            ctx.status(401);
            return;
        }

        int fleetId = user.getFleetId();

        // check to see if the user has upload access for this fleet.
        if (!user.hasUploadAccess(fleetId)) {
            LOG.severe("INVALID ACCESS: user did not have access to upload flights for this fleet.");
            ctx.status(401);
            ctx.result("User did not have access to upload flights for this fleet.");
        }

        try (Connection connection = Database.getConnection()) {
            int currentPage = Integer.parseInt(Objects.requireNonNull(ctx.queryParam("currentPage")));
            int pageSize = Integer.parseInt(Objects.requireNonNull(ctx.queryParam("pageSize")));
            int totalUploads = AirSyncImport.getNumUploads(connection, fleetId, null);
            int numberPages = totalUploads / pageSize;

            List<Upload> uploads = AirSyncImport.getUploads(connection, fleetId,
                    " LIMIT " + (currentPage * pageSize) + "," + pageSize);

            ctx.json(new PaginationResponse<Upload>(uploads, numberPages));
        } catch (SQLException e) {
            ctx.json(new ErrorResponse(e));
        }
    }

    public static void postAirsyncManualUpdate(Context ctx) {
        User user = ctx.sessionAttribute("user");
        if (user == null) {
            LOG.severe("INVALID ACCESS: user was not logged in.");
            ctx.status(401);
            return;
        }

        int fleetId = user.getFleetId();

        // check to see if the user has upload access for this fleet.
        if (!user.hasUploadAccess(fleetId)) {
            LOG.severe("INVALID ACCESS: user did not have access to upload flights for this fleet.");
            ctx.status(401);
            ctx.result("User did not have access to upload flights for this fleet.");
            return;
        }

        try (Connection connection = Database.getConnection()) {
            AirSyncFleet fleet = AirSyncFleet.getAirSyncFleet(connection, fleetId);
            if (fleet == null) {
                LOG.severe("INVALID ACCESS: user did not have access to upload flights for this fleet.");
                ctx.status(401);
                ctx.result("User did not have access to upload flights for this fleet.");
                return;
            }

            LOG.info("Beginning AirSync update process!");
            String status = fleet.update(connection);
            LOG.info("AirSync update process complete! Status: " + status);

            fleet.setOverride(connection, true);

            ctx.json("OK");
        } catch (Exception e) {
            ctx.json(new ErrorResponse(e));
        }
    }

    public static void postAirsyncTimeout(Context ctx) {
        User user = ctx.sessionAttribute("user");
        if (user == null) {
            LOG.severe("INVALID ACCESS: user was not logged in.");
            ctx.status(401);
            return;
        }

        int fleetId = user.getFleetId();
        String newTimeout = ctx.queryParam("timeout");
        if (newTimeout == null) {
            LOG.severe("INVALID ACCESS: user did not provide a new timeout.");
            ctx.status(401);
            return;
        }

        try (Connection connection = Database.getConnection()) {
            LOG.info("User set new timeout: " + newTimeout + ", requesting user: " + user.getFullName());

            AirSyncFleet fleet = AirSyncFleet.getAirSyncFleet(connection, fleetId);
            if (fleet == null) {
                LOG.severe("INVALID ACCESS: user did not have access to upload flights for this fleet.");
                ctx.status(401);
                return;
            }

            fleet.updateTimeout(connection, user, newTimeout);
            ctx.json(newTimeout);
        } catch (Exception e) {
            e.printStackTrace();
            ctx.json(new ErrorResponse(e));
        }
    }
}
