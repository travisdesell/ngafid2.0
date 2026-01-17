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
    private const val PRE_CLIMB_OFFSET = 9
    private const val MIN_RPM_FOR_TAKEOFF = 2100.0
    private const val MIN_GROUNDSPEED_FOR_TAKEOFF = 14.5
    private const val MAX_GROUNDSPEED_FOR_TAKEOFF = 80.0
    private const val TAKEOFF_INIT_FRAME_COUNT = 15
    private const val CLIMB_ALTITUDE_THRESHOLD = 500.0

    override fun bind(app: JavalinConfig) {

        app.router.apiBuilder {
            path("/api/cesium") {
                get("data", CesiumRoutes::handleGetCesiumData, Role.LOGGED_IN)      // Multi-flight GET -> raw CesiumResponse map as JSON.
                post("data", CesiumRoutes::handlePostCesiumData, Role.LOGGED_IN)    // Single-flight POST -> raw CesiumResponse map as JSON.
            }
        }

    }

    private fun dataPointIsValid(lon: Double, lat: Double, agl: Double, index: Int, dateSize: Int): Boolean {

        return (
            !lon.isNaN()
            && !lat.isNaN()
            && !agl.isNaN()
            && index < dateSize
        )

    }

    private fun takeoffConditionMet(rpmVal: Double?, groundSpeedVal: Double): Boolean {

        return (
            rpmVal != null
            && rpmVal >= MIN_RPM_FOR_TAKEOFF
            && groundSpeedVal > MIN_GROUNDSPEED_FOR_TAKEOFF
            && groundSpeedVal < MAX_GROUNDSPEED_FOR_TAKEOFF
        )

    }

    private fun handlePostCesiumData(ctx: Context) {

        LOG.info("Handling /protected/cesium_data route")

        val user: User? = ctx.sessionAttribute("user")

        // User not logged in -> Unauthorized
        if (user == null) {
            LOG.severe("INVALID ACCESS: user was not logged in.")
            ctx.status(401)
            return
        }

        val fleetId = user.fleetId

        // User does not have View access to this fleet -> Unauthorized
        if (!user.hasViewAccess(fleetId)) {

            LOG.severe("INVALID ACCESS: User did not have view access for this fleet.")
            ctx.status(401).result("User did not have access to view this fleet.")
            return

        }

        try {

            val flightId = ctx.formParam("flightId")?.toInt()
            if (flightId == null) {
                val errorResponse = ErrorResponse("Missing parameter", "Required form parameter 'flightId' was not provided.")
                ctx.status(400) .json(errorResponse)
                return
            }

            LOG.info("Fetching Cesium data for flight id: $flightId")

            Database.getConnection().use { connection ->

                val flight = Flight.getFlight(connection, flightId)

                // User does not have access to this flight -> Unauthorized
                if (flight.fleetId != fleetId) {
                    LOG.severe( "INVALID ACCESS: user did not have access to flight id: $flightId, it belonged to " + "fleet: ${flight.fleetId} and the user's fleet id was: $fleetId" )
                    ctx.status(401).result("User did not have access to view this fleet.")
                    return
                }

                // Construct a response map with a single flight's CesiumResponse
                val flights = HashMap<Int, CesiumResponse>()
                val cesiumResponse = buildCesiumResponse(connection, flight)
                flights[flightId] = cesiumResponse

                LOG.info("Completed building CesiumResponse for flight id: $flightId")
                ctx.json(flights)

            }
            
        } catch (e: Exception) {
            LOG.severe("Database error: ${e.message}")
            ctx.status(500).json(ErrorResponse(e))
        }
    }
    
    private fun handleGetCesiumData(ctx: Context) {

        LOG.info("Handling /api/cesium/data GET route")

        val user = SessionUtility.getUser(ctx)
        val fleetId = user.fleetId

        val flightIdParams = ctx.queryParams("flight_id")

        // No flight_id parameters provided -> Bad Request
        if (flightIdParams.isEmpty()) {
            val errorResponse = ErrorResponse("Missing parameter", "At least one 'flight_id' query parameter is required.")
            ctx.status(400).json(errorResponse)
            return
        }

        try {

            Database.getConnection().use { connection ->
                val flights = HashMap<Int, CesiumResponse>()

                for (idStr in flightIdParams) {

                    val flightId = idStr.toIntOrNull()

                    // Target flight ID is not a valid integer, skip it
                    if (flightId == null) {
                        LOG.warning("Skipping invalid flight_id '$idStr'")
                        continue
                    }

                    LOG.info("Fetching Cesium data for flight id: $flightId")

                    val flight = Flight.getFlight(connection, flightId)

                    // User does not have access to this flight -> Unauthorized
                    if (!user.hasViewAccess(fleetId)) {
                        LOG.severe( "INVALID ACCESS: user did not have access to flight id: $flightId, it belonged to " + "fleet: ${flight.fleetId} and the user's fleet id was: $fleetId" )
                        ctx.status(401) .json( ErrorResponse( "Unauthorized", "User did not have access to view this fleet." ) )
                        return
                    }

                    // Build and add the CesiumResponse for this flight
                    flights[flightId] = buildCesiumResponse(connection, flight)
                    LOG.info("Completed building CesiumResponse for flight id: $flightId")
                }

                LOG.info("Completed building CesiumResponses for ${flights.size} flights.")
                ctx.json(flights)

            }

        } catch (e: Exception) {
            LOG.severe("Database error: ${e.message}")
            ctx.status(500).json(ErrorResponse(e))
        }

    }
    
    private fun buildCesiumResponse(connection: Connection, flight: Flight): CesiumResponse {

        val flightId = flight.id
        val airframeType = flight.airframeType

        // Time series
        val altMsl = flight.getDoubleTimeSeries(connection, "AltMSL")   // <-- (Currently Unused ⚠️)
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
        
        val dateSize = date.size()
        var initCounter = 0

        // Calculate the taxiing phase
        for (i in 0 until altAgl.size()) {

            val lon = longitude.get(i)
            val lat = latitude.get(i)
            val agl = altAgl.get(i)

            // Invalid data point, skip it
            if (!dataPointIsValid(lon, lat, agl, i, dateSize))
                continue

            initCounter++
            flightGeoAglTaxiing.add(lon)
            flightGeoAglTaxiing.add(lat)
            flightGeoAglTaxiing.add(agl)
            flightTaxiingTimes.add("${date.get(i)}T${time.get(i).trim()}Z")

            val rpmVal = rpm?.get(i)
            if (takeoffConditionMet(rpmVal, groundSpeed.get(i)))
                break

        }

        // Calculate the takeoff-init phase
        var takeoffCounter = 0
        for (i in 0 until altAgl.size()) {

            val lon = longitude.get(i)
            val lat = latitude.get(i)
            val agl = altAgl.get(i)

            // Invalid data point, skip it
            if (!dataPointIsValid(lon, lat, agl, i, dateSize))
                continue
            
            val rpmVal = rpm?.get(i)

            // Meets takeoff conditions...
            if (takeoffConditionMet(rpmVal, groundSpeed.get(i))) {
            
                // ...Still within the initial frame count, add to takeoff phase
                if (takeoffCounter <= TAKEOFF_INIT_FRAME_COUNT) {

                    flightGeoAglTakeOff.add(lon)
                    flightGeoAglTakeOff.add(lat)
                    flightGeoAglTakeOff.add(agl)
                    flightTakeOffTimes.add("${date.get(i)}T${time.get(i).trim()}Z")

                    initCounter++

                // Otherwise, exit loop
                } else if (takeoffCounter > TAKEOFF_INIT_FRAME_COUNT) {

                    break

                }

                takeoffCounter++

            // Otherwise, reset counter
            } else {

                takeoffCounter = 0

            }
            
        }

        // Calculate the climb phase
        var countPostTakeoff = 0
        for (i in 0 until altAgl.size()) {

            val lon = longitude.get(i)
            val lat = latitude.get(i)
            val agl = altAgl.get(i)

            // Invalid data point, skip it
            if (!dataPointIsValid(lon, lat, agl, i, dateSize))
                continue

            val rpmVal = rpm?.get(i)

            // Meets takeoff conditions...
            if (takeoffConditionMet(rpmVal, groundSpeed.get(i))) {

                // Met/Exceeds the post-takeoff frame count, add to climb phase
                if (countPostTakeoff >= TAKEOFF_INIT_FRAME_COUNT) {

                    flightGeoAglClimb.add(lon)
                    flightGeoAglClimb.add(lat)
                    flightGeoAglClimb.add(agl)
                    flightClimbTimes.add("${date.get(i)}T${time.get(i).trim()}Z")

                    initCounter++

                }

                // Exceeded climb altitude threshold, exit loop
                if (agl >= CLIMB_ALTITUDE_THRESHOLD)
                    break
                
                countPostTakeoff++
            
            }
            
        }

        // Calculate the cruise to final phase
        val preClimb = (flightGeoAglTaxiing.size + flightGeoAglTakeOff.size + flightGeoAglClimb.size) - PRE_CLIMB_OFFSET
        var sizePreClimb = (preClimb / 3)

        var countPostCruise = 0
        for (i in 0 until altAgl.size()) {

            val lon = longitude.get(i)
            val lat = latitude.get(i)
            val agl = altAgl.get(i)

            // Invalid data point, skip it
            if (!dataPointIsValid(lon, lat, agl, i, dateSize))
                continue
            
            // Pre-climb frames have been passed, add to cruise phase
            if (countPostCruise >= sizePreClimb) {
                flightGeoAglCruise.add(lon)
                flightGeoAglCruise.add(lat)
                flightGeoAglCruise.add(agl)
                flightCruiseTimes.add("${date.get(i)}T${time.get(i).trim()}Z")
            }

            countPostCruise++
            
        }

        // Calculate the full phase (avoid NaN)
        for (i in 0 until altAgl.size()) {

            val lon = longitude.get(i)
            val lat = latitude.get(i)
            val agl = altAgl.get(i)

            // Invalid data point, skip it
            if (!dataPointIsValid(lon, lat, agl, i, dateSize))
                continue

            flightGeoInfoAgl.add(lon)
            flightGeoInfoAgl.add(lat)
            flightGeoInfoAgl.add(agl)
            flightAglTimes.add("${date.get(i)}T${time.get(i).trim()}Z")
        
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
        
        //  val events: List<Event>? = null

        val startTime: String = flightAglTimes.firstOrNull().orEmpty()
        val endTime: String = flightAglTimes.lastOrNull().orEmpty()
    }

}
