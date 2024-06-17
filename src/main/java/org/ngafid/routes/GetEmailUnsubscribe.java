package org.ngafid.routes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;
import spark.Spark;

import org.ngafid.Database;
import org.ngafid.accounts.User;
import org.ngafid.accounts.UserPreferences;
import org.ngafid.accounts.UserEmailPreferences;
import org.ngafid.accounts.EmailType;
import org.ngafid.flights.DoubleTimeSeries;

import static org.ngafid.flights.calculations.Parameters.*;



public class GetEmailUnsubscribe implements Route {

    private static final Logger LOG = Logger.getLogger(GetEmailUnsubscribe.class.getName());
    private Gson gson;
    private static Connection connection = Database.getConnection();

    public GetEmailUnsubscribe(Gson gson) {
        this.gson = gson;
        LOG.info("email unsubscribe route initialized.");
    }

    @Override
    public Object handle(Request request, Response response) throws SQLException {


        int id = Integer.parseInt( request.queryParams("id") );
        String token = request.queryParams("token");

        LOG.info("Attempting to unsubscribe from emails... (id: "+id+", token: "+token+")");


        //Check if the token is valid
        try {
                
            PreparedStatement query = connection.prepareStatement("SELECT * FROM email_unsubscribe_tokens WHERE token=? AND user_id=?");
            query.setString(1, token);
            query.setInt(2, id);
            ResultSet resultSet = query.executeQuery();
            if (!resultSet.next()) {
                String exceptionMessage = "Provided token/id pairing was not found: ("+token+", "+id+"), may have already been used";
                LOG.severe(exceptionMessage);
                throw new Exception(exceptionMessage);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return gson.toJson(new ErrorResponse(e));
        }

        //Remove the token from the database
        PreparedStatement queryTokenRemoval;
        queryTokenRemoval = connection.prepareStatement("DELETE FROM email_unsubscribe_tokens WHERE token=? AND user_id=?");
        queryTokenRemoval.setString(1, token);
        queryTokenRemoval.setInt(2, id);
        queryTokenRemoval.executeUpdate();

        //Set all non-forced email preferences to 0 in the database
        PreparedStatement queryClearPreferences;
        queryClearPreferences = connection.prepareStatement("SELECT * FROM email_preferences WHERE user_id=?");
        queryClearPreferences.setInt(1, id);
        ResultSet resultSet = queryClearPreferences.executeQuery();
        
        while (resultSet.next()) {

            String emailType = resultSet.getString("email_type");
            if (EmailType.isForced(emailType)) {
                continue;
            }

            PreparedStatement update = connection.prepareStatement("UPDATE email_preferences SET enabled=0 WHERE user_id=? AND email_type=?");
            update.setInt(1, id);
            update.setString(2, emailType);
            update.executeUpdate();
        }

        return "Successfully unsubscribed from emails...";
    
    }

}