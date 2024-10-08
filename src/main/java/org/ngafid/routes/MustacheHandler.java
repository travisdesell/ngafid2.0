package org.ngafid.routes;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import org.ngafid.WebServer;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;

public class MustacheHandler {
    private static final MustacheFactory mf = new DefaultMustacheFactory();

    public static Object handle(String templateFilename, HashMap<String, Object> scopes) throws IOException {
        String templateFile = WebServer.MUSTACHE_TEMPLATE_DIR + templateFilename;
        Mustache mustache = mf.compile(templateFile);
        StringWriter stringOut = new StringWriter();

        mustache.execute(new PrintWriter(stringOut), scopes).flush();

        return stringOut.toString();
    }
}
