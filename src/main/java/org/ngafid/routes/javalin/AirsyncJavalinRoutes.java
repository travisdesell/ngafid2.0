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

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
}
