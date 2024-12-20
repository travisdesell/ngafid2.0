package org.ngafid.routes;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import io.javalin.util.JavalinLogger;
import org.ngafid.WebServer;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

public class MustacheHandler {
    public static String handle(String templateFilename, Map<String, Object> scopes) throws IOException {
        MustacheFactory mf = new DefaultMustacheFactory();
        String templateFile = WebServer.MUSTACHE_TEMPLATE_DIR + templateFilename;
        JavalinLogger.info("handling mustache template: " + templateFile);
        Mustache mustache = mf.compile(templateFile);
        StringWriter stringOut = new StringWriter();

        mustache.execute(new PrintWriter(stringOut), scopes).flush();

        return stringOut.toString();
    }
}
