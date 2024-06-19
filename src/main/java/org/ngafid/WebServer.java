package org.ngafid;

import org.ngafid.common.ConvertToHTML;
import org.ngafid.routes.*;
import org.ngafid.accounts.User;

import spark.Spark;
import spark.Service;

import java.io.InputStream;
<<<<<<< Updated upstream
=======
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.IOException;

import java.time.*;
>>>>>>> Stashed changes

import java.time.format.DateTimeFormatter;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.*;
import com.google.gson.TypeAdapter;

import static org.ngafid.SendEmail.sendAdminEmails;

/**
 * The entry point for the NGAFID web server.
 *
 * @author <a href='mailto:tjdvse@rit.edu'>Travis Desell</a>
 */
public final class WebServer {
    private static final Logger LOG = Logger.getLogger(WebServer.class.getName());
    public static final Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();
	
    public static final String NGAFID_UPLOAD_DIR;
    public static final String NGAFID_ARCHIVE_DIR;
    public static final String MUSTACHE_TEMPLATE_DIR;

    public static class LocalDateTimeTypeAdapter extends TypeAdapter<LocalDateTime> {
        @Override
        public void write(final JsonWriter jsonWriter, final LocalDateTime localDate) throws IOException {
            if (localDate == null) {
                jsonWriter.nullValue();
                return;
            }
            jsonWriter.value(localDate.toString());
        }
    
        @Override
        public LocalDateTime read(final JsonReader jsonReader) throws IOException {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            }
            return ZonedDateTime.parse(jsonReader.nextString()).toLocalDateTime();
        }
    }

    public static final Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter()).create();
    
    static {
        if (System.getenv("NGAFID_UPLOAD_DIR") == null) {
            System.err.println("ERROR: 'NGAFID_UPLOAD_DIR' environment variable not specified at runtime.");
            System.err.println("Please add the following to your ~/.bash_rc or ~/.profile file:");
            System.err.println("export NGAFID_UPLOAD_DIR=<path/to/upload_dir>");
            System.exit(1);
        }
        NGAFID_UPLOAD_DIR = System.getenv("NGAFID_UPLOAD_DIR");

        if (System.getenv("NGAFID_ARCHIVE_DIR") == null) {
            System.err.println("ERROR: 'NGAFID_ARCHIVE_DIR' environment variable not specified at runtime.");
            System.err.println("Please add the following to your ~/.bash_rc or ~/.profile file:");
            System.err.println("export NGAFID_ARCHIVE_DIR=<path/to/archive_dir>");
            System.exit(1);
        }
        NGAFID_ARCHIVE_DIR = System.getenv("NGAFID_ARCHIVE_DIR");

        if (System.getenv("MUSTACHE_TEMPLATE_DIR") == null) {
            System.err.println("ERROR: 'MUSTACHE_TEMPLATE_DIR' environment variable not specified at runtime.");
            System.err.println("Please add the following to your ~/.bash_rc or ~/.profile file:");
            System.err.println("export MUSTACHE_TEMPLATE_DIR=<path/to/template_dir>");
            System.exit(1);
        }
        MUSTACHE_TEMPLATE_DIR = System.getenv("MUSTACHE_TEMPLATE_DIR");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            String message = "NGAFID WebServer has shutdown at " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss"));
            LOG.info(message);
            sendAdminEmails(message, "");
        }));
    }

    /** 
     * Entry point for the NGAFID web server.
     *
     * @param args
     *    Command line arguments; none expected.
     */
    public static void main(String[] args) {
        // initialize Logging
        try {
            ClassLoader classLoader = WebServer.class.getClassLoader();
            final InputStream logConfig = classLoader.getResourceAsStream("log.properties");
            LogManager.getLogManager().readConfiguration(logConfig);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Could not initialize log manager because: " + e.getMessage());
        }

        // The application uses Gson to generate JSON representations of Java objects.
        // This should be used by your Ajax Routes to generate JSON for the HTTP
        // response to Ajax requests.

        LOG.info("NGAFID WebServer is initializing.");

        // Get the port for the NGAFID webserver to listen on
        int port = Integer.parseInt(System.getenv("NGAFID_PORT"));
        Spark.port(port);
        
        //----- FOR HTTPS ONLY -----
        if (port == 8443 || port == 443) {
            LOG.info("HTTPS Detected, using a keyfile");
            Spark.secure(System.getenv("HTTPS_CERT_PATH"), System.getenv("HTTPS_PASSKEY"), null, null);

            // Make sure we redirect all HTTP traffic to HTTPS now
            Service http = Service.ignite().port(8080);
            http.before(((request, response) -> {
                final String url = request.url();
                if (url.startsWith("http://")) {
                    final String[] toHttps = url.split("http://");
                    response.redirect("https://" + toHttps[1]);
                }
            }));
        }
        //--------------------------
        
        Spark.webSocketIdleTimeoutMillis(1000 * 60 * 5);

        int maxThreads = 32;
        int minThreads = 2;
        int timeOutMillis = 1000 * 60 * 5;
        Spark.threadPool(maxThreads, minThreads, timeOutMillis);
        //String base = "/" + System.getenv("NGAFID_NAME") + "/";

        // Configuration to serve static files
        //Spark.staticFiles.location("/public");
        if (System.getenv("SPARK_STATIC_FILES") == null) {
            System.err.println("ERROR: 'SPARK_STATIC_FILES' environment variable not specified at runtime.");
            System.err.println("Please add the following to your ~/.bash_rc or ~/.profile file:");
            System.err.println("export SPARK_STATIC_FILES=<path/to/template_dir>");
            System.exit(1);
        }
        Spark.staticFiles.externalLocation(System.getenv("SPARK_STATIC_FILES"));

        Spark.before("/protected/*", (request, response) -> {
            LOG.info("protected URI: " + request.uri());

            //if the user session variable has not been set, then don't allow
            //access to the protected pages (the user is not logged in).
            User user = (User)request.session().attribute("user");

            String previousURI = request.session().attribute("previous_uri");
            if (user == null) {
                //save the previous uri so we can redirect there after the user logs in
                LOG.info("request uri: '" + request.uri());
                LOG.info("request url: '" + request.url());
                LOG.info("request queryString: '" + request.queryString());

                if (request.queryString() != null) {
                    request.session().attribute("previous_uri", request.url() + "?" + request.queryString());
                } else {
                    request.session().attribute("previous_uri", request.url());
                }

                LOG.info("redirecting to access_denied");
                response.redirect("/access_denied");
            } else if (!request.uri().equals("/protected/waiting") && !user.hasViewAccess(user.getFleetId())) {
                LOG.info("user waiting status, redirecting to waiting page!");
                response.redirect("/protected/waiting");
            } else if (previousURI != null) {
                response.redirect(previousURI);
                request.session().attribute("previous_uri", null);
            }
        });

        Spark.before("/", (request, response) -> {
            User user = (User)request.session().attribute("user");
            if (user != null) {
                String previousURI = request.session().attribute("previous_uri");
                if (previousURI != null) {
                    LOG.info("user already logged in, redirecting to the previous page because previous URI was not null");
                    response.redirect(previousURI);
                    request.session().attribute("previous_uri", null);
                } else {
                    LOG.info("user already logged in but accessing the '/' route, redirecting to welcome!");
                    response.redirect("/protected/welcome");
                }
            }
        });

        Spark.get("/", new GetHome(gson));
        Spark.get("/access_denied", new GetHome(gson, "danger", "You attempted to load a page you did not have access to or attempted to access a page while not logged in."));
        Spark.get("/logout_success", new GetHome(gson, "primary", "You have logged out successfully."));
        Spark.get("/sandbox", new GetSandbox(gson));


        //the following need to be accessible for non-logged in users, and
        //logout doesn't need to be protected
        Spark.post("/login", new PostLogin(gson));
        Spark.post("/logout", new PostLogout(gson));

        //for account creation
        Spark.get("/create_account", new GetCreateAccount(gson));
        Spark.post("/create_account", new PostCreateAccount(gson));

        //for submitting forgot password request
        Spark.get("/forgot_password", new GetForgotPassword(gson));
        Spark.post("/forgot_password", new PostForgotPassword(gson));

        //to reset a password
        Spark.get("/reset_password", new GetResetPassword(gson));
        Spark.post("/reset_password", new PostResetPassword(gson));

        Spark.get("/protected/welcome", new GetWelcome(gson));
        Spark.get("/protected/aggregate", new GetAggregate(gson));
        Spark.post("/protected/event_counts", new PostEventCounts(gson));
        Spark.post("/protected/all_event_counts", new PostAllEventCounts(gson));

        Spark.get("/protected/trends", new GetTrends(gson));
        Spark.get("/protected/aggregate_trends", new GetAggregateTrends(gson));
        Spark.post("/protected/monthly_event_counts", new PostMonthlyEventCounts(gson));
        Spark.get("/protected/severities", new GetSeverities(gson));
        Spark.post("/protected/severities", new PostSeverities(gson));

        Spark.get("/protected/event_statistics", new GetEventStatistics(gson));
        Spark.get("/protected/waiting", new GetWaiting(gson));

        Spark.get("/protected/event_definitions", new GetEventDefinitions(gson));

        Spark.get("/protected/manage_fleet", new GetManageFleet(gson));
        Spark.post("/protected/update_user_access", new PostUpdateUserAccess(gson));

        Spark.get("/protected/update_profile", new GetUpdateProfile(gson));
        Spark.post("/protected/update_profile", new PostUpdateProfile(gson));

        Spark.get("/protected/update_password", new GetUpdatePassword(gson));
        Spark.post("/protected/update_password", new PostUpdatePassword(gson));

        Spark.get("/protected/uploads", new GetUploads(gson));
        Spark.get("/protected/airsync_uploads", new GetAirSyncUploads(gson));
        Spark.post("/protected/airsync_uploads", new PostAirSyncUploads(gson));
        Spark.post("/protected/remove_upload", new PostRemoveUpload(gson));

        Spark.get("/protected/imports", new GetImports(gson));
        Spark.get("/protected/airsync_imports", new GetAirSyncImports(gson));
        Spark.post("/protected/airsync_update", new PostManualAirSyncUpdate(gson));
        Spark.post("/protected/airsync_imports", new PostAirSyncImports(gson));
        Spark.post("/protected/airsync_settings", new PostUpdateAirSyncTimeout(gson));
        Spark.post("/protected/upload_details", new PostUploadDetails(gson));

        Spark.post("/protected/uploads", new PostUploads(gson));
        Spark.post("/protected/get_imports", new PostImports(gson));
        Spark.get("/protected/flights", new GetFlights(gson));
        Spark.post("/protected/get_flights", new PostFlights(gson));

        Spark.get("/protected/flight", new GetFlight(gson));

        Spark.get("/protected/ttf", new GetTurnToFinal());
        Spark.post("/protected/ttf", new PostTurnToFinal(gson));

        //add the pagination route
        //Spark.post("/protected/get_page", new PostFlightPage(gson));
        Spark.get("/protected/get_kml", new GetKML(gson));
        Spark.get("/protected/get_xplane", new GetXPlane(gson));
        Spark.get("/protected/get_csv", new GetCSV(gson));
        Spark.get("/protected/sim_acft", new GetSimAircraft(gson));
        Spark.post("/protected/sim_acft", new PostSimAircraft(gson));

        //Flight-Tagging routes
        Spark.post("/protected/flight_tags", new PostTags(gson));
        Spark.post("/protected/create_tag", new PostCreateTag(gson));
        Spark.post("/protected/get_unassociated_tags", new PostUnassociatedTags(gson));
        Spark.post("/protected/associate_tag", new PostAssociateTag(gson));
        Spark.post("/protected/remove_tag", new PostRemoveTag(gson));
        Spark.post("/protected/edit_tag", new PostEditTag(gson));
        
        Spark.get("/protected/flight_display", new GetFlightDisplay(gson));

        // Saving filters routes
        Spark.get("/protected/stored_filters", new GetStoredFilters(gson));
        Spark.post("/protected/store_filter", new PostStoreFilter(gson));
        Spark.post("/protected/remove_filter", new PostRemoveFilter(gson));
        Spark.post("/protected/modify_filter", new PostModifyFilter(gson));

        // Cesium related routes
        Spark.get("/protected/ngafid_cesium", new GetNgafidCesium(gson));
        
        Spark.get("/protected/create_event", new GetCreateEvent(gson));
        Spark.post("/protected/create_event", new PostCreateEvent(gson));
        Spark.get("/protected/update_event", new GetUpdateEvent(gson));
        Spark.post("/protected/update_event", new PostUpdateEvent(gson));

        //routes for uploading files
        Spark.post("/protected/new_upload", "multipart/form-data", new PostNewUpload(gson));
        Spark.post("/protected/upload", "multipart/form-data", new PostUpload(gson));

        // Routes for downloading files
        Spark.get("/protected/download_upload", new GetUpload(gson));

        Spark.post("/protected/coordinates", new PostCoordinates(gson));
        Spark.post("/protected/double_series", new PostDoubleSeries(gson));
        Spark.post("/protected/double_series_names", new PostDoubleSeriesNames(gson));
        Spark.post("/protected/loci_metrics", new PostLOCIMetrics(gson));
        Spark.post("/protected/rate_of_closure", new PostRateOfClosure(gson));

        Spark.post("/protected/events", new PostEvents(gson));
        Spark.post("/protected/event_stat", new PostEventStatistics(gson));

        Spark.get("/protected/system_ids", new GetSystemIds(gson));
        Spark.get("/protected/user_preference", new GetUserPreferences(gson));
        Spark.get("/protected/all_double_series_names", new GetAllDoubleSeriesNames(gson));
        Spark.get("/protected/preferences", new GetUserPreferencesPage(gson));
        Spark.get("/protected/get_event_description", new GetEventDescription(gson));
        Spark.get("/protected/get_all_event_descriptions", new GetAllEventDescriptions(gson));
        Spark.post("/protected/preferences", new PostUserPreferences(gson));
        Spark.post("/protected/preferences_metric", new PostUserPreferencesMetric(gson));
        Spark.post("/protected/update_tail", new PostUpdateTail(gson));

        Spark.get("/protected/*", new GetWelcome(gson, "danger", "The page you attempted to access does not exist."));
        Spark.get("/*", new GetHome(gson, "danger", "The page you attempted to access does not exist."));

        Spark.exception(Exception.class, (exception, request, response) -> {
            String message = new StringBuilder().append("An uncaught exception was thrown in the NGAFID WebServer at ")
                                                .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss")))
                                                .append(".\n The exception was: ").append(exception.getMessage()).append("\n")
                                                .append("\nThe stack trace was:\n").append(ConvertToHTML.convertError(exception)).append("\n").toString();

            LOG.severe("Exception occurred: " + exception.getMessage());
            sendAdminEmails(String.format("Uncaught Exception in NGAFID: %s", exception.getMessage()), ConvertToHTML.convertString(message));
        });

        LOG.info("NGAFID WebServer initialization complete.");
    }
}
