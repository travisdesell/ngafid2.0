package org.ngafid;

import java.sql.Connection;
import java.util.*;
import java.sql.*;

import org.ngafid.flights.Flight;

import java.util.logging.Logger;

public class ValidateImporting {

    private static final Logger LOG = Logger.getLogger(Database.class.getName());
    
    public static void main(String[] args) throws SQLException {
        Connection connectionNew = Database.getConnection();
        Connection connectionOld = connectionNew; // Database.createConnection("ngafid", Database.dbName, Database.dbHost, Database.dbPassword);

        HashMap<String, Integer> newFlights = idMap(connectionNew);
        HashMap<String, Integer> oldFlights = idMap(connectionOld);

        HashSet<String> hashes = new HashSet<>();
        hashes.addAll(newFlights.keySet());
        hashes.addAll(oldFlights.keySet());

        LOG.info("Start");
        for (String hash : hashes) {
            if (!oldFlights.containsKey(hash)) {
                LOG.info("Old flight import does not contain flight file with hash '" + hash + "'");
                continue;
            } else if (!newFlights.containsKey(hash)) {
                LOG.info("New flight import does not contain flight file with hash '" + hash + "'");
                continue;
            }

            int oldId = oldFlights.get(hash);
            int newId = newFlights.get(hash);

            LOG.info("Validating flight with hash " + hash);

            Flight oldFlight = Flight.getFlight(connectionOld, oldId);
            Flight newFlight = Flight.getFlight(connectionNew, newId);

            compareFlights(oldFlight, newFlight);
        }
        LOG.info("Stop");
    }

    static void compareFlights(Flight oldFlight, Flight newFlight) {
        Set<String> oldStringColumns = oldFlight.getStringTimeSeriesMap().keySet();
        Set<String> newStringColumns = newFlight.getStringTimeSeriesMap().keySet();

        oldStringColumns.removeAll(newStringColumns);
        for (String column : oldStringColumns) {
            LOG.info("Column '" + column + "' present in one flight but not other.");
        }
    }

    static HashMap<String, Integer> idMap(Connection connection) throws SQLException {
        String idQuery = "SELECT id, md5_hash FROM flights LIMIT 1000000;";

        HashMap<String, Integer> hash2id = new HashMap<>();
        PreparedStatement statement = connection.prepareStatement(idQuery);
        ResultSet results = statement.executeQuery();

        while (results.next()) {
            int id = results.getInt(1);
            String md5 = results.getString(2);
            LOG.info("md5 = " + md5);
            hash2id.put(md5, id);
        }

        results.close();
        statement.close();
    
        return hash2id;
    }

}
