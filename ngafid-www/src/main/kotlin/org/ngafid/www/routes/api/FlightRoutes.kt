package org.ngafid.www.routes.api

import com.fasterxml.jackson.annotation.JsonProperty
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.config.JavalinConfig
import io.javalin.http.Context
import io.javalin.http.NotFoundResponse
import org.ngafid.core.Database
import org.ngafid.core.event.Event
import org.ngafid.core.event.EventDefinition
import org.ngafid.core.flights.Flight
import org.ngafid.core.labels.FlightLabelSection
import org.ngafid.core.util.FlightTag
import org.ngafid.www.ErrorResponse
import org.ngafid.www.routes.*
import java.util.*
import java.sql.PreparedStatement
import java.sql.Timestamp
import java.time.LocalDate
import kotlin.booleanArrayOf
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object FlightRoutes : RouteProvider() {
    override fun bind(app: JavalinConfig) {
        app.router.apiBuilder {
            path("/api/flight") {
                get(FlightsJavalinRoutes::postFlights, Role.LOGGED_IN)
                get("double-series", DoubleSeriesJavalinRoutes::getAllDoubleSeriesNames, Role.LOGGED_IN)
                get("turn-to-final", AnalysisJavalinRoutes::postTurnToFinal, Role.LOGGED_IN)
                get("flight_hours_by_airframe", FlightRoutes::getFlightHoursByAirframe, Role.LOGGED_IN)
                get("aggregate/flight_hours_by_airframe", FlightRoutes::getAggregateFlightHoursByAirframe, Role.LOGGED_IN)

                RouteUtility.getStat("time/past-month") { ctx, stats -> ctx.json(stats.monthFlightTime()) }
                RouteUtility.getStat("time/past-year") { ctx, stats -> ctx.json(stats.yearFlightTime()) }
                RouteUtility.getStat("time") { ctx, stats -> ctx.json(stats.flightTime()) }

                RouteUtility.getStat("count/past-month") { ctx, stats -> ctx.json(stats.monthNumberFlights()) }
                RouteUtility.getStat("count/past-year") { ctx, stats -> ctx.json(stats.yearNumberFlights()) }
                RouteUtility.getStat("count/with-warning") { ctx, stats -> ctx.json(stats.flightsWithWarning()) }
                RouteUtility.getStat("count/with-error") { ctx, stats -> ctx.json(stats.flightsWithError()) }
                RouteUtility.getStat("count") { ctx, stats -> ctx.json(stats.numberFlights()) }

                path("{fid}") {
                    // TODO: There is no reason for this to exist as a separate route from fetching normal double series.
                    // but the JS code is not set up to handle that and it would be a minor pain in the ass to change it.
                    get("coordinates", AnalysisJavalinRoutes::postCoordinates, Role.LOGGED_IN)

                    // Same goes for this, although it does have some body parameters
                    get("loci-metrics", AnalysisJavalinRoutes::postLociMetrics, Role.LOGGED_IN)

                    get("csv", { ctx -> DataJavalinRoutes.getCSV(ctx, false) }, Role.LOGGED_IN)
                    get("/csv/generated", { ctx -> DataJavalinRoutes.getCSV(ctx, true) }, Role.LOGGED_IN)
                    get("kml", DataJavalinRoutes::getKML, Role.LOGGED_IN)
                    get("xplane", DataJavalinRoutes::getXPlane, Role.LOGGED_IN)

                    get("double-series", DoubleSeriesJavalinRoutes::postDoubleSeriesNames, Role.LOGGED_IN)
                    get("double-series/{series}", DoubleSeriesJavalinRoutes::postDoubleSeries, Role.LOGGED_IN)

                    get("events", FlightRoutes::getFlightEvents, Role.LOGGED_IN)

                    path("labels") {
                        get(FlightRoutes::getFlightLabels, Role.LOGGED_IN)
                        get("csv", FlightRoutes::getFlightLabelsCsv, Role.LOGGED_IN)
                        post(FlightRoutes::postFlightLabel, Role.LOGGED_IN)
                        post("import", FlightRoutes::postFlightLabelsImport, Role.LOGGED_IN)
                        path("{lid}") {
                            put(FlightRoutes::putFlightLabel, Role.LOGGED_IN)
                            delete(FlightRoutes::deleteFlightLabel, Role.LOGGED_IN)
                        }
                    }

                    path("tag") {
                        // Delete all tags
                        delete(FlightRoutes::deleteAllFlightTags, Role.LOGGED_IN)
                        get("unassociated", FlightRoutes::getUnassociatedTags, Role.LOGGED_IN);
                        get(FlightRoutes::getFlightTags, Role.LOGGED_IN)

                        path("{tid}") {
                            // Delete specified tag
                            delete(FlightRoutes::deleteFlightTag, Role.LOGGED_IN)

                            // Associate specified tag
                            put(FlightRoutes::putFlightAssociateTag, Role.LOGGED_IN)
                        }
                    }

                    // TODO
                    // get(FlightRoutes::getFlight, Role.LOGGED_IN)
                }

            }
        }
    }

    fun getFlightTags(ctx: Context) {
        Database.getConnection().use { connection ->
            ctx.json(
                Objects.requireNonNullElse<Any>(
                    Flight.getTags(connection, ctx.pathParam("fid").toInt()),
                    ErrorResponse("error", "No tags found for flight.")
                )
            )
        }
    }

    data class FlightLabelSectionDto(
        @field:JsonProperty val id: Int?,
        @field:JsonProperty val flightId: Int? = null,
        @field:JsonProperty val tailNumber: String? = null,
        @field:JsonProperty val airframe: String? = null,
        @field:JsonProperty val startIndex: Int,
        @field:JsonProperty val endIndex: Int,
        @field:JsonProperty val startTime: Long,
        @field:JsonProperty val endTime: Long,
        @field:JsonProperty val startTimeDisplay: String? = null,
        @field:JsonProperty val endTimeDisplay: String? = null,
        @field:JsonProperty val startValue: Double?,
        @field:JsonProperty val endValue: Double?,
        @field:JsonProperty val labelText: String?,
        @field:JsonProperty val parameterNames: List<String>
    )

    data class FlightLabelUpdateDto(
        @field:JsonProperty val labelText: String?
    )

    fun getFlightLabels(ctx: Context) {
        val user = SessionUtility.getUser(ctx)
        val flightId = ctx.pathParam("fid").toInt()

        Database.getConnection().use { connection ->
            val flight = Flight.getFlight(connection, flightId)
            if (flight == null || flight.fleetId != user.fleetId) {
                throw NotFoundResponse("Flight with id $flightId not found.")
            }
            val sections = FlightLabelSection.getByFlight(connection, flightId).map {
                FlightLabelSectionDto(
                    id = it.id,
                    flightId = it.flightId,
                    tailNumber = it.tailNumber,
                    airframe = it.airframe,
                    startIndex = it.startIndex,
                    endIndex = it.endIndex,
                    startTime = it.startTime.time / 1000,
                    endTime = it.endTime.time / 1000,
                    startTimeDisplay = it.startTimeRaw?.trim()?.takeIf { s -> s.isNotEmpty() } ?: formatTimestampForDisplay(it.startTime),
                    endTimeDisplay = it.endTimeRaw?.trim()?.takeIf { s -> s.isNotEmpty() } ?: formatTimestampForDisplay(it.endTime),
                    startValue = it.startValue,
                    endValue = it.endValue,
                    labelText = it.labelText,
                    parameterNames = it.parameterNames
                )
            }
            ctx.json(sections)
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

    /** Format timestamp for display in UTC so create (POST) and read (GET) show the same time. */
    private fun formatTimestampForDisplay(ts: java.sql.Timestamp?): String? {
        if (ts == null) return null
        return Instant.ofEpochMilli(ts.time).atZone(ZoneOffset.UTC).format(csvDateTimeFormat)
    }

    /** Format timestamp for CSV in UTC (matches flight times and table display). If ts is in 1970 (offset), add flightStartMs first. */
    private fun formatTimestampForCsv(ts: java.sql.Timestamp?, flightStartMs: Long? = null): String {
        if (ts == null) return ""
        val epochMs = if (flightStartMs != null && ts.toLocalDateTime().year < 1980) {
            flightStartMs + ts.time
        } else ts.time
        return Instant.ofEpochMilli(epochMs).atZone(ZoneOffset.UTC).format(csvDateTimeFormat)
    }

    /** Parse CSV datetime: "yyyy-MM-dd HH:mm:ss" as UTC (matches flight times), or bare digits as Unix seconds. */
    private fun parseTimestampFromCsv(value: String?): Long? {
        if (value == null || value.isBlank()) return null
        val trimmed = value.trim()
        if (trimmed.all { it.isDigit() }) return trimmed.toLongOrNull()
        return try {
            val ldt = LocalDateTime.parse(trimmed, csvDateTimeFormat)
            ldt.atZone(ZoneOffset.UTC).toInstant().epochSecond
        } catch (_: Exception) {
            null
        }
    }

    fun getFlightLabelsCsv(ctx: Context) {
        val user = SessionUtility.getUser(ctx)
        val flightId = ctx.pathParam("fid").toInt()

        Database.getConnection().use { connection ->
            val flight = Flight.getFlight(connection, flightId)
            if (flight == null || flight.fleetId != user.fleetId) {
                throw NotFoundResponse("Flight with id $flightId not found.")
            }
            val sections = FlightLabelSection.getByFlight(connection, flightId)
            val flightStartMs = java.sql.Timestamp.valueOf(flight.getStartDateTime()).time
            val header = "flight_id,tail_number,airframe,start_time,end_time,start_index,end_index,start_value,end_value,label_text,parameter_names"
            val rows = sections.map { s ->
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
            ctx.header("Content-Disposition", "attachment; filename=\"flight_${flightId}_labels.csv\"")
            ctx.result(csv)
        }
    }

    fun postFlightLabelsImport(ctx: Context) {
        val user = SessionUtility.getUser(ctx)
        val flightId = ctx.pathParam("fid").toInt()
        val file = ctx.uploadedFile("file") ?: throw NotFoundResponse("No file uploaded. Use form field 'file'.")

        Database.getConnection().use { connection ->
            val flight = Flight.getFlight(connection, flightId)
            if (flight == null || flight.fleetId != user.fleetId) {
                throw NotFoundResponse("Flight with id $flightId not found.")
            }
            val lines = file.content().bufferedReader(Charsets.UTF_8).use { it.readLines() }
            if (lines.size < 2) {
                ctx.status(400).json(mapOf("error" to "CSV must have header and at least one row"))
                return
            }
            val header = lines[0].split(",").map { it.trim().lowercase() }
            val startIndexIdx = header.indexOf("start_index").takeIf { it >= 0 } ?: header.indexOf("startindex")
            val endIndexIdx = header.indexOf("end_index").takeIf { it >= 0 } ?: header.indexOf("endindex")
            val startTimeIdx = header.indexOf("start_time").takeIf { it >= 0 } ?: header.indexOf("starttime")
            val endTimeIdx = header.indexOf("end_time").takeIf { it >= 0 } ?: header.indexOf("endtime")
            val startValueIdx = header.indexOf("start_value").takeIf { it >= 0 } ?: header.indexOf("startvalue")
            val endValueIdx = header.indexOf("end_value").takeIf { it >= 0 } ?: header.indexOf("endvalue")
            val labelTextIdx = header.indexOf("label_text").takeIf { it >= 0 } ?: header.indexOf("labeltext")
            val paramNamesIdx = header.indexOf("parameter_names").takeIf { it >= 0 } ?: header.indexOf("parameternames")
            if (startIndexIdx < 0 || endIndexIdx < 0 || startTimeIdx < 0 || endTimeIdx < 0) {
                ctx.status(400).json(mapOf("error" to "CSV must include start_index, end_index, start_time, end_time columns"))
                return
            }
            fun parseCsvLine(line: String): List<String> {
                val out = mutableListOf<String>()
                var i = 0
                while (i < line.length) {
                    if (line[i] == '"') {
                        val sb = StringBuilder()
                        i++
                        while (i < line.length) {
                            if (line[i] == '"') {
                                if (i + 1 < line.length && line[i + 1] == '"') { sb.append('"'); i += 2 }
                                else { i++; break }
                            } else { sb.append(line[i]); i++ }
                        }
                        out.add(sb.toString())
                    } else {
                        val end = line.indexOf(',', i).let { if (it < 0) line.length else it }
                        out.add(line.substring(i, end).trim())
                        i = if (end < line.length) end + 1 else line.length
                    }
                }
                return out
            }
            var imported = 0
            for (idx in 1 until lines.size) {
                val row = parseCsvLine(lines[idx])
                if (row.size <= maxOf(startIndexIdx, endIndexIdx, startTimeIdx, endTimeIdx)) continue
                val startIndex = row.getOrNull(startIndexIdx)?.toIntOrNull() ?: continue
                val endIndex = row.getOrNull(endIndexIdx)?.toIntOrNull() ?: continue
                var startTimeSec = parseTimestampFromCsv(row.getOrNull(startTimeIdx))
                var endTimeSec = parseTimestampFromCsv(row.getOrNull(endTimeIdx))
                if (startTimeSec == null || endTimeSec == null) continue
                // If value is small (< 1e9), it's seconds-from-flight-start; convert to Unix seconds. Else it's already absolute (datetime string or Unix sec).
                val flightStartSec = java.sql.Timestamp.valueOf(flight.getStartDateTime()).time / 1000
                if (startTimeSec < 1_000_000_000L) startTimeSec = flightStartSec + startTimeSec
                if (endTimeSec < 1_000_000_000L) endTimeSec = flightStartSec + endTimeSec
                val startTimeStr = Instant.ofEpochSecond(startTimeSec).atZone(ZoneOffset.UTC).format(csvDateTimeFormat)
                val endTimeStr = Instant.ofEpochSecond(endTimeSec).atZone(ZoneOffset.UTC).format(csvDateTimeFormat)
                val startValue = row.getOrNull(startValueIdx)?.toDoubleOrNull()
                val endValue = row.getOrNull(endValueIdx)?.toDoubleOrNull()
                val labelText = if (labelTextIdx >= 0 && labelTextIdx < row.size) row[labelTextIdx] else ""
                val paramNames = if (paramNamesIdx >= 0 && paramNamesIdx < row.size) {
                    row[paramNamesIdx].split("|").map { it.trim() }.filter { it.isNotEmpty() }
                } else emptyList()
                val section = FlightLabelSection().apply {
                    this.flightId = flightId
                    this.startIndex = startIndex
                    this.endIndex = endIndex
                    startTime = Timestamp(startTimeSec * 1000)
                    endTime = Timestamp(endTimeSec * 1000)
                    this.startTimeStr = startTimeStr
                    this.endTimeStr = endTimeStr
                    this.startValue = startValue
                    this.endValue = endValue
                    this.labelText = labelText
                    parameterNames = paramNames.toMutableList()
                }
                FlightLabelSection.insert(connection, section)
                imported++
            }
            ctx.json(mapOf("imported" to imported))
        }
    }

    fun postFlightLabel(ctx: Context) {
        val user = SessionUtility.getUser(ctx)
        val flightId = ctx.pathParam("fid").toInt()
        val dto = ctx.bodyAsClass(FlightLabelSectionDto::class.java)

        Database.getConnection().use { connection ->
            val flight = Flight.getFlight(connection, flightId)
            if (flight == null || flight.fleetId != user.fleetId) {
                throw NotFoundResponse("Flight with id $flightId not found.")
            }

            // Frontend sends startTime/endTime as seconds from flight start; convert to wall-clock time.
            val flightStartMs = java.sql.Timestamp.valueOf(flight.getStartDateTime()).time
            val startTs = java.sql.Timestamp(flightStartMs + dto.startTime * 1000)
            val endTs = java.sql.Timestamp(flightStartMs + dto.endTime * 1000)
            val startDisplayStr = formatTimestampForDisplay(startTs)
            val endDisplayStr = formatTimestampForDisplay(endTs)
            val s = FlightLabelSection().apply {
                this.flightId = flightId
                startIndex = dto.startIndex
                endIndex = dto.endIndex
                startTime = startTs
                endTime = endTs
                startTimeStr = startDisplayStr
                endTimeStr = endDisplayStr
                startValue = dto.startValue
                endValue = dto.endValue
                labelText = dto.labelText
                parameterNames = dto.parameterNames.toMutableList()
            }

            val saved = FlightLabelSection.insert(connection, s)
            ctx.json(
                FlightLabelSectionDto(
                    id = saved.id,
                    flightId = saved.flightId,
                    tailNumber = saved.tailNumber,
                    airframe = saved.airframe,
                    startIndex = saved.startIndex,
                    endIndex = saved.endIndex,
                    startTime = saved.startTime.time / 1000,
                    endTime = saved.endTime.time / 1000,
                    startTimeDisplay = startDisplayStr,
                    endTimeDisplay = endDisplayStr,
                    startValue = saved.startValue,
                    endValue = saved.endValue,
                    labelText = saved.labelText,
                    parameterNames = saved.parameterNames
                )
            )
        }
    }

    fun putFlightLabel(ctx: Context) {
        val user = SessionUtility.getUser(ctx)
        val flightId = ctx.pathParam("fid").toInt()
        val labelId = ctx.pathParam("lid").toInt()
        val body = ctx.bodyAsClass(FlightLabelUpdateDto::class.java)

        Database.getConnection().use { connection ->
            val flight = Flight.getFlight(connection, flightId)
            if (flight == null || flight.fleetId != user.fleetId) {
                throw NotFoundResponse("Flight with id $flightId not found.")
            }
            val labelFlightId = FlightLabelSection.getFlightIdForLabel(connection, labelId)
                ?: throw NotFoundResponse("Label $labelId not found.")
            if (labelFlightId != flightId) {
                throw NotFoundResponse("Label $labelId does not belong to flight $flightId.")
            }
            FlightLabelSection.updateLabelText(connection, labelId, body.labelText ?: "")
            ctx.status(204)
        }
    }

    fun deleteFlightLabel(ctx: Context) {
        val user = SessionUtility.getUser(ctx)
        val flightId = ctx.pathParam("fid").toInt()
        val labelId = ctx.pathParam("lid").toInt()

        Database.getConnection().use { connection ->
            val flight = Flight.getFlight(connection, flightId)
            if (flight == null || flight.fleetId != user.fleetId) {
                throw NotFoundResponse("Flight with id $flightId not found.")
            }
            val labelFlightId = FlightLabelSection.getFlightIdForLabel(connection, labelId)
                ?: throw NotFoundResponse("Label $labelId not found.")
            if (labelFlightId != flightId) {
                throw NotFoundResponse("Label $labelId does not belong to flight $flightId.")
            }
            FlightLabelSection.delete(connection, labelId)
            ctx.status(204)
        }
    }

    fun getUnassociatedTags(ctx: Context) {
        val user = SessionUtility.getUser(ctx)
        val fleetId = user.fleetId
        val flightId = ctx.pathParam("fid").toInt()

        Database.getConnection().use { connection ->
            ctx.json(Flight.getUnassociatedTags(connection, flightId, fleetId))
        }
    }

    fun putFlightAssociateTag(ctx: Context) {
        val tagId = Objects.requireNonNull(ctx.pathParam("tid")).toInt()
        val flightId = ctx.pathParam("fid").toInt()

        Database.getConnection().use { connection ->

            //Attempt to associate the tag with the flight
            Flight.associateTag(flightId, tagId, connection)

            //Verify that the tag is now associated with the flight
            ctx.json(Objects.requireNonNull<FlightTag>(Flight.getTag(connection, tagId)))
        }
    }

    fun deleteAllFlightTags(ctx: Context) {
        val flightId = ctx.pathParam("fid").toInt()

        Database.getConnection().use { connection ->
            if (Flight.getFlight(connection, flightId) == null)
                throw NotFoundResponse("Flight with id $flightId not found.")

            Flight.disassociateAllTags(flightId, connection)
            ctx.json(Object())
        }
    }

    fun deleteFlightTag(ctx: Context) {
        val flightId = ctx.pathParam("fid").toInt()
        val tagId = ctx.pathParam("tid").toInt()

        Database.getConnection().use { connection ->
            if (Flight.getFlight(connection, flightId) == null)
                throw NotFoundResponse("Flight with id $flightId not found.")

            Flight.disassociateTags(tagId, connection, flightId)
            ctx.json(Object())
        }
    }


    class EventInfo(
        @field:JsonProperty val events: List<Event>,
        @field:JsonProperty val definitions: List<EventDefinition>?
    )

    fun getFlightEvents(ctx: Context) {
        val flightId = ctx.pathParam("fid").toInt()

        // TODO: Event definitions should just get loaded with a separate query... no need to complicate things
        val eventDefinitionsLoadedStr = ctx.queryParam("eventDefinitionsLoaded")

        val eventDefinitionsLoaded = eventDefinitionsLoadedStr != null && eventDefinitionsLoadedStr.toBoolean()

        Database.getConnection().use { connection ->
            val events: List<Event> = Event.getAll(connection, flightId)
            var definitions: List<EventDefinition>? = null

            if (!eventDefinitionsLoaded) {
                definitions = EventDefinition.getAll(connection)
            }

            val eventInfo = EventInfo(events, definitions)
            var output = ctx.jsonMapper().toJsonString(eventInfo, EventInfo::class.java)
            // need to convert NaNs to null so they can be parsed by JSON
            output = output.replace("NaN".toRegex(), "null")
            ctx.result(output)
        }

    }

    fun getFlightHoursByAirframe(ctx: Context) {

        val user = SessionUtility.getUser(ctx)
        val fleetId = user.fleetId

        val startDateIn = ctx.queryParam("startDate")
        val endDateIn = ctx.queryParam("endDate")

        val startDate = if (startDateIn != null) LocalDate.parse(startDateIn) else LocalDate.MIN
        val endDate = if (endDateIn != null) LocalDate.parse(endDateIn) else LocalDate.MAX


        Database.getConnection().use { connection ->
            val results = mutableListOf<Map<String, Any>>()

            val dateClause = StatisticsJavalinRoutes.buildDateClause(startDate, endDate)
            val sql = """
                SELECT
                    a.airframe,
                    v.airframe_id,
                    SUM(v.num_flights)                  AS num_flights,
                    SUM(v.flight_time_seconds)/3600.0   AS total_flight_hours
                FROM
                    v_fleet_flight_stats_by_airframe v
                JOIN
                    airframes a ON a.id = v.airframe_id
                WHERE
                    ((? = -1 OR v.airframe_id = ?) AND v.fleet_id = ?)
                AND
                    $dateClause
                GROUP
                    BY a.airframe, v.airframe_id
                ORDER
                    BY a.airframe;
            """.trimIndent()

            val stmt: PreparedStatement = connection.prepareStatement(sql)

            val airframeId = ctx.queryParam("airframeID")?.toInt() ?: -1
            stmt.setInt(1, airframeId)
            stmt.setInt(2, airframeId)

            stmt.setInt(3, fleetId)


            val rs = stmt.executeQuery()
            while (rs.next()) {
                results.add(
                    mapOf(
                        "airframe" to rs.getString("airframe"),
                        "airframe_id" to rs.getInt("airframe_id"),
                        "num_flights" to rs.getInt("num_flights"),
                        "total_flight_hours" to rs.getDouble("total_flight_hours")
                    )
                )
            }
            ctx.json(results)
        }

    }

    fun getAggregateFlightHoursByAirframe(ctx: Context) {
        val user = SessionUtility.getUser(ctx)
        // TODO: Restore aggregate access check before production
        // if (user == null || !user.hasAggregateView()) {
        //     ctx.status(401).result("User does not have aggregate access.")
        //     return
        // }


        val startDateIn = ctx.queryParam("startDate")
        val endDateIn = ctx.queryParam("endDate")

        val startDate = if (startDateIn != null) LocalDate.parse(startDateIn) else LocalDate.MIN
        val endDate = if (endDateIn != null) LocalDate.parse(endDateIn) else LocalDate.MAX


        Database.getConnection().use { connection ->
            val results = mutableListOf<Map<String, Any>>()

            val dateClause = StatisticsJavalinRoutes.buildDateClause(startDate, endDate)
            val sql = """
                SELECT
                    a.airframe,
                    v.airframe_id,
                    SUM(v.num_flights)                  AS num_flights,
                    SUM(v.flight_time_seconds)/3600.0   AS total_flight_hours
                FROM
                    v_aggregate_flight_stats_by_airframe_alt v
                JOIN
                    airframes a ON a.id = v.airframe_id
                WHERE
                    (? = -1 OR v.airframe_id = ?)
                AND
                    $dateClause
                GROUP
                    BY a.airframe, v.airframe_id
                ORDER
                    BY a.airframe;
            """.trimIndent()

            val stmt: PreparedStatement = connection.prepareStatement(sql)

            val airframeId = ctx.queryParam("airframeID")?.toInt() ?: -1
            stmt.setInt(1, airframeId)
            stmt.setInt(2, airframeId)


            val rs = stmt.executeQuery()
            while (rs.next()) {
                results.add(
                    mapOf(
                        "airframe" to rs.getString("airframe"),
                        "airframe_id" to rs.getInt("airframe_id"),
                        "num_flights" to rs.getInt("num_flights"),
                        "total_flight_hours" to rs.getDouble("total_flight_hours")
                    )
                )
            }
            ctx.json(results)
        }
    }
}
