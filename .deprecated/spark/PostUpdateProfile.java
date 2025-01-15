package org.ngafid.routes.spark;

import java.util.logging.Logger;

import java.sql.Connection;
import java.sql.SQLException;

import com.google.gson.Gson;

import org.ngafid.routes.ErrorResponse;
import spark.Route;
import spark.Request;
import spark.Response;

import org.ngafid.Database;
import org.ngafid.accounts.User;

public class PostUpdateProfile implements Route {
    private static final Logger LOG = Logger.getLogger(PostUpdateProfile.class.getName());
    private Gson gson;

    public PostUpdateProfile(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    private class UpdatedProfile {
        private User user;

        public UpdatedProfile(User user) {
            this.user = user;
        }
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName());

        String firstName = request.formParams("firstName");
        String lastName = request.formParams("lastName");
        String country = request.formParams("country");
        String state = request.formParams("state");
        String city = request.formParams("city");
        String address = request.formParams("address");
        String phoneNumber = request.formParams("phoneNumber");
        String zipCode = request.formParams("zipCode");

        LOG.info("new firstName: '" + firstName + "'");
        LOG.info("new lastName: '" + lastName + "'");
        LOG.info("new country: '" + country + "'");
        LOG.info("new state: '" + state + "'");
        LOG.info("new city: '" + city + "'");
        LOG.info("new address: '" + address + "'");
        LOG.info("new phoneNumber: '" + phoneNumber + "'");
        LOG.info("new zipCode: '" + zipCode + "'");

        try (Connection connection = Database.getConnection()) {
            User user = (User) request.session().attribute("user");

            user.updateProfile(connection, firstName, lastName, country, state, city, address, phoneNumber, zipCode);

            return gson.toJson(new UpdatedProfile(user));

        } catch (SQLException e) {
            LOG.severe(e.toString());
            e.printStackTrace();
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
