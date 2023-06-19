package org.ngafid.routes;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.gson.Gson;
import org.ngafid.WebServer;
import spark.Request;
import spark.Response;
import spark.Route;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.logging.Logger;

public class GetForgotPasswordPage implements Route {

    private static final Logger LOG = Logger.getLogger(GetForgotPasswordPage.class.getName());
    private Gson gson;

    public GetForgotPasswordPage(Gson gson) {
        this.gson = gson;
        LOG.info("GET " + this.getClass().getName() + " initalized");
    }
    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        String resultString = "";
        String templateFile = WebServer.MUSTACHE_TEMPLATE_DIR + "forgot_password.html";
        LOG.severe("template file: '" + templateFile + "'");
        HashMap<String, Object> scopes = new HashMap<String, Object>();
        try  {
            MustacheFactory mf = new DefaultMustacheFactory();
            Mustache mustache = mf.compile(templateFile);
            StringWriter stringOut = new StringWriter();
            scopes.put("", "");
            mustache.execute(stringOut, scopes);
            mustache.execute(new PrintWriter(stringOut), scopes).flush();
            resultString = stringOut.toString();

        } catch (Exception e) {
            LOG.severe(e.toString());
        }

        return resultString;
    }
}
