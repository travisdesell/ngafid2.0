package org.ngafid.routes;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.gson.Gson;
import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.accounts.User;
import org.ngafid.events.EventDefinition;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

public class GetImportEventStats implements Route {
    private static final Logger LOG = Logger.getLogger(GetFlight.class.getName());
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

    public GetImportEventStats(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    public GetImportEventStats(Gson gson, String messageType, String messageText) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");

        messages = new ArrayList<Message>();
        messages.add(new Message(messageType, messageText));
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        LOG.info("handling " + this.getClass().getName() + " route");

        String resultString = "";
        String templateFile = WebServer.MUSTACHE_TEMPLATE_DIR + "event_statistics.html";
        LOG.severe("template file: '" + templateFile + "'");

        try  {
            MustacheFactory mf = new DefaultMustacheFactory();
            Mustache mustache = mf.compile(templateFile);

            Connection connection = Database.getConnection();

            final Session session = request.session();
            User user = session.attribute("user");
            int fleetId = user.getFleetId();

            String[] uploadIDs = request.queryParamsValues("upload_id");
            LOG.info("Upload id(s) are: " + Arrays.toString(uploadIDs));

            //ArrayList<EventStatistics> eventStatistics = EventStatistics.getAll(connection, fleetId);

            HashMap<String, Object> scopes = new HashMap<String, Object>();

            if (messages != null) {
                scopes.put("messages", messages);
            }

            scopes.put("navbar_js", Navbar.getJavascript(request));

            long startTime = System.currentTimeMillis();
            scopes.put("events_js",
                    //"var eventStats = JSON.parse('" + gson.toJson(eventStatistics) + "');\n"
                    "var eventDefinitions = JSON.parse('" + gson.toJson(EventDefinition.getAll(connection)) + "');\n" +
                    "var airframeMap = JSON.parse('" + gson.toJson(Airframes.getIdToNameMap(connection, fleetId)) + "');\n" +
                    "var uploadID = " + uploadIDs[0] + ";"
            );
            long endTime = System.currentTimeMillis();
            LOG.info("converting event statistics to JSON took " + (endTime-startTime) + "ms.");

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
