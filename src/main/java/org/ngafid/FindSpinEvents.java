package org.ngafid;

import java.util.*;

import org.ngafid.*;
import org.ngafid.flights.*;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * A Custom Exceedence calculation to find spin events.
 *
 * @author <a href = mailto:apl1341@cs.rit.edu>Aidan LaBella</a>
 */

public class FindSpinEvents {
    static final Connection connection = Database.getConnection();

    public static void findSpinEvents(Upload upload) {
        try {
            String whereClause = "upload_id = " + upload.getId() + " AND insert_completed = 1 AND NOT EXISTS (SELECT flight_id FROM events WHERE id = -2)"; //change this to include spin ends eventually

            List<Flight> flights = Flight.getFlights(connection, whereClause);
            System.out.println("Finding spin events for " + flights.size() + " flights.");

            for (Flight flight : flights) {
                try {
                    flight.findSpinEvents(connection);
                } catch (MalformedFlightFileException mffe) {
                    System.out.println("Can't process flight " + flight.getId());
                }
            }
        } catch (Exception e)  {
            e.printStackTrace();
            System.exit(1);
        } 
    }

    public static void main(String [] args) {
        int fleetId = Integer.parseInt(args[0]);

        System.out.println("Processing spin events for fleet: " + fleetId);

        try {
            List<Upload> uploads = Upload.getUploads(connection, fleetId);

            for (Upload upload : uploads) {
                findSpinEvents(upload);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        System.exit(0);
    }
}
