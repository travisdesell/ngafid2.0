package org.ngafid.uploads.process.format;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import org.ngafid.common.MD5;
import org.ngafid.common.TimeUtils;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.StringTimeSeries;
import org.ngafid.uploads.process.*;

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
import java.util.stream.Stream;

/**
 * This class is responsible for parsing JSON files.
 *
 * @author Aaron Chan
 */

public class JSONFileProcessor extends FlightFileProcessor {
    private static final Logger LOG = Logger.getLogger(JSONFileProcessor.class.getName());

    public JSONFileProcessor(Connection connection, InputStream stream, String filename, Pipeline pipeline) throws IOException {
        super(connection, stream, filename, pipeline);
    }

    @Override
    public Stream<FlightBuilder> parse() throws FlightProcessingException {
        FlightMeta flightMeta = new FlightMeta();
        final Map<String, DoubleTimeSeries> doubleTimeSeries = new HashMap<>();
        final Map<String, StringTimeSeries> stringTimeSeries = new HashMap<>();

        try {
            processTimeSeries(flightMeta, doubleTimeSeries, stringTimeSeries);
        } catch (SQLException | MalformedFlightFileException | IOException | FatalFlightFileException
                 | FlightAlreadyExistsException e) {
            throw new FlightProcessingException(e);
        }

        return Stream.of(new FlightBuilder(flightMeta, doubleTimeSeries, stringTimeSeries));
    }

    private void processTimeSeries(FlightMeta flightMeta, Map<String, DoubleTimeSeries> doubleTimeSeries,
                                   Map<String, StringTimeSeries> stringTimeSeries) throws SQLException, MalformedFlightFileException,
            IOException, FatalFlightFileException, FlightAlreadyExistsException {
        String status = "";
        Gson gson = new Gson();
        JsonReader reader = new JsonReader(new InputStreamReader(super.stream));
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

        DoubleTimeSeries lat = new DoubleTimeSeries("Latitude", "degrees", len);
        DoubleTimeSeries lon = new DoubleTimeSeries("Longitude", "degrees", len);
        DoubleTimeSeries agl = new DoubleTimeSeries("AltAGL", "ft", len);
        DoubleTimeSeries spd = new DoubleTimeSeries("GndSpd", "kt", len);

        ArrayList<Timestamp> timestamps = new ArrayList<>(len);
        StringTimeSeries localDateSeries = new StringTimeSeries("Lcl Date", "yyyy-mm-dd");
        StringTimeSeries localTimeSeries = new StringTimeSeries("Lcl Time", "hh:mm:ss");
        StringTimeSeries utcOfstSeries = new StringTimeSeries("UTCOfst", "hh:mm");

        SimpleDateFormat lclDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat lclTimeFormat = new SimpleDateFormat("HH:mm:ss");

        int latIndex = headers.indexOf("product_gps_latitude");
        int lonIndex = headers.indexOf("product_gps_longitude");
        int altIndex = headers.indexOf("altitude");
        int spdIndex = headers.indexOf("speed");
        int timeIndex = headers.indexOf("time");

        double timeDiff = ((double) lines.get(lines.size() - 1).get(timeIndex))
                - ((double) lines.get(0).get(timeIndex));
        if (timeDiff < 180)
            throw new FatalFlightFileException("Flight file was less than 3 minutes long, ignoring.");

        double prevSeconds = 0;
        double metersToFeet = 3.28084;

        for (ArrayList<?> line : lines) {
            double milliseconds = (double) line.get(timeIndex) - prevSeconds;
            prevSeconds = (double) line.get(timeIndex);
            parsedDate = TimeUtils.addMilliseconds(parsedDate, (int) milliseconds);

            if ((double) line.get(latIndex) > 90 || (double) line.get(latIndex) < -90) {
                status = "WARNING";
                lat.add(Double.NaN);
            } else {
                lat.add((Double) line.get(latIndex));
            }

            if ((double) line.get(lonIndex) > 180 || (double) line.get(lonIndex) < -180) {
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

        DoubleTimeSeries nspd = spd.subSeries(start, end);
        DoubleTimeSeries nlon = lon.subSeries(start, end);
        DoubleTimeSeries nlat = lat.subSeries(start, end);
        DoubleTimeSeries nagl = agl.subSeries(start, end);

        doubleTimeSeries.put("GndSpd", nspd);
        doubleTimeSeries.put("Longitude", nlon);
        doubleTimeSeries.put("Latitude", nlat);
        doubleTimeSeries.put("AltAGL", nagl); // Parrot data is stored as AGL and most likely in meters

        StringTimeSeries localDate = localDateSeries.subSeries(start, end);
        StringTimeSeries localTime = localTimeSeries.subSeries(start, end);
        StringTimeSeries offset = utcOfstSeries.subSeries(start, end);

        stringTimeSeries.put("Lcl Date", localDate);
        stringTimeSeries.put("Lcl Time", localTime);
        stringTimeSeries.put("UTCOfst", offset);


        flightMeta.setStartDateTime(localDateSeries.get(0) + " " + localTimeSeries.get(0) + " " + utcOfstSeries.get(0));
        flightMeta.setEndDateTime(localDateSeries.get(localDateSeries.size() - 1) + " "
                + localTimeSeries.get(localTimeSeries.size() - 1) + " " + utcOfstSeries.get(utcOfstSeries.size() - 1));
        flightMeta.setAirframeType("UAS Rotorcraft");
        stream.reset();
        flightMeta.setMd5Hash(MD5.computeHexHash(stream));
        flightMeta.setSystemId((String) jsonMap.get("serial_number"));
        flightMeta.setFilename(super.filename);
        flightMeta.setAirframe((String) jsonMap.get("controller_model"));
        flightMeta.setCalculated(""); // TODO: Figure this out
        flightMeta.setSuggestedTailNumber((String) jsonMap.get("serial_number"));
    }
}
