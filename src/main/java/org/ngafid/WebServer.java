package org.ngafid;


import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import org.ngafid.routes.*;
import org.ngafid.accounts.User;
import org.ngafid.accounts.PasswordAuthentication;

import spark.Spark;
import spark.Session;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.Random;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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
    }

    //to test mustache
    static class Item {
        String name;
        String price;

        public Item(String name, String price) { this.name = name; this.price = price;}
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
        Spark.port( Integer.parseInt(System.getenv("NGAFID_PORT")) );
        //String base = "/" + System.getenv("NGAFID_NAME") + "/";

        // Configuration to serve static files
        Spark.staticFileLocation("/public");

        Spark.before("/protected/*", (request, response) -> {
            LOG.info("protected URI: " + request.uri());

            //if the user session variable has not been set, then don't allow
            //access to the protected pages (the user is not logged in).
            User user = (User)request.session().attribute("user");
            if (user == null) {
                Spark.halt(401, "Access not allowed, you are not logged in."); 
            } 
        });

        Spark.get("", (request, response) -> {
            LOG.severe("''  route!");
            return "Hello!" + MUSTACHE_TEMPLATE_DIR;
        });

        Spark.get("/", (request, response) -> {
            LOG.severe("'/'  route!");
            String resultString = "";
            String templateFile = MUSTACHE_TEMPLATE_DIR + "home.html";
            LOG.severe("template file: '" + templateFile + "'");

            try  {
                MustacheFactory mf = new DefaultMustacheFactory();
                Mustache mustache = mf.compile(templateFile);

                HashMap<String, Object> scopes = new HashMap<String, Object>();

                List<Item> items = Arrays.asList(
                    new Item("Travis", "3.00"),
                    new Item("Shannon", "300.00"),
                    new Item("Momo", "30.00")
                    );

                scopes.put("items", items);

                StringWriter stringOut = new StringWriter();
                mustache.execute(new PrintWriter(stringOut), scopes).flush();
                resultString = stringOut.toString();

            } catch (IOException e) {
                LOG.severe(e.toString());
            }

            return resultString;
        });

        //the following need to be accessible for non-logged in users, and
        //logout doesn't need to be protected
        Spark.post("/login", new PostLogin(gson));
        Spark.post("/logout", new PostLogout(gson));
        Spark.post("/create_account", new PostCreateAccount(gson));

        //routes for initial webpage content
        Spark.post("/get_fleet_names", new PostFleetNames(gson));

        //Spark.post("/protected/main_content", new PostMainContent(gson));
        Spark.post("/protected/upload_details", new PostUploadDetails(gson));
        Spark.post("/protected/get_uploads", new PostUploads(gson));
        Spark.post("/protected/get_imports", new PostImports(gson));
        Spark.post("/protected/get_flights", new PostFlights(gson));
        Spark.post("/protected/update_user_access", new PostUpdateUserAccess(gson));


        //routes for uploading files
        Spark.post("/protected/new_upload", "multipart/form-data", new PostNewUpload(gson));
        Spark.post("/protected/upload", "multipart/form-data", new PostUpload(gson));

        Spark.post("/protected/coordinates", new PostCoordinates(gson));
        Spark.post("/protected/double_series", new PostDoubleSeries(gson));
        Spark.post("/protected/double_series_names", new PostDoubleSeriesNames(gson));

        LOG.info("NGAFID WebServer initialization complete.");
    }
}

