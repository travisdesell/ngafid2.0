package org.ngafid.www.routes.api

import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.config.JavalinConfig
import org.ngafid.www.routes.AirsyncJavalinRoutes
import org.ngafid.www.routes.Role
import org.ngafid.www.routes.RouteProvider

object AirSyncRoutes : RouteProvider() {
    override fun bind(app: JavalinConfig) {
        app.router.apiBuilder {
            path("/api/airsync") {
                // Fetch uploads
                get("/uploads", AirsyncJavalinRoutes::postAirsyncUploads, Role.LOGGED_IN)

                // Fetch imports
                get("/imports", AirsyncJavalinRoutes::postAirsyncImports, Role.LOGGED_IN)

                // Force-override to update, even if timeout has not elapsed.
                patch("/update", AirsyncJavalinRoutes::postAirsyncManualUpdate, Role.LOGGED_IN)

                // Update update window.
                patch("/timeout", AirsyncJavalinRoutes::postAirsyncTimeout, Role.LOGGED_IN)
            }
        }
    }
}