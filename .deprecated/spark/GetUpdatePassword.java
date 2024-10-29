package org.ngafid.routes.spark;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;


import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.HashMap;

import com.google.gson.Gson;

import spark.Route;
import spark.Request;
import spark.Response;

import org.ngafid.WebServer;
import org.ngafid.accounts.User;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;


public class GetUpdatePassword implements Route {
    private static final Logger LOG = Logger.getLogger(GetUpdatePassword.class.getName());
    private Gson gson;

    private static class Message {
        String type;
        String message;

        Message(String type, String message) {
            this.type = type;
            this.message = message;
        }
    }

    private List<Message> messages = null;

    public GetUpdatePassword(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    public GetUpdatePassword(Gson gson, String messageType, String messageText) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");

        messages = new ArrayList<Message>();
        messages.add(new Message(messageType, messageText));
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        String resultString = "";
        String templateFile = WebServer.MUSTACHE_TEMPLATE_DIR + "update_password.html";
        LOG.severe("template file: '" + templateFile + "'");

        try  {
            MustacheFactory mf = new DefaultMustacheFactory();
            Mustache mustache = mf.compile(templateFile);

            HashMap<String, Object> scopes = new HashMap<String, Object>();

            if (messages != null) {
                scopes.put("messages", messages);
            }

            scopes.put("navbar_js", Navbar.getJavascript(request));

            User user = (User)request.session().attribute("user");
            scopes.put("user_js", "var user = JSON.parse('" + gson.toJson(user)  + "');");

            StringWriter stringOut = new StringWriter();
            mustache.execute(new PrintWriter(stringOut), scopes).flush();
            resultString = stringOut.toString();

        } catch (IOException e) {
            LOG.severe(e.toString());
        }

        return resultString;
    }
}
