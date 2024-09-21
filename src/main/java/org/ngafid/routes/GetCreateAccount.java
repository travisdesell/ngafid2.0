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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

public class GetCreateAccount implements Route {
    private static final Logger LOG = Logger.getLogger(GetCreateAccount.class.getName());
    private Gson gson;

    public GetCreateAccount(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName() + " route");

        String resultString = "";
        String templateFile = WebServer.MUSTACHE_TEMPLATE_DIR + "create_account.html";
        LOG.severe("template file: '" + templateFile + "'");

        try {
            MustacheFactory mf = new DefaultMustacheFactory();
            Mustache mustache = mf.compile(templateFile);

            HashMap<String, Object> scopes = new HashMap<String, Object>();

            StringBuilder fleetnamesJavascript = new StringBuilder("var fleetNames = [");
            try (Connection connection = Database.getConnection()) {
                ArrayList<String> names = new ArrayList<String>();

                PreparedStatement query = connection
                        .prepareStatement("SELECT fleet_name FROM fleet ORDER BY fleet_name");
                ResultSet resultSet = query.executeQuery();

                boolean first = true;
                while (resultSet.next()) {
                    if (first) {
                        first = false;
                        fleetnamesJavascript.append("\"");
                        fleetnamesJavascript.append(resultSet.getString(1));
                        fleetnamesJavascript.append("\"");
                    } else {
                        fleetnamesJavascript.append(", \"");
                        fleetnamesJavascript.append(resultSet.getString(1));
                        fleetnamesJavascript.append("\"");
                    }
                }
            } catch (SQLException e) {
                return gson.toJson(new ErrorResponse(e));
            }

            fleetnamesJavascript.append("];");

            /*
             * List<Item> items = Arrays.asList(
             * new Item("Travis", "3.00"),
             * new Item("Shannon", "300.00"),
             * new Item("Momo", "30.00")
             * );
             */

            scopes.put("fleetnames_js", fleetnamesJavascript);

            StringWriter stringOut = new StringWriter();
            mustache.execute(new PrintWriter(stringOut), scopes).flush();
            resultString = stringOut.toString();

        } catch (IOException e) {
            LOG.severe(e.toString());
        }

        return resultString;
    }
}
