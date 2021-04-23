package org.ngafid.routes;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;


import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.HashMap;

import java.sql.SQLException;

import com.google.gson.Gson;

import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;

import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.accounts.User;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;


public class GetHome implements Route {
    private static final Logger LOG = Logger.getLogger(GetHome.class.getName());
    private Gson gson;

    private static class Message {
        String type;
        String message;

        Message(String type, String message) {
            this.type = type;
            this.message = message;
        }
    }

    private List<Message> messages = new ArrayList<Message>();

    public GetHome(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
        messages.add(new Message("primary", "This is a beta site for the NGAFID project intended for use by authorized personnel only. Please contact 'aidan <at> labellahome <dot> org' for more information."));

    }

    public GetHome(Gson gson, String messageType, String messageText) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");

        messages.add(new Message(messageType, messageText));
        messages.add(new Message("warning", "We are currently transitioning the NGAFID to a new server and doing significant database and website reimplementation to allow for better performance and new features. Please bear with us while the site is being updated. If you had an account on the old NGAFID website you will need to create a new account."));
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        String resultString = "";
        String templateFile = WebServer.MUSTACHE_TEMPLATE_DIR + "home.html";
        LOG.severe("template file: '" + templateFile + "'");

        try  {
            MustacheFactory mf = new DefaultMustacheFactory();
            Mustache mustache = mf.compile(templateFile);

            HashMap<String, Object> scopes = new HashMap<String, Object>();
            if (messages != null) {
                scopes.put("messages", messages);
            }

            /*
            List<Item> items = Arrays.asList(
                    new Item("Travis", "3.00"),
                    new Item("Shannon", "300.00"),
                    new Item("Momo", "30.00")
                    );

            scopes.put("items", items);
            */

            StringWriter stringOut = new StringWriter();
            mustache.execute(new PrintWriter(stringOut), scopes).flush();
            resultString = stringOut.toString();

        } catch (IOException e) {
            LOG.severe(e.toString());
        }

        return resultString;
    }
}
