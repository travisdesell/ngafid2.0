package org.ngafid.www.routes.api

import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.config.JavalinConfig
import io.javalin.http.Context
import org.ngafid.www.routes.AccountJavalinRoutes
import org.ngafid.www.routes.Role
import org.ngafid.www.routes.RouteProvider

object Account : RouteProvider() {
    override fun bind(app: JavalinConfig) {
        app.router.apiBuilder {
            path("/api") {
                path("/auth") {
                    post("/login", AccountJavalinRoutes::postLogin, Role.OPEN)
                    post("/logout", AccountJavalinRoutes::postLogout, Role.OPEN)

                    // Create account
                    post("register", AccountJavalinRoutes::postCreateAccount, Role.OPEN)

                    // Request password reset link
                    post("forgot-password", AccountJavalinRoutes::postForgotPassword, Role.OPEN)

                    // Submit new password w/ token
                    post("reset-password", AccountJavalinRoutes::postResetPassword, Role.OPEN)

                    // TODO: Authenticated user changes password
                    // patch("change-password", ..., Role.LOGGED_IN)
                }

                path("/user") {
                    path("/me") {
                        // Get currently logged in account
                        get(Account::getMe, Role.LOGGED_IN)

                        // Update currently logged in account
                        put(Account::getMe, Role.LOGGED_IN)

                        // Soft delete currently logged in account
                        delete(Account::softDelete, Role.LOGGED_IN)

                        // TODO: get all fleets user has access to.
                        // get("/fleet-access", ..., Role.LOGGED_IN)

                        get("/metric-prefs", AccountJavalinRoutes::postUserPreferences, Role.LOGGED_IN)
                        patch("/metric-prefs", AccountJavalinRoutes::postUserPreferencesMetric, Role.LOGGED_IN)

                        get("/email-prefs", AccountJavalinRoutes::getUserEmailPreferences, Role.LOGGED_IN)
                        patch("/email-prefs", AccountJavalinRoutes::postUpdateUserEmailPreferences, Role.LOGGED_IN)
                    }

                    get(Account::getAll, Role.LOGGED_IN)

                    // Get user with specified ID only if the currently logged in user has managerial access.
                    get("/{id}", Account::getOne, Role.LOGGED_IN);

                    // Manager modifies user fleet access
                    put("/{id}/fleet-access", AccountJavalinRoutes::postUpdateUserAccess, Role.LOGGED_IN)

                    // Send invitation
                    post("/invite", AccountJavalinRoutes::postSendUserInvite, Role.LOGGED_IN)
                }
            }
        }
    }

    /**
     * Fetch all users the currently logged in user has access to.
     */
    private fun getAll(context: Context) {
    }

    private fun softDelete(context: Context) {
        // TODO:
    }

    fun getMe(ctx: Context) {

    }

    fun getOne(ctx: Context) {

    }
}