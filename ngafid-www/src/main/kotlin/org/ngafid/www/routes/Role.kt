package org.ngafid.www.routes

import io.javalin.security.RouteRole

enum class Role : RouteRole {
    OPEN,
    LOGGED_IN,
}