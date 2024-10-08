package org.ngafid.webserver;

import org.ngafid.WebServer;
import io.javalin.Javalin;

public class JavalinWebServer extends WebServer {
    private final Javalin app = Javalin.create();

    public JavalinWebServer(int port, String staticFilesLocation) {
        super(port, staticFilesLocation);

        app.start(port);
    }


    @Override
    protected void configurePort() {

    }

    @Override
    protected void configureHttps() {

    }

    @Override
    protected void configureRoutes() {

    }

    @Override
    protected void configureThreads() {

    }

    @Override
    protected void configureStaticFilesLocation() {

    }

    @Override
    protected void configureAuthChecks() {

    }

    @Override
    protected void configureExceptions() {
        app.exception(Exception.class, (exception, ctx) -> {
            exceptionHandler(exception);
        });
    }
}
