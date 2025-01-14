package org.ngafid.routes;

import com.google.gson.Gson;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;
import org.ngafid.Database;
import org.ngafid.accounts.User;
import spark.Request;
import spark.Response;
import spark.Route;

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

        String firstName = request.queryParams("firstName");
        String lastName = request.queryParams("lastName");
        String country = request.queryParams("country");
        String state = request.queryParams("state");
        String city = request.queryParams("city");
        String address = request.queryParams("address");
        String phoneNumber = request.queryParams("phoneNumber");
        String zipCode = request.queryParams("zipCode");

        LOG.info("new firstName: '" + firstName + "'");
        LOG.info("new lastName: '" + lastName + "'");
        LOG.info("new country: '" + country + "'");
        LOG.info("new state: '" + state + "'");
        LOG.info("new city: '" + city + "'");
        LOG.info("new address: '" + address + "'");
        LOG.info("new phoneNumber: '" + phoneNumber + "'");
        LOG.info("new zipCode: '" + zipCode + "'");

        try {
            Connection connection = Database.getConnection();

            User user = (User)request.session().attribute("user");

            user.updateProfile(connection, firstName, lastName, country, state, city, address, phoneNumber, zipCode);

            return gson.toJson(new UpdatedProfile(user));

        } catch (SQLException e) {
            LOG.severe(e.toString());
            e.printStackTrace();
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
