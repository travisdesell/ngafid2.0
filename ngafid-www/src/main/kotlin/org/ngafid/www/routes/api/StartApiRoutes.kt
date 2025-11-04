package org.ngafid.www.routes.api

import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.config.JavalinConfig
import io.javalin.http.Context
import org.ngafid.core.Database
import org.ngafid.core.accounts.FleetAccess
import org.ngafid.core.accounts.User
import org.ngafid.core.flights.Airframes
import org.ngafid.www.ErrorResponse
import org.ngafid.www.routes.Role
import org.ngafid.www.routes.RouteProvider
import org.ngafid.www.routes.SessionUtility
import java.sql.SQLException
import java.util.logging.Logger

object StartApiRoutes : RouteProvider() {
    private val LOG = Logger.getLogger(StartApiRoutes::class.java.name)

    data class Message(
        val type: String,
        val message: String
    )

    data class UserInfo(
        val email: String,
        val firstName: String,
        val lastName: String,
        val fleetId: Int
    )

    override fun bind(app: JavalinConfig) {
        app.router.apiBuilder {
            path("/api") {
                get("start", StartApiRoutes::getStart, Role.OPEN)
                get("navbar", StartApiRoutes::getNavbar, Role.LOGGED_IN)
                get("welcome", StartApiRoutes::getWelcome, Role.LOGGED_IN)
                get("waiting", StartApiRoutes::getWaiting, Role.LOGGED_IN)
            }
        }
    }

    /**
     * Returns public start/home page information
     * Includes any messages to display and basic session state
     */
    fun getStart(ctx: Context) {
        data class StartResponse(
            val isLoggedIn: Boolean,
            val messages: List<Message>? = null,
            val user: UserInfo? = null
        )

        val user: User? = ctx.sessionAttribute("user")
        val messages = mutableListOf<Message>()

        // Check for special query params that indicate messages
        when (ctx.queryParam("msg")) {
            "logout_success" -> messages.add(Message("success", "You have been successfully logged out."))
            "access_denied" -> messages.add(Message("danger", "You attempted to load a page you did not have access to or attempted to access a page while not logged in."))
        }

        val userInfo = user?.let {
            UserInfo(
                email = it.email,
                firstName = it.firstName,
                lastName = it.lastName,
                fleetId = it.fleetId
            )
        }

        ctx.json(
            StartResponse(
                isLoggedIn = user != null,
                messages = messages.ifEmpty { null },
                user = userInfo
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
                        "not_found" -> messages.add(Message("danger", "The page you attempted to access does not exist."))
                    }
                }

                ctx.json(
                    WelcomeResponse(
                        navbar = navbarData,
                        airframes = airframes,
                        messages = messages.ifEmpty { null }
                    )
                )
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
            val status: String,
            val message: String
        )

        ctx.json(
            WaitingResponse(
                status = "waiting",
                message = "Your account is awaiting approval from your fleet manager."
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
}

