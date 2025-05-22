package org.ngafid.www.routes

import io.javalin.http.Context
import io.javalin.http.UnauthorizedResponse
import org.ngafid.core.Database
import org.ngafid.core.accounts.User
import org.ngafid.www.routes.StatisticsJavalinRoutes.StatFetcher

object SessionUtility {
    fun getUser(ctx: Context): User {
        val user = ctx.sessionAttribute<User>("user")
        if (user == null) {
            throw UnauthorizedResponse("User is not logged in.")
        } else {
            return user
        }
    }

    inline fun withStatFetcher(ctx: Context, aggregate: Boolean, block: (StatisticsJavalinRoutes.StatFetcher) -> Unit) {
        Database.getConnection().use { connection -> block(StatFetcher(connection, ctx, aggregate)) }
    }
}