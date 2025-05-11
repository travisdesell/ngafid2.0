package org.ngafid.www.routes.api

import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.config.JavalinConfig
import io.javalin.http.Context
import org.ngafid.core.Database
import org.ngafid.core.flights.Flight
import org.ngafid.www.routes.Role
import org.ngafid.www.routes.RouteProvider
import org.ngafid.www.routes.SessionUtility
import org.ngafid.www.routes.TagFilterJavalinRoutes
import org.ngafid.www.routes.TagFilterJavalinRoutes.RemoveTagResponse
import org.ngafid.www.routes.status.NotFoundException
import org.ngafid.www.routes.status.UnauthorizedException

object TagRoutes : RouteProvider() {
    override fun bind(app: JavalinConfig) {
        app.router.apiBuilder {
            path("/api/tag") {
                get(TagFilterJavalinRoutes::postTags, Role.LOGGED_IN)
                post(TagFilterJavalinRoutes::postCreateTag, Role.LOGGED_IN)
                path("/{tid}") {
                    patch(TagFilterJavalinRoutes::postEditTag, Role.LOGGED_IN)
                    delete(TagRoutes::deleteTag, Role.LOGGED_IN)
                }
            }
        }
    }


    fun deleteTag(ctx: Context) {
        val user = SessionUtility.getUser(ctx)
        val flightId = ctx.pathParam("fid").toInt()
        val tagId = ctx.pathParam("tid").toInt()

        Database.getConnection().use { connection ->
            // check to see if the user has access to this data
            if (!user.hasFlightAccess(connection, flightId))
                throw UnauthorizedException()

            if (Flight.getFlight(connection, flightId) == null)
                throw NotFoundException()

            val tag = Flight.getTag(connection, tagId)
            Flight.deleteTag(tagId, connection)
            ctx.json(RemoveTagResponse(tag))
        }
    }
}