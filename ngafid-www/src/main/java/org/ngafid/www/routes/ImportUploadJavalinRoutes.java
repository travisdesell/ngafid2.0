package org.ngafid.www.routes;

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.ngafid.core.Database;
import org.ngafid.core.accounts.User;
import org.ngafid.core.uploads.Upload;
import org.ngafid.www.ErrorResponse;
import org.ngafid.www.Navbar;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import static org.ngafid.www.WebServer.gson;

public class ImportUploadJavalinRoutes {
    public static final Logger LOG = Logger.getLogger(ImportUploadJavalinRoutes.class.getName());


    public static void getUploads(Context ctx) {
        final String templateFile = "uploads.html";

        try (Connection connection = Database.getConnection()) {
            Map<String, Object> scopes = new HashMap<>();

            scopes.put("navbar_js", Navbar.getJavascript(ctx));

            final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
            final int fleetId = user.getFleetId();

            // default page values
            final int pageSize = 10;
            int currentPage = 0;

            final int totalUploads = Upload.getNumUploads(connection, fleetId, null);
            final int numberPages = totalUploads / pageSize;

            List<Upload> pending_uploads = Upload.getUploads(connection, fleetId, new Upload.Status[]{Upload.Status.UPLOADING});

            // update the status of all the uploads currently uploading to incomplete so the
            // webpage knows they
            // need to be restarted and aren't currently being uploaded.
            // TODO: This will cause a problem if a user is uploading something while another user views the uploads page.
            for (Upload upload : pending_uploads) {
                upload.setStatus(Upload.Status.UPLOADING_FAILED);
            }

            List<Upload> other_uploads = Upload.getUploads(connection, fleetId, " LIMIT " + (currentPage * pageSize) + "," + pageSize);

            scopes.put("numPages_js", "var numberPages = " + numberPages + ";");
            scopes.put("index_js", "var currentPage = 0;");

            scopes.put("uploads_js", "var uploads = JSON.parse('" + gson.toJson(other_uploads) + "'); var pending_uploads = JSON.parse('" + gson.toJson(pending_uploads) + "');");

            ctx.header("Content-Type", "text/html; charset=UTF-8");
            ctx.render(templateFile, scopes);
        } catch (SQLException e) {
            ctx.json(new ErrorResponse(e)).status(500);
        } catch (Exception e) {
            LOG.severe(e.toString());
        }
    }

    public static void getImports(Context ctx) {
        final String templateFile = "imports.html";

        try (Connection connection = Database.getConnection()) {
            final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
            final int fleetId = user.getFleetId();
            Map<String, Object> scopes = new HashMap<String, Object>();

            // default page values
            final int totalImports = Upload.getNumUploads(connection, fleetId, null);
            final int startPage = 0;
            final int pageSize = 10;
            final int numberPages = totalImports / pageSize;
            final List<Upload> imports = Upload.getUploads(connection, fleetId, Upload.Status.IMPORTED_SET, " LIMIT " + startPage + "," + pageSize);

            scopes.put("numPages_js", "var numberPages = " + numberPages + ";");
            scopes.put("index_js", "var currentPage = 0;");
            scopes.put("navbar_js", Navbar.getJavascript(ctx));
            scopes.put("imports_js", "var imports = JSON.parse('" + gson.toJson(imports) + "');");

            for (String key : scopes.keySet()) {
                if (scopes.get(key) == null) {
                    LOG.severe("ERROR! key '" + key + "' was null.");
                }
            }

            ctx.header("Content-Type", "text/html; charset=UTF-8");
            ctx.render(templateFile, scopes);
        } catch (SQLException e) {
            ctx.json(new ErrorResponse(e)).status(500);
        } catch (Exception e) {
            LOG.severe(e.toString());
        }
    }

    public static void bindRoutes(Javalin app) {
        // app.post("/protected/download_upload", ImportUploadJavalinRoutes::getUpload);
        // app.get("/protected/download_upload", ImportUploadJavalinRoutes::getUpload);
        // app.post("/protected/new_upload", ImportUploadJavalinRoutes::postNewUpload);
        // app.post("/protected/upload", ImportUploadJavalinRoutes::postUpload); // Might be weird. Spark has a "multipart/form-data" in args
        // app.post("/protected/remove_upload", ImportUploadJavalinRoutes::postRemoveUpload);

        app.get("/protected/uploads", ImportUploadJavalinRoutes::getUploads);
        // app.post("/protected/uploads", ImportUploadJavalinRoutes::postUploads);

        app.get("/protected/imports", ImportUploadJavalinRoutes::getImports);
        // app.post("/protected/get_imports", ImportUploadJavalinRoutes::postImports);

        // app.post("/protected/upload_details", ImportUploadJavalinRoutes::postUploadDetails);
    }
}
