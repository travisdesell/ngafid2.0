package org.ngafid.www.routes.api

import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.config.JavalinConfig
import io.javalin.http.Context
import org.ngafid.core.Database
import org.ngafid.core.accounts.Fleet
import org.ngafid.core.flights.Flight
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
                get("labels/csv", FleetRoutes::getFleetLabelsCsv, Role.LOGGED_IN)
            }
        }
    }

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
}
