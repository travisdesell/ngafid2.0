package org.ngafid.routes;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.gson.Gson;
import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.accounts.User;
import org.ngafid.flights.Airframes;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Session;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

public class GetWelcome implements Route {
    private static final Logger LOG = Logger.getLogger(GetWelcome.class.getName());
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

    public GetWelcome(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initialized");
    }

    public GetWelcome(Gson gson, String messageType, String messageText) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initialized");

        messages = new ArrayList<Message>();
        messages.add(new Message(messageType, messageText));
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        String resultString = "";
        String templateFile = WebServer.MUSTACHE_TEMPLATE_DIR + "welcome.html";
        LOG.severe("template file: '" + templateFile + "'");

        final Session session = request.session();
        User user = session.attribute("user");
        int fleetId = user.getFleetId();

        try {
            MustacheFactory mf = new DefaultMustacheFactory();
            Mustache mustache = mf.compile(templateFile);

            Connection connection = Database.getConnection();

            HashMap<String, Object> scopes = new HashMap<String, Object>();

            if (messages != null)
                scopes.put("messages", messages);

            scopes.put("navbar_js", Navbar.getJavascript(request));
            String fleetInfo = "var airframes = " + gson.toJson(Airframes.getAll(connection, fleetId)) + ";\n";
            scopes.put("fleet_info_js", fleetInfo);
            StringWriter stringOut = new StringWriter();
            mustache.execute(new PrintWriter(stringOut), scopes).flush();
            resultString = stringOut.toString();

        } catch (SQLException e) {
            LOG.severe(e.toString());
            return gson.toJson(new ErrorResponse(e));

        } catch (IOException e) {
            LOG.severe(e.toString());
        }

        return resultString;
    }
}
