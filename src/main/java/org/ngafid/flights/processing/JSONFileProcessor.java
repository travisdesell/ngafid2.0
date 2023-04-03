package org.ngafid.flights.processing;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import org.ngafid.common.TimeUtils;
import org.ngafid.flights.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class JSONFileProcessor implements FileProcessor {
    private static final Logger LOG = Logger.getLogger(JSONFileProcessor.class.getName());

    @Override
    public Flight process(int fleetId, String entry, InputStream stream, Connection connection) throws SQLException, MalformedFlightFileException, IOException, FatalFlightFileException, FlightAlreadyExistsException {
        String status = "";
        Gson gson = new Gson();
        JsonReader reader = new JsonReader(new InputStreamReader(stream));
        Map jsonMap = gson.fromJson(reader, Map.class);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HHmmssZ");

        Date parsedDate;
        try {
          parsedDate = dateFormat.parse((String) jsonMap.get("date"));
        } catch (Exception e) {
          throw new MalformedFlightFileException("Could not parse date from JSON file: " + e.getMessage());
        }

        int timezoneOffset = parsedDate.getTimezoneOffset() / 60;
        String timezoneOffsetString = (timezoneOffset >= 0 ? "+" : "-") + String.format("%02d:00", timezoneOffset);

        ArrayList<String> headers = (ArrayList<String>) jsonMap.get("details_headers");
        ArrayList<ArrayList<?>> lines = (ArrayList<ArrayList<?>>) jsonMap.get("details_data");
        int len = headers.size();

        DoubleTimeSeries lat = new DoubleTimeSeries(connection, "Latitude", "degrees", len);
        DoubleTimeSeries lon = new DoubleTimeSeries(connection, "Longitude", "degrees", len);
        DoubleTimeSeries agl = new DoubleTimeSeries(connection, "AltAGL", "ft", len);
        DoubleTimeSeries spd = new DoubleTimeSeries(connection, "GndSpd", "kt", len);

        ArrayList<Timestamp> timestamps = new ArrayList<>(len);
        StringTimeSeries localDateSeries = new StringTimeSeries(connection, "Lcl Date", "yyyy-mm-dd");
        StringTimeSeries localTimeSeries = new StringTimeSeries(connection, "Lcl Time", "hh:mm:ss");
        StringTimeSeries utcOfstSeries = new StringTimeSeries(connection, "UTCOfst", "hh:mm");

        SimpleDateFormat lclDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat lclTimeFormat = new SimpleDateFormat("HH:mm:ss");

        int latIndex = headers.indexOf("product_gps_latitude");
        int lonIndex = headers.indexOf("product_gps_longitude");
        int altIndex = headers.indexOf("altitude");
        int spdIndex = headers.indexOf("speed");
        int timeIndex = headers.indexOf("time");

        double timeDiff = ((double) lines.get(lines.size() - 1).get(timeIndex)) - ((double) lines.get(0).get(timeIndex));
        if (timeDiff < 180) throw new FatalFlightFileException("Flight file was less than 3 minutes long, ignoring.");

        double prevSeconds = 0;
        double metersToFeet = 3.28084;

        for (ArrayList<?> line : lines) {
            double milliseconds = (double) line.get(timeIndex) - prevSeconds;
            prevSeconds = (double) line.get(timeIndex);
            parsedDate = TimeUtils.addMilliseconds(parsedDate, (int) milliseconds);

            if ((double) line.get(latIndex) > 90 || (double) line.get(latIndex) < -90) {
                LOG.severe("Invalid latitude: " + line.get(latIndex));
                status = "WARNING";
                lat.add(Double.NaN);
            } else {
                lat.add((Double) line.get(latIndex));
            }

            if((double) line.get(lonIndex) > 180 || (double) line.get(lonIndex) < -180) {
                LOG.severe("Invalid longitude: " + line.get(lonIndex));
                status = "WARNING";
                lon.add(Double.NaN);
            } else {
                lon.add((Double) line.get(lonIndex));
            }

            agl.add((Double) line.get(altIndex) * metersToFeet);
            spd.add((Double) line.get(spdIndex));

            localDateSeries.add(lclDateFormat.format(parsedDate));
            localTimeSeries.add(lclTimeFormat.format(parsedDate));
            utcOfstSeries.add(timezoneOffsetString);
            timestamps.add(new Timestamp(parsedDate.getTime()));
        }

        int start = 0;
        int end = timestamps.size() - 1;

        DoubleTimeSeries nspd = spd.subSeries(connection, start, end);
        DoubleTimeSeries nlon = lon.subSeries(connection, start, end);
        DoubleTimeSeries nlat = lat.subSeries(connection, start, end);
        DoubleTimeSeries nagl = agl.subSeries(connection, start, end);

        HashMap<String, DoubleTimeSeries> doubleSeries = new HashMap<>();
        doubleSeries.put("GndSpd", nspd);
        doubleSeries.put("Longitude", nlon);
        doubleSeries.put("Latitude", nlat);
        doubleSeries.put("AltAGL", nagl); // Parrot data is stored as AGL and most likely in meters

        StringTimeSeries localDate = localDateSeries.subSeries(connection, start, end);
        StringTimeSeries localTime = localTimeSeries.subSeries(connection, start, end);
        StringTimeSeries offset = utcOfstSeries.subSeries(connection, start, end);

        HashMap<String, StringTimeSeries> stringSeries = new HashMap<>();
        stringSeries.put("Lcl Date", localDate);
        stringSeries.put("Lcl Time", localTime);
        stringSeries.put("UTCOfst", offset);

        Flight flight = new Flight(fleetId, entry, (String) jsonMap.get("serial_number"), (String) jsonMap.get("controller_model"), doubleSeries, stringSeries, connection);
        flight.status = status;
        flight.airframeType = "UAS Rotorcraft";
        flight.airframeTypeId = 4;

//        try {
//            flight.calculateAGL(connection, "AltAGL", "AltMSL", "Latitude", "Longitude");
//        } catch (MalformedFlightFileException e) {
//            flight.exceptions.add(e);
//        }

        return flight;
    }
}
