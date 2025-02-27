package org.ngafid.uploads.process.format;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import org.ngafid.common.MD5;
import org.ngafid.common.TimeUtils;
import org.ngafid.flights.Airframes;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.Parameters;
import org.ngafid.flights.StringTimeSeries;
import org.ngafid.uploads.process.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;

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
        } catch (SQLException | MalformedFlightFileException | IOException | FatalFlightFileException e) {
            e.printStackTrace();
            throw new FlightProcessingException(e);
        }

        return Stream.of(new FlightBuilder(flightMeta, doubleTimeSeries, stringTimeSeries));
    }

    /**
     * Taken directly from here:
     * https://stackoverflow.com/questions/46487403/java-8-date-and-time-parse-iso-8601-string-without-colon-in-offset
     */
    private static final DateTimeFormatter PARROT_DATE_TIME_FORMAT = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(ISO_LOCAL_DATE)
            .appendLiteral('T')
            .appendPattern("HHmmss")
            .optionalStart().appendOffset("+HH:MM", "+00:00").optionalEnd()
            .optionalStart().appendOffset("+HHMM", "+0000").optionalEnd()
            .optionalStart().appendOffset("+HH", "Z").optionalEnd()
            .toFormatter();

    private void processTimeSeries(FlightMeta flightMeta, Map<String, DoubleTimeSeries> doubleTimeSeries,
                                   Map<String, StringTimeSeries> stringTimeSeries) throws SQLException, MalformedFlightFileException,
            IOException, FatalFlightFileException {
        Gson gson = new Gson();
        JsonReader reader = new JsonReader(new InputStreamReader(super.stream));
        Map jsonMap = gson.fromJson(reader, Map.class);

        OffsetDateTime parsedDate;
        try {
            parsedDate = OffsetDateTime.parse((String) jsonMap.get("date"), PARROT_DATE_TIME_FORMAT);
        } catch (Exception e) {
            throw new MalformedFlightFileException("Could not parse date from JSON file: " + e.getMessage());
        }

        ArrayList<String> headers = (ArrayList<String>) jsonMap.get("details_headers");
        ArrayList<ArrayList<?>> lines = (ArrayList<ArrayList<?>>) jsonMap.get("details_data");
        int len = headers.size();

        DoubleTimeSeries lat = new DoubleTimeSeries(Parameters.LATITUDE, Parameters.Unit.DEGREES.toString(), len);
        DoubleTimeSeries lon = new DoubleTimeSeries(Parameters.LONGITUDE, Parameters.Unit.DEGREES.toString(), len);
        DoubleTimeSeries agl = new DoubleTimeSeries(Parameters.ALT_AGL, Parameters.Unit.FT.toString(), len);
        DoubleTimeSeries spd = new DoubleTimeSeries(Parameters.GND_SPD, Parameters.Unit.KNOTS.toString(), len);
        DoubleTimeSeries unix = new DoubleTimeSeries(Parameters.UNIX_TIME_SECONDS, Parameters.Unit.SECONDS.toString(), len);

        StringTimeSeries utc = new StringTimeSeries(Parameters.UTC_DATE_TIME, Parameters.Unit.UTC_DATE_TIME.toString(), len);

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
            parsedDate = parsedDate.plusNanos((long) milliseconds * 1000000);
            utc.add(parsedDate.format(TimeUtils.ISO_8601_FORMAT));
            unix.add(parsedDate.toEpochSecond());

            if ((double) line.get(latIndex) > 90 || (double) line.get(latIndex) < -90) {
                lat.add(Double.NaN);
            } else {
                lat.add((Double) line.get(latIndex));
            }

            if ((double) line.get(lonIndex) > 180 || (double) line.get(lonIndex) < -180) {
                lon.add(Double.NaN);
            } else {
                lon.add((Double) line.get(lonIndex));
            }

            agl.add((Double) line.get(altIndex) * metersToFeet);
            spd.add((Double) line.get(spdIndex));
        }

        List.of(spd, lat, lon, agl, unix).forEach(series -> doubleTimeSeries.put(series.getName(), series));
        stringTimeSeries.put(utc.getName(), utc);

        flightMeta.setStartDateTime(TimeUtils.UTCtoSQL(utc.get(0)));
        flightMeta.setEndDateTime(TimeUtils.UTCtoSQL(utc.get(utc.size() - 1)));
        stream.reset();
        flightMeta.setMd5Hash(MD5.computeHexHash(stream));
        flightMeta.setSystemId((String) jsonMap.get("serial_number"));
        flightMeta.setFilename(super.filename);
        flightMeta.airframe = new Airframes.Airframe(String.valueOf(jsonMap.get("controller_model")), new Airframes.Type("UAS Rotorcraft"));
        flightMeta.setCalculated(""); // TODO: Figure this out
        flightMeta.setSuggestedTailNumber((String) jsonMap.get("serial_number"));
    }
}
