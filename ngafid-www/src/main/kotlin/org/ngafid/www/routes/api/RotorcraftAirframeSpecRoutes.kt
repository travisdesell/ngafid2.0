package org.ngafid.www.routes.api

import com.google.gson.Gson
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.config.JavalinConfig
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import org.ngafid.core.Database
import org.ngafid.core.flights.RotorcraftAirframeSpecs
import org.ngafid.www.WebServer
import org.ngafid.www.routes.Role
import org.ngafid.www.routes.RouteProvider
import org.ngafid.www.routes.SessionUtility
import java.sql.SQLException
import java.util.logging.Logger

object RotorcraftAirframeSpecRoutes : RouteProvider() {

    private val LOG: Logger = Logger.getLogger(RotorcraftAirframeSpecRoutes::class.java.name)
    private val GSON: Gson = WebServer.GSON

    override fun bind(app: JavalinConfig) {
        app.router.apiBuilder {
            path("/api/aircraft/rotorcraft-airframe-specs") {
                get(RotorcraftAirframeSpecRoutes::listSpecs, Role.LOGGED_IN)
                post(RotorcraftAirframeSpecRoutes::createSpec, Role.LOGGED_IN)
                patch("{id}", RotorcraftAirframeSpecRoutes::updateSpec, Role.LOGGED_IN)
            }
        }
    }

    private fun listSpecs(ctx: Context) {
        val user = SessionUtility.getUser(ctx)
        val page = ctx.queryParam("page")?.toIntOrNull() ?: 0
        val pageSize = ctx.queryParam("pageSize")?.toIntOrNull() ?: 10

        Database.getConnection().use { connection ->
            val result = RotorcraftAirframeSpecs.listVisiblePage(
                connection,
                user.fleetId,
                user.isAdmin,
                user.managesFleet(user.fleetId),
                page,
                pageSize,
            )
            ctx.json(result)
        }
    }

    private fun createSpec(ctx: Context) {
        val user = SessionUtility.getUser(ctx)
        val spec = GSON.fromJson(ctx.body(), RotorcraftAirframeSpecs.Spec::class.java)
        if (spec.manufacturer.isNullOrBlank() || spec.model.isNullOrBlank()) {
            ctx.status(HttpStatus.BAD_REQUEST)
            ctx.result("manufacturer and model are required")
            return
        }
        try {
            Database.getConnection().use { connection ->
                val created = RotorcraftAirframeSpecs.insert(connection, spec, user.fleetId)
                RotorcraftAirframeSpecs.getById(
                    connection,
                    created.id,
                    user.fleetId,
                    user.isAdmin,
                    user.managesFleet(user.fleetId),
                )?.let { ctx.json(it) } ?: ctx.json(created)
            }
        } catch (e: SQLException) {
            LOG.severe(e.toString())
            ctx.status(HttpStatus.BAD_REQUEST)
            ctx.result(e.message ?: "Could not create spec")
        }
    }

    private fun updateSpec(ctx: Context) {
        val user = SessionUtility.getUser(ctx)
        val specId = ctx.pathParam("id").toIntOrNull()
        if (specId == null) {
            ctx.status(HttpStatus.BAD_REQUEST)
            return
        }
        val spec = GSON.fromJson(ctx.body(), RotorcraftAirframeSpecs.Spec::class.java)
        spec.id = specId
        try {
            Database.getConnection().use { connection ->
                val updated = RotorcraftAirframeSpecs.update(
                    connection,
                    spec,
                    user.fleetId,
                    user.isAdmin,
                    user.managesFleet(user.fleetId),
                )
                ctx.json(updated)
            }
        } catch (e: SQLException) {
            LOG.severe(e.toString())
            val status = if (e.message?.contains("Not authorized", ignoreCase = true) == true) {
                HttpStatus.UNAUTHORIZED
            } else {
                HttpStatus.BAD_REQUEST
            }
            ctx.status(status)
            ctx.result(e.message ?: "Could not update spec")
        }
    }
}
