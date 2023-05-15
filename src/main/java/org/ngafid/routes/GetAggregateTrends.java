package org.ngafid.routes;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.HashMap;
import java.sql.Connection;
import java.sql.SQLException;
import com.google.gson.Gson;
import spark.*;
import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.accounts.User;
import org.ngafid.events.EventDefinition;
import org.ngafid.flights.Airframes;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;



public class GetAggregateTrends implements Route {
    private static final Logger LOG = Logger.getLogger(GetTrends.class.getName());
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

    public GetAggregateTrends(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    public GetAggregateTrends(Gson gson, String messageType, String messageText) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");

        messages = new ArrayList<Message>();
        messages.add(new Message(messageType, messageText));
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        String resultString = "";
        String templateFile = WebServer.MUSTACHE_TEMPLATE_DIR + "aggregate_trends.html";
        LOG.severe("template file: '" + templateFile + "'");

        final Session session = request.session();
        User user = session.attribute("user");

        if (!user.hasAggregateView()) {
            LOG.severe("INVALID ACCESS: user did not have aggregate access to view aggregate trends page.");
            Spark.halt(401, "User did not have aggregate access to view aggregate trends page.");
            return null;
        }
        try  {
            MustacheFactory mf = new DefaultMustacheFactory();
            Mustache mustache = mf.compile(templateFile);

            Connection connection = Database.getConnection();

            HashMap<String, Object> scopes = new HashMap<String, Object>();

            if (messages != null) {
                scopes.put("messages", messages);
            }

            scopes.put("navbar_js", Navbar.getJavascript(request));

            long startTime = System.currentTimeMillis();
            System.out.println("-------------------- In GetAggragateTrends -------------------");
            String fleetInfo =
                    "var airframes = " + gson.toJson(Airframes.getAll(connection)) + ";\n" +
                            "var eventNames = " + gson.toJson(EventDefinition.getAllUniqueNames(connection)) + ";\n";
            System.out.println("Fleet info : " + fleetInfo);
            scopes.put("fleet_info_js", fleetInfo);
            long endTime = System.currentTimeMillis();
            LOG.info("getting fleet info took " + (endTime-startTime) + "ms.");

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
