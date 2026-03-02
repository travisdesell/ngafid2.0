package org.ngafid.www.routes.api

import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.config.JavalinConfig
import io.javalin.http.Context
import org.ngafid.core.Database
import org.ngafid.core.flights.Itinerary
import org.ngafid.www.routes.Role
import org.ngafid.www.routes.RouteProvider
import org.ngafid.www.routes.SessionUtility
import java.util.logging.Logger

object AirportRoutes : RouteProvider() {

    val LOG: Logger = Logger.getLogger(
        AirportRoutes::class.java.name
    )

    override fun bind(app: JavalinConfig) {
        app.router.apiBuilder {
            path("/api/aircraft") {

                path("airports") {
                    get(AirportRoutes::getAllVisitedAirports, Role.LOGGED_IN)
                }

                path("runways") {
                    get(AirportRoutes::getAllVisitedRunways, Role.LOGGED_IN)
                }
            }
        }
    }

    fun getAllVisitedAirports(ctx: Context) {
        val user = SessionUtility.getUser(ctx)

        if (!user.hasViewAccess(user.fleetId)) {
            ctx.status(401)
            return
        }

        Database.getConnection().use { connection ->
            ctx.json(Itinerary.getAllAirports(connection, user.fleetId))
        }
    }

    fun getAllVisitedRunways(ctx: Context) {
        val user = SessionUtility.getUser(ctx)

        if (!user.hasViewAccess(user.fleetId)) {
            ctx.status(401)
            return
        }

        Database.getConnection().use { connection ->
            ctx.json(Itinerary.getAllAirportRunways(connection, user.fleetId))
        }
    }
}
