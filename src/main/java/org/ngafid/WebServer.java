package org.ngafid;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import org.ngafid.accounts.EmailType;
import org.ngafid.common.ConvertToHTML;
import org.ngafid.webserver.JavalinWebServer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import static org.ngafid.SendEmail.sendAdminEmails;

/**
 * The entry point for the NGAFID web server.
 */
public abstract class WebServer {
    public static final String NGAFID_UPLOAD_DIR;
    public static final String NGAFID_ARCHIVE_DIR;
    public static final String MUSTACHE_TEMPLATE_DIR;
    private static final Logger LOG = Logger.getLogger(WebServer.class.getName());

    public static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        NGAFID_UPLOAD_DIR = getEnvironmentVariable("NGAFID_UPLOAD_DIR");
        NGAFID_ARCHIVE_DIR = getEnvironmentVariable("NGAFID_ARCHIVE_DIR");
        MUSTACHE_TEMPLATE_DIR = getEnvironmentVariable("MUSTACHE_TEMPLATE_DIR");
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDateTime.class, new JsonSerializer<>() {
            @Override
            public void serialize(LocalDateTime value, com.fasterxml.jackson.core.JsonGenerator gen,
                                  com.fasterxml.jackson.databind.SerializerProvider serializers) throws IOException {
                gen.writeString(value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
        });
        module.addDeserializer(LocalDateTime.class, new JsonDeserializer<>() {
            @Override
            public LocalDateTime deserialize(com.fasterxml.jackson.core.JsonParser p,
                                             com.fasterxml.jackson.databind.DeserializationContext ctxt) throws IOException {
                return LocalDateTime.parse(p.getText(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
        });
        objectMapper.registerModule(module);
    }

    protected final String staticFilesLocation;
    protected final int port;
    protected final int maxThreads = 32;
    protected final int minThreads = 2;
    protected final int timeOutMillis = 1000 * 60 * 5;

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

        // The application uses Jackson to generate JSON representations of Java objects.
        WebServer webserver = new JavalinWebServer(port, staticFiles);
        LOG.info("NGAFID web server initialization complete.");
    }

    protected void preInitialize() {
    }

    protected abstract void configurePort();

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
        String stackTrace = sw.toString(); // stack trace as a string
        LOG.severe("stack trace:\n" + stackTrace);

        String message =
                "An uncaught exception was thrown in the NGAFID SparkWebServer at " +
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss")) +
                        ".\n The exception was: " + exception + "\n" + ".\n The exception message was: " +
                        exception.getMessage() + "\n" + ".\n The exception (to string): " + exception + "\n" +
                        "\n The non-pretty stack trace is:\n" + stackTrace + "\n" + "\nThe stack trace was:\n" +
                        ConvertToHTML.convertError(exception) + "\n";

        sendAdminEmails(String.format("Uncaught Exception in NGAFID: %s", exception.getMessage()), ConvertToHTML.convertString(message),
                EmailType.ADMIN_EXCEPTION_NOTIFICATION);
    }

    protected void configureLogging() {
        try {
            ClassLoader classLoader = org.ngafid.WebServer.class.getClassLoader();
            final InputStream logConfig = classLoader.getResourceAsStream("log.properties");
            LogManager.getLogManager().readConfiguration(logConfig);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Could not initialize log manager because: " + e.getMessage());
        }
    }
}
