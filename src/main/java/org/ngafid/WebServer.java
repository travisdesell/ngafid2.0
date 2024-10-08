package org.ngafid;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.ngafid.accounts.EmailType;
import org.ngafid.common.ConvertToHTML;
import org.ngafid.webserver.SparkWebServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import static org.ngafid.SendEmail.sendAdminEmails;


/**
 * The entry point for the NGAFID web server.
 *
 * @author <a href='mailto:tjdvse@rit.edu'>Travis Desell</a>
 */
public abstract class WebServer {
    private static final Logger LOG = Logger.getLogger(WebServer.class.getName());

    public static final String NGAFID_UPLOAD_DIR;
    public static final String NGAFID_ARCHIVE_DIR;
    public static final String MUSTACHE_TEMPLATE_DIR;

    protected String ipAddress;
    protected int port;

    protected int maxThreads = 32;
    protected int minThreads = 2;
    protected int timeOutMillis = 1000 * 60 * 5;

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

        // Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        //     String message = "NGAFID WebServer has shutdown at " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss"));
        //     LOG.info(message);
        //     sendAdminEmails(message, "", EmailType.ADMIN_SHUTDOWN_NOTIFICATION);
        // }));
    }

    public WebServer(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;

        configureLogging();
        LOG.info("NGAFID WebServer has started at " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss")));

        setIpAndPort();
        configureThreads();

        if (port == 8443 || port == 443) {
            configureHttps();
        }

        configureStaticFilesLocation();
        configureAuthChecks();
        configureRoutes();
        configureExceptions();
    }


    protected abstract void setIpAndPort();

    protected abstract void configureHttps();

    protected abstract void configureRoutes();

    protected abstract void configureThreads();

    protected abstract void configureStaticFilesLocation();

    protected abstract void configureAuthChecks();

    protected abstract void configureExceptions();

    protected void exceptionHandler(Exception exception) {
        LOG.severe("Exception: " + exception);
        LOG.severe("Exception message: " + exception.getMessage());

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        String sStackTrace = sw.toString(); // stack trace as a string
        LOG.severe("stack trace:\n" + sStackTrace);

        String message = "An uncaught exception was thrown in the NGAFID SparkWebServer at " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss")) + ".\n The exception was: " + exception + "\n" + ".\n The exception message was: " + exception.getMessage() + "\n" + ".\n The exception (to string): " + exception + "\n" + "\n The non-pretty stack trace is:\n" + sStackTrace + "\n" + "\nThe stack trace was:\n" + ConvertToHTML.convertError(exception) + "\n";

        sendAdminEmails(String.format("Uncaught Exception in NGAFID: %s", exception.getMessage()), ConvertToHTML.convertString(message), EmailType.ADMIN_EXCEPTION_NOTIFICATION);
    }

    private void configureLogging() {
        try {
            ClassLoader classLoader = org.ngafid.webserver.SparkWebServer.class.getClassLoader();
            final InputStream logConfig = classLoader.getResourceAsStream("log.properties");
            LogManager.getLogManager().readConfiguration(logConfig);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Could not initialize log manager because: " + e.getMessage());
        }
    }

    /**
     * Entry point for the NGAFID web server.
     *
     * @param args Command line arguments; none expected.
     */
    public static void main(String[] args) {
        if (System.getenv().containsKey("SERVER_ADDRESS") || System.getenv().containsKey("NGAFID_PORT")) {
            System.err.println("ERROR: 'SERVER_ADDRESS' and 'NGAFID_PORT' environment variables must be set at runtime.");
            System.err.println("Please add the following to your ~/.bash_rc or ~/.profile file:");
            System.err.println("export SERVER_ADDRESS=<ip_address>");
            System.err.println("export NGAFID_PORT=<port>");
            System.exit(1);
        }

        if (System.getenv("SPARK_STATIC_FILES") == null) {
            System.err.println("ERROR: 'SPARK_STATIC_FILES' environment variable not specified at runtime.");
            System.err.println("Please add the following to your ~/.bash_rc or ~/.profile file:");
            System.err.println("export SPARK_STATIC_FILES=<path/to/template_dir>");
            System.exit(1);
        }

        // Get the port for the NGAFID SparkWebServer to listen on
        String ipAddress = System.getenv("SERVER_ADDRESS");
        int port = Integer.parseInt(System.getenv("NGAFID_PORT"));

        // The application uses Gson to generate JSON representations of Java objects.
        // This should be used by your Ajax Routes to generate JSON for the HTTP
        // response to Ajax requests.
        WebServer webserver = new SparkWebServer(ipAddress, port);
        LOG.info("NGAFID SparkWebServer initialization complete.");
    }
}
