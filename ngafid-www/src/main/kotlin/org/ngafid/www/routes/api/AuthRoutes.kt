// AuthRoutes.kt
package org.ngafid.www.routes.api

import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.config.JavalinConfig
import io.javalin.http.Context
import io.javalin.http.UnauthorizedResponse
import org.ngafid.core.Database
import org.ngafid.core.accounts.AccountException
import org.ngafid.core.accounts.FleetAccess
import org.ngafid.core.accounts.User
import org.ngafid.www.ErrorResponse
import org.ngafid.www.routes.AccountJavalinRoutes
import org.ngafid.www.routes.AccountJavalinRoutes.*
import org.ngafid.www.routes.Role
import org.ngafid.www.routes.RouteProvider
import org.ngafid.www.routes.SessionUtility
import org.ngafid.www.services.TwoFactorAuthService
import java.util.*
import java.util.logging.Logger

object AuthRoutes : RouteProvider() {
    val LOG: Logger = Logger.getLogger(AuthRoutes::class.java.name)

    override fun bind(config: JavalinConfig) {
        config.router.apiBuilder {
            path("/api/auth") {
                post("login", AuthRoutes::postLogin, Role.OPEN)
                post("logout", AuthRoutes::postLogout, Role.OPEN)

                // Create account
                post("register", AuthRoutes::postRegister, Role.OPEN)

                // Request password reset link
                post("forgot-password", AuthRoutes::postForgotPassword, Role.OPEN)

                // Submit new password w/ token
                post("reset-password", AuthRoutes::postResetPassword, Role.OPEN)

                // Logged in user changes password
                patch("change-password", AuthRoutes::patchUpdatePassword, Role.LOGGED_IN)

                // 2FA endpoints
                post("setup-2fa", AuthRoutes::postSetup2FA, Role.LOGGED_IN)
                post("verify-2fa-setup", AuthRoutes::postVerify2FASetup, Role.LOGGED_IN)
                post("disable-2fa", AuthRoutes::postDisable2FA, Role.LOGGED_IN)
                post("generate-backup-codes", AuthRoutes::postGenerateBackupCodes, Role.LOGGED_IN)
                post("reset-2fa-setup", AuthRoutes::postReset2FASetup, Role.LOGGED_IN)
                post("cancel-2fa-setup", AuthRoutes::postCancel2FASetup, Role.LOGGED_IN)
            }
        }
    }

    fun postLogin(ctx: Context) {
        val email: String = ctx.formParam("email")!!
        val password: String = ctx.formParam("password")!!
        val totpCode: String? = ctx.formParam("totpCode")

        LOG.info("Login attempt for email: $email, TOTP provided: ${totpCode != null}")

        try {
            Database.getConnection().use { connection ->
                LOG.info("Attempting to get user from database for email: $email")
                val user = User.get(connection, email, password)
                if (user == null) {
                    LOG.info("Could not get user, get returned null.")
                    ctx.json(LoginResponse(true, false, false, false, "Invalid email or password.", null))
                    return
                }
                
                LOG.info("User found: ${user.email}, 2FA Enabled: ${user.isTwoFactorEnabled}, Setup Complete: ${user.isTwoFactorSetupComplete}")

                // Check if 2FA is enabled but not set up
                if (user.isTwoFactorEnabled && !user.isTwoFactorSetupComplete) {
                    LOG.info("User has 2FA enabled but not set up.")
                    // Store user in session for 2FA setup
                    ctx.sessionAttribute("user", user)
                    ctx.json(LoginResponse(false, false, false, false, "2FA_SETUP_REQUIRED", user))
                    return
                }

                // Check if 2FA is enabled and requires code
                LOG.info("2FA Debug - Enabled: ${user.isTwoFactorEnabled}, SetupComplete: ${user.isTwoFactorSetupComplete}, TOTPCode: $totpCode")
                if (user.isTwoFactorEnabled && user.isTwoFactorSetupComplete && (totpCode == null || totpCode.isBlank())) {
                    LOG.info("User has 2FA enabled, code required.")
                    ctx.json(LoginResponse(false, false, false, false, "2FA_CODE_REQUIRED", user))
                    return
                }

                // Verify 2FA code if provided
                if (user.isTwoFactorEnabled && user.isTwoFactorSetupComplete && totpCode != null && totpCode.isNotEmpty()) {
                    try {
                        val secret = user.getTwoFactorSecret()
                        LOG.info("2FA Debug - Secret: $secret")
                        if (secret == null) {
                            LOG.info("2FA secret is null, cannot verify code.")
                            ctx.json(LoginResponse(true, false, false, false, "2FA configuration error.", null))
                            return
                        }
                        LOG.info("Verifying 2FA code: $totpCode against secret: $secret")
                        if (!TwoFactorAuthService.verifyCode(secret, totpCode.toInt())) {
                            LOG.info("Invalid 2FA code provided.")
                            ctx.json(LoginResponse(true, false, false, false, "Invalid 2FA code.", null))
                            return
                        }
                        LOG.info("2FA code verification successful")
                    } catch (e: NumberFormatException) {
                        LOG.info("Invalid 2FA code format provided: $totpCode")
                        ctx.json(LoginResponse(true, false, false, false, "Invalid 2FA code format.", null))
                        return
                    } catch (e: Exception) {
                        LOG.severe("Error during 2FA verification: ${e.message}")
                        e.printStackTrace()
                        ctx.json(LoginResponse(true, false, false, false, "Error verifying 2FA code.", null))
                        return
                    }
                }

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
        } catch (e: AccountException) {
            ctx.json(LoginResponse(true, false, false, false, "Incorrect email or password.", null))
        } catch (e: Exception) {
            LOG.severe("Unexpected error during login: ${e.message}")
            e.printStackTrace()
            ctx.json(LoginResponse(true, false, false, false, "An unexpected error occurred. Please try again.", null))
        }
    }

    fun postLogout(ctx: Context) {
        try {
            val user = SessionUtility.getUser(ctx)
            
            // Set the session attribute for this user so it will be considered logged in.
            ctx.sessionAttribute("user", null)
            ctx.req().session.invalidate()
            LOG.info("removed user '" + user.email + "' from the session.")

            ctx.json(LogoutResponse(true, false, false, "Successfully logged out.", null))
        } catch (e: UnauthorizedResponse) {
            // User is already logged out or session is invalid
            LOG.info("User already logged out or session invalid")
            ctx.sessionAttribute("user", null)
            ctx.req().session.invalidate()
            ctx.json(LogoutResponse(true, false, false, "Successfully logged out.", null))
        }
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

        val user = SessionUtility.getUser(ctx)
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

    // 2FA Setup - Generate secret and QR code
    fun postSetup2FA(ctx: Context) {
        val user = SessionUtility.getUser(ctx)
        
        Database.getConnection().use { connection ->
            val secret = TwoFactorAuthService.generateSecret()
            val qrCodeUrl = TwoFactorAuthService.generateQRCodeUrl(secret, user.email)
            
            // Update user with 2FA secret but DON'T enable 2FA yet
            // Only set twoFactorEnabled = true after setup is complete
            user.setTwoFactorSecret(secret)
            user.setTwoFactorEnabled(false)  // Keep disabled until setup is complete
            user.setTwoFactorSetupComplete(false)
            
            // Save to database - note: twoFactorEnabled = false
            updateUser2FA(connection, user.id, secret, false, false)
            
            ctx.json(mapOf(
                "success" to true,
                "secret" to secret,
                "qrCodeUrl" to qrCodeUrl,
                "message" to "2FA setup initiated. Scan the QR code with your authenticator app."
            ))
        }
    }

    // Verify 2FA setup with a code
    fun postVerify2FASetup(ctx: Context) {
        val user = SessionUtility.getUser(ctx)
        val code: String = ctx.formParam("code")!!
        
        Database.getConnection().use { connection ->
            if (!TwoFactorAuthService.verifyCode(user.getTwoFactorSecret(), code.toInt())) {
                ctx.json(mapOf(
                    "success" to false,
                    "message" to "Invalid verification code. Please try again."
                ))
                return
            }
            
            // Generate backup codes
            val backupCodes = TwoFactorAuthService.generateBackupCodes()
            val hashedCodes = backupCodes.map { TwoFactorAuthService.hashBackupCode(it) }
            
            // Update user - NOW enable 2FA since setup is complete
            user.setTwoFactorEnabled(true)  // Enable 2FA only after setup is complete
            user.setTwoFactorSetupComplete(true)
            user.setBackupCodes(hashedCodes.joinToString(","))
            
            // Save to database - now twoFactorEnabled = true
            updateUser2FA(connection, user.id, user.getTwoFactorSecret(), true, true, hashedCodes.joinToString(","))
            
            ctx.json(mapOf(
                "success" to true,
                "backupCodes" to backupCodes,
                "message" to "2FA setup completed successfully. Please save your backup codes."
            ))
        }
    }

    // Disable 2FA
    fun postDisable2FA(ctx: Context) {
        val user = SessionUtility.getUser(ctx)
        val password: String = ctx.formParam("password")!!
        
        Database.getConnection().use { connection ->
            // Verify current password
            if (!user.validate(connection, password)) {
                ctx.json(mapOf(
                    "success" to false,
                    "message" to "Invalid password."
                ))
                return
            }
            
            // Disable 2FA
            updateUser2FA(connection, user.id, null, false, false, null)
            
            user.setTwoFactorEnabled(false)
            user.setTwoFactorSecret(null)
            user.setTwoFactorSetupComplete(false)
            user.setBackupCodes(null)
            
            ctx.json(mapOf(
                "success" to true,
                "message" to "2FA has been disabled."
            ))
        }
    }

    // Generate new backup codes
    fun postGenerateBackupCodes(ctx: Context) {
        val user = SessionUtility.getUser(ctx)
        val password: String = ctx.formParam("password")!!
        
        Database.getConnection().use { connection ->
            // Verify current password
            if (!user.validate(connection, password)) {
                ctx.json(mapOf(
                    "success" to false,
                    "message" to "Invalid password."
                ))
                return
            }
            
            // Generate new backup codes
            val backupCodes = TwoFactorAuthService.generateBackupCodes()
            val hashedCodes = backupCodes.map { TwoFactorAuthService.hashBackupCode(it) }
            
            // Update user
            user.setBackupCodes(hashedCodes.joinToString(","))
            updateUserBackupCodes(connection, user.id, hashedCodes.joinToString(","))
            
            ctx.json(mapOf(
                "success" to true,
                "backupCodes" to backupCodes,
                "message" to "New backup codes generated successfully."
            ))
        }
    }

    // Helper method to update 2FA settings in database
    private fun updateUser2FA(connection: java.sql.Connection, userId: Int, secret: String?, enabled: Boolean, setupComplete: Boolean, backupCodes: String? = null) {
        val sql = "UPDATE user SET two_factor_enabled = ?, two_factor_secret = ?, two_factor_setup_complete = ?" +
                (if (backupCodes != null) ", backup_codes = ?" else "") +
                " WHERE id = ?"
        
        connection.prepareStatement(sql).use { stmt ->
            stmt.setBoolean(1, enabled)
            stmt.setString(2, secret)
            stmt.setBoolean(3, setupComplete)
            
            if (backupCodes != null) {
                stmt.setString(4, backupCodes)
                stmt.setInt(5, userId)
            } else {
                stmt.setInt(4, userId)
            }
            
            stmt.executeUpdate()
        }
    }

    // Helper method to update backup codes
    private fun updateUserBackupCodes(connection: java.sql.Connection, userId: Int, backupCodes: String) {
        connection.prepareStatement("UPDATE user SET backup_codes = ? WHERE id = ?").use { stmt ->
            stmt.setString(1, backupCodes)
            stmt.setInt(2, userId)
            stmt.executeUpdate()
        }
    }

    // Reset 2FA setup - allows user to start over if they get stuck
    fun postReset2FASetup(ctx: Context) {
        val user = SessionUtility.getUser(ctx)
        val password: String = ctx.formParam("password")!!
        
        Database.getConnection().use { connection ->
            // Verify current password
            if (!user.validate(connection, password)) {
                ctx.json(mapOf(
                    "success" to false,
                    "message" to "Invalid password."
                ))
                return
            }
            
            // Reset 2FA setup
            updateUser2FA(connection, user.id, null, false, false, null)
            
            user.setTwoFactorEnabled(false)
            user.setTwoFactorSecret(null)
            user.setTwoFactorSetupComplete(false)
            user.setBackupCodes(null)
            
            ctx.json(mapOf(
                "success" to true,
                "message" to "2FA setup has been reset. You can now set it up again."
            ))
        }
    }

    // Cancel incomplete 2FA setup - no password required since 2FA is not enabled yet
    fun postCancel2FASetup(ctx: Context) {
        val user = SessionUtility.getUser(ctx)
        
        Database.getConnection().use { connection ->
            // Only allow cancellation if 2FA is not enabled (setup is incomplete)
            if (user.isTwoFactorEnabled) {
                ctx.json(mapOf(
                    "success" to false,
                    "message" to "Cannot cancel 2FA setup when 2FA is already enabled."
                ))
                return
            }
            
            // Cancel incomplete setup by clearing the secret
            updateUser2FA(connection, user.id, null, false, false, null)
            
            user.setTwoFactorSecret(null)
            user.setTwoFactorSetupComplete(false)
            
            ctx.json(mapOf(
                "success" to true,
                "message" to "2FA setup has been cancelled."
            ))
        }
    }
}