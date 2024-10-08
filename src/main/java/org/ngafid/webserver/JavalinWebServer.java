package org.ngafid.webserver;

import org.ngafid.WebServer;

public class JavalinWebServer extends WebServer {
    public JavalinWebServer(int port, String staticFilesLocation) {
        super(port, staticFilesLocation);
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

    }
}
