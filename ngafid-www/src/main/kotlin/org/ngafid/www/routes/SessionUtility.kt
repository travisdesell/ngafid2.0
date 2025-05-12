package org.ngafid.www.routes

import io.javalin.http.Context
import org.ngafid.core.Database
import org.ngafid.core.accounts.User
import org.ngafid.www.routes.StatisticsJavalinRoutes.StatFetcher
import org.ngafid.www.routes.status.UnauthorizedException

object SessionUtility {
    fun getUser(ctx: Context): User {
        val user = ctx.sessionAttribute<User>("user")
        if (user == null) {
            throw UnauthorizedException()
        } else {
            return user
        }
    }

    inline fun withStatFetcher(ctx: Context, aggregate: Boolean, block: (StatisticsJavalinRoutes.StatFetcher) -> Unit) {
        Database.getConnection().use { connection -> block(StatFetcher(connection, ctx, aggregate)) }
    }
}