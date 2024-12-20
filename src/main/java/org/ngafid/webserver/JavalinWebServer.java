package org.ngafid.webserver;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.ngafid.WebServer;
import org.ngafid.accounts.User;
import org.ngafid.routes.MustacheHandler;
import org.ngafid.routes.javalin.*;

import java.util.logging.Logger;

public class JavalinWebServer extends WebServer {
    private static final Logger LOG = Logger.getLogger(JavalinWebServer.class.getName());
    private Javalin app;

    public JavalinWebServer(int port, String staticFilesLocation) {
        super(port, staticFilesLocation);
        LOG.info("Using static files location: " + staticFilesLocation);
        app.start();
    }

    @Override
    protected void preInitialize() {
        app = Javalin.create(config -> {
            config.fileRenderer(new MustacheHandler());
        });
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
        AccountJavalinRoutes.bindRoutes(app);
        AircraftFleetTailsJavalinRoutes.bindRoutes(app);
        AirsyncJavalinRoutes.bindRoutes(app);
        AnalysisJavalinRoutes.bindRoutes(app);
        DataJavalinRoutes.bindRoutes(app);
        DoubleSeriesJavalinRoutes.bindRoutes(app);
        EventJavalinRoutes.bindRoutes(app);
        FlightsJavalinRoutes.bindRoutes(app);
        ImportUploadJavalinRoutes.bindRoutes(app);
        StartPageJavalinRoutes.bindRoutes(app);
        StatisticsJavalinRoutes.bindRoutes(app);
        TagFilterJavalinRoutes.bindRoutes(app);
        UncategorizedJavalinRoutes.bindRoutes(app);
    }

    @Override
    protected void configureThreads() {
        app.unsafeConfig().jetty.threadPool = new QueuedThreadPool(maxThreads, minThreads, timeOutMillis);
    }

    @Override
    protected void configureStaticFilesLocation() {
        app.unsafeConfig().staticFiles.add(staticFilesLocation, Location.EXTERNAL);
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
