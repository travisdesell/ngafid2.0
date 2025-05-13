package org.ngafid.www.routes.api

import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.config.JavalinConfig
import io.javalin.http.Context
import org.ngafid.core.Database
import org.ngafid.core.accounts.Fleet
import org.ngafid.www.routes.Role
import org.ngafid.www.routes.RouteProvider
import org.ngafid.www.routes.SessionUtility

object FleetRoutes : RouteProvider() {
    override fun bind(app: JavalinConfig) {
        app.router.apiBuilder {
            path("/api/fleet") {
                get(FleetRoutes::get, Role.LOGGED_IN)
                get("count", { ctx ->
                    Database.getConnection().use {
                        ctx.json(Fleet.getNumberFleets(it))
                    }
                }, Role.LOGGED_IN)
                get("count/aggregate", { ctx -> Database.getConnection().use { ctx.json(Fleet.getNumberFleets(it)) } })
            }
        }
    }

    fun get(ctx: Context) {
        val user = SessionUtility.getUser(ctx)

        Database.getConnection().use { connection ->
            ctx.json(Fleet.get(connection, user.fleetId))
        }
    }
}