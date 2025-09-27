package org.ngafid.www.routes.api

import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.config.JavalinConfig
import io.javalin.http.ContentType
import io.javalin.http.Context
import io.javalin.http.UnauthorizedResponse
import org.ngafid.core.Database
import org.ngafid.core.event.Event
import org.ngafid.core.event.EventDefinition
import org.ngafid.core.event.EventMetaData
import org.ngafid.core.event.RateOfClosure
import org.ngafid.core.flights.Airframes
import org.ngafid.www.routes.*
import java.time.LocalDate
import java.util.*

object EventRoutes : RouteProvider() {
    override fun bind(app: JavalinConfig) {
        app.router.apiBuilder {
            path("/api/event/") {
                path("{eid}") {
                    get("rate-of-closure", EventRoutes::getEventRateOfClosure, Role.LOGGED_IN)
                    get("meta", EventRoutes::getEventMetaData, Role.LOGGED_IN)
                }

                path("severities") {
                    get(EventRoutes::getAllSeverities, Role.LOGGED_IN)
                    get("{eventName}", EventRoutes::getEventSeverities, Role.LOGGED_IN)
                }

                path("definition") {
                    get(EventRoutes::getAllEventDefinitions, Role.LOGGED_IN)
                    post(EventRoutes::postCreateEvent, Role.LOGGED_IN)
                    get("description", EventRoutes::getAllEventDescriptions, Role.LOGGED_IN)

                    path("{edid}") {
                        get(EventRoutes::getOneEventDefinition, Role.LOGGED_IN)
                        patch(EventRoutes::patchEventDefinition, Role.LOGGED_IN)
                        delete(EventRoutes::deleteEventDefinition, Role.LOGGED_IN, Role.ADMIN_ONLY)
                    }

                    // TODO: We should not be querying by event name. Most of the javascript code that does this
                    // could easily be refactored to not require this, so this is a temporary hack.
                    path("by-name/{eventName}") {
                        get("description", EventRoutes::getEventDescription, Role.LOGGED_IN)
                    }
                }

                RouteUtility.getStat("count") { ctx, stats -> ctx.json(stats.totalEvents()) }
                RouteUtility.getStat("count/past-month") { ctx, stats -> ctx.json(stats.monthEvents()) }
                RouteUtility.getStat("count/past-year") { ctx, stats -> ctx.json(stats.yearEvents()) }

                get(
                    "count/by-airframe",
                    { ctx -> StatisticsJavalinRoutes.getAllEventCountsByAirframe(ctx, false) },
                    Role.LOGGED_IN
                )
                get(
                    "count/by-airframe/aggregate",
                    { ctx -> StatisticsJavalinRoutes.getAllEventCountsByAirframe(ctx, true) },
                    Role.LOGGED_IN
                )
                get(
                    "count/by-airframe/{aid}",
                    { ctx -> StatisticsJavalinRoutes.getOneEventCountsByAirframe(ctx) },
                )
                get(
                    "count/monthly/by-name",
                    StatisticsJavalinRoutes::getMonthlyEventCounts,
                    Role.LOGGED_IN
                )

            }
        }
    }

    fun getEventDescription(ctx: Context): Unit =
        Database.getConnection().use { connection ->
            ctx.contentType(ContentType.PLAIN)
            ctx.result(EventDefinition.getEventDefinition(connection, ctx.pathParam("eventName")).toHumanReadable())
        }

    fun getEventRateOfClosure(ctx: Context): Unit =
        Database.getConnection()
            .use { ctx.json(RateOfClosure.getRateOfClosureOfEvent(it, ctx.pathParam("eid").toInt())) }


    fun getEventMetaData(ctx: Context): Unit =
        Database.getConnection()
            .use { ctx.json(EventMetaData.getEventMetaData(it, ctx.pathParam("eid").toInt())) }


    fun getAllSeverities(ctx: Context) {
        val user = SessionUtility.getUser(ctx)
        val startDate = ctx.queryParam("startDate")!!
        val endDate = ctx.queryParam("endDate")!!
        val eventNames = ctx.queryParam("eventNames")!!
        val tagName = ctx.queryParam("tagName")!!
        val fleetId = user.fleetId

        Database.getConnection().use { connection ->
            val eventNamesArray = eventNames.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val eventMap: MutableMap<String, Map<String, ArrayList<Event>>> = HashMap()

            for (eventName in eventNamesArray) {
                // Remove leading and trailing quotes
                var eventName = eventName
                    .replace("\"", "")
                    .replace("[", "")
                    .replace("]", "")
                    .trim { it <= ' ' }

                if (eventName == "ANY Event") continue

                val events: Map<String, ArrayList<Event>> = Event.getEvents(
                    connection,
                    fleetId,
                    eventName,
                    LocalDate.parse(startDate),
                    LocalDate.parse(endDate),
                    tagName
                )

                eventMap[eventName] = events
            }

            ctx.json(eventMap)
        }
    }

    fun getEventSeverities(ctx: Context) {
        val startDate = ctx.queryParam("startDate")!!
        val endDate = ctx.queryParam("endDate")!!
        val tagName = ctx.queryParam("tagName")!!

        val eventName = ctx.pathParam("eventName")

        val user = SessionUtility.getUser(ctx)
        val fleetId = user.fleetId

        Database.getConnection().use { connection ->
            ctx.json(
                Event.getEvents(
                    connection,
                    fleetId,
                    eventName,
                    LocalDate.parse(startDate),
                    LocalDate.parse(endDate),
                    tagName
                )
            )
        }
    }

    fun getAllEventDefinitions(ctx: Context): Unit =
        Database.getConnection().use { connection ->
            ctx.json(EventDefinition.getAll(connection))
        }

    fun getOneEventDefinition(ctx: Context): Unit =
        Database.getConnection().use { connection ->
            ctx.json(EventDefinition.getEventDefinition(connection, ctx.pathParam("edid").toInt()))
        }

    fun postCreateEvent(ctx: Context) {
        val fleetId = 0 // all events work on all fleets for now
        val eventName = ctx.formParam("eventName")
        val startBuffer = ctx.formParam("startBuffer")!!.toInt()
        val stopBuffer = ctx.formParam("stopBuffer")!!.toInt()
        val airframe = ctx.formParam("airframe")
        val filterJSON = ctx.formParam("filterQuery")
        val severityColumnNamesJSON = ctx.formParam("severityColumnNames")
        val severityType = ctx.formParam("severityType")

        Database.getConnection().use { connection ->
            EventDefinition.insert(
                connection,
                fleetId,
                eventName,
                startBuffer,
                stopBuffer,
                airframe,
                filterJSON,
                severityColumnNamesJSON,
                severityType
            )

            ctx.json(Object())
        }
    }

    fun getAllEventDescriptions(ctx: Context) {
        Database.getConnection().use { connection ->
            val definitions: MutableMap<String, MutableMap<String?, String>> = TreeMap()
            val airframeNames: MutableMap<Int, String> = HashMap()
            airframeNames[0] = "Any"

            for (eventDefinition in EventDefinition.getAll(connection)) {
                if (!definitions.containsKey(eventDefinition.name)) {
                    definitions[eventDefinition.name] = HashMap()
                }

                if (!airframeNames.containsKey(eventDefinition.airframeNameId)) {
                    airframeNames[eventDefinition.airframeNameId] =
                        Airframes.Airframe(connection, eventDefinition.airframeNameId).name
                }

                println("${airframeNames[eventDefinition.airframeNameId]} -> ${eventDefinition.toHumanReadable()}")
                definitions[eventDefinition.name]!![airframeNames[eventDefinition.airframeNameId]] =
                    eventDefinition.toHumanReadable()
            }

            ctx.json(definitions)
        }
    }

    fun patchEventDefinition(ctx: Context): Unit =
        Database.getConnection().use { connection ->
            // TODO: Validate that this def matches the route.
            EventJavalinRoutes.GSON.fromJson(
                ctx.body(),
                EventDefinition::class.java
            ).updateSelf(connection)
        }

    fun deleteEventDefinition(ctx: Context) {
        val user = SessionUtility.getUser(ctx)
        if (!user.isAdmin) {
            throw UnauthorizedResponse("User does not have admin privileges.")
        }

        Database.getConnection().use { connection ->
            EventDefinition.getEventDefinition(connection, ctx.pathParam("edid").toInt()).delete(connection)
        }
    }

}
