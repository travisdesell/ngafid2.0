package org.ngafid.routes;

import com.google.gson.Gson;

import spark.Request;
import spark.Response;

import java.util.logging.Logger;

public class GetFlightUpload {
    private static final Logger LOG = Logger.getLogger(GetFlightUpload.class.getName());
    private Gson gson;


    public GetFlightUpload(Gson gson) {
        this.gson = gson;

        LOG.info("get " + this.getClass().getName() + " initalized");
    }

    @Override
    public Object handle(Request request, Response response) {

        return null;
    }
}
