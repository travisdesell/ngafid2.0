package org.ngafid;

import org.ngafid.airports.Airports;
import org.ngafid.common.TimeUtils;
import org.ngafid.events.RateOfClosure;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.StringTimeSeries;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CalculateRateOfClosure {

    protected static class FlightTimeLocation {

        int flightId;
        int fleetId;
        long[] epochTime;
        double[] altitudeMSL;
        double[] altitudeAGL;
        double[] latitude;
        double[] longitude;
        StringTimeSeries dateSeries;
        StringTimeSeries timeSeries;
        int startLine;
        int endLine;
        double[] indicatedAirspeed;


        public FlightTimeLocation(Connection connection, int fleetId, int flightId,int startLine, int endLine) throws SQLException {
            this.fleetId = fleetId;
            this.flightId = flightId;
            this.startLine = startLine;
            this.endLine = endLine;
            this.epochTime = getEpochTime(connection);

        }

        public long[] getEpochTime(Connection connection) throws SQLException {
            //get the time series data for altitude, latitude and longitude
            DoubleTimeSeries altMSLSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "AltMSL");
            DoubleTimeSeries altAGLSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "AltAGL");
            DoubleTimeSeries latitudeSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "Latitude");
            DoubleTimeSeries longitudeSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "Longitude");
            DoubleTimeSeries indicatedAirspeedSeries = DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "IAS");

            altitudeMSL = altMSLSeries.innerArray();
            altitudeAGL = altAGLSeries.innerArray();
            latitude = latitudeSeries.innerArray();
            longitude = longitudeSeries.innerArray();
            indicatedAirspeed = indicatedAirspeedSeries.innerArray();

            //calculate the epoch time for each row as longs so they can most be quickly compared
            //we need to keep track of the date and time series for inserting in the event info
            dateSeries = StringTimeSeries.getStringTimeSeries(connection, flightId, "Lcl Date");
            timeSeries = StringTimeSeries.getStringTimeSeries(connection, flightId, "Lcl Time");
            StringTimeSeries utcOffsetSeries = StringTimeSeries.getStringTimeSeries(connection, flightId, "UTCOfst");
            //check to see if we could get these columns
            //System.out.println("date length: " + dateSeries.size() + ", time length: " + timeSeries.size() + ", utc length: " + utcOffsetSeries.size());
            int length = dateSeries.size();

            long[] epochTime = new long[length];
            for (int i = 0; i < length; i++) {
                if (dateSeries.get(i) == null || dateSeries.get(i).equals("")
                        || timeSeries.get(i) == null || timeSeries.get(i).equals("")
                        || utcOffsetSeries.get(i) == null || utcOffsetSeries.get(i).equals("")) {
                    epochTime[i] = 0;
                    continue;
                }
                epochTime[i] = TimeUtils.toEpochSecond(dateSeries.get(i), timeSeries.get(i), utcOffsetSeries.get(i));
            }
            return epochTime;
        }

    }
    public static double calculateDistance(double flightLatitude, double flightLongitude, double otherFlightLatitude,
                                           double otherFlightLongitude, double flightAltitude, double otherFlightAltitude) {

        double distanceFt = Airports.calculateDistanceInFeet(flightLatitude, flightLongitude, otherFlightLatitude, otherFlightLongitude);
        double altDiff = Math.abs(flightAltitude - otherFlightAltitude);
        distanceFt = Math.sqrt((distanceFt * distanceFt) + (altDiff * altDiff));
        return distanceFt;
    }
    public static double[] calculateRateOfClosure(FlightTimeLocation flightInfo, FlightTimeLocation otherInfo ) {


        int startLine = flightInfo.startLine;
        int endLine = flightInfo.endLine;
        int otherStartLine = otherInfo.startLine;
        int otherEndLine = otherInfo.endLine;
        startLine = (startLine - 6) > 0 ? (startLine - 5) : 0;
        endLine = (endLine + 5) < flightInfo.epochTime.length ? (endLine + 5) : endLine;
        otherStartLine = (otherStartLine - 6) > 0 ? (otherStartLine - 5) : 0;
        otherEndLine = (otherEndLine + 5) < otherInfo.epochTime.length ? (otherEndLine + 5) : otherEndLine;
        double previousDistance = 0;
        if (startLine > 0 && otherStartLine > 0) {
            previousDistance = calculateDistance(flightInfo.latitude[startLine-1], flightInfo.longitude[startLine-1],
                    otherInfo.latitude[otherStartLine-1], otherInfo.longitude[otherStartLine-1],
                    flightInfo.altitudeMSL[startLine-1], otherInfo.altitudeMSL[otherStartLine-1]);
        }
        else {
            previousDistance = calculateDistance(flightInfo.latitude[startLine], flightInfo.longitude[startLine],
                    otherInfo.latitude[otherStartLine], otherInfo.longitude[otherStartLine],
                    flightInfo.altitudeMSL[startLine], otherInfo.altitudeMSL[otherStartLine]);
        }

        double rateOfClosure[] = new double[endLine - startLine];
        int i = startLine, j = otherStartLine, index = 0;

        while (i < endLine && j < otherEndLine) {
            if (flightInfo.epochTime[i] == 0) {
                i++;
                continue;
            }
            if (otherInfo.epochTime[j] == 0) {
                j++;
                continue;
            }
            //make sure both iterators are for the same time
            if (flightInfo.epochTime[i] < otherInfo.epochTime[j]) {
                i++;
                continue;
            }
            if (otherInfo.epochTime[j] < flightInfo.epochTime[i]) {
                j++;
                continue;
            }
            double currentDistance = calculateDistance(flightInfo.latitude[i], flightInfo.longitude[i],
                    otherInfo.latitude[j], otherInfo.longitude[j], flightInfo.altitudeMSL[i], otherInfo.altitudeMSL[j]);
            rateOfClosure[index] = previousDistance - currentDistance;
            previousDistance = currentDistance;
            i++;
            j++;
            index++;
        }
        return rateOfClosure;
    }


    public static void main(String[] args) {
        try {
            Connection connection = Database.getConnection();
            String queryString = "select  e.flight_id as flight_id, e.other_flight_id as otherFlightId, e.severity as severity ,e.fleet_id as fleet_id," +
                    " e.start_line as startLine, e.end_line as endLine, e.id as eventId from events as e " +
                    " where e.event_definition_id = -1 and severity > 0 and e.id not in (select event_id from rate_of_closure)";
            PreparedStatement query = connection.prepareStatement(queryString);
            ResultSet resultSet = query.executeQuery();
            while (resultSet.next()) {

                int flightId = resultSet.getInt(1);
                int otherFlightId = resultSet.getInt(2);
                double severity = resultSet.getDouble(3);
                int fleetId = resultSet.getInt(4);
                int startLine = resultSet.getInt(5);
                int endLine = resultSet.getInt(6);
                int eventId = resultSet.getInt(7);
                String otherEventQueryString = "select e.fleet_id as fleet_id,e.start_line as startLine, e.end_line as endLine, e.id as otherEventId from events as e " +
                        "where e.event_definition_id = -1 and e.severity = ? and e.flight_id = ? and e.other_flight_id = ?";
                PreparedStatement otherEventQuery = connection.prepareStatement(otherEventQueryString);
                otherEventQuery.setDouble(1, severity);
                otherEventQuery.setInt(2, otherFlightId);
                otherEventQuery.setInt(3, flightId);
                ResultSet otherResultSet = otherEventQuery.executeQuery();

                if (otherResultSet.next()) {
                    FlightTimeLocation flightInfo = new FlightTimeLocation(connection, fleetId, flightId, startLine,endLine);
                    int otherFleetId = otherResultSet.getInt(1);
                    int otherStartLine = otherResultSet.getInt(2);
                    int otherEndLine = otherResultSet.getInt(3);
                    int otherEventId = otherResultSet.getInt(4);
                    FlightTimeLocation otherFlightInfo = new FlightTimeLocation(connection, otherFleetId, otherFlightId, otherStartLine, otherEndLine);
                    double [] rateOfClosureArray = calculateRateOfClosure(flightInfo,otherFlightInfo);
                    RateOfClosure roc = new RateOfClosure(rateOfClosureArray);
                    roc.updateDatabase(connection,eventId);
                }
                otherResultSet.close();
                otherEventQuery.close();
                System.out.println("Fleet Id : " + fleetId + " flight id : " + flightId + " other flight id : " + otherFlightId );
            }
            resultSet.close();
            query.close();
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Calculating Rate of Closure for all proximity events complete.");
        System.exit(0);
    }
}