package org.ngafid.routes;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import io.javalin.http.Context;
import io.javalin.rendering.FileRenderer;
import io.javalin.util.JavalinLogger;
import org.jetbrains.annotations.NotNull;
import org.ngafid.WebServer;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

public class MustacheHandler implements FileRenderer {
    public static String handle(String templateFilename, Map<String, ?> scopes) throws IOException {
        MustacheFactory mf = new DefaultMustacheFactory();
        String templateFile = WebServer.MUSTACHE_TEMPLATE_DIR + templateFilename;
        JavalinLogger.info("handling mustache template: " + templateFile);
        Mustache mustache = mf.compile(templateFile);
        StringWriter stringOut = new StringWriter();

        mustache.execute(new PrintWriter(stringOut), scopes).flush();

        return stringOut.toString();
    }

    @NotNull
    @Override
    public String render(@NotNull String s, @NotNull Map<String, ?> map, @NotNull Context context) {
        try {
            return handle(s, map);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
