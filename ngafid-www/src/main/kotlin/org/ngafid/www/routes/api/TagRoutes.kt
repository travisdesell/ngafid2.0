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
import java.sql.SQLIntegrityConstraintViolationException
import java.util.*
import java.util.logging.Logger
import kotlin.error

object TagRoutes : RouteProvider() {
    
    private val LOG: Logger = Logger.getLogger(TagRoutes::class.java.name)

    override fun bind(app: JavalinConfig) {
        app.router.apiBuilder {
            path("/api/tag") {
                get(TagRoutes::getTags, Role.LOGGED_IN)
                post(TagRoutes::postCreateTag, Role.LOGGED_IN)
                path("{tid}") {
                    patch(TagRoutes::patchEditTag, Role.LOGGED_IN)
                    delete(TagRoutes::deleteTag, Role.LOGGED_IN)
                }
            }
        }
    }

    fun getTags(ctx: Context) {
        Database.getConnection().use { connection ->
            val user = SessionUtility.getUser(ctx)
            val tags = Flight.getAllTags(connection, user.fleetId)
            ctx.json(tags).status(200)
        }
    }

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

    fun patchEditTag(ctx: Context) {

        val name = Objects.requireNonNull(ctx.formParam("name"))
        val description = Objects.requireNonNull(ctx.formParam("description"))
        val color = Objects.requireNonNull(ctx.formParam("color"))
        val tagId = Objects.requireNonNull(ctx.pathParam("tid")).toInt()
        val user = Objects.requireNonNull(ctx.sessionAttribute<User>("user"))

        try {
            
            Database.getConnection().use { connection ->
                val currentTag = Flight.getTag(connection, tagId)
                    ?: throw NotFoundResponse("Tag not found")

                // Name is changing, enforce uniqueness within the fleet
                if (currentTag.name != name && Flight.tagExists(connection, user!!.fleetId, name)) {
                    ctx.json("ALREADY_EXISTS")
                    return
                }

                val flightTag = FlightTag(tagId, user!!.fleetId, name, description, color)

                // Nothing changed, return the current tag
                if (flightTag == currentTag) {
                    ctx.json(currentTag)
                    return
                }

                val updatedTag = Objects.requireNonNull(Flight.editTag(connection, flightTag))
                ctx.json(updatedTag)

            }

        } catch (e: SQLException) {
            if (e is SQLIntegrityConstraintViolationException) {
                LOG.warning("Tag with name '$name' already exists for fleet ${user!!.fleetId}")
                ctx.json("ALREADY_EXISTS")
            } else {
                LOG.severe("TagRoutes - patchEditTag - Error in SQL: ${e.message}")
                ctx.status(500).json(ErrorResponse(e))
            }
        }

    }


    class RemoveTagResponse(@JsonProperty val tag: FlightTag?)

    fun deleteTag(ctx: Context) {
        val tagId = ctx.pathParam("tid").toInt()

        Database.getConnection().use { connection ->
            val tag = Flight.getTag(connection, tagId)
            Flight.deleteTag(tagId, connection)
            ctx.json(RemoveTagResponse(tag))
        }
    }
}
