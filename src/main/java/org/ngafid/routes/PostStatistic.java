package org.ngafid.routes;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.HashMap;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import java.sql.Connection;
import java.sql.SQLException;

import com.google.gson.Gson;

import spark.Route;
import spark.Request;
import spark.Response;
import spark.Session;
import spark.Spark;

import org.ngafid.accounts.User;
import org.ngafid.events.EventStatistics;
import org.ngafid.Database;
import org.ngafid.flights.*;
import org.ngafid.accounts.*;

public class PostStatistic implements Route {
    private static final Logger LOG = Logger.getLogger(PostStatistic.class.getName());

    private Gson gson;
    private final boolean aggregate;

    static record SummaryStatistics(
        boolean aggregate,
        
        int numberFlights,
        int numberAircraft,
        int yearNumberFlights,
        int monthNumberFlights,
        int totalEvents,
        int yearEvents,
        int monthEvents,
        int numberUsers,

        // These should be null if `aggregate` is false.
        Integer numberFleets,

        // Null if aggregate is true
        Integer uploads,
        Integer uploadsNotImported,
        Integer uploadsWithError,
        Integer flightsWithWarning,
        Integer flightsWithError,

        long flightTime,
        long yearFlightTime,
        long monthFlightTime
    ) {}


    public PostStatistic(Gson gson, boolean aggregate) {
        this.gson = gson;
        this.aggregate = aggregate;

        LOG.info("post " + this.getClass().getName() + " initalized");
    }

    @Override
    public Object handle(Request request, Response response) {
    }
