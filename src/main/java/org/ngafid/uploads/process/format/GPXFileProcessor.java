package org.ngafid.uploads.process.format;

import ch.randelshofer.fastdoubleparser.JavaDoubleParser;
import org.ngafid.common.TimeUtils;
import org.ngafid.flights.*;
import org.ngafid.uploads.process.*;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * This class is responsible for parsing GPX files.
 *
 * @author Josh Karns
 */

public class GPXFileProcessor extends FlightFileProcessor {
    private static final Logger LOG = Logger.getLogger(GPXFileProcessor.class.getName());

    public GPXFileProcessor(Connection connection, InputStream stream, String filename, Pipeline pipeline) throws IOException {
        super(connection, stream, filename, pipeline);
    }

    @Override
    public Stream<FlightBuilder> parse() throws FlightProcessingException {
        try {
            List<FlightBuilder> flights = parseFlights(filename, stream);

            return flights.stream();
        } catch (SQLException | MalformedFlightFileException | IOException | FatalFlightFileException e) {
            throw new RuntimeException(e);
        }
    }

    public List<FlightBuilder> parseFlights(String entry, InputStream stream) throws SQLException,
            MalformedFlightFileException, IOException, FatalFlightFileException {
        List<FlightBuilder> flights = new ArrayList<>();
        // BE-GPS-2200
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(stream);

            NodeList l = doc.getElementsByTagName("trkseg");
            if (l.getLength() == 0)
                throw new FatalFlightFileException("could not parse GPX data file: failed to find data node.");

            if (l.getLength() != 1)
                throw new FatalFlightFileException("could not parse GPX data file: found multiple data nodes.");

            Node dataNode = l.item(0);
            int len = dataNode.getChildNodes().getLength();

            DoubleTimeSeries lat = new DoubleTimeSeries(Parameters.LATITUDE, Parameters.Unit.DEGREES, len);
            DoubleTimeSeries lon = new DoubleTimeSeries(Parameters.LONGITUDE, Parameters.Unit.DEGREES, len);
            DoubleTimeSeries msl = new DoubleTimeSeries(Parameters.ALT_MSL, Parameters.Unit.FT, len);
            DoubleTimeSeries spd = new DoubleTimeSeries(Parameters.GND_SPD, Parameters.Unit.KNOTS, len);
            DoubleTimeSeries unix = new DoubleTimeSeries(Parameters.UNIX_TIME_SECONDS, Parameters.Unit.SECONDS, len);
            StringTimeSeries utc = new StringTimeSeries(Parameters.UTC_DATE_TIME, Parameters.Unit.UTC_DATE_TIME);

            NodeList nicknameNodes = doc.getElementsByTagName("badelf:modelNickname");
            if (nicknameNodes.item(0) == null)
                throw new FatalFlightFileException("GPX file is missing necessary metadata (modelNickname).");
            String nickname = nicknameNodes.item(0).getTextContent();

            NodeList fdrModel = doc.getElementsByTagName("badelf:modelName");
            if (fdrModel.item(0) == null)
                throw new FatalFlightFileException("GPX file is missing necessary metadata (modelName).");
            String airframeName = fdrModel.item(0).getTextContent();
            LOG.info("Airframe name: " + airframeName);

            NodeList dates = doc.getElementsByTagName("time");
            NodeList datanodes = doc.getElementsByTagName("trkpt");
            NodeList elenodes = doc.getElementsByTagName("ele");
            NodeList spdnodes = doc.getElementsByTagName("badelf:speed");

            if (spdnodes.item(0) == null)
                throw new FatalFlightFileException("GPX file is missing GndSpd.");

            if (!(dates.getLength() == datanodes.getLength() &&
                    dates.getLength() == elenodes.getLength() &&
                    dates.getLength() == spdnodes.getLength())) {
                throw new FatalFlightFileException("Mismatching number of data tags in GPX file");
            }

            for (int i = 0; i < dates.getLength(); i++) {
                OffsetDateTime date = OffsetDateTime.parse(dates.item(i).getTextContent(), DateTimeFormatter.ISO_DATE_TIME);
                unix.add(date.toEpochSecond());
                utc.add(date.format(TimeUtils.ISO_8601_FORMAT));

                Node spdNode = spdnodes.item(i);
                // Convert m / s to knots
                spd.add(JavaDoubleParser.parseDouble(spdNode.getTextContent()) * 1.94384);

                Node eleNode = elenodes.item(i);
                // Convert meters to feet.
                msl.add(JavaDoubleParser.parseDouble(eleNode.getTextContent()) * 3.28084);

                Node d = datanodes.item(i);
                NamedNodeMap attrs = d.getAttributes();

                Node latNode = attrs.getNamedItem("lat");
                lat.add(JavaDoubleParser.parseDouble(latNode.getTextContent()));

                Node lonNode = attrs.getNamedItem("lon");
                lon.add(JavaDoubleParser.parseDouble(lonNode.getTextContent()));
            }

            int start = 0;
            for (int end = 1; end < utc.size(); end++) {
                // 1 minute delay -> new flight.
                if (unix.get(end) - unix.get(end - 1) > 60000
                        || end == utc.size() - 1) {
                    if (end == utc.size() - 1) {
                        end += 1;
                    }

                    if (end - start < 60) {
                        start = end;
                        continue;
                    }

                    final int lo = start, hi = end;
                    HashMap<String, DoubleTimeSeries> doubleSeries = new HashMap<>();
                    List.of(spd, lon, lat, msl, unix).forEach(series -> doubleSeries.put(series.getName(), series.subSeries(lo, hi)));

                    HashMap<String, StringTimeSeries> stringSeries = new HashMap<>();
                    stringSeries.put(utc.getName(), utc.subSeries(start, end));

                    FlightMeta meta = new FlightMeta();
                    meta.setFilename(this.filename + ":" + start + "-" + end);
                    meta.airframe = new Airframes.Airframe(airframeName, new Airframes.Type("Fixed Wing"));
                    meta.flightDataRecorder = new FlightDataRecorder("Bad Elf 2200");
                    meta.setSuggestedTailNumber(nickname);
                    meta.setSystemId(nickname);

                    flights.add(new FlightBuilder(meta, doubleSeries, stringSeries));
                    start = end;
                }
            }

        } catch (ParserConfigurationException | SAXException e) {
            throw new FatalFlightFileException("Could not parse GPX data file: " + e.getMessage());
        }

        return flights;
    }
}
