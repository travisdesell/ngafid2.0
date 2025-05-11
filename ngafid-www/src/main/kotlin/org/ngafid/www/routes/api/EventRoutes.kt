package org.ngafid.www.routes.api

import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.config.JavalinConfig
import org.ngafid.www.routes.*

object EventRoutes : RouteProvider() {
    override fun bind(app: JavalinConfig) {
        app.router.apiBuilder {
            path("/api/event/") {
                path("/{eid}") {
                    get("/rate-of-closure", AnalysisJavalinRoutes::postRateOfClosure, Role.LOGGED_IN)
                    get("/meta", EventJavalinRoutes::postEventMetaData, Role.LOGGED_IN)
                }

                path("/severities") {
                    get(AnalysisJavalinRoutes::postAllSeverities, Role.LOGGED_IN)
                    get("/{eventName}", AnalysisJavalinRoutes::postSeverities, Role.LOGGED_IN)
                }

                path("/definition") {
                    get(EventJavalinRoutes::getAllEventDefinitions, Role.LOGGED_IN)
                    post(EventJavalinRoutes::postCreateEvent, Role.LOGGED_IN)
                    get("/description", EventJavalinRoutes::getAllEventDescriptions, Role.LOGGED_IN)

                    path("/{edid}") {
                        get(EventJavalinRoutes::getEventDefinition, Role.LOGGED_IN)
                        patch(EventJavalinRoutes::putEventDefinitions, Role.LOGGED_IN)
                        delete(EventJavalinRoutes::deleteEventDefinitions, Role.LOGGED_IN)
                    }

                    // TODO: We should not be querying by event name. Most of the javascript code that does this
                    // could easily be refactored to not require this, so this is a temporary hack.
                    path("/by-name/{eventName}") {
                        get("/description", EventJavalinRoutes::getEventDefinition, Role.LOGGED_IN)
                    }
                }

                RouteUtility.getStat("/count") { ctx, stats ->
                    ctx.json(stats.totalEvents())
                }
                RouteUtility.getStat("/count/past-month") { ctx, stats -> ctx.json(stats.monthEvents()) }
                RouteUtility.getStat("/count/past-year") { ctx, stats -> ctx.json(stats.yearEvents()) }

                get(
                    "/count/by-airframe",
                    { ctx -> StatisticsJavalinRoutes.postEventCounts(ctx, false) },
                    Role.LOGGED_IN
                )
                get(
                    "/count/by-airframe/aggregate",
                    { ctx -> StatisticsJavalinRoutes.postEventCounts(ctx, true) },
                    Role.LOGGED_IN
                )
                get(
                    "/count/monthly/by-name",
                    StatisticsJavalinRoutes::postMonthlyEventCounts,
                    Role.LOGGED_IN
                )

            }
        }
    }
}