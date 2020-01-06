package org.ngafid.routes;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.gson.Gson;
import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.flights.TurnToFinal;
import spark.Request;
import spark.Response;
import spark.Route;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class GetTurnToFinal implements Route {
    private static final Logger LOG = Logger.getLogger(PostCreateAccount.class.getName());

    private final Gson gson;

    public GetTurnToFinal(Gson gson) {
        this.gson = gson;
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        String flightIdS = request.queryParams("flightId");
        int flightId;
        try {
            flightId = Integer.parseInt(flightIdS);
        } catch (NumberFormatException nfe) {
            // Invalid flight Id
            LOG.warning("GetTurnToFinal request supplied an invalid flight id: '" + flightIdS + "'");
            return  gson.toJson(new ErrorResponse("Invalid Flight Id",
                        "A turn to final request was made for an invalid flight Id"));
        }

        String resultString = "";

        try {
            List<String> ttf = TurnToFinal.getTurnToFinal(Database.getConnection(), flightId)
                    .stream()
                    .map(TurnToFinal::jsonify)
                    .collect(Collectors.toList());

            String templateFile = WebServer.MUSTACHE_TEMPLATE_DIR + "turn_to_final.html";
            LOG.severe("template file: '" + templateFile + "'");

            MustacheFactory mf = new DefaultMustacheFactory();
            Mustache mustache = mf.compile(templateFile);

            HashMap<String, Object> scope = new HashMap<>();
            scope.put("navbar_js", Navbar.getJavascript(request));
            scope.put("ttfs_js",  "var ttfs = " + gson.toJson(ttf) + ";");

            StringWriter stringOut = new StringWriter();
            mustache.execute(new PrintWriter(stringOut), scope).flush();
            resultString = stringOut.toString();
        } catch (SQLException | IOException e) {
            LOG.severe(e.toString());
            return gson.toJson(new ErrorResponse(e));
        }

        return resultString;
    }
}
