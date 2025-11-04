package org.ngafid.www.routes.api

import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.config.JavalinConfig
import io.javalin.http.Context
import org.ngafid.core.Database
import org.ngafid.core.event.EventDefinition
import org.ngafid.core.flights.Airframes
import org.ngafid.core.flights.Flight
import org.ngafid.www.ErrorResponse
import org.ngafid.www.EventStatistics
import org.ngafid.www.routes.Role
import org.ngafid.www.routes.RouteProvider
import org.ngafid.www.routes.SessionUtility
import java.sql.SQLException
import java.time.LocalDate
import java.util.logging.Logger

object StatisticsApiRoutes : RouteProvider() {
    private val LOG = Logger.getLogger(StatisticsApiRoutes::class.java.name)

    override fun bind(app: JavalinConfig) {
        app.router.apiBuilder {
            path("/api") {
                get("aggregate/airframes", StatisticsApiRoutes::getAggregateAirframes, Role.LOGGED_IN)
                get("aggregate/meta", StatisticsApiRoutes::getAggregateMeta, Role.LOGGED_IN)
                get("aggregate/trends/meta", StatisticsApiRoutes::getAggregateTrendsMeta, Role.LOGGED_IN)

                get("event_statistics/meta", StatisticsApiRoutes::getEventStatisticsMeta, Role.LOGGED_IN)
                get("event_statistics/event_counts", StatisticsApiRoutes::getAllEventCountsByAirframe, Role.LOGGED_IN)
                get("event_statistics/monthly_event_counts", StatisticsApiRoutes::getMonthlyEventCounts, Role.LOGGED_IN)
                get("event_statistics/airframe/{aid}", StatisticsApiRoutes::getOneEventCountsByAirframe, Role.LOGGED_IN)

                get("airframes", StatisticsApiRoutes::getFleetAirframes, Role.LOGGED_IN)
            }
        }
    }

    /**
     * Returns all airframes (for aggregate view)
     * GET /api/aggregate/airframes
     */
    fun getAggregateAirframes(ctx: Context) {
        val user = SessionUtility.getUser(ctx)

        if (!user.hasAggregateView()) {
            LOG.severe("INVALID ACCESS: user did not have aggregate access to view aggregate airframes.")
            ctx.status(401).json(ErrorResponse("Access denied", "User does not have aggregate access."))
            return
        }

        try {
            Database.getConnection().use { connection ->
                val airframes = Airframes.getAllWithIds(connection)
                ctx.json(airframes)
            }
        } catch (e: SQLException) {
            LOG.severe("Error getting aggregate airframes: ${e.message}")
            ctx.json(ErrorResponse(e)).status(500)
        }
    }

    /**
     * Returns metadata for aggregate page (airframes)
     * GET /api/aggregate/meta
     */
    fun getAggregateMeta(ctx: Context) {
        data class AggregateMetaResponse(
            val airframes: Array<Airframes.AirframeNameID>
        )

        val user = SessionUtility.getUser(ctx)

        if (!user.hasAggregateView()) {
            LOG.severe("INVALID ACCESS: user did not have aggregate access.")
            ctx.status(401).json(ErrorResponse("Access denied", "User does not have aggregate access."))
            return
        }

        try {
            Database.getConnection().use { connection ->
                val airframes = Airframes.getAllWithIds(connection)

                ctx.json(AggregateMetaResponse(airframes = airframes))
            }
        } catch (e: SQLException) {
            LOG.severe("Error getting aggregate meta: ${e.message}")
            ctx.json(ErrorResponse(e)).status(500)
        }
    }

    /**
     * Returns metadata for aggregate trends page (airframes, event names, tag names)
     * GET /api/aggregate/trends/meta
     */
    fun getAggregateTrendsMeta(ctx: Context) {
        data class AggregateTrendsMetaResponse(
            val airframes: Array<Airframes.AirframeNameID>,
            val eventNames: List<String>,
            val tagNames: List<String>
        )

        val user = SessionUtility.getUser(ctx)

        if (!user.hasAggregateView()) {
            LOG.severe("INVALID ACCESS: user did not have aggregate access to view aggregate trends.")
            ctx.status(401).json(ErrorResponse("Access denied", "User does not have aggregate access."))
            return
        }

        try {
            Database.getConnection().use { connection ->
                val airframes = Airframes.getAllWithIds(connection)
                val eventNames = EventDefinition.getUniqueNames(connection)
                val tagNames = Flight.getAllTagNames(connection)

                ctx.json(
                    AggregateTrendsMetaResponse(
                        airframes = airframes,
                        eventNames = eventNames,
                        tagNames = tagNames
                    )
                )
            }
        } catch (e: SQLException) {
            LOG.severe("Error getting aggregate trends meta: ${e.message}")
            ctx.json(ErrorResponse(e)).status(500)
        }
    }

    /**
     * Returns metadata for event statistics page
     * GET /api/event_statistics/meta
     */
    fun getEventStatisticsMeta(ctx: Context) {
        data class EventStatisticsMetaResponse(
            val eventDefinitions: List<EventDefinition>,
            val airframeMap: Map<Int, String>
        )

        val user = SessionUtility.getUser(ctx)
        val fleetId = user.fleetId

        try {
            Database.getConnection().use { connection ->
                val eventDefinitions = EventDefinition.getAll(connection)
                val airframeMap = Airframes.getIdToNameMap(connection, fleetId)

                ctx.json(
                    EventStatisticsMetaResponse(
                        eventDefinitions = eventDefinitions,
                        airframeMap = airframeMap
                    )
                )
            }
        } catch (e: SQLException) {
            LOG.severe("Error getting event statistics meta: ${e.message}")
            ctx.json(ErrorResponse(e)).status(500)
        }
    }

    /**
     * Returns event counts by airframe
     * GET /api/event_statistics/event_counts?startDate=...&endDate=...&aggregate=true|false
     */
    fun getAllEventCountsByAirframe(ctx: Context) {
        val user = SessionUtility.getUser(ctx)
        val startDate = ctx.queryParam("startDate")
        if (startDate == null) {
            ctx.status(400).json(ErrorResponse("Missing parameter", "startDate is required"))
            return
        }
        val endDate = ctx.queryParam("endDate")
        if (endDate == null) {
            ctx.status(400).json(ErrorResponse("Missing parameter", "endDate is required"))
            return
        }
        val aggregate = ctx.queryParam("aggregate")?.toBoolean() ?: false

        var fleetId = user.fleetId

        // Check access permissions
        if (aggregate) {
            if (!user.hasAggregateView()) {
                LOG.severe("INVALID ACCESS: user did not have aggregate access to view all event counts.")
                ctx.status(401).json(ErrorResponse("Access denied", "User does not have aggregate access."))
                return
            }
            fleetId = -1
        } else {
            if (!user.hasViewAccess(fleetId)) {
                LOG.severe("INVALID ACCESS: user did not have access to view events for this fleet.")
                ctx.status(401).json(ErrorResponse("Access denied", "User does not have access to view events for this fleet."))
                return
            }
        }

        try {
            Database.getConnection().use { connection ->
                val eventCountsMap = EventStatistics.getEventCounts(
                    connection,
                    fleetId,
                    LocalDate.parse(startDate),
                    LocalDate.parse(endDate)
                )
                ctx.json(eventCountsMap)
            }
        } catch (e: SQLException) {
            LOG.severe("Error getting event counts: ${e.message}")
            ctx.json(ErrorResponse(e)).status(500)
        } catch (e: Exception) {
            LOG.severe("Error parsing dates: ${e.message}")
            ctx.status(400).json(ErrorResponse("Invalid date format", "Dates must be in ISO format (YYYY-MM-DD)"))
        }
    }

    /**
     * Returns monthly event counts
     * GET /api/event_statistics/monthly_event_counts?startDate=...&endDate=...&aggregatePage=true|false&eventName=...
     */
    fun getMonthlyEventCounts(ctx: Context) {
        val user = SessionUtility.getUser(ctx)
        val startDate = ctx.queryParam("startDate")
        if (startDate == null) {
            ctx.status(400).json(ErrorResponse("Missing parameter", "startDate is required"))
            return
        }
        val endDate = ctx.queryParam("endDate")
        if (endDate == null) {
            ctx.status(400).json(ErrorResponse("Missing parameter", "endDate is required"))
            return
        }
        val aggregatePage = ctx.queryParam("aggregatePage")?.toBoolean() ?: false
        val eventName = ctx.queryParam("eventName") // Optional

        try {
            Database.getConnection().use { connection ->
                val map: Map<String, Map<String, EventStatistics.MonthlyEventCounts>>

                if (aggregatePage) {
                    if (!user.hasAggregateView()) {
                        LOG.severe("INVALID ACCESS: user did not have aggregate access to view aggregate trends page.")
                        ctx.status(401).json(ErrorResponse("Access denied", "User does not have aggregate access."))
                        return
                    }
                    map = EventStatistics.getMonthlyEventCounts(
                        connection,
                        -1,
                        LocalDate.parse(startDate),
                        LocalDate.parse(endDate)
                    )
                } else {
                    val fleetId = user.fleetId
                    if (!user.hasViewAccess(fleetId)) {
                        LOG.severe("INVALID ACCESS: user did not have access to view imports for this fleet.")
                        ctx.status(401).json(ErrorResponse("Access denied", "User does not have access to view imports for this fleet."))
                        return
                    }
                    map = EventStatistics.getMonthlyEventCounts(
                        connection,
                        fleetId,
                        LocalDate.parse(startDate),
                        LocalDate.parse(endDate)
                    )
                }

                // Filter by event name if provided
                if (eventName != null) {
                    ctx.json(map[eventName] ?: emptyMap<String, EventStatistics.MonthlyEventCounts>())
                } else {
                    ctx.json(map)
                }
            }
        } catch (e: SQLException) {
            LOG.severe("Error getting monthly event counts: ${e.message}")
            ctx.json(ErrorResponse(e)).status(500)
        } catch (e: Exception) {
            LOG.severe("Error parsing dates: ${e.message}")
            ctx.status(400).json(ErrorResponse("Invalid date format", "Dates must be in ISO format (YYYY-MM-DD)"))
        }
    }

    /**
     * Returns event counts for a single airframe
     * GET /api/event_statistics/airframe/{aid}
     */
    fun getOneEventCountsByAirframe(ctx: Context) {
        val user = SessionUtility.getUser(ctx)
        val fleetId = user.fleetId
        val airframeNameId = ctx.pathParam("aid").toIntOrNull()
        if (airframeNameId == null) {
            ctx.status(400).json(ErrorResponse("Invalid parameter", "airframe ID must be a number"))
            return
        }

        try {
            Database.getConnection().use { connection ->
                val eventStats = if (airframeNameId == 0) {
                    EventStatistics(connection, 0, "Generic", fleetId)
                } else {
                    val airframe = Airframes.Airframe(connection, airframeNameId)
                    EventStatistics(connection, airframe.id, airframe.name, fleetId)
                }
                ctx.json(eventStats)
            }
        } catch (e: SQLException) {
            LOG.severe("Error getting event counts for airframe: ${e.message}")
            ctx.json(ErrorResponse(e)).status(500)
        }
    }

    /**
     * Returns airframes for the current user's fleet
     * GET /api/airframes
     */
    fun getFleetAirframes(ctx: Context) {
        val user = SessionUtility.getUser(ctx)
        val fleetId = user.fleetId

        try {
            Database.getConnection().use { connection ->
                val airframes = Airframes.getAllWithIds(connection, fleetId)
                ctx.json(airframes)
            }
        } catch (e: SQLException) {
            LOG.severe("Error getting fleet airframes: ${e.message}")
            ctx.json(ErrorResponse(e)).status(500)
        }
    }
}

