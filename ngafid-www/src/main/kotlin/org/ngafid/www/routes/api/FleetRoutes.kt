package org.ngafid.www.routes.api

import io.javalin.apibuilder.ApiBuilder.delete
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.config.JavalinConfig
import io.javalin.http.Context
import io.javalin.http.NotFoundResponse
import org.ngafid.core.Database
import org.ngafid.core.accounts.Fleet
import org.ngafid.core.flights.Flight
import org.ngafid.core.labels.FleetLabel
import org.ngafid.core.labels.FlightLabelSection
import org.ngafid.www.routes.Role
import org.ngafid.www.routes.RouteProvider
import org.ngafid.www.routes.SessionUtility
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object FleetRoutes : RouteProvider() {
    override fun bind(app: JavalinConfig) {
        app.router.apiBuilder {
            path("/api/fleet") {
                get(FleetRoutes::get, Role.LOGGED_IN)
                get("count", { ctx ->
                    Database.getConnection().use {
                        ctx.json(Fleet.getNumberFleets(it))
                    }
                }, Role.LOGGED_IN)
                get("count/aggregate", { ctx -> Database.getConnection().use { ctx.json(Fleet.getNumberFleets(it)) } })
                get("names", FleetRoutes::getNames, Role.LOGGED_IN)
                get("labels/csv", FleetRoutes::getFleetLabelsCsv, Role.LOGGED_IN)
                get("labels", FleetRoutes::getLabelDefinitions, Role.LOGGED_IN)
                post("labels", FleetRoutes::postLabelDefinition, Role.LOGGED_IN)
                path("labels/{lid}") {
                    delete(FleetRoutes::deleteLabelDefinition, Role.LOGGED_IN)
                }
            }
        }
    }

    fun getLabelDefinitions(ctx: Context) {
        val user = SessionUtility.getUser(ctx)
        Database.getConnection().use { connection ->
            val list = FleetLabel.getByFleet(connection, user.fleetId)
            ctx.json(list.map { mapOf("id" to it.id, "labelText" to it.labelText, "displayOrder" to it.displayOrder) })
        }
    }

    fun postLabelDefinition(ctx: Context) {
        val user = SessionUtility.getUser(ctx)
        val body = ctx.bodyAsClass(LabelDefinitionDto::class.java)
        val labelText = body.labelText?.trim() ?: ""
        if (labelText.isEmpty()) {
            ctx.status(400).json(mapOf("error" to "labelText is required"))
            return
        }
        Database.getConnection().use { connection ->
            val created = FleetLabel.insert(connection, user.fleetId, labelText)
                ?: run {
                    ctx.status(400).json(mapOf("error" to "Label already exists for this fleet"))
                    return@use
                }
            ctx.json(mapOf("id" to created.id, "labelText" to created.labelText, "displayOrder" to created.displayOrder))
        }
    }

    fun deleteLabelDefinition(ctx: Context) {
        val user = SessionUtility.getUser(ctx)
        val id = ctx.pathParam("lid").toInt()
        Database.getConnection().use { connection ->
            val fleetId = FleetLabel.getFleetIdForDefinition(connection, id)
                ?: throw NotFoundResponse("Label definition $id not found.")
            if (fleetId != user.fleetId) {
                throw NotFoundResponse("Label definition $id does not belong to your fleet.")
            }
            FleetLabel.delete(connection, id)
            ctx.status(204)
        }
    }

    private data class LabelDefinitionDto(val labelText: String?)

    private fun escapeCsv(value: String?): String {
        if (value == null) return ""
        if (value.contains('"') || value.contains(',') || value.contains('\n') || value.contains('|')) {
            return "\"" + value.replace("\"", "\"\"") + "\""
        }
        return value
    }

    private val csvDateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    /** Format timestamp for CSV in UTC (matches flight times and table display). If ts is in 1970 (offset), add flightStartMs first. */
    private fun formatTimestampForCsv(ts: java.sql.Timestamp?, flightStartMs: Long? = null): String {
        if (ts == null) return ""
        val epochMs = if (flightStartMs != null && ts.toLocalDateTime().year < 1980) {
            flightStartMs + ts.time
        } else ts.time
        return Instant.ofEpochMilli(epochMs).atZone(ZoneOffset.UTC).format(csvDateTimeFormat)
    }

    fun getFleetLabelsCsv(ctx: Context) {
        val user = SessionUtility.getUser(ctx)
        Database.getConnection().use { connection ->
            val sections = FlightLabelSection.getByFleet(connection, user.fleetId)
            val flightStartMsByFlightId = sections.map { it.flightId }.distinct().associateWith { fid ->
                Flight.getFlight(connection, fid)?.let { java.sql.Timestamp.valueOf(it.getStartDateTime()).time } ?: 0L
            }
            val header = "flight_id,tail_number,airframe,start_time,end_time,start_index,end_index,start_value,end_value,label_text,parameter_names"
            val rows = sections.map { s ->
                val flightStartMs = flightStartMsByFlightId[s.flightId] ?: 0L
                val startStr = s.startTimeRaw?.trim()?.takeIf { it.isNotEmpty() } ?: formatTimestampForCsv(s.startTime, flightStartMs)
                val endStr = s.endTimeRaw?.trim()?.takeIf { it.isNotEmpty() } ?: formatTimestampForCsv(s.endTime, flightStartMs)
                listOf(
                    s.flightId.toString(),
                    escapeCsv(s.tailNumber),
                    escapeCsv(s.airframe),
                    startStr,
                    endStr,
                    s.startIndex.toString(),
                    s.endIndex.toString(),
                    s.startValue?.toString() ?: "",
                    s.endValue?.toString() ?: "",
                    escapeCsv(s.labelText),
                    escapeCsv(s.parameterNames.joinToString("|"))
                ).joinToString(",")
            }
            val csv = (listOf(header) + rows).joinToString("\n")
            ctx.contentType("text/csv")
            ctx.header("Content-Disposition", "attachment; filename=\"fleet_labels.csv\"")
            ctx.result(csv)
        }
    }

    fun get(ctx: Context) {
        val user = SessionUtility.getUser(ctx)

        Database.getConnection().use { connection ->
            ctx.json(Fleet.get(connection, user.fleetId))
        }
    }

    fun getNames(ctx: Context) {
        
        //Get the name of all fleets in the database
        Database.getConnection().use { connection ->
            ctx.json(Fleet.getAllFleetNames(connection))
        }

    }

}