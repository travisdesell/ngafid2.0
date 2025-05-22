package org.ngafid.www.routes.api

import com.fasterxml.jackson.annotation.JsonProperty
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.config.JavalinConfig
import io.javalin.http.Context
import io.javalin.http.NotFoundResponse
import org.ngafid.core.Database
import org.ngafid.core.accounts.User
import org.ngafid.core.flights.Flight
import org.ngafid.core.util.FlightTag
import org.ngafid.www.ErrorResponse
import org.ngafid.www.routes.Role
import org.ngafid.www.routes.RouteProvider
import org.ngafid.www.routes.SessionUtility
import java.sql.SQLException
import java.util.*

object TagRoutes : RouteProvider() {
    override fun bind(app: JavalinConfig) {
        app.router.apiBuilder {
            path("/api/tag") {
                get(TagRoutes::getTags, Role.LOGGED_IN)
                post(TagRoutes::postCreateTag, Role.LOGGED_IN)
                path("{tid}") {
                    patch(TagRoutes::postEditTag, Role.LOGGED_IN)
                    delete(TagRoutes::deleteTag, Role.LOGGED_IN)
                }
            }
        }
    }

    fun getTags(ctx: Context): Unit =
        Database.getConnection()
            .use { connection -> Flight.getAllTags(connection, SessionUtility.getUser(ctx).fleetId) }

    fun postCreateTag(ctx: Context) {
        val user = SessionUtility.getUser(ctx)
        val name = ctx.formParam("name")!!
        val description = ctx.formParam("description")!!
        val color = ctx.formParam("color")
        val flightId = ctx.formParam("id")!!.toInt()
        val fleetId = user.fleetId

        Database.getConnection().use { connection ->
            if (Flight.tagExists(connection, fleetId, name)) {
                ctx.json("ALREADY_EXISTS")
            } else {
                ctx.json(Flight.createTag(fleetId, flightId, name, description, color, connection))
            }
        }
    }

    fun postEditTag(ctx: Context) {
        val name = Objects.requireNonNull(ctx.formParam("name"))
        val description = Objects.requireNonNull(ctx.formParam("description"))
        val color = Objects.requireNonNull(ctx.formParam("color"))
        val tagId = Objects.requireNonNull(ctx.pathParam("tid")).toInt()
        val user = Objects.requireNonNull(ctx.sessionAttribute<User>("user"))

        try {
            Database.getConnection().use { connection ->
                val flightTag = FlightTag(tagId, user!!.fleetId, name, description, color)
                val currentTag = Flight.getTag(connection, tagId)

                if (flightTag == currentTag) {
                    ctx.json("NOCHANGE")
                }
                ctx.json(Objects.requireNonNull(Flight.editTag(connection, flightTag)))
            }
        } catch (e: SQLException) {
            System.err.println("Error in SQL ")
            ctx.json(ErrorResponse(e)).status(500)
        }
    }

    class RemoveTagResponse(@JsonProperty val tag: FlightTag?, @JsonProperty val allTagsCleared: Boolean = false)

    fun deleteTag(ctx: Context) {
        val user = SessionUtility.getUser(ctx)
        val flightId = ctx.pathParam("fid").toInt()
        val tagId = ctx.pathParam("tid").toInt()

        Database.getConnection().use { connection ->
            if (Flight.getFlight(connection, flightId) == null)
                throw NotFoundResponse("Flight with id $flightId not found.")

            val tag = Flight.getTag(connection, tagId)
            Flight.deleteTag(tagId, connection)
            ctx.json(RemoveTagResponse(tag))
        }
    }
}