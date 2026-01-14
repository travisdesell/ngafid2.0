// ngafid-www/src/main/kotlin/org/ngafid/www/routes/api/CesiumRoutes.kt
package org.ngafid.www.routes.api

import com.github.mustachejava.DefaultMustacheFactory
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.config.JavalinConfig
import io.javalin.http.Context
import java.io.PrintWriter
import java.io.StringWriter
import java.sql.Connection
import java.util.logging.Logger
import org.ngafid.core.Database
import org.ngafid.core.accounts.User
import org.ngafid.core.event.Event
import org.ngafid.core.flights.DoubleTimeSeries
import org.ngafid.core.flights.Flight
import org.ngafid.www.ErrorResponse
import org.ngafid.www.WebServer
import org.ngafid.www.routes.Role
import org.ngafid.www.routes.RouteProvider
import org.ngafid.www.routes.SessionUtility

object CesiumRoutes : RouteProvider() {
    private val LOG: Logger = Logger.getLogger(CesiumRoutes::class.java.name)
    private const val CESIUM_DATA = "cesium_data"

    /** Legacy binding for /protected routes (used from JavalinWebServer.configureRoutes). */
    @JvmStatic
    fun bindRoutes(app: Javalin) {
        app.get("/protected/ngafid_cesium", CesiumRoutes::handleGetNgafidCesium)
        app.post("/protected/cesium_data", CesiumRoutes::handlePostCesiumData)
    }

    /**
     * New-style /api routes using RouteProvider. Call CesiumDataJavalinRoutes.bind(config) from
     * JavalinWebServer.preInitialize().
     */
    override fun bind(config: JavalinConfig) {
        config.router.apiBuilder {
            path("/api/cesium") {
                // Multi-flight GET, returns raw CesiumResponse map as JSON.
                get("data", CesiumRoutes::handleGetCesiumDataApi, Role.LOGGED_IN)

                // Single-flight POST, compatible with old form body (flightId).
                post("data", CesiumRoutes::handlePostCesiumDataApi, Role.LOGGED_IN)
            }
        }
    }

    // --------------------------------------------------------------------
    // Legacy /protected HTML route
    // --------------------------------------------------------------------
    private fun handleGetNgafidCesium(ctx: Context) {
        LOG.info("Handling /protected/ngafid_cesium route")

        val flightIdStr = ctx.queryParam("flight_id")
        if (flightIdStr == null) {
            ctx.status(400)
                    .json(
                            ErrorResponse(
                                    "Missing parameter",
                                    "Required query parameter 'flight_id' was not provided."
                            )
                    )
            return
        }

        LOG.info("Getting information for flight ID: $flightIdStr")
        val flightId = flightIdStr.toInt()

        val otherFlightId = ctx.queryParam("other_flight_id")
        LOG.info("URL flight ID is: $flightId")
        LOG.info("URL other flight ID is: $otherFlightId")

        val user: User? = ctx.sessionAttribute("user")
        if (user == null) {
            LOG.severe("INVALID ACCESS: user was not logged in.")
            ctx.status(401)
            return
        }
        val fleetId = user.fleetId

        if (!user.hasViewAccess(fleetId)) {
            LOG.severe("INVALID ACCESS: User did not have view access for this fleet.")
            ctx.status(401).result("User did not have access to view this fleet.")
            return
        }

        try {
            Database.getConnection().use { connection ->
                val flight = Flight.getFlight(connection, flightId)
                val flightIdsAll: List<String> = ctx.queryParams("flight_id")
                LOG.info("Flight IDs are: $flightIdsAll")

                if (flight.fleetId != fleetId) {
                    LOG.severe("INVALID ACCESS: User did not have access to flight ID: $flightId")
                    ctx.status(401).result("User did not have access to view this fleet.")
                    return
                }

                val scopes = HashMap<String, Any?>()
                val flights = HashMap<String, CesiumResponse>()

                var cesiumDataJsSnippet = ""

                for (flightIdNew in flightIdsAll) {
                    val flightIdNewInt = flightIdNew.toInt()
                    val incomingFlight = Flight.getFlight(connection, flightIdNewInt)

                    if (incomingFlight.fleetId != fleetId) {
                        LOG.severe(
                                "INVALID ACCESS: user did not have access to flight id: $flightIdNew, it belonged to " +
                                        "fleet: ${incomingFlight.fleetId} and the user's fleet id was: $fleetId"
                        )
                        ctx.status(401).result("User did not have access to view this fleet.")
                        return
                    }

                    val cesiumResponse = buildCesiumResponse(connection, incomingFlight)
                    // Build a JS snippet: var cesium_data_new = {...};
                    val cesiumJson = WebServer.gson.toJson(cesiumResponse)
                    cesiumDataJsSnippet = "var cesium_data_new = $cesiumJson;\n"

                    flights[flightIdNew] = cesiumResponse
                }

                scopes[CESIUM_DATA] = WebServer.gson.toJson(flights)
                // Original Java stored JSON-encoded JS snippet
                scopes["cesium_data_js"] = WebServer.gson.toJson(cesiumDataJsSnippet)

                val templateFile = "ngafid_cesium.html"
                LOG.severe("template file: '$templateFile'")

                val mf = DefaultMustacheFactory()
                val mustache = mf.compile(templateFile)

                val stringOut = StringWriter()
                mustache.execute(PrintWriter(stringOut), scopes).flush()
                val resultString = stringOut.toString()

                // Keep original behavior: HTML string wrapped as JSON
                ctx.json(resultString)
            }
        } catch (e: Exception) {
            LOG.severe("Database error: ${e.message}")
            ctx.status(500).json(ErrorResponse(e))
        }
    }

    // --------------------------------------------------------------------
    // Legacy /protected JSON route (single flight)
    // --------------------------------------------------------------------
    private fun handlePostCesiumData(ctx: Context) {
        LOG.info("Handling /protected/cesium_data route")

        val user: User? = ctx.sessionAttribute("user")
        if (user == null) {
            LOG.severe("INVALID ACCESS: user was not logged in.")
            ctx.status(401)
            return
        }
        val fleetId = user.fleetId

        if (!user.hasViewAccess(fleetId)) {
            LOG.severe("INVALID ACCESS: User did not have view access for this fleet.")
            ctx.status(401).result("User did not have access to view this fleet.")
            return
        }

        try {
            val flightId = ctx.formParam("flightId")?.toInt()
            if (flightId == null) {
                ctx.status(400)
                        .json(
                                ErrorResponse(
                                        "Missing parameter",
                                        "Required form parameter 'flightId' was not provided."
                                )
                        )
                return
            }

            Database.getConnection().use { connection ->
                val flight = Flight.getFlight(connection, flightId)

                if (flight.fleetId != fleetId) {
                    LOG.severe(
                            "INVALID ACCESS: user did not have access to flight id: $flightId, it belonged to " +
                                    "fleet: ${flight.fleetId} and the user's fleet id was: $fleetId"
                    )
                    ctx.status(401).result("User did not have access to view this fleet.")
                    return
                }

                val flights = HashMap<Int, CesiumResponse>()
                val cesiumResponse = buildCesiumResponse(connection, flight)
                flights[flightId] = cesiumResponse

                ctx.json(flights)
            }
        } catch (e: Exception) {
            LOG.severe("Database error: ${e.message}")
            ctx.status(500).json(ErrorResponse(e))
        }
    }

    // --------------------------------------------------------------------
    // New /api routes
    // --------------------------------------------------------------------

    /**
     * GET /api/cesium/data?flight_id=1&flight_id=2... Returns: { "<flightId>": CesiumResponse, ...
     * }
     */
    private fun handleGetCesiumDataApi(ctx: Context) {
        LOG.info("Handling /api/cesium/data GET route")

        // Will be enforced by /api auth filter, but we still need the user for fleet checks.
        val user = SessionUtility.getUser(ctx)
        val fleetId = user.fleetId

        val flightIdParams = ctx.queryParams("flight_id")
        if (flightIdParams.isEmpty()) {
            ctx.status(400)
                    .json(
                            ErrorResponse(
                                    "Missing parameter",
                                    "At least one 'flight_id' query parameter is required."
                            )
                    )
            return
        }

        try {
            Database.getConnection().use { connection ->
                val flights = HashMap<Int, CesiumResponse>()

                for (idStr in flightIdParams) {
                    val flightId = idStr.toIntOrNull()
                    if (flightId == null) {
                        LOG.warning("Skipping invalid flight_id '$idStr'")
                        continue
                    }

                    val flight = Flight.getFlight(connection, flightId)
                    if (flight.fleetId != fleetId) {
                        LOG.severe(
                                "INVALID ACCESS: user did not have access to flight id: $flightId, it belonged to " +
                                        "fleet: ${flight.fleetId} and the user's fleet id was: $fleetId"
                        )
                        ctx.status(401)
                                .json(
                                        ErrorResponse(
                                                "Unauthorized",
                                                "User did not have access to view this fleet."
                                        )
                                )
                        return
                    }

                    flights[flightId] = buildCesiumResponse(connection, flight)
                }

                ctx.json(flights)
            }
        } catch (e: Exception) {
            LOG.severe("Database error: ${e.message}")
            ctx.status(500).json(ErrorResponse(e))
        }
    }

    /**
     * POST /api/cesium/data Body: form field "flightId" (same as legacy /protected endpoint).
     * Behavior is identical to handlePostCesiumData but under /api.
     */
    private fun handlePostCesiumDataApi(ctx: Context) {
        // Simply reuse the legacy handler for body parsing and logic.
        handlePostCesiumData(ctx)
    }

    // --------------------------------------------------------------------
    // Shared Cesium computation
    // --------------------------------------------------------------------
    private fun buildCesiumResponse(connection: Connection, flight: Flight): CesiumResponse {
        val flightId = flight.id
        val airframeType = flight.airframeType

        // Time series
        val altMsl = flight.getDoubleTimeSeries(connection, "AltMSL")
        val latitude = flight.getDoubleTimeSeries(connection, "Latitude")
        val longitude = flight.getDoubleTimeSeries(connection, "Longitude")
        val altAgl = flight.getDoubleTimeSeries(connection, "AltAGL")
        val rpm: DoubleTimeSeries? = flight.getDoubleTimeSeries(connection, "E1 RPM")
        val groundSpeed = flight.getDoubleTimeSeries(connection, "GndSpd")

        val date = flight.getStringTimeSeries(connection, "Lcl Date")
        val time = flight.getStringTimeSeries(connection, "Lcl Time")

        val flightGeoAglTaxiing = ArrayList<Double>()
        val flightGeoAglTakeOff = ArrayList<Double>()
        val flightGeoAglClimb = ArrayList<Double>()
        val flightGeoAglCruise = ArrayList<Double>()
        val flightGeoInfoAgl = ArrayList<Double>()

        val flightTaxiingTimes = ArrayList<String>()
        val flightTakeOffTimes = ArrayList<String>()
        val flightClimbTimes = ArrayList<String>()
        val flightCruiseTimes = ArrayList<String>()
        val flightAglTimes = ArrayList<String>()

        var initCounter = 0
        var takeoffCounter = 0
        var countPostTakeoff = 0
        var sizePreClimb = 0
        var countPostCruise = 0
        val dateSize = date.size()

        // Calculate the taxiing phase
        for (i in 0 until altAgl.size()) {
            val lon = longitude.get(i)
            val lat = latitude.get(i)
            val agl = altAgl.get(i)

            if (!lon.isNaN() && !lat.isNaN() && !agl.isNaN() && i < dateSize) {
                initCounter++
                flightGeoAglTaxiing.add(lon)
                flightGeoAglTaxiing.add(lat)
                flightGeoAglTaxiing.add(agl)
                flightTaxiingTimes.add("${date.get(i)}T${time.get(i).trim()}Z")

                val rpmVal = rpm?.get(i)
                if (rpmVal != null &&
                                rpmVal >= 2100.0 &&
                                groundSpeed.get(i) > 14.5 &&
                                groundSpeed.get(i) < 80.0
                ) {
                    break
                }
            }
        }

        // Calculate the takeoff-init phase
        takeoffCounter = 0
        for (i in 0 until altAgl.size()) {
            val lon = longitude.get(i)
            val lat = latitude.get(i)
            val agl = altAgl.get(i)

            if (!lon.isNaN() && !lat.isNaN() && !agl.isNaN() && i < dateSize) {
                val rpmVal = rpm?.get(i)
                if (rpmVal != null &&
                                rpmVal >= 2100.0 &&
                                groundSpeed.get(i) > 14.5 &&
                                groundSpeed.get(i) < 80.0
                ) {
                    if (takeoffCounter <= 15) {
                        flightGeoAglTakeOff.add(lon)
                        flightGeoAglTakeOff.add(lat)
                        flightGeoAglTakeOff.add(agl)
                        flightTakeOffTimes.add("${date.get(i)}T${time.get(i).trim()}Z")

                        initCounter++
                    } else if (takeoffCounter > 15) {
                        break
                    }
                    takeoffCounter++
                } else {
                    takeoffCounter = 0
                }
            }
        }

        // Calculate the climb phase
        countPostTakeoff = 0
        for (i in 0 until altAgl.size()) {
            val lon = longitude.get(i)
            val lat = latitude.get(i)
            val agl = altAgl.get(i)

            if (!lon.isNaN() && !lat.isNaN() && !agl.isNaN() && i < dateSize) {
                val rpmVal = rpm?.get(i)
                if (rpmVal != null &&
                                rpmVal >= 2100.0 &&
                                groundSpeed.get(i) > 14.5 &&
                                groundSpeed.get(i) <= 80.0
                ) {
                    if (countPostTakeoff >= 15) {
                        flightGeoAglClimb.add(lon)
                        flightGeoAglClimb.add(lat)
                        flightGeoAglClimb.add(agl)
                        flightClimbTimes.add("${date.get(i)}T${time.get(i).trim()}Z")

                        initCounter++
                    }
                    if (agl >= 500.0) {
                        break
                    }
                    countPostTakeoff++
                }
            }
        }

        // Calculate the cruise to final phase
        val preClimb =
                (flightGeoAglTaxiing.size + flightGeoAglTakeOff.size + flightGeoAglClimb.size) - 9
        sizePreClimb = preClimb / 3

        countPostCruise = 0
        for (i in 0 until altAgl.size()) {
            val lon = longitude.get(i)
            val lat = latitude.get(i)
            val agl = altAgl.get(i)

            if (!lon.isNaN() && !lat.isNaN() && !agl.isNaN() && i < dateSize) {
                if (countPostCruise >= sizePreClimb) {
                    flightGeoAglCruise.add(lon)
                    flightGeoAglCruise.add(lat)
                    flightGeoAglCruise.add(agl)
                    flightCruiseTimes.add("${date.get(i)}T${time.get(i).trim()}Z")
                }
                countPostCruise++
            }
        }

        // Calculate the full phase (avoid NaN)
        for (i in 0 until altAgl.size()) {
            val lon = longitude.get(i)
            val lat = latitude.get(i)
            val agl = altAgl.get(i)

            if (!lon.isNaN() && !lat.isNaN() && !agl.isNaN() && i < dateSize) {
                flightGeoInfoAgl.add(lon)
                flightGeoInfoAgl.add(lat)
                flightGeoInfoAgl.add(agl)
                flightAglTimes.add("${date.get(i)}T${time.get(i).trim()}Z")
            }
        }

        LOG.info("Built CesiumResponse for flight id: $flightId, airframeType: $airframeType")

        return CesiumResponse(
                flightGeoAglTaxiing = flightGeoAglTaxiing,
                flightGeoAglTakeOff = flightGeoAglTakeOff,
                flightGeoAglClimb = flightGeoAglClimb,
                flightGeoAglCruise = flightGeoAglCruise,
                flightGeoInfoAgl = flightGeoInfoAgl,
                flightTaxiingTimes = flightTaxiingTimes,
                flightTakeOffTimes = flightTakeOffTimes,
                flightClimbTimes = flightClimbTimes,
                flightCruiseTimes = flightCruiseTimes,
                flightAglTimes = flightAglTimes,
                airframeType = airframeType
        )
    }

    // --------------------------------------------------------------------
    // Response DTO (compatible with Gson)
    // --------------------------------------------------------------------
    class CesiumResponse(
            val flightGeoAglTaxiing: List<Double>,
            val flightGeoAglTakeOff: List<Double>,
            val flightGeoAglClimb: List<Double>,
            val flightGeoAglCruise: List<Double>,
            val flightGeoInfoAgl: List<Double>,
            val flightTaxiingTimes: List<String>,
            val flightTakeOffTimes: List<String>,
            val flightClimbTimes: List<String>,
            val flightCruiseTimes: List<String>,
            val flightAglTimes: List<String>,
            val airframeType: String
    ) {
        // Not currently populated, but kept for forward compatibility with Java version.
        val events: List<Event>? = null

        val startTime: String = flightAglTimes.firstOrNull().orEmpty()
        val endTime: String = flightAglTimes.lastOrNull().orEmpty()
    }
}
