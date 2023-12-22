package org.ngafid.routes.EventDefManagement;

import com.google.gson.Gson;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.logging.Logger;

public class PostEventDefinitions implements Route {
    private static final Logger LOG = Logger.getLogger(PostEventDefinitions.class.getName());
    private Gson gson;

    public PostEventDefinitions(Gson gson) {
        this.gson = gson;
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        return null;
    }
}
