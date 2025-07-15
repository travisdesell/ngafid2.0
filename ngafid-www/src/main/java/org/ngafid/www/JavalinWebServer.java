package org.ngafid.www;

import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JavalinGson;
import io.javalin.openapi.JsonSchemaLoader;
import io.javalin.openapi.JsonSchemaResource;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.redoc.ReDocPlugin;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;
import io.javalin.security.RouteRole;
import org.eclipse.jetty.server.session.*;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.ngafid.core.Database;
import org.ngafid.core.accounts.FleetAccess;

import org.ngafid.core.accounts.User;
import org.ngafid.www.routes.*;
import org.ngafid.www.routes.api.*; //<-- For Kotlin routes

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

import org.eclipse.jetty.server.session.DatabaseAdaptor;
import org.eclipse.jetty.server.session.DefaultSessionCache;
import org.eclipse.jetty.server.session.FileSessionDataStore;
import org.eclipse.jetty.server.session.JDBCSessionDataStore;
import org.eclipse.jetty.server.session.JDBCSessionDataStoreFactory;
import org.eclipse.jetty.server.session.SessionCache;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.ngafid.core.Database;
import org.ngafid.core.accounts.User;
import org.ngafid.www.routes.AccountJavalinRoutes;
import org.ngafid.www.routes.AircraftFleetTailsJavalinRoutes;
import org.ngafid.www.routes.AirsyncJavalinRoutes;
import org.ngafid.www.routes.AnalysisJavalinRoutes;
import org.ngafid.www.routes.BugReportJavalinRoutes;
import org.ngafid.www.routes.CesiumDataJavalinRoutes;
import org.ngafid.www.routes.DataJavalinRoutes;
import org.ngafid.www.routes.DoubleSeriesJavalinRoutes;
import org.ngafid.www.routes.EventJavalinRoutes;
import org.ngafid.www.routes.FlightsJavalinRoutes;
import org.ngafid.www.routes.ImportUploadJavalinRoutes;
import org.ngafid.www.routes.ProximityMapJavalinRoutes;
import org.ngafid.www.routes.StartPageJavalinRoutes;
import org.ngafid.www.routes.StatisticsJavalinRoutes;
import org.ngafid.www.routes.StatusJavalinRoutes;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JavalinGson;

public class JavalinWebServer extends WebServer {
    private static final Logger LOG = Logger.getLogger(JavalinWebServer.class.getName());
    private Javalin app;

    public JavalinWebServer(int port, String staticFilesLocation) {
        super(port, staticFilesLocation);
        LOG.info("Using static files location: " + staticFilesLocation);
    }

    public void start() {
        app.start();
    }

    @Override
    protected void preInitialize() {
        app = Javalin.create(config -> {
            config.fileRenderer(new MustacheHandler());
            config.jsonMapper(new JavalinGson(WebServer.gson, false));

            //Bind Kotlin Routes
            AuthRoutes.INSTANCE.bind(config);
            AircraftRoutes.INSTANCE.bind(config);
            AirSyncRoutes.INSTANCE.bind(config);
            EventRoutes.INSTANCE.bind(config);
            FilterRoutes.INSTANCE.bind(config);
            FleetRoutes.INSTANCE.bind(config);
            FlightRoutes.INSTANCE.bind(config);
            TagRoutes.INSTANCE.bind(config);
            UploadRoutes.INSTANCE.bind(config);
            UserRoutes.INSTANCE.bind(config);

        });

    }

    private void configureSwagger(JavalinConfig config) {
        final String deprecatedDocsPath = "/api/openapi.json"; // by default it's /openapi
        config.registerPlugin(new OpenApiPlugin(openApiConfig -> {
            openApiConfig
                    .withDocumentationPath("/openapi.json")
                    .withDefinitionConfiguration((version, openApiDefinition) ->
                            openApiDefinition
                                    .withServer(openApiServer ->
                                            openApiServer
                                                    .description("Server description goes here")
                                                    .url("http://localhost:{port}/{basePath}")
                                                    .variable("port", "Server's port", "8111", "8112", "7070")
                                                    .variable("/swagger", "Base path of the server", "", "", "v1")
                                    )
                    );
        }));

        config.registerPlugin(new SwaggerPlugin(swaggerConfiguration -> {
            swaggerConfiguration.setDocumentationPath(deprecatedDocsPath);
        }));

        config.registerPlugin(new ReDocPlugin(reDocConfiguration -> {
            reDocConfiguration.setDocumentationPath(deprecatedDocsPath);
        }));

        for (JsonSchemaResource generatedJsonSchema : new JsonSchemaLoader().loadGeneratedSchemes()) {
            System.out.println(generatedJsonSchema.getName());
            System.out.println(generatedJsonSchema.getContentAsString());
        }
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
        ProximityMapJavalinRoutes.bindRoutes(app);
        StartPageJavalinRoutes.bindRoutes(app);
        StatisticsJavalinRoutes.bindRoutes(app);
        CesiumDataJavalinRoutes.bindRoutes(app);
        StatusJavalinRoutes.bindRoutes(app);
        BugReportJavalinRoutes.bindRoutes(app);
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

        // API Logging compatible with new-style access control as well as old style
        app.before("/*", ctx -> {
            final Set<RouteRole> roles = ctx.routeRoles();
            final String path = ctx.path();

            if (path.startsWith("/protected") || roles.contains(Role.LOGGED_IN)) {
                int statusCode = Integer.parseInt(ctx.status().toString().split(" ")[0]);
                String parsedIp = ctx.ip().replace("[", "").replace("]", "");

                APILogger.logRequest(ctx.method().toString(), ctx.path(), statusCode, parsedIp, ctx.header("Referer"));
            }
        });

        // New style role-based access control
        app.before("/api/*", ctx -> {
            final Set<RouteRole> roles = ctx.routeRoles();
            final User user = ctx.sessionAttribute("user");

            if (roles.contains(Role.LOGGED_IN)) {
                if (user == null) {
                    throw new UnauthorizedResponse("Not logged in.");
                } else if (!user.hasViewAccess(user.getFleetId())) {
                    throw new UnauthorizedResponse("User does not have view access.");
                }

                String accessType = user.getFleetAccessType();

                if (roles.contains(Role.MANAGER_ONLY)) {
                    if (!accessType.equals(FleetAccess.MANAGER))
                        throw new UnauthorizedResponse("Manager access is required.");
                }

                if (roles.contains(Role.UPLOADER_ONLY)) {
                    if (!accessType.equals(FleetAccess.UPLOAD) && !accessType.equals(FleetAccess.MANAGER))
                        throw new UnauthorizedResponse("Uploader access is required.");
                }

                if (roles.contains(Role.ADMIN_ONLY)) {
                    if (!user.isAdmin())
                        throw new UnauthorizedResponse("Admin access is required.");
                }
            }
        });

        app.before("/protected/*", ctx -> {
            LOG.info("protected URI: " + ctx.path());

            User user = ctx.sessionAttribute("user");
            String previousURI = ctx.sessionAttribute("previous_uri");

            if (user == null) {
                if (ctx.queryString() != null) {
                    ctx.sessionAttribute("previous_uri", ctx.url() + "?" + ctx.queryString());
                } else {
                    ctx.sessionAttribute("previous_uri", ctx.url());
                }

                ctx.redirect("/access_denied");
                // Note 401 status is not set since it is non-standard to redirect and Javalin won't render the page
            } else if (!ctx.path().equals("/protected/waiting") && !user.hasViewAccess(user.getFleetId())) {
                ctx.redirect("/protected/waiting");
            } else if (previousURI != null) {
                ctx.redirect(previousURI);
                ctx.sessionAttribute("previous_uri", null);
            }
        });

        // To redirect users to the welcome page.
        app.before("/", ctx -> {
            User user = ctx.sessionAttribute("user");
            if (user != null) {
                String previousURI = ctx.sessionAttribute("previous_uri");
                if (previousURI != null) {
                    ctx.redirect(previousURI);
                    ctx.sessionAttribute("previous_uri", null);
                } else {
                    ctx.redirect("/protected/welcome");
                }
            }
        });
    }

    @Override
    protected void configureExceptions() {
        // will only execute of a more specific handler is not found
        app.exception(Exception.class, (e, ctx) -> {
            LOG.info("Encountered exception: " + e.getMessage());
            e.printStackTrace();
            ctx.json(new ErrorResponse(e));
            ctx.status(500);
        });
    }

    @Override
    protected void configurePersistentSessions() {
        app.unsafeConfig().jetty.modifyServletContextHandler(
                handler -> handler.setSessionHandler(createSessionHandler())
        );
    }

    private static SessionHandler createSessionHandler() {
        SessionHandler sessionHandler = new SessionHandler();
        SessionCache sessionCache = new DefaultSessionCache(sessionHandler);
//        sessionCache.setSessionDataStore(createFileSessionDataStore());
        sessionCache.setSessionDataStore(Objects.requireNonNull(createJDBCDataStore()).getSessionDataStore(sessionHandler));
        sessionHandler.setSessionCache(sessionCache);
        sessionHandler.setHttpOnly(true);
        return sessionHandler;
    }

    private static FileSessionDataStore createFileSessionDataStore() {
        FileSessionDataStore fileSessionDataStore = new FileSessionDataStore();
        File baseDir = new File(System.getProperty("java.io.tmpdir"));
        File storeDir = new File(baseDir, "javalin-session-store");
        storeDir.mkdir();
        fileSessionDataStore.setStoreDir(storeDir);
        return fileSessionDataStore;
    }

    private static JDBCSessionDataStoreFactory createJDBCDataStore() {
        DatabaseAdaptor databaseAdaptor = new DatabaseAdaptor();

        try (Connection connection = Database.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            databaseAdaptor.setDriverInfo(metaData.getDriverName(), metaData.getURL());
            databaseAdaptor.setDatasource(Database.getDataSource());

            JDBCSessionDataStore.SessionTableSchema schema = new JDBCSessionDataStore.SessionTableSchema();
            schema.setTableName("jetty_sessions");

            JDBCSessionDataStoreFactory jdbcSessionDataStoreFactory = new JDBCSessionDataStoreFactory();
            jdbcSessionDataStoreFactory.setDatabaseAdaptor(databaseAdaptor);
            jdbcSessionDataStoreFactory.setSessionTableSchema(schema);

            return jdbcSessionDataStoreFactory;
        } catch (SQLException e) {
            LOG.severe("Failed to get database connection for persistent logins.");
        }
        return null;
    }
}
