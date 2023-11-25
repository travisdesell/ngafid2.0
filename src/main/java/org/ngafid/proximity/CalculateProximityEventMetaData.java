package org.ngafid.proximity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.ngafid.Database;
import org.ngafid.events.EventMetaData;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.Flight;


/**
 * CalculateProximityEventMetaData
 */
public class CalculateProximityEventMetaData {

    public static void main(String[] args) {
       
        try {
            Connection connection = Database.getConnection();
            String queryString = "SELECT e.flight_id as flight_id, e.other_flight_id as otherFlightId," +
                    " e.start_line as startLine, e.end_line as endLine, e.id as eventId FROM events AS e " +
                    " WHERE e.event_definition_id = -1 AND e.id NOT IN (SELECT event_id FROM event_metadata)";
            PreparedStatement statement = connection.prepareStatement(queryString);
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {

                                
                int flightId = resultSet.getInt(1);
                int otherFlightId = resultSet.getInt(2);
                int startLine = resultSet.getInt(3);
                int endLine = resultSet.getInt(4);
                int eventId = resultSet.getInt(5);
                System.out.println("Processing event : " + eventId);

                DoubleTimeSeries latitudeSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "Latitude");
                DoubleTimeSeries longitudeSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "Longitude");
                DoubleTimeSeries altMSLSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "AltMSL");
                
                DoubleTimeSeries otherLatitudeSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, otherFlightId, "Latitude");
                DoubleTimeSeries otherLongitudeSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, otherFlightId, "Longitude");
                DoubleTimeSeries otherAltMSLSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, otherFlightId, "AltMSL");

                double latitude[] = latitudeSeries.innerArray();
                double longitude[] = longitudeSeries.innerArray();
                double altMSL[] = altMSLSeries.innerArray();
                
                double otherLatitude[] = otherLatitudeSeries.innerArray();
                double otherLongitude[] = otherLongitudeSeries.innerArray();
                double otherAslMSL[] = otherAltMSLSeries.innerArray();

                double lateralDistance = Double.MAX_VALUE;
                double verticalDistance = Double.MAX_VALUE;
                
                for (int i = startLine; i < endLine; i++) {
                     double lateralDistanceFt = CalculateProximity.calculateLateralDistance(latitude[i], longitude[i], otherLatitude[i], otherLongitude[i]);
                     double verticalDistanceFt = CalculateProximity.calculateVerticalDistance(altMSL[i], otherAslMSL[i]);

                     lateralDistance = lateralDistanceFt < lateralDistance ? lateralDistanceFt : lateralDistance;
                     verticalDistance = verticalDistanceFt < verticalDistance ? verticalDistanceFt : verticalDistance;
                }
                
                EventMetaData lateralDistanceMetaData = new EventMetaData("lateral_distance", lateralDistance);
                EventMetaData verticalDistanceMetaData = new EventMetaData("vertical_distance", verticalDistance);

                lateralDistanceMetaData.updateDatabase(connection, eventId);
                verticalDistanceMetaData.updateDatabase(connection, eventId);

                
            }
            resultSet.close();
            statement.close();
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Calculate Event MetaData for proximity events in complete.........");
        System.exit(1);
    }
    
}
