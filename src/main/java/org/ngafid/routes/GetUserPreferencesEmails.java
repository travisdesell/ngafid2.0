package org.ngafid.routes;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;


import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.HashMap;

import java.sql.Connection;
import java.sql.SQLException;

import com.google.gson.Gson;

import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;

import org.ngafid.Database;
import org.ngafid.WebServer;
import org.ngafid.accounts.User;
import org.ngafid.accounts.UserPreferences;
import org.ngafid.accounts.UserPreferencesEmails;
import org.ngafid.flights.Tail;
import org.ngafid.flights.Tails;

public class GetUserPreferencesEmails implements Route {

	private static final Logger LOG = Logger.getLogger(GetUserPreferencesEmails.class.getName());
	private static Connection connection = Database.getConnection();
	private Gson gson;

	public GetUserPreferencesEmails(Gson gson) {

    	this.gson = gson;
    	LOG.info("get " + this.getClass().getName() + " initalized");
   	 
    	}

	@Override
	public Object handle(Request request, Response response) {

    	LOG.info("handling " + this.getClass().getName() + " route");

    	final Session session = request.session();
    	User sessionUser = session.attribute("user");

    	String userIDParam = request.queryParams("userID");
    	int userID;

    	//No userID parameter specified, try using the session user
    	if (userIDParam == null) {

        	if (sessionUser == null) {
            	response.status(400);
            	return gson.toJson(new ErrorResponse("[GET Email Preference Error]", "Missing userID parameter + no session user available"));
            	}

        	userID = sessionUser.getId();

        	}
   	 
    	//userID was specified, try to parse it
    	else {

        	try {
            	userID = Integer.parseInt(userIDParam);
            	}

        	catch (NumberFormatException e) {
            	response.status(400);
            	return gson.toJson(new ErrorResponse("[GET Email Preference Error]", "Specified an invalid userID parameter"));
            	}

        	}

    	try {
        	UserPreferencesEmails userPreferences = User.getUserPreferencesEmails(connection, userID);
        	return gson.toJson(userPreferences);
        	}

    	catch (Exception se) {
        	LOG.severe("Error in GetUserPreferencesEmails.java");
        	se.printStackTrace();
        	response.status(500);
        	return gson.toJson(new ErrorResponse(se));
        	}
    
    	}

	}