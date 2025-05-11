package org.ngafid.www.routes.api

import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.config.JavalinConfig
import org.ngafid.www.routes.Role
import org.ngafid.www.routes.RouteProvider
import org.ngafid.www.routes.TagFilterJavalinRoutes

object FilterRoutes : RouteProvider() {
    override fun bind(app: JavalinConfig) {
        app.router.apiBuilder {
            path("/api/filter") {
                get(TagFilterJavalinRoutes::getStoredFilters, Role.LOGGED_IN)
                post(TagFilterJavalinRoutes::postStoreFilter, Role.LOGGED_IN)

                path("/{fid}") {
                    delete(TagFilterJavalinRoutes::postRemoveFilter, Role.LOGGED_IN)
                    put(TagFilterJavalinRoutes::postModifyFilter, Role.LOGGED_IN)
                }
            }
        }
    }
}