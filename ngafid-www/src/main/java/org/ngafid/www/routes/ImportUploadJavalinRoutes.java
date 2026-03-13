package org.ngafid.www.routes;

import static org.ngafid.www.WebServer.GSON;

import io.javalin.Javalin;
import io.javalin.http.Context;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import org.ngafid.core.Database;
import org.ngafid.core.accounts.User;
import org.ngafid.core.uploads.Upload;
import org.ngafid.www.ErrorResponse;
import org.ngafid.www.Navbar;

public class ImportUploadJavalinRoutes {
    public static final Logger LOG = Logger.getLogger(ImportUploadJavalinRoutes.class.getName());
    private static final int DEFAULT_PAGE_SIZE = 10;

    private ImportUploadJavalinRoutes() {
        // Utility class
    }

    private static int calculateNumberPages(int totalItems, int pageSize) {
        if (pageSize <= 0) return 0;
        return (int) Math.ceil((double) totalItems / pageSize);
    }

    private static int clampPage(int requestedPage, int numberPages) {
        if (numberPages <= 0) return 0;
        return Math.max(0, Math.min(requestedPage, numberPages - 1));
    }

    private static int convertPublicPageToInternalPage(int publicPage) {
        return Math.max(0, publicPage - 1);
    }

    private static String buildPagePath(String basePath, int internalPage) {
        return basePath + "/" + (internalPage + 1);
    }

    private static int getRequestedPage(Context ctx) {

        String pageParam = ctx.pathParam("page");

        // Page param is not a valid number, default to page 0 (first page)
        if (!pageParam.matches("\\d+"))
            return 0;

        return Integer.parseInt(pageParam);

    }

    private static boolean redirectIfClamped(Context ctx, String basePath, int requestedPage, int currentPage) {
        final int canonicalPublicPage = currentPage + 1;
        if (requestedPage == canonicalPublicPage) return false;

        ctx.redirect(buildPagePath(basePath, currentPage));
        return true;
    }

    public static void getUploads(Context ctx) {
        ctx.redirect(buildPagePath("/protected/uploads", 0));
    }

    public static void getUploadsPage(Context ctx) {
        renderUploads(ctx, getRequestedPage(ctx), true);
    }

    private static void renderUploads(Context ctx, int requestedPage, boolean isPageRoute) {
        final String templateFile = "uploads.html";

        try (Connection connection = Database.getConnection()) {
            Map<String, Object> scopes = new HashMap<>();

            scopes.put("navbar_js", Navbar.getJavascript(ctx));

            final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
            final int fleetId = user.getFleetId();

            // default page values
            final int pageSize = DEFAULT_PAGE_SIZE;
            final int requestedInternalPage = convertPublicPageToInternalPage(requestedPage);

            final int totalUploads = Upload.getNumUploads(connection, fleetId, null);
            final int numberPages = calculateNumberPages(totalUploads, pageSize);
            final int currentPage = clampPage(requestedInternalPage, numberPages);

            if (isPageRoute && redirectIfClamped(ctx, "/protected/uploads", requestedPage, currentPage)) {
                return;
            }

            List<Upload> pendingUploads =
                    Upload.getUploads(connection, fleetId, new Upload.Status[] {Upload.Status.UPLOADING});

            // update the status of all the uploads currently uploading to incomplete so the
            // webpage knows they
            // need to be restarted and aren't currently being uploaded.
            // TODO: This will cause a problem if a user is uploading something while another user views the uploads
            // page.
            for (Upload upload : pendingUploads) {
                upload.setStatus(Upload.Status.UPLOADING_FAILED);
            }

            List<Upload> otherUploads =
                    Upload.getUploads(connection, fleetId, " LIMIT " + (currentPage * pageSize) + "," + pageSize);

            scopes.put("numPages_js", "var numberPages = " + numberPages + ";");
            scopes.put("index_js", "var currentPage = " + currentPage + ";");

            scopes.put(
                    "uploads_js",
                    "var uploads = JSON.parse('" + GSON.toJson(otherUploads) + "'); var pendingUploads = JSON.parse('"
                            + GSON.toJson(pendingUploads) + "');");

            ctx.header("Content-Type", "text/html; charset=UTF-8");
            ctx.render(templateFile, scopes);
        } catch (SQLException e) {
            ctx.json(new ErrorResponse(e)).status(500);
        } catch (Exception e) {
            LOG.severe(e.toString());
        }
    }

    public static void getImports(Context ctx) {
        ctx.redirect(buildPagePath("/protected/imports", 0));
    }

    public static void getImportsPage(Context ctx) {
        renderImports(ctx, getRequestedPage(ctx), true);
    }

    private static void renderImports(Context ctx, int requestedPage, boolean isPageRoute) {
        final String templateFile = "imports.html";

        try (Connection connection = Database.getConnection()) {
            final User user = Objects.requireNonNull(ctx.sessionAttribute("user"));
            final int fleetId = user.getFleetId();
            Map<String, Object> scopes = new HashMap<>();

            // default page values
            final int pageSize = DEFAULT_PAGE_SIZE;
            final int requestedInternalPage = convertPublicPageToInternalPage(requestedPage);
            final int totalImports = Upload.getNumUploadsByStatus(connection, fleetId, Upload.Status.getImportedSet());
            final int numberPages = calculateNumberPages(totalImports, pageSize);
            final int currentPage = clampPage(requestedInternalPage, numberPages);

            if (isPageRoute && redirectIfClamped(ctx, "/protected/imports", requestedPage, currentPage)) {
                return;
            }

            final List<Upload> imports = Upload.getUploads(
                    connection,
                    fleetId,
                    Upload.Status.getImportedSet(),
                    " LIMIT " + (currentPage * pageSize) + "," + pageSize);

            scopes.put("numPages_js", "var numberPages = " + numberPages + ";");
            scopes.put("index_js", "var currentPage = " + currentPage + ";");
            scopes.put("navbar_js", Navbar.getJavascript(ctx));
            scopes.put("imports_js", "var imports = JSON.parse('" + GSON.toJson(imports) + "');");

            for (String key : scopes.keySet()) {
                if (scopes.get(key) == null) {
                    LOG.severe(() -> "ERROR! key '" + key + "' was null.");
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
        // app.post("/protected/upload", ImportUploadJavalinRoutes::postUpload); // Might be weird. Spark has a
        // "multipart/form-data" in args
        // app.post("/protected/remove_upload", ImportUploadJavalinRoutes::postRemoveUpload);

        app.get("/protected/uploads", ImportUploadJavalinRoutes::getUploads);
        app.get("/protected/uploads/{page}", ImportUploadJavalinRoutes::getUploadsPage);
        // app.post("/protected/uploads", ImportUploadJavalinRoutes::postUploads);

        app.get("/protected/imports", ImportUploadJavalinRoutes::getImports);
        app.get("/protected/imports/{page}", ImportUploadJavalinRoutes::getImportsPage);
        // app.post("/protected/get_imports", ImportUploadJavalinRoutes::postImports);

        // app.post("/protected/upload_details", ImportUploadJavalinRoutes::postUploadDetails);
    }
}
