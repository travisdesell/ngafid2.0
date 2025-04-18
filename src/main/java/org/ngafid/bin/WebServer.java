package org.ngafid.bin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.ngafid.accounts.EmailType;
import org.ngafid.common.ConvertToHTML;
import org.ngafid.routes.JavalinWebServer;

import java.io.*;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import static org.ngafid.common.SendEmail.sendAdminEmails;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


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

    /**
     * Checks if Python dependencies are satisfied.
     * Activates python virtual environment and runs chartServer.py script using bash command.
     * The method runs on a separate thread so that it doesn't interfere with the NGAFID server
     * Tread management is managed using Executor Service.
     */
    protected void startChartServer() {
        if (!checkChartsDependenciesAndEnvironment()) {
            return;
        }

        String venvActivatePath = "./services/chart_processor/python_venv/bin/activate";
        String chartServerScriptPath = "./services/chart_processor/chartServer.py";

        executorService.submit(() -> { // Run asynchronously
            try {
                String absoluteScriptPath = new File(chartServerScriptPath).getCanonicalPath();
                String command = "source " + venvActivatePath + " && python " + absoluteScriptPath;
                ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command);

                processBuilder.directory(new File(System.getProperty("user.dir")));
                processBuilder.redirectErrorStream(true);

                Process process = processBuilder.start();

                // Read logs in the same thread
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("[Chart Server] " + line);
                    }
                } catch (IOException e) {
                    System.err.println("Error reading chart server logs: " + e.getMessage());
                }

                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    System.err.println("Chart server terminated with exit code: " + exitCode);
                } else {
                    System.out.println("Chart server started successfully with PID: " + process.pid());
                }

            } catch (Exception e) {
                System.err.println("Failed to start chart server: " + e.getMessage());
            }
        });
    }

    /**
     * Checks for Python 3 and GDAL (gdalwarp) are installed.
     * Verifies the existence of a Python virtual environment (python_venv). If not found, it creates one.
     * Installs required Python dependencies from requirements.txt into the virtual environment.
     * @return true if requirements are satisfied and the system can run chart processor.
     */
    protected boolean checkChartsDependenciesAndEnvironment() {
        String venvActivatePath = "./services/chart_processor/python_venv/bin/activate";
        String venvPath = "./services/chart_processor/python_venv";
        String systemPythonPath = "python3";
        String requirementsPath = "./services/chart_processor/requirements.txt";

        try {
            // Check if python3 is installed
            ProcessBuilder checkPython = new ProcessBuilder("bash", "-c", "which python3");
            checkPython.redirectErrorStream(true);
            Process pythonProcess = checkPython.start();
            int pythonExitCode = pythonProcess.waitFor();

            if (pythonExitCode != 0) {
                System.err.println("Error: python3 is not installed.");
                return false;
            }

            // Check if gdalwarp is installed
            ProcessBuilder checkGdalwarp = new ProcessBuilder("bash", "-c", "which gdalwarp");
            checkGdalwarp.redirectErrorStream(true);
            Process gdalwarpProcess = checkGdalwarp.start();
            int gdalwarpExitCode = gdalwarpProcess.waitFor();

            if (gdalwarpExitCode != 0) {
                System.err.println("Error: gdalwarp not found. Please install it before running the script.");
                System.err.println("On macOS: brew install gdal");
                System.err.println("On Linux: sudo apt-get install gdal-bin");
                System.err.println("On Windows: Download from https://www.gisinternals.com/ or install via OSGeo4W");
                return false;
            }

            // Check if virtual environment exists
            File venvActivateFile = new File(venvActivatePath);

            if (!venvActivateFile.exists()) {
                System.out.println("Virtual environment not found. Creating a new one...");

                // Create virtual environment
                ProcessBuilder createVenv = new ProcessBuilder(systemPythonPath, "-m", "venv", venvPath);
                createVenv.redirectErrorStream(true);
                Process venvProcess = createVenv.start();
                int venvExitCode = venvProcess.waitFor();
                if (venvExitCode != 0) {
                    System.err.println("Failed to create virtual environment. Exit code: " + venvExitCode);
                    return false;
                }
            }

            // Install required dependencies into the virtual environment
            File requirementsFile = new File(requirementsPath);
            if (requirementsFile.exists()) {
                System.out.println("Installing dependencies from requirements.txt...");
                ProcessBuilder installDeps = new ProcessBuilder(venvPath + "/bin/pip", "install", "--upgrade", "-r", requirementsPath);
                installDeps.redirectErrorStream(true);
                Process installProcess = installDeps.start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(installProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // Comment this out / remove when the chart service system is tested and stable.
                        System.out.println(line);
                    }
                }

                int installExitCode = installProcess.waitFor();
                if (installExitCode != 0) {
                    System.err.println("Failed to install dependencies. Exit code: " + installExitCode);
                    return false;
                }
            } else {
                System.err.println("Error: requirements.txt not found in ./services/chart_processor/. Cannot install dependencies.");
                return false;
            }

            return true;

        } catch (IOException | InterruptedException e) {
            System.err.println("Dependency check and activation failed: " + e.getMessage());
            return false;
        }
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
        // Chart service
        executorService.submit(this::startChartServer);
      
      

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
            ClassLoader classLoader = WebServer.class.getClassLoader();
            final InputStream logConfig = classLoader.getResourceAsStream("log.properties");
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
