package org.ngafid.www.routes.api

import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.config.JavalinConfig
import io.javalin.http.Context
import org.ngafid.airsync.AirSyncFleet
import org.ngafid.airsync.AirSyncImport
import org.ngafid.core.Database
import org.ngafid.www.PaginationResponse
import org.ngafid.www.routes.Role
import org.ngafid.www.routes.RouteProvider
import org.ngafid.www.routes.SessionUtility
import org.ngafid.www.routes.status.UnauthorizedException

object AirSyncRoutes : RouteProvider() {
    override fun bind(app: JavalinConfig) {
        app.router.apiBuilder {
            path("/api/airsync") {
                // Fetch uploads
                get("/uploads", AirSyncRoutes::getAirSyncUploads, Role.LOGGED_IN)

                // Fetch imports
                get("/imports", AirSyncRoutes::getAirSyncImports, Role.LOGGED_IN)

                // Force-override to update, even if timeout has not elapsed.
                patch("/update", AirSyncRoutes::patchAirSyncManualUpdate, Role.LOGGED_IN)

                // Update update window.
                patch("/timeout", AirSyncRoutes::patchAirSyncTimeout, Role.LOGGED_IN)
            }
        }
    }

    fun getAirSyncImports(ctx: Context) {
        val user = SessionUtility.getUser(ctx)
        val fleetId = user.fleetId

        Database.getConnection().use { connection ->
            val currentPage = ctx.formParam("currentPage")!!.toInt()
            val pageSize = ctx.formParam("pageSize")!!.toInt()
            val totalImports = AirSyncImport.getNumImports(connection, fleetId, null)
            val numberPages = totalImports / pageSize

            val imports = AirSyncImport.getImports(
                connection, fleetId,
                " LIMIT " + (currentPage * pageSize) + "," + pageSize
            )
            ctx.json(PaginationResponse(imports, numberPages))
        }
    }

    fun getAirSyncUploads(ctx: Context) {
        val user = SessionUtility.getUser(ctx)
        val fleetId = user.fleetId

        Database.getConnection().use { connection ->
            val currentPage = ctx.queryParam("currentPage")!!.toInt()
            val pageSize = ctx.queryParam("pageSize")!!.toInt()
            val totalUploads = AirSyncImport.getNumUploads(connection, fleetId, null)
            val numberPages = totalUploads / pageSize

            val uploads = AirSyncImport.getUploads(
                connection, fleetId,
                " LIMIT " + (currentPage * pageSize) + "," + pageSize
            )

            ctx.json(PaginationResponse(uploads, numberPages))
        }
    }


    fun patchAirSyncManualUpdate(ctx: Context) {
        val user = SessionUtility.getUser(ctx)
        val fleetId = user.fleetId

        // check to see if the user has upload access for this fleet.
        if (!user.hasUploadAccess(fleetId)) {
            throw UnauthorizedException()
        }

        Database.getConnection().use { connection ->
            val fleet = AirSyncFleet.getAirSyncFleet(connection, fleetId)
            fleet.setOverride(connection, true)
            ctx.json("OK")
        }
    }


    fun patchAirSyncTimeout(ctx: Context) {
        val user = SessionUtility.getUser(ctx)
        val fleetId = user.fleetId
        val newTimeout: String = ctx.formParam("timeout")!!

        Database.getConnection().use { connection ->
            AirSyncFleet
                .getAirSyncFleet(connection, fleetId)
                .updateTimeout(connection, user, newTimeout)

            ctx.json(newTimeout)
        }

    }
}