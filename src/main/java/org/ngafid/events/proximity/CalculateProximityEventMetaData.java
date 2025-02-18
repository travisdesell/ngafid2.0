package org.ngafid.events.proximity;

import org.ngafid.common.Database;
import org.ngafid.events.EventMetaData;
import org.ngafid.flights.DoubleTimeSeries;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * CalculateProximityEventMetaData
 */
public final class CalculateProximityEventMetaData {
    private CalculateProximityEventMetaData() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static void main(String[] args) {

        try (Connection connection = Database.getConnection()) {

            String queryString = "SELECT e.flight_id as flight_id, e.other_flight_id as otherFlightId," +
                    " e.start_line as startLine, e.end_line as endLine, " +
                    "e.id as eventId, e.severity as severity FROM events AS e " +
                    " WHERE e.event_definition_id = -1 AND e.severity > 0 " +
                    "AND e.id NOT IN (SELECT event_id FROM event_metadata)";

            PreparedStatement statement = connection.prepareStatement(queryString);
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {

                int flightId = resultSet.getInt(1);
                int otherFlightId = resultSet.getInt(2);
                int startLine = resultSet.getInt(3);
                int endLine = resultSet.getInt(4);
                int eventId = resultSet.getInt(5);
                double severity = resultSet.getDouble(6);

                DoubleTimeSeries latitudeSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId,
                        "Latitude");
                DoubleTimeSeries longitudeSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId,
                        "Longitude");
                DoubleTimeSeries altMSLSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "AltMSL");
                DoubleTimeSeries altAglSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "AltAGL");
                DoubleTimeSeries indicatedAirspeedSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId,
                        "IAS");

                String otherEventQueryString = "select e.id as id, e.start_line as otherStartLine, e.end_line as " +
                        "otherEndLine from events as e where e.severity = ? and " +
                        "e.event_definition_id = -1 and e.flight_id = ? and e.other_flight_id = ?";
                PreparedStatement otherStatement = connection.prepareStatement(otherEventQueryString);

                otherStatement.setDouble(1, severity);
                otherStatement.setInt(2, otherFlightId);
                otherStatement.setInt(3, flightId);

                ResultSet otherResultSet = otherStatement.executeQuery();

                if (otherResultSet.next()) {
                    int otherEventId = otherResultSet.getInt(1);
                    int otherStartLine = otherResultSet.getInt(2);
                    int otherEndLine = otherResultSet.getInt(3);

                    DoubleTimeSeries otherLatitudeSeries = DoubleTimeSeries.getDoubleTimeSeries(connection,
                            otherFlightId, "Latitude");
                    DoubleTimeSeries otherLongitudeSeries = DoubleTimeSeries.getDoubleTimeSeries(connection,
                            otherFlightId, "Longitude");
                    DoubleTimeSeries otherAltMSLSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, otherFlightId,
                            "AltMSL");
                    DoubleTimeSeries otherAltAglSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, otherFlightId,
                            "AltAGL");
                    DoubleTimeSeries otherIndicatedAirspeedSeries = DoubleTimeSeries.getDoubleTimeSeries(connection,
                            otherFlightId, "IAS");

                    double[] latitude = latitudeSeries.innerArray();
                    double[] longitude = longitudeSeries.innerArray();
                    double[] altMSL = altMSLSeries.innerArray();
                    double[] altAGL = altAglSeries.innerArray();
                    double[] indicatedAirSpeed = indicatedAirspeedSeries.innerArray();

                    double[] otherLatitude = otherLatitudeSeries.innerArray();
                    double[] otherLongitude = otherLongitudeSeries.innerArray();
                    double[] otherAltMSL = otherAltMSLSeries.innerArray();
                    double[] otherAltAgl = otherAltAglSeries.innerArray();
                    double[] otherIndicatedAirSpeed = otherIndicatedAirspeedSeries.innerArray();

                    double lateralDistance = CalculateProximity.calculateLateralDistance(latitude[startLine],
                            longitude[startLine], otherLatitude[otherStartLine], otherLongitude[otherEndLine]);
                    double verticalDistance = CalculateProximity.calculateVerticalDistance(altMSL[startLine],
                            otherAltMSL[otherStartLine]);

                    startLine++;
                    otherStartLine++;

                    while (startLine < endLine && otherStartLine < otherEndLine) {

                        double distance = CalculateProximity.calculateDistance(latitude[startLine],
                                longitude[startLine], otherLatitude[otherStartLine], otherLongitude[otherStartLine],
                                altMSL[startLine], otherAltMSL[otherStartLine]);

                        if (distance < 1000.0 && indicatedAirSpeed[startLine] > 20
                                && otherIndicatedAirSpeed[otherStartLine] > 20 && altAGL[startLine] >= 50
                                && otherAltAgl[otherStartLine] >= 50) {

                            double lateralDistanceFt = CalculateProximity.calculateLateralDistance(latitude[startLine],
                                    longitude[startLine], otherLatitude[otherStartLine],
                                    otherLongitude[otherStartLine]);
                            double verticalDistanceFt = CalculateProximity.calculateVerticalDistance(altMSL[startLine],
                                    otherAltMSL[otherStartLine]);
                            /*
                             * System.out.println(
                             * "-------------------------------------------------------------");
                             * System.out.println(" Event Id : " + eventId +" Flight Id : " + flightId +
                             * " otherflightid : " + otherFlightId +
                             * " Lateral Distance : " + lateralDistanceFt + " Vertical Distance : " +
                             * verticalDistanceFt + " Severity : " + severity);
                             * System.out.println("Flight Altitude : " + altMSL[startLine] +
                             * " Other Flight Altitude : " + otherAltMSL[otherStartLine]);
                             * System.out.println(
                             * "-------------------------------------------------------------");
                             */

                            lateralDistance = Math.min(lateralDistance, lateralDistanceFt);
                            verticalDistance = Math.min(verticalDistance, verticalDistanceFt);

                        }
                        startLine++;
                        otherStartLine++;

                    }

                    EventMetaData lateralDistanceMetaData = new EventMetaData(EventMetaData.EventMetaDataKey.LATERAL_DISTANCE, lateralDistance);
                    EventMetaData verticalDistanceMetaData = new EventMetaData(EventMetaData.EventMetaDataKey.VERTICAL_DISTANCE, verticalDistance);

                    lateralDistanceMetaData.updateDatabase(connection, eventId);
                    verticalDistanceMetaData.updateDatabase(connection, eventId);
                }

                otherResultSet.close();
                otherStatement.close();
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
