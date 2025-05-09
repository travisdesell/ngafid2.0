package org.ngafid.www.routes.api

import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.config.JavalinConfig
import io.javalin.http.Context
import io.javalin.http.pathParamAsClass
import org.ngafid.core.Database
import org.ngafid.core.accounts.EmailType
import org.ngafid.core.accounts.User
import org.ngafid.core.util.SendEmail
import org.ngafid.www.routes.AccountJavalinRoutes
import org.ngafid.www.routes.Role
import org.ngafid.www.routes.RouteProvider
import java.net.URLEncoder
import java.util.*

object Account : RouteProvider() {
    override fun bind(app: JavalinConfig) {
        app.router.apiBuilder {
            path("/api/user") {
                path("/me") {
                    // Get currently logged in account
                    get(Account::getMe, Role.LOGGED_IN)

                    // Update currently logged in account
                    put(Account::putMe, Role.LOGGED_IN)

                    // Soft delete currently logged in account
                    delete(Account::softDelete, Role.LOGGED_IN)

                    // TODO: get all fleets user has access to.
                    // get("/fleet-access", ..., Role.LOGGED_IN)

                    get("/metric-prefs", Account::getMetricPreferencesMe, Role.LOGGED_IN)
                    patch("/metric-prefs", Account::patchMetricPreferencesMe, Role.LOGGED_IN)
                    patch("/metric-prefs/precision", Account::patchMetricPrecisionMe, Role.LOGGED_IN)

                    get("/email-prefs", Account::getEmailPreferencesMe, Role.LOGGED_IN)
                    put("/email-prefs", Account::putEmailPreferences, Role.LOGGED_IN)
                }

                get(Account::getAll, Role.LOGGED_IN)

                path("/{uid}") {
                    get(Account::getOne, Role.LOGGED_IN)
                    get("/email-prefs", Account::getUserEmailPreferences, Role.LOGGED_IN)
                    put("/email-prefs", Account::putUserEmailPreferences, Role.LOGGED_IN)
                    patch("/fleet-access", AccountJavalinRoutes::postUpdateUserAccess, Role.LOGGED_IN)
                }

                // Manager modifies user fleet access

                // Send invitation
                post("/invite", Account::postSendUserInvite, Role.LOGGED_IN)
            }
        }
    }

    fun postSendUserInvite(ctx: Context) {
        class InvitationSent {
            val message: String = "Invitation Sent."
        }

        val user = ctx.sessionAttribute<User>("user")!!
        val fleetId = ctx.formParam("fleetId")!!.toInt()
        val fleetName = ctx.formParam("fleetName")!!
        val inviteEmail = ctx.formParam("email")!!

        // check to see if the logged-in user can invite users to this fleet
        if (!user.managesFleet(fleetId)) {
            AccountJavalinRoutes.LOG.severe("INVALID ACCESS: user did not have access to invite other users.")
            ctx.status(401)
            ctx.result("User did not have access to invite other users.")
        } else {
            val recipient: MutableList<kotlin.String> = ArrayList<String>()
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

            ctx.json(InvitationSent())
        }
    }

    fun getEmailPreferencesMe(ctx: Context) {
        val sessionUser = ctx.sessionAttribute<User>("user")!!
        var fleetUserID = sessionUser.id

        Database.getConnection().use { connection ->
            ctx.json(User.getUserEmailPreferences(connection, fleetUserID))
        }
    }

    fun getUserEmailPreferences(ctx: Context) {
        val sessionUser = ctx.sessionAttribute<User>("user")!!
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

    fun putEmailPreferences(ctx: Context) {
        val sessionUser = ctx.sessionAttribute<User>("user")!!

        val userID = sessionUser.id

        val emailTypesUser: MutableMap<String, Boolean> = HashMap()
        for (emailKey in ctx.formParamMap().keys) {
            if (emailKey == "handleUpdateType") {
                continue
            }

            emailTypesUser[emailKey] = ctx.formParam(emailKey).toBoolean()
        }

        Database.getConnection().use { connection ->
            ctx.json(User.updateUserEmailPreferences(connection, userID, emailTypesUser))
        }
    }

    fun putUserEmailPreferences(ctx: Context) {
        val sessionUser = ctx.sessionAttribute<User>("user")!!

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

    fun patchMetricPrecisionMe(ctx: Context) {
        val user = Objects.requireNonNull(ctx.sessionAttribute<User>("user"))!!
        val decimalPrecision = Objects.requireNonNull(ctx.formParam("decimal_precision"))!!.toInt()

        Database.getConnection().use { connection ->
            ctx.json(User.updateUserPreferencesPrecision(connection, user.id, decimalPrecision))
        }
    }

    fun patchMetricPreferencesMe(ctx: Context) {
        val user: User = ctx.sessionAttribute<User>("user")!!
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
        throw NotImplementedError()
    }

    /**
     * Performs a soft-delete for the currently logged-in user. Logging in will be disabled as will all email
     * communications.
     */
    private fun softDelete(context: Context) {
        throw NotImplementedError()
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

        Database.getConnection().use { connection ->
            val user = Objects.requireNonNull(ctx.sessionAttribute<User>("user"))
            user!!.updateProfile(
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

    /**
     * GET /api/user/me
     *
     * Fetches the user with the specified ID.
     */
    fun getMe(ctx: Context) {
        throw NotImplementedError()
    }

    /**
     * GET /api/user/{id}
     *
     * Fetches user with the specified ID, so long as the currently logged in user has managerial permission over the user.
     *
     * Returns 401 if the user does not have permission.
     */
    fun getOne(ctx: Context) {
        throw NotImplementedError()
    }


    /**
     * Fetches user preferences for the currently logged in user
     */
    fun getMetricPreferencesMe(ctx: Context) {
        val user = Objects.requireNonNull(ctx.sessionAttribute<User>("user"))!!

        Database.getConnection().use { connection ->
            ctx.json(User.getUserPreferences(connection, user.id))
        }

    }
}