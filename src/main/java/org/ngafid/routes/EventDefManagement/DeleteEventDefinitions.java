package org.ngafid.routes.EventDefManagement;

import com.google.gson.Gson;
import org.ngafid.routes.EventDefManagement.PutEventDefinitions;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.logging.Logger;

public class DeleteEventDefinitions implements Route {
    private static final Logger LOG = Logger.getLogger(DeleteEventDefinitions.class.getName());
    private Gson gson;

    public DeleteEventDefinitions(Gson gson) {
        this.gson = gson;
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        return null;
    }
}
