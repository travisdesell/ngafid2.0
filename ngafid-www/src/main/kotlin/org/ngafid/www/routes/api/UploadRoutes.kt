package org.ngafid.www.routes.api

import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.config.JavalinConfig
import org.ngafid.www.routes.ImportUploadJavalinRoutes
import org.ngafid.www.routes.Role
import org.ngafid.www.routes.RouteProvider
import org.ngafid.www.routes.RouteUtility

object UploadRoutes : RouteProvider() {
    override fun bind(app: JavalinConfig) {
        app.router.apiBuilder {
            path("/api/upload") {
                get(ImportUploadJavalinRoutes::postUploads, Role.LOGGED_IN)
                get("/imported", ImportUploadJavalinRoutes::postImports, Role.LOGGED_IN);
                post(ImportUploadJavalinRoutes::postNewUpload, Role.LOGGED_IN)

                path("/{uid}") {
                    get("/file", ImportUploadJavalinRoutes::getUpload, Role.LOGGED_IN)
                    get("/errors", ImportUploadJavalinRoutes::postUploadDetails, Role.LOGGED_IN)
                    put(ImportUploadJavalinRoutes::postUpload, Role.LOGGED_IN)
                    delete(ImportUploadJavalinRoutes::postRemoveUpload, Role.LOGGED_IN)
                }

                RouteUtility.getStat("/count") { ctx, stats -> ctx.json(stats.uploads()) }
                RouteUtility.getStat("/count/success") { ctx, stats -> ctx.json(stats.uploadsOK()) }
                RouteUtility.getStat("/count/warning") { ctx, stats -> ctx.json(stats.uploadsWithWarning()) }
                RouteUtility.getStat("/count/error") { ctx, stats -> ctx.json(stats.uploadsWithError()) }
                RouteUtility.getStat("/count/pending") { ctx, stats -> ctx.json(stats.uploadsNotImported()) }
            }
        }
    }
}