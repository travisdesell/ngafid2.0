package org.ngafid.www.routes.api

import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.config.JavalinConfig
import io.javalin.http.Context
import io.javalin.http.NotFoundResponse
import io.javalin.http.pathParamAsClass
import io.javalin.openapi.*
import org.ngafid.core.Database
import org.ngafid.core.accounts.EmailType
import org.ngafid.core.accounts.Fleet
import org.ngafid.core.accounts.FleetAccess
import org.ngafid.core.accounts.FleetAccessNamed
import org.ngafid.core.accounts.User
import org.ngafid.core.util.SendEmail
import org.ngafid.www.ErrorResponse
import org.ngafid.www.routes.*
import java.net.URLEncoder
import java.util.*
import java.sql.SQLException
import java.util.logging.Logger

object UserRoutes : RouteProvider() {

    val LOG: Logger = Logger.getLogger(AuthRoutes::class.java.name)

    override fun bind(app: JavalinConfig) {
        app.router.apiBuilder {
            path("/api/user") {
                get(UserRoutes::getAll, Role.LOGGED_IN)
                post("invite", UserRoutes::postSendUserInvite, Role.LOGGED_IN)
                RouteUtility.getStat("count") { ctx, stats -> ctx.json(stats.numberUsers()) }

                path("multifleet-invites") {
                    get(UserRoutes::getMultifleetInvites, Role.LOGGED_IN)
                    post("accept", UserRoutes::acceptMultifleetInvite, Role.LOGGED_IN)
                    post("decline", UserRoutes::removeMultifleetInvite, Role.LOGGED_IN)
                }

                path("me") {
                    // Get currently logged in account
                    get(UserRoutes::getMe, Role.LOGGED_IN)

                    // Update currently logged in account
                    put(UserRoutes::putMe, Role.LOGGED_IN)

                    // Soft delete currently logged in account
                    delete(UserRoutes::softDeleteMe, Role.LOGGED_IN)

                    // TODO: get all fleets user has access to.
                    // get("/fleet-access", ..., Role.LOGGED_IN)

                    get("metric-prefs", UserRoutes::getMetricPreferencesMe, Role.LOGGED_IN)
                    patch("metric-prefs", UserRoutes::patchMetricPreferencesMe, Role.LOGGED_IN)
                    put("metric-prefs/precision", UserRoutes::putMetricPrecisionMe, Role.LOGGED_IN)

                    get("email-prefs", UserRoutes::getEmailPreferencesMe, Role.LOGGED_IN)
                    put("email-prefs", UserRoutes::putEmailPreferencesMe, Role.LOGGED_IN)
                }

                get("fleet-access", UserRoutes::getUserFullFleetAccess, Role.LOGGED_IN)
                put("select-fleet", UserRoutes::putUserFleetSelected, Role.LOGGED_IN)
                put("leave-fleet", UserRoutes::putUserLeaveCurrentFleet, Role.LOGGED_IN)

                path("{uid}") {
                    get(UserRoutes::getOne, Role.LOGGED_IN, Role.MANAGER_ONLY)
                    get("email-prefs", UserRoutes::getUserEmailPreferences, Role.LOGGED_IN)
                    put("email-prefs", UserRoutes::putUserEmailPreferences, Role.LOGGED_IN)
                    patch("fleet-access", UserRoutes::patchUserFleetAccess, Role.LOGGED_IN)
                }

            }
        }
    }

    fun putUserFleetSelected(ctx: Context) {
        val user = SessionUtility.getUser(ctx)
        val fleetIdSelected = ctx.formParam("fleetIdSelected")!!.toInt()

        Database.getConnection().use { connection ->

            //Check that the user has access to this fleet
            var hasAccess = false
            val allFleets:ArrayList<FleetAccess> = FleetAccess.get(connection, user.getId())
            for (fleetAccess in allFleets) {
                if (fleetAccess.fleetId == fleetIdSelected) {
                    hasAccess = true
                    break
                }
            }

            //User has no access -> reject request
            if (!hasAccess) {
                AccountJavalinRoutes.LOG.severe("INVALID ACCESS: user did not have access to select this fleet.")
                ctx.status(401)
                ctx.result("User did not have access to select this fleet.")

            //Otherwise, update their selected fleet
            } else {
                user.setSelectedFleetId(connection, fleetIdSelected)
                ctx.status(200)
                ctx.json(user)
            }
            
        }

    }

    fun putUserLeaveCurrentFleet(ctx: Context) {

        val user = SessionUtility.getUser(ctx)

        try {
            user.leaveSelectedFleet(Database.getConnection())
        } catch (e: SQLException) {
            LOG.severe("Error when user ${user.getId()} attempted to leave fleet ${user.getFleetId()}: ${e.message}")
            ctx.status(500)
            ctx.result("Error when attempting to leave fleet.")
            return
        }

        ctx.status(200)
        ctx.json(user)

    }

    @Throws(SQLException::class)
    fun getUserFullFleetAccess(ctx: Context) {

        val user = SessionUtility.getUser(ctx)

        //Get all the fleets this user has access to
        Database.getConnection().use { connection ->
            val allFleets: ArrayList<FleetAccess> = FleetAccessNamed.get(connection, user.getId())
            ctx.status(200)
            ctx.json(allFleets)
        }

    }

    fun patchUserFleetAccess(ctx: Context) {
        class UpdateUserAccess {
            val message: String = "Success."
        }

        val user = SessionUtility.getUser(ctx)
        val fleetUserId = Objects.requireNonNull(ctx.formParam("fleetUserId"))!!.toInt()
        val fleetId = Objects.requireNonNull(ctx.formParam("fleetId"))!!.toInt()
        val accessType = Objects.requireNonNull(ctx.formParam("accessType"))

        // check to see if the logged-in user can update access to this fleet
        if (!user.managesFleet(fleetId)) {
            AccountJavalinRoutes.LOG.severe("INVALID ACCESS: user did not have access to modify user access rights on this fleet.")
            ctx.status(401)
            ctx.result("User did not have access to modify user access rights on this fleet.")
        } else {
            Database.getConnection().use { connection ->
                FleetAccess.update(connection, fleetUserId, fleetId, accessType)
                user.updateFleet(connection)
                ctx.json(UpdateUserAccess())
            }
        }
    }

    @Throws(SQLException::class)
    fun postSendUserInvite(ctx: Context) {
        class InvitationSent {
            val message: String = "Invitation Sent."
        }

        val user = SessionUtility.getUser(ctx)
        val fleetId = ctx.formParam("fleetId")!!.toInt()
        val fleetName = ctx.formParam("fleetName")!!
        val inviteEmail = ctx.formParam("email")!!

        // check to see if the logged-in user can invite users to this fleet
        if (!user.managesFleet(fleetId)) {
            AccountJavalinRoutes.LOG.severe("INVALID ACCESS: user did not have access to invite other users.")
            ctx.status(401)
            ctx.result("User did not have access to invite other users.")
        } else {
            val recipient: MutableList<String> = ArrayList<String>()
            recipient.add(inviteEmail)

            val encodedFleetName = URLEncoder.encode(fleetName, java.nio.charset.StandardCharsets.UTF_8)
            val formattedInviteLink =
                "https://ngafid.org/create_account?fleet_name=$encodedFleetName&email=$inviteEmail"
            val body =
                "<html><body><p>Hi,<p><br><p>A account creation invitation was sent to your account for fleet: $fleetName<p><p>Please click the link below to create an account.<p><p> <a href=$formattedInviteLink>Create Account</a></p><br></body></html>"

            val bccRecipients: List<String> = ArrayList<String>()
            SendEmail.sendEmail(
                recipient,
                bccRecipients,
                "NGAFID Account Creation Invite",
                body,
                EmailType.ACCOUNT_CREATION_INVITE
            )

            /*
                Write new invitation to the multifleet_invites table.

                If an existing invitation already exists, the operation
                is ignored.

                (TODO: Double check if we should bother informing the sender
                of existing invitations. Might only be a problem when the
                sender email doesn't get updated if someone else sends another
                invite, but this probably won't matter.)
            */
            Database.getConnection().use { connection ->
                val statement = connection.prepareStatement(
                    """
                    INSERT IGNORE INTO multifleet_invites
                        (email, fleet_id, invited_by)
                        VALUES (?, ?, ?)
                    """
                )
                statement.setString(1, inviteEmail)
                statement.setInt(2, fleetId)
                statement.setString(3, user.email)
                statement.executeUpdate()
            }

            ctx.json(InvitationSent())
        }
    }

    data class MultifleetInvite(
        val email: String,
        val fleetId: Int,
        val invitedBy: String
    )
    data class MultifleetInviteResponse(
        val inviteEmail: String,
        val fleetName: String,
        val fleetId: Int = -1
    )

    @Throws(SQLException::class)
    fun getMultifleetInvites(ctx: Context) {
        val user = SessionUtility.getUser(ctx)

        //Fetch all multifleet invites for this user
        Database.getConnection().use { connection ->
            val statement = connection.prepareStatement(
                """
                SELECT * FROM multifleet_invites
                WHERE email = ?
                """
            )
            statement.setString(1, user.email)
            val resultSet = statement.executeQuery()

            val invites = mutableListOf<MultifleetInviteResponse>()
            while (resultSet.next()) {

                val inviteEmail = resultSet.getString("invited_by")
                val fleetId = resultSet.getInt("fleet_id")
                
                val fleet = Fleet.get(connection, fleetId)
                val fleetName = fleet.getName();

                invites.add(
                    MultifleetInviteResponse(
                        inviteEmail = inviteEmail,
                        fleetName = fleetName,
                        fleetId = fleetId
                    )
                )
                
            }

            ctx.json(invites)
        }
    }

    @Throws(SQLException::class)
    fun removeMultifleetInvite(ctx: Context) {

        /*
            NOTE: Removes via the fleet's name, not its ID
        */

        val user = SessionUtility.getUser(ctx)
        val fleetName = ctx.formParam("fleetName")!!
        val fleet = Fleet.get(Database.getConnection(), fleetName)
        val fleetId = fleet.getId()

        LOG.info("Attempting to remove Multifleet Invite with email: ${user.email} and fleetId: ${fleetId}")

        //Verify that this user is the one who was invited
        Database.getConnection().use { connection ->
            val statement = connection.prepareStatement(
                """
                SELECT * FROM multifleet_invites
                WHERE email = ? AND fleet_id = ?
                """
            )
            statement.setString(1, user.email)
            statement.setInt(2, fleetId)
            val resultSet = statement.executeQuery()

            //User is the one who was invited, proceed with removal
            if (resultSet.next()) {
                Database.getConnection().use { conn ->
                    val deleteStatement = conn.prepareStatement(
                        """
                        DELETE FROM multifleet_invites
                        WHERE email = ? AND fleet_id = ?
                        """
                    )
                    deleteStatement.setString(1, user.email)
                    deleteStatement.setInt(2, fleetId)
                    deleteStatement.executeUpdate()
                }
                // ctx.json(InvitationRemoved())
                ctx.status(200);
            } else {
                ctx.status(404)
                ctx.result("Invitation not found.")
            }
        }
    }

    @Throws (SQLException::class)
    fun acceptMultifleetInvite(ctx: Context) {
        
        val user = SessionUtility.getUser(ctx)
        val fleetName = ctx.formParam("fleetName")!!
        val fleet = Fleet.get(Database.getConnection(), fleetName)
        val fleetId = fleet.getId()

        //Create fleet access entry for this user (with VIEW access)
        FleetAccess.create(
            Database.getConnection(),
            user.id,
            fleetId,
            "VIEW"
        )

        LOG.info("Accepted Multifleet access for user: ${user.email} on fleet: ${fleetName}")

        //Remove the invite now that it has been accepted
        removeMultifleetInvite(ctx)

        ctx.status(200)

    }

    fun getEmailPreferencesMe(ctx: Context) {
        val sessionUser = SessionUtility.getUser(ctx)
        var fleetUserID = sessionUser.id

        Database.getConnection().use { connection ->
            ctx.json(User.getUserEmailPreferences(connection, fleetUserID))
        }
    }

    fun getUserEmailPreferences(ctx: Context) {
        val sessionUser = SessionUtility.getUser(ctx)
        var fleetUserID = ctx.pathParam("uid").toInt()
        val fleetID = sessionUser.fleetId

        if (!sessionUser.managesFleet(fleetID)) {
            ctx.status(401)
            ctx.result("User did not have access to fetch user email preferences on this fleet.")
            return
        }

        Database.getConnection().use { connection ->
            ctx.json(User.getUserEmailPreferences(connection, fleetUserID))
        }
    }

    fun putEmailPreferencesMe(ctx: Context) {
        val sessionUser = SessionUtility.getUser(ctx)

        val emailTypesUser: MutableMap<String, Boolean> = HashMap()
        for (emailKey in ctx.formParamMap().keys) {
            emailTypesUser[emailKey] = ctx.formParam(emailKey).toBoolean()
        }

        Database.getConnection().use { connection ->
            ctx.json(User.updateUserEmailPreferences(connection, sessionUser.id, emailTypesUser))
        }
    }

    fun putUserEmailPreferences(ctx: Context) {
        val sessionUser = SessionUtility.getUser(ctx)

        // Unpack Submission Data
        val fleetUserID: Int = ctx.pathParamAsClass<Int>("uid").get()
        val fleetID = sessionUser.fleetId

        val emailTypesUser = HashMap<String, Boolean>()
        for (emailKey in ctx.formParamMap().keys) {
            emailTypesUser[emailKey] = ctx.formParam(emailKey).toBoolean()
        }

        // Check to see if the logged-in user can update access to this fleet
        if (!sessionUser.managesFleet(fleetID)) {
            AccountJavalinRoutes.LOG.severe("INVALID ACCESS: user did not have access to modify user email preferences on this fleet.")
            ctx.status(401)
            ctx.result("User did not have access to modify user email preferences on this fleet.")
            return
        }

        Database.getConnection().use { connection ->
            ctx.json(User.updateUserEmailPreferences(connection, fleetUserID, emailTypesUser))
        }


        // ERROR -- Unknown Update!
        AccountJavalinRoutes.LOG.severe("INVALID ACCESS: handleUpdateType not specified.")
        ctx.status(401)
        ctx.result("handleUpdateType not specified.")
    }

    fun putMetricPrecisionMe(ctx: Context) {
        val user = SessionUtility.getUser(ctx)
        val decimalPrecision = Objects.requireNonNull(ctx.formParam("decimal_precision"))!!.toInt()

        Database.getConnection().use { connection ->
            ctx.json(User.updateUserPreferencesPrecision(connection, user.id, decimalPrecision))
        }
    }

    fun patchMetricPreferencesMe(ctx: Context) {
        val user = SessionUtility.getUser(ctx)
        val userId = user.id
        val metric = Objects.requireNonNull(ctx.formParam("metricName"))
        val type = Objects.requireNonNull(ctx.formParam("modificationType"))

        Database.getConnection().use { connection ->
            AccountJavalinRoutes.LOG.info("Modifying $metric ($type) for user: $user")
            if (type == "addition") {
                User.addUserPreferenceMetric(connection, userId, metric)
            } else {
                User.removeUserPreferenceMetric(connection, userId, metric)
            }

            ctx.json(User.getUserPreferences(connection, userId).flightMetrics)
        }
    }

    /**
     * Fetch all users the currently logged in user has access to.
     */
    private fun getAll(context: Context) {
        val user = SessionUtility.getUser(context)

        if (user.fleetAccessType.equals(FleetAccess.MANAGER)) {
            Database.getConnection().use { connection ->
                val fleet = Fleet.get(connection, user.fleetId)
                context.json(fleet.getUsers(connection))
            }
        } else {
            context.json(listOf(user))
        }
    }

    /**
     * Performs a soft-delete for the currently logged-in user. Logging in will be disabled as will all email
     * communications.
     */
    private fun softDeleteMe(context: Context) {
        TODO()
    }

    /**
     * Updates currently logged in profile
     */
    fun putMe(ctx: Context) {
        val firstName = Objects.requireNonNull(ctx.formParam("firstName"))
        val lastName = Objects.requireNonNull(ctx.formParam("lastName"))
        val country = Objects.requireNonNull(ctx.formParam("country"))
        val state = Objects.requireNonNull(ctx.formParam("state"))
        val city = Objects.requireNonNull(ctx.formParam("city"))
        val address = Objects.requireNonNull(ctx.formParam("address"))
        val phoneNumber = Objects.requireNonNull(ctx.formParam("phoneNumber"))
        val zipCode = Objects.requireNonNull(ctx.formParam("zipCode"))

        val user = SessionUtility.getUser(ctx)
        Database.getConnection().use { connection ->
            user.updateProfile(
                connection,
                firstName,
                lastName,
                country,
                state,
                city,
                address,
                phoneNumber,
                zipCode
            )
            ctx.json(AccountJavalinRoutes.Profile(user))
        }
    }

    @OpenApi(
        summary = "Obtains currently logged in user.",
        operationId = "getMe",
        tags = ["User"],
        responses = [
            OpenApiResponse("200", [OpenApiContent(User::class)]),
        ],
        path = "/api/user/me",
        methods = [HttpMethod.GET]
    )
    fun getMe(ctx: Context) {
        ctx.json(SessionUtility.getUser(ctx))
    }

    @OpenApi(
        summary = "Obtains user with the specified ID, if currently logged in user has manager permissions over the specified user.",
        operationId = "getUser",
        tags = ["User"],
        pathParams = [OpenApiParam("uid", Int::class, "The user ID")],
        responses = [
            OpenApiResponse("200", [OpenApiContent(User::class)]),
            OpenApiResponse("404", [OpenApiContent(ErrorResponse::class)]),
            OpenApiResponse("401", [OpenApiContent(ErrorResponse::class)])
        ],
        path = "/api/user/{uid}",
        methods = [HttpMethod.GET]
    )
    fun getOne(ctx: Context) {
        val currentUser = SessionUtility.getUser(ctx)
        val targetUser = ctx.pathParam("uid").toInt()

        Database.getConnection().use { connection ->
            val fleetUsers = Fleet.get(connection, currentUser.fleetId).getUsers(connection)
            val user = fleetUsers.find { fleetUser -> fleetUser.id == targetUser }

            if (user == null) {
                throw NotFoundResponse("No user with id $targetUser")
            } else {
                ctx.json(user)
            }
        }
    }


    /**
     * Fetches user preferences for the currently logged in user
     */
    fun getMetricPreferencesMe(ctx: Context) {
        val user = SessionUtility.getUser(ctx)

        Database.getConnection().use { connection ->
            ctx.json(User.getUserPreferences(connection, user.id))
        }
    }
}