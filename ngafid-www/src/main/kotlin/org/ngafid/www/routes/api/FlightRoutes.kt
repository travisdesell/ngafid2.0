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
import org.ngafid.core.util.FlightTag
import org.ngafid.www.ErrorResponse
import org.ngafid.www.routes.*
import java.util.*
import java.sql.PreparedStatement
import java.time.LocalDate
import kotlin.booleanArrayOf

object FlightRoutes : RouteProvider() {
    override fun bind(app: JavalinConfig) {
        app.router.apiBuilder {
            path("/api/flight") {
                get(FlightsJavalinRoutes::postFlights, Role.LOGGED_IN)
                get("double-series", DoubleSeriesJavalinRoutes::getAllDoubleSeriesNames, Role.LOGGED_IN)
                get("turn-to-final", AnalysisJavalinRoutes::postTurnToFinal, Role.LOGGED_IN)
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

        Database.getConnection().use { connection ->
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