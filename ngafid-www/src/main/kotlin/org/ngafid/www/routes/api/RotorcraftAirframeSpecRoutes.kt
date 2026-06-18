package org.ngafid.www.routes.api

import com.google.gson.Gson
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.config.JavalinConfig
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.http.UnauthorizedResponse
import org.ngafid.core.Database
import org.ngafid.core.accounts.User
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

    private fun requireViewAccess(user: User) {
        if (!user.hasRotorcraftSpecsView()) {
            throw UnauthorizedResponse("You do not have access to rotorcraft airframe specs.")
        }
    }

    private fun requireEditAccess(user: User) {
        if (!user.hasRotorcraftSpecsEdit()) {
            throw UnauthorizedResponse("You do not have permission to edit rotorcraft airframe specs.")
        }
    }

    private fun listSpecs(ctx: Context) {
        val user = SessionUtility.getUser(ctx)
        requireViewAccess(user)
        val page = ctx.queryParam("page")?.toIntOrNull() ?: 0
        val pageSize = ctx.queryParam("pageSize")?.toIntOrNull() ?: 10

        Database.getConnection().use { connection ->
            val result = RotorcraftAirframeSpecs.listPage(
                connection,
                user.hasRotorcraftSpecsEdit(),
                page,
                pageSize,
            )
            ctx.json(result)
        }
    }

    private fun createSpec(ctx: Context) {
        val user = SessionUtility.getUser(ctx)
        requireEditAccess(user)
        val spec = GSON.fromJson(ctx.body(), RotorcraftAirframeSpecs.Spec::class.java)
        if (spec.manufacturer.isNullOrBlank() || spec.model.isNullOrBlank()) {
            ctx.status(HttpStatus.BAD_REQUEST)
            ctx.result("manufacturer and model are required")
            return
        }
        try {
            Database.getConnection().use { connection ->
                val created = RotorcraftAirframeSpecs.insert(connection, spec)
                RotorcraftAirframeSpecs.getById(connection, created.id)?.let { ctx.json(it) } ?: ctx.json(created)
            }
        } catch (e: SQLException) {
            LOG.severe(e.toString())
            ctx.status(HttpStatus.BAD_REQUEST)
            ctx.result(e.message ?: "Could not create spec")
        }
    }

    private fun updateSpec(ctx: Context) {
        val user = SessionUtility.getUser(ctx)
        requireEditAccess(user)
        val specId = ctx.pathParam("id").toIntOrNull()
        if (specId == null) {
            ctx.status(HttpStatus.BAD_REQUEST)
            return
        }
        val spec = GSON.fromJson(ctx.body(), RotorcraftAirframeSpecs.Spec::class.java)
        spec.id = specId
        try {
            Database.getConnection().use { connection ->
                val updated = RotorcraftAirframeSpecs.update(connection, spec)
                ctx.json(updated)
            }
        } catch (e: SQLException) {
            LOG.severe(e.toString())
            ctx.status(HttpStatus.BAD_REQUEST)
            ctx.result(e.message ?: "Could not update spec")
        }
    }
}
