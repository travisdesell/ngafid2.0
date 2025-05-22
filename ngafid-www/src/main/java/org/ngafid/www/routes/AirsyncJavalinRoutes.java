package org.ngafid.www.routes;

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.ngafid.airsync.AirSyncFleet;
import org.ngafid.airsync.AirSyncImport;
import org.ngafid.airsync.AirSyncImportResponse;
import org.ngafid.core.Database;
import org.ngafid.core.accounts.User;
import org.ngafid.core.uploads.Upload;
import org.ngafid.www.ErrorResponse;
import org.ngafid.www.PaginationResponse;
import org.ngafid.www.Navbar;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.ngafid.airsync.AirSyncImport.getImports;
import static org.ngafid.airsync.AirSyncImport.getNumImports;
import static org.ngafid.www.WebServer.gson;
import static org.ngafid.www.routes.AircraftFleetTailsJavalinRoutes.GSON;

public class AirsyncJavalinRoutes {
    private static final Logger LOG = Logger.getLogger(AirsyncJavalinRoutes.class.getName());

    private static void getAirsyncImports(Context ctx) throws IOException {
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
            scopes.put("imports_js", "var imports = JSON.parse('" + GSON.toJson(imports) + "');");

            ctx.header("Content-Type", "text/html; charset=UTF-8");
            ctx.render(templateFile, scopes);
        } catch (SQLException e) {
            ctx.json(new ErrorResponse(e)).status(500);
        } catch (Exception e) {
            LOG.severe(e.toString());
        }
    }

    private static void getAirsyncUploads(Context ctx) throws IOException {
        String templateFile = "airsync_uploads.html";
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

            ctx.header("Content-Type", "text/html; charset=UTF-8");
            ctx.render(templateFile, scopes);
        } catch (SQLException e) {
            e.printStackTrace();
            ctx.json(new ErrorResponse(e)).status(500);
        } catch (Exception e) {
            e.printStackTrace();
            LOG.severe(e.toString());
        }
    }

    public static void bindRoutes(Javalin app) {
        app.get("/protected/airsync_uploads", AirsyncJavalinRoutes::getAirsyncUploads);
        // app.post("/protected/airsync_uploads", AirsyncJavalinRoutes::postAirsyncUploads);

        app.get("/protected/airsync_imports", AirsyncJavalinRoutes::getAirsyncImports);
        // app.post("/protected/airsync_update", AirsyncJavalinRoutes::postAirsyncManualUpdate);
        // app.post("/protected/airsync_imports", AirsyncJavalinRoutes::postAirsyncImports);
        // app.post("/protected/airsync_settings", AirsyncJavalinRoutes::postAirsyncTimeout);
    }
}
