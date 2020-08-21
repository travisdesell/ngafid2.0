package org.ngafid.routes;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;


import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.HashMap;

import java.sql.Connection;
import java.sql.SQLException;

import com.google.gson.Gson;

import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;

import org.ngafid.Database;
import org.ngafid.common.*;
import org.ngafid.WebServer;
import org.ngafid.accounts.User;
import org.ngafid.flights.Upload;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;


public class GetUploads implements Route {
    private static final Logger LOG = Logger.getLogger(GetUploads.class.getName());
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

    public GetUploads(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    public GetUploads(Gson gson, String messageType, String messageText) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");

        messages = new ArrayList<Message>();
        messages.add(new Message(messageType, messageText));
    }

    public void replaceAll(StringBuilder builder, String from, String to)
    {
        int index = builder.indexOf(from);
        while (index != -1)
        {
            builder.replace(index, index + from.length(), to);
            index += to.length(); // Move to the end of the replacement
            index = builder.indexOf(from, index);
        }
    }


    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        String resultString = "";
        String templateFile = WebServer.MUSTACHE_TEMPLATE_DIR + "uploads.html";
        LOG.severe("template file: '" + templateFile + "'");

        try  {
            MustacheFactory mf = new DefaultMustacheFactory();
            Mustache mustache = mf.compile(templateFile);

            HashMap<String, Object> scopes = new HashMap<String, Object>();

            if (messages != null) {
                scopes.put("messages", messages);
            }

            scopes.put("navbar_js", Navbar.getJavascript(request));

            final Session session = request.session();
            User user = session.attribute("user");
            int fleetId = user.getFleetId();

            //default page values
            int currentPage = 0;
            int pageSize = 10;

            Connection connection = Database.getConnection();

            int totalUploads = Upload.getNumUploads(connection, fleetId, null);
            int numberPages = totalUploads / pageSize;

            ArrayList<Upload> pending_uploads = Upload.getUploads(connection, fleetId, new String[]{"UPLOADING"});
            //update the status of all the uploads currently uploading to incomplete so the webpage knows they
            //need to be restarted and aren't currently being uploaded.
            for (Upload upload : pending_uploads) {
                if (upload.getStatus().equals("UPLOADING")) {
                    upload.setStatus("UPLOAD INCOMPLETE");
                }
            }

            ArrayList<Upload> other_uploads = Upload.getUploads(connection, fleetId, new String[]{"UPLOADED", "IMPORTED", "ERROR"}, " LIMIT "+ (currentPage * pageSize) + "," + pageSize);


            scopes.put("numPages_js", "var numberPages = " + numberPages + ";");
            scopes.put("index_js", "var currentPage = 0;");

            scopes.put("uploads_js", "var uploads = JSON.parse('" + gson.toJson(other_uploads) + "'); var pending_uploads = JSON.parse('" + gson.toJson(pending_uploads) + "');");

            StringWriter stringOut = new StringWriter();
            mustache.execute(new PrintWriter(stringOut), scopes).flush();
            resultString = stringOut.toString();

        } catch (SQLException e) {
            return gson.toJson(new ErrorResponse(e));

        } catch (Exception e) {
            LOG.severe(e.toString());
        }

        return resultString;
    }
}
