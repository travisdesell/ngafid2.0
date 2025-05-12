package org.ngafid.www.routes.api

import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.config.JavalinConfig
import io.javalin.http.Context
import org.ngafid.core.Database
import org.ngafid.core.util.filters.StoredFilter
import org.ngafid.www.routes.Role
import org.ngafid.www.routes.RouteProvider
import org.ngafid.www.routes.SessionUtility
import java.sql.SQLIntegrityConstraintViolationException

object FilterRoutes : RouteProvider() {
    override fun bind(app: JavalinConfig) {
        app.router.apiBuilder {
            path("/api/filter") {
                get(FilterRoutes::getStoredFilters, Role.LOGGED_IN)
                post(FilterRoutes::postStoreFilter, Role.LOGGED_IN)

                // fid is the filter name
                path("/{fid}") {
                    delete(FilterRoutes::deleteFilter, Role.LOGGED_IN)
                    put(FilterRoutes::putFilter, Role.LOGGED_IN)
                }
            }
        }
    }

    /**
     * Fetches all fleet filters
     */
    fun getStoredFilters(ctx: Context): Unit =
        Database.getConnection().use { connection ->
            ctx.json(StoredFilter.getStoredFilters(connection, SessionUtility.getUser(ctx).fleetId))
        }

    /**
     * Creates a new filter with the given name
     */
    fun postStoreFilter(ctx: Context) {
        try {
            Database.getConnection().use { connection ->
                val user = SessionUtility.getUser(ctx)
                val name = ctx.formParam("name")
                val filterJSON = ctx.formParam("filterJSON")
                val color = ctx.formParam("color")
                val fleetId = user.fleetId

                StoredFilter.storeFilter(connection, fleetId, filterJSON, name, color)
                ctx.json("SUCCESS")
            }
        } catch (se: SQLIntegrityConstraintViolationException) {
            ctx.json("DUPLICATE_PK")
        }
    }

    /**
     * Overwrites existing filter by name
     */
    fun putFilter(ctx: Context) {
        val currentName = ctx.pathParam("fid")
        val newName = ctx.formParam("newName")!!
        val filterJSON = ctx.formParam("filterJSON")!!
        val color = ctx.formParam("color")!!
        val user = SessionUtility.getUser(ctx)
        val fleetId = user.fleetId

        Database.getConnection().use { connection ->
            StoredFilter.modifyFilter(connection, fleetId, filterJSON, currentName, newName, color)
            ctx.json("SUCCESS")
        }
    }

    /**
     * Deletes filter by name
     */
    fun deleteFilter(ctx: Context): Unit =
        Database.getConnection().use { connection ->
            val user = SessionUtility.getUser(ctx)
            StoredFilter.removeFilter(connection, user.fleetId, ctx.pathParam("fid"))
            ctx.json(StoredFilter.getStoredFilters(connection, user.fleetId))
        }
}