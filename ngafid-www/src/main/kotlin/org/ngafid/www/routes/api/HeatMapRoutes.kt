// ngafid-www/src/main/kotlin/org/ngafid/www/routes/api/HeatMapRoutes.kt
package org.ngafid.www.routes.api

import com.google.gson.JsonSyntaxException
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.config.JavalinConfig
import io.javalin.http.Context
import java.sql.Date
import java.sql.SQLException
import java.util.logging.Logger
import org.ngafid.core.Config
import org.ngafid.core.heatmap.HeatmapPointsProcessor
import org.ngafid.www.ErrorResponse
import org.ngafid.www.WebServer
import org.ngafid.www.routes.Role
import org.ngafid.www.routes.RouteProvider
import org.ngafid.www.routes.SessionUtility

object HeatMapRoutes : RouteProvider() {

    private val LOG: Logger = Logger.getLogger(HeatMapRoutes::class.java.name)
    private const val DEFAULT_CHART_TILE_BASE_URL = "http://localhost:8187"

    override fun bind(app: JavalinConfig) {

        app.router.apiBuilder {
            path("/api/heatmap") {
                get("config", HeatMapRoutes::getMapConfig, Role.LOGGED_IN)
                get("events", HeatMapRoutes::getEventsInBox, Role.LOGGED_IN)
                post("points/batch", HeatMapRoutes::postPointsBatch, Role.LOGGED_IN)
                get("event-columns", HeatMapRoutes::getEventColumnsValues, Role.LOGGED_IN)
            }
        }

    }

    private fun getMapConfig(ctx: Context) {

        SessionUtility.getUser(ctx)

        val azureMapsKey = readConfig("ngafid.azure.maps.key")
        val chartTileBaseUrl = sanitizeChartTileBase(readConfig("ngafid.chart.tile.base.url"))

        ctx.json(
            mapOf(
                "azureMapsKey" to azureMapsKey,
                "chartTileBaseUrl" to chartTileBaseUrl,
            )
        )

    }

    private fun getEventsInBox(ctx: Context) {

        SessionUtility.getUser(ctx)

        try {
            val airframe = ctx.queryParam("airframe")
            val eventDefinitionIds = parseEventDefinitionIds(ctx.queryParam("event_definition_ids"))
            val startDate = Date.valueOf(requiredQueryParam(ctx, "start_date"))
            val endDate = Date.valueOf(requiredQueryParam(ctx, "end_date"))
            val areaMinLat = parseRequiredDouble(ctx, "area_min_lat")
            val areaMaxLat = parseRequiredDouble(ctx, "area_max_lat")
            val areaMinLon = parseRequiredDouble(ctx, "area_min_lon")
            val areaMaxLon = parseRequiredDouble(ctx, "area_max_lon")
            val minSeverity = parseOptionalDouble(ctx, "min_severity")
            val maxSeverity = parseOptionalDouble(ctx, "max_severity")

            val events = HeatmapPointsProcessor.getEvents(
                airframe,
                eventDefinitionIds,
                startDate,
                endDate,
                areaMinLat,
                areaMaxLat,
                areaMinLon,
                areaMaxLon,
                minSeverity,
                maxSeverity,
            )

            ctx.json(events)
        } catch (e: IllegalArgumentException) {
            ctx.status(400).json(ErrorResponse("Invalid Request", e.message ?: "Invalid request."))
        } catch (e: SQLException) {
            LOG.severe("Failed to fetch heat map events: ${e.message}")
            ctx.status(500).json(ErrorResponse(e))
        }

    }

    @Suppress("UNCHECKED_CAST")
    private fun postPointsBatch(ctx: Context) {

        SessionUtility.getUser(ctx)

        try {

            val body = WebServer.GSON.fromJson(ctx.body(), Map::class.java) as? Map<*, *>
                ?: throw IllegalArgumentException("Request body must be a JSON object.")

            val eventIdsRaw = body["event_ids"] as? List<*>
                ?: throw IllegalArgumentException("Missing required field: event_ids")

            val eventIds = eventIdsRaw.mapNotNull { (it as? Number)?.toInt() }

            if (eventIds.isEmpty()) {
                ctx.json(mapOf("results" to emptyList<Map<String, Any?>>()))
                return
            }

            val results = HeatmapPointsProcessor.getCoordinatesForEventIds(eventIds)
            ctx.json(mapOf("results" to results))
        } catch (e: JsonSyntaxException) {
            ctx.status(400).json(ErrorResponse("Invalid Request", "Request body must be valid JSON."))
        } catch (e: IllegalArgumentException) {
            ctx.status(400).json(ErrorResponse("Invalid Request", e.message ?: "Invalid request."))
        } catch (e: Exception) {
            LOG.severe("Failed to fetch heat map points batch: ${e.message}")
            ctx.status(500).json(ErrorResponse(e))
        }

    }

    private fun getEventColumnsValues(ctx: Context) {

        SessionUtility.getUser(ctx)

        try {
            val eventId = parseRequiredInt(ctx, "event_id")
            val flightId = parseRequiredInt(ctx, "flight_id")
            val timestamp = requiredQueryParam(ctx, "timestamp")

            val response = HeatmapPointsProcessor.getEventColumnsValues(eventId, flightId, timestamp)
            ctx.json(response)
        } catch (e: IllegalArgumentException) {
            ctx.status(400).json(ErrorResponse("Invalid Request", e.message ?: "Invalid request."))
        } catch (e: Exception) {
            LOG.severe("Failed to fetch heat map event columns: ${e.message}")
            ctx.status(500).json(ErrorResponse(e))
        }

    }

    private fun requiredQueryParam(ctx: Context, name: String): String {

        val value = ctx.queryParam(name)?.trim()
        if (value.isNullOrEmpty())
            throw IllegalArgumentException("Missing required query parameter: $name")

        return value

    }

    private fun parseRequiredInt(ctx: Context, name: String): Int {

        val value = requiredQueryParam(ctx, name)
        return value.toIntOrNull() ?: throw IllegalArgumentException("Query parameter '$name' must be an integer.")

    }

    private fun parseRequiredDouble(ctx: Context, name: String): Double {

        val value = requiredQueryParam(ctx, name)
        return value.toDoubleOrNull() ?: throw IllegalArgumentException("Query parameter '$name' must be a number.")

    }

    private fun parseOptionalDouble(ctx: Context, name: String): Double? {

        val value = ctx.queryParam(name)?.trim() ?: return null
        if (value.isEmpty())
            return null

        return value.toDoubleOrNull() ?: throw IllegalArgumentException("Query parameter '$name' must be a number.")

    }

    private fun parseEventDefinitionIds(raw: String?): List<Int> {

        if (raw.isNullOrBlank())
            return emptyList()

        return raw.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map {
                it.toIntOrNull() ?: throw IllegalArgumentException("Invalid event definition id: $it")
            }

    }

    private fun readConfig(property: String): String? {

        return try {
            Config.getProperty(property)?.trim()?.takeIf { it.isNotEmpty() }
        } catch (_: RuntimeException) {
            null
        }

    }

    private fun sanitizeChartTileBase(value: String?): String {

        val trimmed = value?.trim()?.trimEnd('/')
        return if (trimmed.isNullOrEmpty())
            DEFAULT_CHART_TILE_BASE_URL
        else
            trimmed

    }

}