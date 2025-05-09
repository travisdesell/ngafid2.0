package org.ngafid.www.routes.api

import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.config.JavalinConfig
import io.javalin.http.Context
import org.ngafid.core.Database
import org.ngafid.core.accounts.AccountException
import org.ngafid.core.accounts.FleetAccess
import org.ngafid.core.accounts.User
import org.ngafid.www.ErrorResponse
import org.ngafid.www.routes.AccountJavalinRoutes
import org.ngafid.www.routes.AccountJavalinRoutes.*
import org.ngafid.www.routes.Role
import org.ngafid.www.routes.RouteProvider
import java.util.*
import java.util.logging.Logger

object AuthRoutes : RouteProvider() {
    val LOG: Logger = Logger.getLogger(AuthRoutes::class.java.name)

    override fun bind(app: JavalinConfig) {
        app.router.apiBuilder {
            path("/api/auth") {
                post("/login", AuthRoutes::postLogin, Role.OPEN)
                post("/logout", AuthRoutes::postLogout, Role.OPEN)

                // Create account
                post("/register", AuthRoutes::postRegister, Role.OPEN)

                // Request password reset link
                post("/forgot-password", AuthRoutes::postForgotPassword, Role.OPEN)

                // Submit new password w/ token
                post("/reset-password", AuthRoutes::postResetPassword, Role.OPEN)

                // Logged in user changes password
                patch("/change-password", AuthRoutes::patchUpdatePassword, Role.LOGGED_IN)
            }
        }
    }

    fun postLogin(ctx: Context) {
        val email: String = ctx.formParam("email")!!
        val password: String = ctx.formParam("password")!!

        try {
            Database.getConnection().use { connection ->
                val user = User.get(connection, email, password)
                if (user == null) {
                    LOG.info("Could not get user, get returned null.")

                    ctx.json(LoginResponse(true, false, false, false, "Invalid email or password.", null))
                } else {
                    LOG.info("User authentication successful.")

                    user.updateLastLoginTimeStamp(connection)

                    ctx.sessionAttribute("user", user)
                    if (user.fleetAccessType == FleetAccess.DENIED) {
                        ctx.json(LoginResponse(false, false, true, false, "Waiting!", user))
                    } else if (user.fleetAccessType == FleetAccess.WAITING) {
                        ctx.json(LoginResponse(false, true, false, false, "Waiting!", user))
                    } else {
                        ctx.json(LoginResponse(false, false, false, true, "Success!", user))
                    }
                }
            }
        } catch (e: AccountException) {
            ctx.json(LoginResponse(true, false, false, false, "Incorrect email or password.", null))
        }
    }

    fun postLogout(ctx: Context) {
        val user = ctx.sessionAttribute<User>("user")

        // Set the session attribute for this user so it will be considered logged in.
        ctx.sessionAttribute("user", null)
        ctx.req().session.invalidate()
        LOG.info("removed user '" + user!!.email + "' from the session.")

        ctx.json(LogoutResponse(true, false, false, "Successfully logged out.", null))
    }

    fun postForgotPassword(ctx: Context) {
        Database.getConnection().use { connection ->
            val email = ctx.formParam("email")
            if (User.exists(connection, email)) {
                AccountJavalinRoutes.LOG.info("User exists. Sending reset password email.")
                User.sendPasswordResetEmail(connection, email)
                ctx.json(
                    ForgotPasswordResponse(
                        "A password reset link has been sent to your registered email address. Please click on it to reset your password.",
                        true
                    )
                )
            } else {
                AccountJavalinRoutes.LOG.info("User with email : $email doesn't exist.")
                ctx.json(ForgotPasswordResponse("User doesn't exist in database", false))
            }
        }
    }

    fun postRegister(ctx: Context) {
        val email = ctx.formParam("email")
        val password = ctx.formParam("password")
        val firstName = ctx.formParam("firstName")
        val lastName = ctx.formParam("lastName")
        val country = ctx.formParam("country")
        val state = ctx.formParam("state")
        val city = ctx.formParam("city")
        val address = ctx.formParam("address")
        val phoneNumber = ctx.formParam("phoneNumber")
        val zipCode = ctx.formParam("zipCode")
        val accountType = ctx.formParam("accountType")

        try {
            Database.getConnection().use { connection ->
                if (accountType != null) {
                    ctx.json(
                        ErrorResponse(
                            "Invalid Account Type",
                            "A request was made to create an account with an unknown account type '$accountType'."
                        )
                    )
                }
                if (accountType != null && accountType == "gaard") {
                    ctx.json(
                        ErrorResponse(
                            "Gaard Account Creation Disabled",
                            "We apologize but Gaard account creation is currently disabled as we transition to the beta version of the NGAFID 2.0."
                        )
                    )
                } else if (accountType == "newFleet") {
                    val fleetName = ctx.formParam("fleetName")
                    val user = User.createNewFleetUser(
                        connection,
                        email,
                        password,
                        firstName,
                        lastName,
                        country,
                        state,
                        city,
                        address,
                        phoneNumber,
                        zipCode,
                        fleetName
                    )
                    ctx.sessionAttribute("user", user)

                    ctx.json(CreatedAccount(accountType, user))
                } else if (accountType == "existingFleet") {
                    val fleetName = ctx.formParam("fleetName")
                    val user = User.createExistingFleetUser(
                        connection,
                        email,
                        password,
                        firstName,
                        lastName,
                        country,
                        state,
                        city,
                        address,
                        phoneNumber,
                        zipCode,
                        fleetName
                    )
                    ctx.sessionAttribute("user", user)

                    ctx.json(CreatedAccount(accountType, user))
                } else {
                    ctx.json(
                        ErrorResponse(
                            "Invalid Account Type",
                            "A request was made to create an account with an unknown account type '$accountType'."
                        )
                    )
                }
            }
        } catch (e: AccountException) {
            ctx.json(ErrorResponse(e)).status(500)
        }
    }

    fun postResetPassword(ctx: Context) {
        val emailAddress = Objects.requireNonNull(ctx.formParam("emailAddress"))
        val passphrase = Objects.requireNonNull(ctx.formParam("passphrase"))
        val newPassword = Objects.requireNonNull(ctx.formParam("newPassword"))
        val confirmPassword = Objects.requireNonNull(ctx.formParam("confirmPassword"))

        try {
            Database.getConnection().use { connection ->
                // 1. make sure the new password and confirm password are the same
                if (newPassword != confirmPassword) {
                    ctx.json(
                        ErrorResponse(
                            "Could not reset password.",
                            "The server received different new and confirmation passwords."
                        )
                    )
                    return
                }

                // 2. make sure the passphrase is valid
                if (!User.validatePassphrase(connection, emailAddress, passphrase)) {
                    ctx.json(ErrorResponse("Could not reset password.", "The passphrase provided was not correct."))
                    return
                }

                User.updatePassword(connection, emailAddress, newPassword)
                val user = User.get(connection, emailAddress, newPassword)

                ctx.sessionAttribute("user", user)
                ctx.json(ResetSuccessResponse(false, false, false, true, "Success!", user))
            }
        } catch (e: AccountException) {
            ctx.json(ResetSuccessResponse(true, false, false, false, "Incorrect email or password.", null))
        }
    }

    fun patchUpdatePassword(ctx: Context) {
        val currentPassword = Objects.requireNonNull(ctx.formParam("currentPassword"))
        val newPassword = Objects.requireNonNull(ctx.formParam("newPassword"))
        val confirmPassword = Objects.requireNonNull(ctx.formParam("confirmPassword"))

        val user = ctx.sessionAttribute<User>("user")!!
        Database.getConnection().use { connection ->
            // 1. make sure currentPassword authenticates against what's in the database
            if (!user.validate(connection, currentPassword)) {
                ctx.json(ErrorResponse("Could not update password.", "The current password was not correct."))
            }

            // 2. make sure the new password and confirm password are the same
            if (newPassword != confirmPassword) {
                ctx.json(
                    ErrorResponse(
                        "Could not update password.",
                        "The server received different new and confirmation passwords."
                    )
                )
            }

            // 3. make sure the new password is different from the old password
            if (currentPassword == newPassword) {
                ctx.json(
                    ErrorResponse(
                        "Could not update password.",
                        "The current password was the same as the new password."
                    )
                )
            }

            user.updatePassword(connection, newPassword)
            ctx.json(Profile(user))
        }
    }
}