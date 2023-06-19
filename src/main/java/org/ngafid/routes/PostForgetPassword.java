package org.ngafid.routes;

import com.google.gson.Gson;
import org.ngafid.Database;
import org.ngafid.accounts.User;
import spark.Request;
import spark.Response;
import spark.Route;

import java.sql.Connection;
import java.util.logging.Logger;

public class PostForgetPassword implements Route {

    private static final Logger LOG = Logger.getLogger(PostForgetPassword.class.getName());
    private Gson gson;

    public PostForgetPassword(Gson gson) {
        this.gson = gson;
        LOG.info("post " + this.getClass().getName() + " initalized");
    }


    @Override
    public Object handle(Request request, Response response) throws Exception {
        Connection connection = Database.getConnection();
        LOG.info("handling " + this.getClass().getName() + " route");
        String email = request.queryParams("email");
        LOG.info("email: '" + email + "'");


        if (User.exists(connection, email)) {
            LOG.info("User exists. Sending reset password email.");

            //TODO : generate random reset phrase and send email and send boolean value if email was sent or not.
            // TODO : Complete redirect to reset password page in js

            User.sendPasswordResetEmail(connection, email);
            return gson.toJson(true);

        }
        else {
            LOG.info("User doesn't exist.");
            return gson.toJson(false);


        }

    }
}
