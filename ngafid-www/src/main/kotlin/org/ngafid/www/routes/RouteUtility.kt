package org.ngafid.www.routes

import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.http.Context
import org.ngafid.www.routes.SessionUtility.withStatFetcher

object RouteUtility {
    inline fun getStat(
        route: String,
        crossinline block: (Context, StatisticsJavalinRoutes.StatFetcher) -> Unit
    ) {
        path(route) {
            get({ ctx -> withStatFetcher(ctx, false) { stats -> block(ctx, stats) } }, Role.LOGGED_IN)
            get("/aggregate", { ctx -> withStatFetcher(ctx, true) { stats -> block(ctx, stats) } }, Role.LOGGED_IN)
        }
    }
}