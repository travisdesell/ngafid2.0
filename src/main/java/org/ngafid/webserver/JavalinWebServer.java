package org.ngafid.webserver;

import io.javalin.Javalin;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.ngafid.WebServer;
import org.ngafid.accounts.User;
import spark.Spark;

import java.util.logging.Logger;

public class JavalinWebServer extends WebServer {
    private static final Logger LOG = Logger.getLogger(JavalinWebServer.class.getName());
    private final Javalin app = Javalin.create();

    public JavalinWebServer(int port, String staticFilesLocation) {
        super(port, staticFilesLocation);
        app.start();
    }

    @Override
    protected void configureLogging() {
        super.configureLogging();

        app.unsafeConfig().requestLogger.http((ctx, ms) -> {
            LOG.info(ctx.method() + " " + ctx.path() + " took " + ms + "ms");
        });
    }

    @Override
    protected void configurePort() {
        app.unsafeConfig().jetty.defaultPort = port;
    }

    @Override
    protected void configureHttps() {

    }

    @Override
    protected void configureRoutes() {


    }

    @Override
    protected void configureThreads() {
        app.unsafeConfig().jetty.threadPool = new QueuedThreadPool(maxThreads, minThreads, timeOutMillis);
    }

    @Override
    protected void configureStaticFilesLocation() {
        app.unsafeConfig().staticFiles.add(staticFilesLocation);
    }

    @Override
    protected void configureAuthChecks() {
        app.before("/protected/*", ctx -> {
            LOG.info("protected URI: " + ctx.path());

            User user = ctx.sessionAttribute("user");
            String previousURI = ctx.sessionAttribute("previous_uri");

            if (user == null) {
                LOG.info("request uri: '" + ctx.path() + "'");
                LOG.info("request url: '" + ctx.url() + "'");
                LOG.info("request queryString: '" + ctx.queryString() + "'");

                if (ctx.queryString() != null) {
                    ctx.sessionAttribute("previous_uri", ctx.url() + "?" + ctx.queryString());
                } else {
                    ctx.sessionAttribute("previous_uri", ctx.url());
                }

                LOG.info("redirecting to access_denied");
                ctx.redirect("/access_denied");
                ctx.status(401).result("Go Away!");

            } else if (!ctx.path().equals("/protected/waiting") && !user.hasViewAccess(user.getFleetId())) {
                LOG.info("user waiting status, redirecting to waiting page!");
                ctx.redirect("/protected/waiting");
                ctx.status(401).result("Go Away!");

            } else if (previousURI != null) {
                ctx.redirect(previousURI);
                ctx.sessionAttribute("previous_uri", null);
            }
        });

        app.before("/", ctx -> {
            User user = ctx.sessionAttribute("user");
            if (user != null) {
                String previousURI = ctx.sessionAttribute("previous_uri");
                if (previousURI != null) {
                    LOG.info("user already logged in, redirecting to the previous page because previous URI was not null");
                    ctx.redirect(previousURI);
                    ctx.sessionAttribute("previous_uri", null);
                } else {
                    LOG.info("user already logged in but accessing the '/' route, redirecting to welcome!");
                    ctx.redirect("/protected/welcome");
                }
            }
        });
    }

    @Override
    protected void configureExceptions() {
        app.exception(Exception.class, (exception, ctx) -> {
            exceptionHandler(exception);
        });
    }
}
