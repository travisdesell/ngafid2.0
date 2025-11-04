package org.ngafid.www.routes.api

import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.config.JavalinConfig
import io.javalin.http.Context
import io.javalin.http.UnauthorizedResponse
import org.ngafid.core.Database
import org.ngafid.core.accounts.FleetAccess
import org.ngafid.core.accounts.User
import org.ngafid.core.flights.Airframes
import org.ngafid.www.ErrorResponse
import org.ngafid.www.routes.Role
import org.ngafid.www.routes.RouteProvider
import org.ngafid.www.routes.SessionUtility
import org.ngafid.www.routes.StatisticsJavalinRoutes
import java.sql.SQLException
import java.util.logging.Logger

object StartApiRoutes : RouteProvider() {
    private val LOG = Logger.getLogger(StartApiRoutes::class.java.name)

    data class Message(
        val type: String, val message: String
    )

    data class UserInfo(
        val email: String, val fullName: String, val fleetId: Int
    )

    override fun bind(app: JavalinConfig) {
        app.router.apiBuilder {
            path("/api") {
                get("start", StartApiRoutes::getStart, Role.OPEN)
                get("navbar", StartApiRoutes::getNavbar, Role.LOGGED_IN)
                get("welcome", StartApiRoutes::getWelcome, Role.LOGGED_IN)
                get("waiting", StartApiRoutes::getWaiting, Role.LOGGED_IN)

                get("bootstrap", StartApiRoutes::getBootstrap, Role.LOGGED_IN)

                // Stats endpoints
                path("statistics") {
                    // Per‑fleet
                    get("flight-time", StartApiRoutes::fleetFlightTime, Role.LOGGED_IN)
                    get("flight-time/year", StartApiRoutes::fleetFlightTimeYear, Role.LOGGED_IN)
                    get("flight-time/30day", StartApiRoutes::fleetFlightTime30Day, Role.LOGGED_IN)

                    get("flights/count", StartApiRoutes::fleetFlightsCount, Role.LOGGED_IN)
                    get("flights/count/year", StartApiRoutes::fleetFlightsCountYear, Role.LOGGED_IN)
                    get("flights/count/30day", StartApiRoutes::fleetFlightsCount30Day, Role.LOGGED_IN)

                    get("events/count", StartApiRoutes::fleetEventsCount, Role.LOGGED_IN)
                    get("events/count/year", StartApiRoutes::fleetEventsCountYear, Role.LOGGED_IN)
                    get("events/count/month", StartApiRoutes::fleetEventsCountMonth, Role.LOGGED_IN)

                    get("users/count", StartApiRoutes::fleetUsersCount, Role.LOGGED_IN)

                    // Aggregate
                    path("aggregate") {
                        get("flight-time", StartApiRoutes::aggFlightTime, Role.LOGGED_IN)
                        get("flight-time/year", StartApiRoutes::aggFlightTimeYear, Role.LOGGED_IN)
                        get("flight-time/30day", StartApiRoutes::aggFlightTime30Day, Role.LOGGED_IN)

                        get("flights/count", StartApiRoutes::aggFlightsCount, Role.LOGGED_IN)
                        get("flights/count/year", StartApiRoutes::aggFlightsCountYear, Role.LOGGED_IN)
                        get("flights/count/30day", StartApiRoutes::aggFlightsCount30Day, Role.LOGGED_IN)

                        get("events/count", StartApiRoutes::aggEventsCount, Role.LOGGED_IN)
                        get("events/count/year", StartApiRoutes::aggEventsCountYear, Role.LOGGED_IN)
                        get("events/count/month", StartApiRoutes::aggEventsCountMonth, Role.LOGGED_IN)

                        get("fleets/count", StartApiRoutes::aggFleetsCount, Role.LOGGED_IN)
                    }
                }
            }
        }
    }

    /**
     * Returns public start/home page information
     * Includes any messages to display and basic session state
     */
    fun getStart(ctx: Context) {
        data class StartResponse(
            val isLoggedIn: Boolean, val messages: List<Message>? = null, val user: UserInfo? = null
        )

        val user: User? = ctx.sessionAttribute("user")
        val messages = mutableListOf<Message>()

        // Check for special query params that indicate messages
        when (ctx.queryParam("msg")) {
            "logout_success" -> messages.add(Message("success", "You have been successfully logged out."))
            "access_denied" -> messages.add(
                Message(
                    "danger",
                    "You attempted to load a page you did not have access to or attempted to access a page while not logged in."
                )
            )
        }

        val userInfo = user?.let {
            UserInfo(
                email = it.email, fullName = it.fullName, fleetId = it.fleetId
            )
        }

        ctx.json(
            StartResponse(
                isLoggedIn = user != null, messages = messages.ifEmpty { null }, user = userInfo
            )
        )
    }

    /**
     * Returns navbar data for the current logged-in user
     * This replaces the JavaScript injection in the old navbar_js
     */
    fun getNavbar(ctx: Context) {
        data class NavbarData(
            val admin: Boolean,
            val aggregateView: Boolean,
            val hasStatusView: Boolean,
            val fleetManager: Boolean,
            val waitingUserCount: Int,
            val modifyTailsAccess: Boolean,
            val unconfirmedTailsCount: Int,
            val airSyncEnabled: Boolean,
            val isUploader: Boolean
        )

        val user = SessionUtility.getUser(ctx)
        var fleetManager = false
        var airSyncEnabled = false
        var waitingUserCount = 0
        var modifyTailsAccess = false
        var hasUploadAccess = false
        var unconfirmedTailsCount = 0

        try {
            Database.getConnection().use { connection ->
                // User is a fleet manager
                if (user.fleetAccessType == FleetAccess.MANAGER) {
                    fleetManager = true
                    waitingUserCount = user.getWaitingUserCount(connection)
                }

                val fleetId = user.fleetId
                if (fleetId > 0) {
                    val sql = "SELECT EXISTS(SELECT fleet_id FROM airsync_fleet_info WHERE fleet_id = ?)"
                    connection.prepareStatement(sql).use { query ->
                        query.setInt(1, fleetId)
                        query.executeQuery().use { resultSet ->
                            if (resultSet.next()) {
                                airSyncEnabled = resultSet.getBoolean(1)
                            }
                        }
                    }

                    modifyTailsAccess = user.hasUploadAccess(fleetId)
                    hasUploadAccess = user.hasUploadAccess(fleetId)
                    unconfirmedTailsCount = user.getUnconfirmedTailsCount(connection)
                }
            }
        } catch (e: SQLException) {
            LOG.warning("Error getting navbar data: ${e.message}")
            // Continue with defaults so navbar still displays
        }

        ctx.json(
            NavbarData(
                admin = user.isAdmin,
                aggregateView = user.hasAggregateView(),
                hasStatusView = true,
                fleetManager = fleetManager,
                waitingUserCount = waitingUserCount,
                modifyTailsAccess = modifyTailsAccess,
                unconfirmedTailsCount = unconfirmedTailsCount,
                airSyncEnabled = airSyncEnabled,
                isUploader = hasUploadAccess
            )
        )
    }

    /**
     * Returns welcome page data (replaces welcome.html)
     * Includes navbar, airframes, and any messages
     */
    fun getWelcome(ctx: Context) {
        data class WelcomeResponse(
            val navbar: Map<String, Any>,
            val airframes: Array<Airframes.AirframeNameID>,
            val messages: List<Message>? = null
        )

        val user = SessionUtility.getUser(ctx)
        val fleetId = user.fleetId

        try {
            Database.getConnection().use { connection ->
                val airframes = Airframes.getAllWithIds(connection, fleetId)

                // Get navbar data by calling our own endpoint logic
                val navbarData = getNavbarData(user)

                val messages = mutableListOf<Message>()
                ctx.queryParam("msg")?.let { msg ->
                    when (msg) {
                        "not_found" -> messages.add(
                            Message(
                                "danger",
                                "The page you attempted to access does not exist."
                            )
                        )
                    }
                }

                ctx.json(
                    WelcomeResponse(
                    navbar = navbarData, airframes = airframes, messages = messages.ifEmpty { null }))
            }
        } catch (e: Exception) {
            LOG.severe("Error getting welcome data: ${e.message}")
            ctx.json(ErrorResponse(e)).status(500)
        }
    }

    /**
     * Returns waiting page status
     * Used when user is awaiting fleet approval
     */
    fun getWaiting(ctx: Context) {
        data class WaitingResponse(
            val status: String, val message: String
        )

        ctx.json(
            WaitingResponse(
                status = "waiting", message = "Your account is awaiting approval from your fleet manager."
            )
        )
    }

    /**
     * Helper function to get navbar data without duplicating code
     */
    private fun getNavbarData(user: User): Map<String, Any> {
        var fleetManager = false
        var airSyncEnabled = false
        var waitingUserCount = 0
        var modifyTailsAccess = false
        var hasUploadAccess = false
        var unconfirmedTailsCount = 0

        try {
            Database.getConnection().use { connection ->
                if (user.fleetAccessType == FleetAccess.MANAGER) {
                    fleetManager = true
                    waitingUserCount = user.getWaitingUserCount(connection)
                }

                val fleetId = user.fleetId
                if (fleetId > 0) {
                    val sql = "SELECT EXISTS(SELECT fleet_id FROM airsync_fleet_info WHERE fleet_id = ?)"
                    connection.prepareStatement(sql).use { query ->
                        query.setInt(1, fleetId)
                        query.executeQuery().use { resultSet ->
                            if (resultSet.next()) {
                                airSyncEnabled = resultSet.getBoolean(1)
                            }
                        }
                    }

                    modifyTailsAccess = user.hasUploadAccess(fleetId)
                    hasUploadAccess = user.hasUploadAccess(fleetId)
                    unconfirmedTailsCount = user.getUnconfirmedTailsCount(connection)
                }
            }
        } catch (e: SQLException) {
            Logger.getLogger(StartApiRoutes::class.java.name).warning("Error getting navbar data: ${e.message}")
        }

        return mapOf(
            "admin" to user.isAdmin,
            "aggregateView" to user.hasAggregateView(),
            "hasStatusView" to true,
            "fleetManager" to fleetManager,
            "waitingUserCount" to waitingUserCount,
            "modifyTailsAccess" to modifyTailsAccess,
            "unconfirmedTailsCount" to unconfirmedTailsCount,
            "airSyncEnabled" to airSyncEnabled,
            "isUploader" to hasUploadAccess
        )
    }

    /**
     * Returns composite bootstrap data for the app
     * Reduces round trips by aggregating user, navbar, airframes, and stats meta
     */
    fun getBootstrap(ctx: Context) {
        // include=comma,separated,keys; supported keys: user,navbar,airframes,aggregateMeta,eventStatsMeta
        val include = (ctx.queryParam("include") ?: "user,navbar,airframes").split(",").map { it.trim() }.toSet()
        val user = SessionUtility.getUser(ctx)

        val result = mutableMapOf<String, Any>()

        // user
        if ("user" in include) {
            result["user"] = mapOf(
                "email" to user.email,
                "fullName" to user.fullName,
                "fleetId" to user.fleetId,
                "admin" to user.isAdmin,
                "aggregateView" to user.hasAggregateView()
            )
        }

        try {
            Database.getConnection().use { connection ->
                if ("navbar" in include) {
                    result["navbar"] = getNavbarData(user)
                }

                if ("airframes" in include) {
                    result["airframes"] = Airframes.getAllWithIds(connection, user.fleetId)
                }

                if ("aggregateMeta" in include) {
                    if (!user.hasAggregateView()) {
                        throw UnauthorizedResponse("Aggregate access is required to include aggregateMeta")
                    }
                    val aggAirframes = Airframes.getAllWithIds(connection)
                    val eventNames = org.ngafid.core.event.EventDefinition.getUniqueNames(connection)
                    val tagNames = org.ngafid.core.flights.Flight.getAllTagNames(connection)
                    result["aggregateMeta"] = mapOf(
                        "airframes" to aggAirframes, "eventNames" to eventNames, "tagNames" to tagNames
                    )
                }

                if ("eventStatsMeta" in include) {
                    val eventDefinitions = org.ngafid.core.event.EventDefinition.getAll(connection)
                    val airframeMap = Airframes.getIdToNameMap(connection, user.fleetId)
                    result["eventStatsMeta"] = mapOf(
                        "eventDefinitions" to eventDefinitions, "airframeMap" to airframeMap
                    )
                }
            }
        } catch (e: SQLException) {
            LOG.severe("Error building bootstrap payload: ${e.message}")
            ctx.json(ErrorResponse(e)).status(500)
            return
        }

        ctx.json(result)
    }

    // Helpers to exec StatFetcher blocks
    private inline fun <reified T> withFleetStats(
        ctx: Context,
        crossinline block: (StatisticsJavalinRoutes.StatFetcher) -> T
    ) {
        Database.getConnection().use { conn ->
            val fetcher = StatisticsJavalinRoutes.StatFetcher(conn, ctx, false)
            val value = block(fetcher)
            @Suppress("UNCHECKED_CAST") ctx.json(value as Any)
        }
    }

    private inline fun <reified T> withAggStats(
        ctx: Context,
        crossinline block: (StatisticsJavalinRoutes.StatFetcher) -> T
    ) {
        val user = SessionUtility.getUser(ctx)
        if (!user.hasAggregateView()) throw UnauthorizedResponse("Aggregate access is required.")
        Database.getConnection().use { conn ->
            val fetcher = StatisticsJavalinRoutes.StatFetcher(conn, ctx, true)
            val value = block(fetcher)
            @Suppress("UNCHECKED_CAST") ctx.json(value as Any)
        }
    }

    // Per‑fleet handlers
    private fun fleetFlightTime(ctx: Context) = withFleetStats(ctx) { it.flightTime() }
    private fun fleetFlightTimeYear(ctx: Context) = withFleetStats(ctx) { it.yearFlightTime() }
    private fun fleetFlightTime30Day(ctx: Context) = withFleetStats(ctx) { it.monthFlightTime() }

    private fun fleetFlightsCount(ctx: Context) = withFleetStats(ctx) { it.numberFlights() }
    private fun fleetFlightsCountYear(ctx: Context) = withFleetStats(ctx) { it.yearNumberFlights() }
    private fun fleetFlightsCount30Day(ctx: Context) = withFleetStats(ctx) { it.monthNumberFlights() }

    private fun fleetEventsCount(ctx: Context) = withFleetStats(ctx) { it.totalEvents() }
    private fun fleetEventsCountYear(ctx: Context) = withFleetStats(ctx) { it.yearEvents() }
    private fun fleetEventsCountMonth(ctx: Context) = withFleetStats(ctx) { it.monthEvents() }

    private fun fleetUsersCount(ctx: Context) = withFleetStats(ctx) { it.numberUsers() }

    // Aggregate handlers
    private fun aggFlightTime(ctx: Context) = withAggStats(ctx) { it.flightTime() }
    private fun aggFlightTimeYear(ctx: Context) = withAggStats(ctx) { it.yearFlightTime() }
    private fun aggFlightTime30Day(ctx: Context) = withAggStats(ctx) { it.monthFlightTime() }

    private fun aggFlightsCount(ctx: Context) = withAggStats(ctx) { it.numberFlights() }
    private fun aggFlightsCountYear(ctx: Context) = withAggStats(ctx) { it.yearNumberFlights() }
    private fun aggFlightsCount30Day(ctx: Context) = withAggStats(ctx) { it.monthNumberFlights() }

    private fun aggEventsCount(ctx: Context) = withAggStats(ctx) { it.totalEvents() }
    private fun aggEventsCountYear(ctx: Context) = withAggStats(ctx) { it.yearEvents() }
    private fun aggEventsCountMonth(ctx: Context) = withAggStats(ctx) { it.monthEvents() }

    private fun aggFleetsCount(ctx: Context) = withAggStats(ctx) { it.numberFleets() }
}
