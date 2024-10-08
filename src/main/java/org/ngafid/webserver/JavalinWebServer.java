package org.ngafid.webserver;

import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.ngafid.WebServer;

public class JavalinWebServer extends WebServer {
    private final Javalin app = Javalin.create();
    private final JavalinConfig config = new JavalinConfig();

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
        config.jetty.threadPool = new QueuedThreadPool(maxThreads, minThreads, timeOutMillis);
    }

    @Override
    protected void configureStaticFilesLocation() {
        config.staticFiles.add(staticFilesLocation);
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
