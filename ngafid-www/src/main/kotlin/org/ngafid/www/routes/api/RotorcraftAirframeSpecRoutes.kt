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

/**
 * REST API for {@code rotorcraft_airframe_specs}. View and edit access are enforced via
 * {@link User#hasRotorcraftSpecsView()} and {@link User#hasRotorcraftSpecsEdit()}.
 */
object RotorcraftAirframeSpecRoutes : RouteProvider() {

    private val LOG: Logger = Logger.getLogger(RotorcraftAirframeSpecRoutes::class.java.name)
    private val GSON: Gson = WebServer.GSON

    /**
     * Registers {@code GET}, {@code POST}, and {@code PATCH} handlers under
     * {@code /api/aircraft/rotorcraft-airframe-specs}.
     *
     * @param app Javalin application configuration
     */
    override fun bind(app: JavalinConfig) {
        app.router.apiBuilder {
            path("/api/aircraft/rotorcraft-airframe-specs") {
                get(RotorcraftAirframeSpecRoutes::listSpecs, Role.LOGGED_IN)
                post(RotorcraftAirframeSpecRoutes::createSpec, Role.LOGGED_IN)
                patch("{id}", RotorcraftAirframeSpecRoutes::updateSpec, Role.LOGGED_IN)
            }
        }
    }

    /**
     * Ensures the user may read rotorcraft airframe specs.
     *
     * @param user authenticated session user
     * @throws UnauthorizedResponse when view access is not granted
     */
    private fun requireViewAccess(user: User) {
        if (!user.hasRotorcraftSpecsView()) {
            throw UnauthorizedResponse("You do not have access to rotorcraft airframe specs.")
        }
    }

    /**
     * Ensures the user may create or update rotorcraft airframe specs.
     *
     * @param user authenticated session user
     * @throws UnauthorizedResponse when edit access is not granted
     */
    private fun requireEditAccess(user: User) {
        if (!user.hasRotorcraftSpecsEdit()) {
            throw UnauthorizedResponse("You do not have permission to edit rotorcraft airframe specs.")
        }
    }

    /**
     * Returns a paginated list of specs. Query parameters {@code page} (default 0) and
     * {@code pageSize} (default 10) control pagination.
     *
     * @param ctx Javalin request context
     * @throws UnauthorizedResponse when view access is not granted
     */
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

    /**
     * Inserts a new spec from the JSON request body. {@code manufacturer} and {@code model}
     * are required.
     *
     * @param ctx Javalin request context
     * @throws UnauthorizedResponse when edit access is not granted
     */
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

    /**
     * Updates an existing spec identified by the {@code id} path parameter. The request body
     * is merged with that id before persistence.
     *
     * @param ctx Javalin request context
     * @throws UnauthorizedResponse when edit access is not granted
     */
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
