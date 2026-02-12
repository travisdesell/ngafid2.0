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
                put(FilterRoutes::putUpsertFilter, Role.LOGGED_IN)

                // fid is the filter name
                path("{fid}") {
                    delete(FilterRoutes::deleteFilter, Role.LOGGED_IN)
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
     * Inserts or updates a filter
     */
    fun putUpsertFilter(ctx: Context) {

        Database.getConnection().use { connection ->
            val user = SessionUtility.getUser(ctx)
            val name = ctx.formParam("name") ?: throw IllegalArgumentException("FilterRoutes - Tried to upsert filter with no name")

            // Got empty name, throw error
            if (name.isBlank())
                throw IllegalArgumentException("FilterRoutes - Tried to upsert filter with empty name")

            val filterJSON = ctx.formParam("filterJSON") ?: "{}"
            val color = ctx.formParam("color") ?: "#000000"
            StoredFilter.upsertFilter(connection, user.fleetId, filterJSON, name, color)
            ctx.status(204);
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
