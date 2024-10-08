package org.ngafid.webserver;

import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import org.apache.commons.logging.Log;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.ngafid.WebServer;

import java.util.logging.Logger;

public class JavalinWebServer extends WebServer {
    private static final Logger LOG = Logger.getLogger(JavalinWebServer.class.getName());
    private final Javalin app = Javalin.create();
    private final JavalinConfig config = new JavalinConfig();

    public JavalinWebServer(int port, String staticFilesLocation) {
        super(port, staticFilesLocation);
    }

    @Override
    protected void configureLogging() {
        super.configureLogging();

        config.requestLogger.http((ctx, ms) -> {
            LOG.info(ctx.method() + " " + ctx.path() + " took " + ms + "ms");
        });
    }


    @Override
    protected void configurePort() {
        config.jetty.defaultPort = port;
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
