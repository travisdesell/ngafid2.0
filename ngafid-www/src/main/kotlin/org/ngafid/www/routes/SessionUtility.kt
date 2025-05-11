package org.ngafid.www.routes

import io.javalin.http.Context
import org.ngafid.core.Database
import org.ngafid.core.accounts.AccountException
import org.ngafid.core.accounts.User
import org.ngafid.www.routes.StatisticsJavalinRoutes.StatFetcher

object SessionUtility {
    fun getUser(ctx: Context): User {
        val user = ctx.sessionAttribute<User>("user")
        if (user == null) {
            throw AccountException("User Session Attribute Not Found", "")
        } else {
            return user
        }
    }

    inline fun withStatFetcher(ctx: Context, aggregate: Boolean, block: (StatisticsJavalinRoutes.StatFetcher) -> Unit) {
        Database.getConnection().use { connection -> block(StatFetcher(connection, ctx, aggregate)) }
    }
}