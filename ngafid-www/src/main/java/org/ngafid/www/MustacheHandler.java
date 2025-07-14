package org.ngafid.www;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import static org.ngafid.core.Config.MUSTACHE_TEMPLATE_DIR;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import io.javalin.http.Context;
import io.javalin.rendering.FileRenderer;
import io.javalin.util.JavalinLogger;

public class MustacheHandler implements FileRenderer {
    public static String handle(String templateFilename, Map<String, ?> scopes) throws IOException {
        MustacheFactory mf = new DefaultMustacheFactory(new File(MUSTACHE_TEMPLATE_DIR));
        String templateFile = MUSTACHE_TEMPLATE_DIR + "/" + templateFilename;
        JavalinLogger.info("handling mustache template: " + templateFile);
        Mustache mustache = mf.compile(templateFilename);
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
