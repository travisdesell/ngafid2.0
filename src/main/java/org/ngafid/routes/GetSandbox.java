package org.ngafid.routes;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.gson.Gson;
import org.ngafid.WebServer;
import spark.Request;
import spark.Response;
import spark.Route;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

public class GetSandbox implements Route {
    private static final Logger LOG = Logger.getLogger(GetHome.class.getName());
    private Gson gson;

    public GetSandbox(Gson gson) {
        this.gson = gson;

        LOG.info("get " + this.getClass().getName() + " initalized");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        String resultString = "";
        String templateFile = WebServer.MUSTACHE_TEMPLATE_DIR + "sandbox.html";
        LOG.severe("template file: '" + templateFile + "'");

        try {
            MustacheFactory mf = new DefaultMustacheFactory();
            Mustache mustache = mf.compile(templateFile);

            HashMap<String, Object> scopes = new HashMap<String, Object>();

            // Just an example demonstrating how to use scopes
            List<Double> coolNumbers = new ArrayList<>();
            for (int i = 0 ; i < 10 ; i += 1)
                coolNumbers.add(Math.random());

            scopes.put("coolNumbers", coolNumbers);

            StringWriter stringOut = new StringWriter();
            mustache.execute(new PrintWriter(stringOut), scopes).flush();
            resultString = stringOut.toString();

        } catch (IOException e) {
            LOG.severe(e.toString());
        }

        return resultString;
    }
}
