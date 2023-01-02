package org.ngafid.routes;

import com.google.gson.Gson;
import org.ngafid.Database;
import org.ngafid.accounts.EmailFrequency;
import org.ngafid.accounts.User;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Session;

import java.sql.Connection;
import java.util.logging.Logger;

public class PutEmailPreferences implements Route {
    private static final Logger LOG = Logger.getLogger(PutEmailPreferences.class.getName());
    private final Gson gson;
    private static final Connection connection = Database.getConnection();

    public PutEmailPreferences(Gson gson) {
        this.gson = gson;

        LOG.info("put email prefs route initialized.");
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling put email prefs route!");

        final Session session = request.session();
        User user = session.attribute("user");

        boolean emailOptOut = Boolean.parseBoolean(request.queryParams("emailOptOut"));
        boolean emailUploadProcessing = Boolean.parseBoolean(request.queryParams("emailUploadProcessing"));
        boolean emailUploadStatus = Boolean.parseBoolean(request.queryParams("emailUploadStatus"));
        boolean emailCriticalEvents = Boolean.parseBoolean(request.queryParams("emailCriticalEvents"));
        boolean emailUploadError = Boolean.parseBoolean(request.queryParams("emailUploadError"));
        EmailFrequency emailFrequency = EmailFrequency.valueOf((request.queryParams("emailFrequency")));

        try {
            return gson.toJson(User.updateUserEmailPreferences(connection, user.getId(), emailOptOut, emailUploadProcessing,
                    emailUploadStatus, emailCriticalEvents, emailUploadError, emailFrequency));
        } catch (Exception e) {
            e.printStackTrace();
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
