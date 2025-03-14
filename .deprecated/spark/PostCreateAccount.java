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
import org.ngafid.accounts.AccountException;
import org.ngafid.accounts.User;

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

        String email = request.formParams("email");
        String password = request.formParams("password");
        String firstName = request.formParams("firstName");
        String lastName = request.formParams("lastName");
        String country = request.formParams("country");
        String state = request.formParams("state");
        String city = request.formParams("city");
        String address = request.formParams("address");
        String phoneNumber = request.formParams("phoneNumber");
        String zipCode = request.formParams("zipCode");
        String accountType = request.formParams("accountType");

        LOG.info("email: '" + email + "'");
        // We should probably not show this for privacy reasons :)
        // LOG.info("password: '" + password + "'");
        LOG.info("firstName: '" + firstName + "'");
        LOG.info("lastName: '" + lastName + "'");
        LOG.info("country: '" + country + "'");
        LOG.info("state: '" + state + "'");
        LOG.info("city: '" + city + "'");
        LOG.info("address: '" + address + "'");
        LOG.info("phoneNumber: '" + phoneNumber + "'");
        LOG.info("zipCode: '" + zipCode + "'");
        LOG.info("accountType: '" + accountType + "'");

        try (Connection connection = Database.getConnection()) {

            if (accountType.equals("gaard")) {
                // User user = User.createGaardUser(connection, email, password, firstName,
                // lastName, country, state, address, phoneNumber, zipCode, accountType);
                // Spark.session().attribute("user", user);
                // return gson.toJson(new CreatedAccount(accountType));

                return gson.toJson(new ErrorResponse("Gaard Account Creation Disabled",
                        "We apologize but Gaard account creation is currently disabled as we transition to the beta version of the NGAFID 2.0."));

            } else if (accountType.equals("newFleet")) {
                String fleetName = request.formParams("fleetName");
                User user = User.createNewFleetUser(connection, email, password, firstName, lastName, country, state,
                        city, address, phoneNumber, zipCode, fleetName);
                request.session().attribute("user", user);

                return gson.toJson(new CreatedAccount(accountType, user));

            } else if (accountType.equals("existingFleet")) {
                String fleetName = request.formParams("fleetName");
                User user = User.createExistingFleetUser(connection, email, password, firstName, lastName, country,
                        state, city, address, phoneNumber, zipCode, fleetName);
                request.session().attribute("user", user);

                return gson.toJson(new CreatedAccount(accountType, user));

            } else {
                return gson.toJson(new ErrorResponse("Invalid Account Type",
                        "A request was made to create an account with an unknown account type '" + accountType + "'."));
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
