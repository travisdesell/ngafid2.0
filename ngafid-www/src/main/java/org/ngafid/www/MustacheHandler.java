package org.ngafid.www;

import static org.ngafid.core.Config.MUSTACHE_TEMPLATE_DIR;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import io.javalin.http.Context;
import io.javalin.rendering.FileRenderer;
import io.javalin.util.JavalinLogger;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Renders Mustache templates for the web server, serving as Javalin's {@link FileRenderer} implementation.
 *
 * <p>Templates are compiled from the configured Mustache template directory and executed against a scope map,
 * so controllers can produce HTML pages from template files plus their data model.
 */
public class MustacheHandler implements FileRenderer {
    /**
     * Compiles the named Mustache template from the template directory and renders it with the given scope values,
     * returning the rendered output as a string.
     *
     * @param templateFilename the template file name (relative to the Mustache template directory)
     * @param scopes the values made available to the template during rendering
     * @return the rendered template output
     * @throws IOException if the template cannot be read or rendered
     */
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
