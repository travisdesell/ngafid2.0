package org.ngafid.routes;

import com.google.gson.Gson;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;
import org.ngafid.Database;
import org.ngafid.accounts.AccountException;
import org.ngafid.accounts.User;
import spark.Request;
import spark.Response;
import spark.Route;

public class PostCreateAccount implements Route {
    private static final Logger LOG = Logger.getLogger(PostCreateAccount.class.getName());
    private Gson gson;

    public PostCreateAccount(Gson gson) {
        this.gson = gson;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    private class CreatedAccount {
        private String accountType;
        private User user;

        public CreatedAccount(String accountType, User user) {
            this.accountType = accountType;
            this.user = user;
        }
    }

    @Override
    public Object handle(Request request, Response response) {
        LOG.info("handling " + this.getClass().getName());

        String email = request.queryParams("email");
        String password = request.queryParams("password");
        String firstName = request.queryParams("firstName");
        String lastName = request.queryParams("lastName");
        String country = request.queryParams("country");
        String state = request.queryParams("state");
        String city = request.queryParams("city");
        String address = request.queryParams("address");
        String phoneNumber = request.queryParams("phoneNumber");
        String zipCode = request.queryParams("zipCode");
        String accountType = request.queryParams("accountType");

        LOG.info("email: '" + email + "'");
        // We should probably not show this for privacy reasons :)
        //LOG.info("password: '" + password + "'");
        LOG.info("firstName: '" + firstName + "'");
        LOG.info("lastName: '" + lastName + "'");
        LOG.info("country: '" + country + "'");
        LOG.info("state: '" + state + "'");
        LOG.info("city: '" + city + "'");
        LOG.info("address: '" + address + "'");
        LOG.info("phoneNumber: '" + phoneNumber + "'");
        LOG.info("zipCode: '" + zipCode + "'");
        LOG.info("accountType: '" + accountType + "'");

        try {
            Connection connection = Database.getConnection();

            if (accountType.equals("gaard")) {
                //User user = User.createGaardUser(connection, email, password, firstName, lastName, country, state, address, phoneNumber, zipCode, accountType);
                //Spark.session().attribute("user", user);
                //return gson.toJson(new CreatedAccount(accountType));

                return gson.toJson(new ErrorResponse("Gaard Account Creation Disabled", "We apologize but Gaard account creation is currently disabled as we transition to the beta version of the NGAFID 2.0."));

            } else if (accountType.equals("newFleet")) {
                String fleetName = request.queryParams("fleetName");
                User user = User.createNewFleetUser(connection, email, password, firstName, lastName, country, state, city, address, phoneNumber, zipCode, fleetName);
                request.session().attribute("user", user);

                return gson.toJson(new CreatedAccount(accountType, user));

            } else if (accountType.equals("existingFleet")) {
                String fleetName = request.queryParams("fleetName");
                User user = User.createExistingFleetUser(connection, email, password, firstName, lastName, country, state, city, address, phoneNumber, zipCode, fleetName);
                request.session().attribute("user", user);

                return gson.toJson(new CreatedAccount(accountType, user));

            } else {
                return gson.toJson(new ErrorResponse("Invalid Account Type", "A request was made to create an account with an unknown account type '" + accountType + "'."));
            }

        } catch (SQLException e) {
            LOG.severe(e.toString());
            e.printStackTrace();
            return gson.toJson(new ErrorResponse(e));
        } catch (AccountException e) {
            return gson.toJson(new ErrorResponse(e));
        }
    }
}
