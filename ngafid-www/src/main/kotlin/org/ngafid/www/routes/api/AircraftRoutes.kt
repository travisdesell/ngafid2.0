package org.ngafid.www.routes.api

import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.config.JavalinConfig
import io.javalin.http.Context
import org.ngafid.core.Database
import org.ngafid.core.accounts.User
import org.ngafid.core.flights.Flight
import org.ngafid.core.flights.Tails
import org.ngafid.www.routes.AircraftFleetTailsJavalinRoutes.UpdateTailResponse
import org.ngafid.www.routes.Role
import org.ngafid.www.routes.RouteProvider
import java.util.*
import java.util.logging.Logger

object AircraftRoutes : RouteProvider() {

    val LOG: Logger = Logger.getLogger(
        AircraftRoutes::class.java.name
    )

    override fun bind(app: JavalinConfig) {
        app.router.apiBuilder {
            path("/api/aircraft") {
                path("/system-id") {
                    get(AircraftRoutes::getAllSystemIds, Role.LOGGED_IN)
                    patch("/{sid}", AircraftRoutes::patchSystemId, Role.LOGGED_IN)
                }

                path("/sim-aircraft") {
                    get(AircraftRoutes::getAllSimAircraft, Role.LOGGED_IN)
                    post(AircraftRoutes::postSimAircraft, Role.LOGGED_IN)
                    delete(AircraftRoutes::deleteSimAircraft, Role.LOGGED_IN)
                }
            }
        }
    }

    fun getAllSystemIds(ctx: Context) {
        val user = ctx.sessionAttribute<User>("user")!!

        if (user.hasViewAccess(user.fleetId)) {
            Database.getConnection().use { connection ->
                ctx.json(Tails.getAll(connection, user.fleetId))
            }
        }
    }

    fun getAllSimAircraft(ctx: Context) {
        val user = ctx.sessionAttribute<User>("user")!!
        val fleetId = user.fleetId

        if (!user.hasViewAccess(fleetId)) {
            ctx.status(401)
            ctx.result("User did not have access to view acces for this fleet.")
            return
        }

        Database.getConnection().use { connection ->
            val paths = Flight.getSimAircraft(connection, fleetId)
            ctx.json(paths)
        }

    }

    fun postSimAircraft(ctx: Context) {
        val user = ctx.sessionAttribute<User>("user")!!
        val path = ctx.formParam("path")!!
        val fleetId = user.fleetId

        Database.getConnection().use { connection ->
            val currPaths = Flight.getSimAircraft(connection, fleetId)
            if (!currPaths.contains(path)) {
                Flight.addSimAircraft(connection, fleetId, path)
                ctx.json("SUCCESS")
            } else {
                ctx.json("FAILURE")
            }
        }
    }

    fun deleteSimAircraft(ctx: Context) {
        val user = Objects.requireNonNull(ctx.sessionAttribute<User>("user"))
        val path = Objects.requireNonNull(ctx.formParam("path"))
        val fleetId = user!!.fleetId

        Database.getConnection().use { connection ->
            Flight.removeSimAircraft(connection, fleetId, path)
            ctx.json(Flight.getSimAircraft(connection, fleetId))
        }


        ctx.json("FAILURE")
    }

    fun patchSystemId(ctx: Context) {
        val systemId = ctx.pathParam("sid")
        val tail = ctx.formParam("tail")

        Database.getConnection().use { connection ->
            val user = ctx.sessionAttribute<User>("user")!!
            val fleetId = user.fleetId

            // check to see if the user has upload access for this fleet.
            if (!user.hasUploadAccess(fleetId)) {
                ctx.status(401)
                ctx.result("User did not have access to view imports for this fleet.")
                return
            }

            Tails.updateTail(connection, fleetId, systemId, tail)
            ctx.json(UpdateTailResponse(fleetId, systemId, tail, 1))
        }
    }
}