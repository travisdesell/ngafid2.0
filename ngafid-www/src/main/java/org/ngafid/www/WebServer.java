package org.ngafid.www;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.ngafid.core.accounts.EmailType;
import org.ngafid.core.util.ConvertToHTML;

import java.io.*;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import static org.ngafid.core.util.SendEmail.sendAdminEmails;


/**
 * The entry point for the NGAFID web server.
 *
 * @author <a href='mailto:tjdvse@rit.edu'>Travis Desell</a>
 */
public abstract class WebServer {
    private static final Logger LOG = Logger.getLogger(WebServer.class.getName());

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static final String NGAFID_UPLOAD_DIR;
    public static final String NGAFID_ARCHIVE_DIR;
    public static final String MUSTACHE_TEMPLATE_DIR;

    protected final String staticFilesLocation;
    protected final int port;

    protected final int maxThreads = 32;
    protected final int minThreads = 2;
    protected final int timeOutMillis = 1000 * 60 * 5;
    protected final Map<String, String> environment = System.getenv();

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

    public static class OffsetDateTimeTypeAdapter extends TypeAdapter<OffsetDateTime> {
        private final DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

        @Override
        public void write(final JsonWriter jsonWriter, final OffsetDateTime offsetDateTime) throws IOException {
            if (offsetDateTime == null) {
                jsonWriter.nullValue();
                return;
            }
            jsonWriter.value(formatter.format(offsetDateTime));
        }

        @Override
        public OffsetDateTime read(final JsonReader jsonReader) throws IOException {
            if (jsonReader.peek() == JsonToken.NULL) {
                jsonReader.nextNull();
                return null;
            }
            return OffsetDateTime.parse(jsonReader.nextString(), formatter);
        }
    }

    public static final Gson gson = new GsonBuilder()
            .serializeSpecialFloatingPointValues()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter())
            .registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeTypeAdapter())
            .create();

    static {
        NGAFID_UPLOAD_DIR = getEnvironmentVariable("NGAFID_UPLOAD_DIR");
        NGAFID_ARCHIVE_DIR = getEnvironmentVariable("NGAFID_ARCHIVE_DIR");
        MUSTACHE_TEMPLATE_DIR = getEnvironmentVariable("MUSTACHE_TEMPLATE_DIR");

        // Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        //     String message = "NGAFID WebServer has shutdown at " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss"));
        //     LOG.info(message);
        //     sendAdminEmails(message, "", EmailType.ADMIN_SHUTDOWN_NOTIFICATION);
        // }));
    }


    public WebServer(int port, String staticFilesLocation) {
        this.port = port;
        this.staticFilesLocation = staticFilesLocation;

        preInitialize();
        configureLogging();
        LOG.info("NGAFID WebServer has started at " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss")));

        configurePort();
        configureThreads();

        if (port == 8443 || port == 443) {
            LOG.info("HTTPS Detected, using a keyfile");
            configureHttps();
        }

        configureStaticFilesLocation();
        configureRoutes();
        configureAuthChecks();
        configureExceptions();


        if (environment.containsKey("DISABLE_PERSISTENT_SESSIONS") && environment.get("DISABLE_PERSISTENT_SESSIONS").equalsIgnoreCase("true")) {
            LOG.info("Persistent sessions are disabled.");
        } else {
            configurePersistentSessions();
        }
    }

    protected void preInitialize() {
    }

    public abstract void start();

    protected abstract void configurePort();

    protected abstract void configureHttps();

    protected abstract void configureRoutes();

    protected abstract void configureThreads();

    protected abstract void configureStaticFilesLocation();

    protected abstract void configureAuthChecks();

    protected abstract void configureExceptions();

    protected abstract void configurePersistentSessions();

    protected void exceptionHandler(Exception exception) {
        LOG.severe("Exception: " + exception);
        LOG.severe("Exception message: " + exception.getMessage());

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        String stackTrace = sw.toString(); // stack trace as a string
        LOG.severe("stack trace:\n" + stackTrace);

        String message = "An uncaught exception was thrown in the NGAFID SparkWebServer at " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss")) + ".\n The exception was: " + exception + "\n" + ".\n The exception message was: " + exception.getMessage() + "\n" + ".\n The exception (to string): " + exception + "\n" + "\n The non-pretty stack trace is:\n" + stackTrace + "\n" + "\nThe stack trace was:\n" + ConvertToHTML.convertError(exception) + "\n";

        sendAdminEmails(String.format("Uncaught Exception in NGAFID: %s", exception.getMessage()), ConvertToHTML.convertString(message), EmailType.ADMIN_EXCEPTION_NOTIFICATION);
    }

    protected void configureLogging() {
        try {
            final InputStream logConfig = Files.newInputStream(new File("resources/log.properties").toPath());
            LogManager.getLogManager().readConfiguration(logConfig);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Could not initialize log manager because: " + e.getMessage());
        }
    }

    public static String getEnvironmentVariable(String key) {
        String value = System.getenv(key);
        if (value == null) {
            System.err.println("ERROR: '" + key + "' environment variable not specified at runtime.");
            System.err.println("Please add the following to your ~/.bash_rc or ~/.profile file:");
            System.err.println("export " + key + "=<value>");
            throw new RuntimeException("Environment variable '" + key + "' not set.");
        }

        return value;
    }

    /**
     * Entry point for the NGAFID web server.
     *
     * @param args Command line arguments; none expected.
     */
    public static void main(String[] args) {
        String staticFiles = getEnvironmentVariable("WEBSERVER_STATIC_FILES");
        int port = Integer.parseInt(getEnvironmentVariable("NGAFID_PORT"));

        WebServer webserver = new JavalinWebServer(port, staticFiles);
        LOG.info("NGAFID web server initialization complete.");
        webserver.start();
    }
}
