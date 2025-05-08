package org.ngafid.www.routes

import io.javalin.config.JavalinConfig

abstract class RouteProvider {
    abstract fun bind(app: JavalinConfig)
}