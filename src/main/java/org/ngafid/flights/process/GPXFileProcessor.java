package org.ngafid.flights.process;

import org.ngafid.flights.*;
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
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Stream;

import ch.randelshofer.fastdoubleparser.JavaDoubleParser;

/**
 * This class is responsible for parsing GPX files.
 *
 * @author Josh Karns
 */

public class GPXFileProcessor extends FlightFileProcessor {
    private static final Logger LOG = Logger.getLogger(GPXFileProcessor.class.getName());

    public GPXFileProcessor(Connection connection, InputStream stream, String filename) {
        super(connection, stream, filename);
    }

    @Override
    public Stream<FlightBuilder> parse() throws FlightProcessingException {
        try {
            List<FlightBuilder> flights = parseFlights(filename, stream);

            return flights.stream();
        } catch (SQLException | MalformedFlightFileException | IOException | FatalFlightFileException |
                 FlightAlreadyExistsException e) {
            throw new RuntimeException(e);
        }
    }

    public List<FlightBuilder> parseFlights(String entry, InputStream stream) throws SQLException, MalformedFlightFileException, IOException, FatalFlightFileException, FlightAlreadyExistsException {
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

            DoubleTimeSeries lat = new DoubleTimeSeries("Latitude", "degrees", len);
            DoubleTimeSeries lon = new DoubleTimeSeries("Longitude", "degrees", len);
            DoubleTimeSeries msl = new DoubleTimeSeries("AltMSL", "ft", len);
            DoubleTimeSeries spd = new DoubleTimeSeries("GndSpd", "kt", len);
            ArrayList<Timestamp> timestamps = new ArrayList<Timestamp>(len);
            StringTimeSeries localDateSeries = new StringTimeSeries("Lcl Date", "yyyy-mm-dd");
            StringTimeSeries localTimeSeries = new StringTimeSeries("Lcl Time", "hh:mm:ss");
            StringTimeSeries utcOfstSeries = new StringTimeSeries("UTCOfst", "hh:mm");
            // ss.SSSSSSXXX
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

            SimpleDateFormat lclDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat lclTimeFormat = new SimpleDateFormat("HH:mm:ss");

            // NodeList serialNumberNodes = doc.getElementsByTagName("badelf:modelSerialNumber");
            // String serialNumber = serialNumberNodes.item(0).getTextContent();
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
                Date parsedDate = dateFormat.parse(dates.item(i).getTextContent());
                timestamps.add(new Timestamp(parsedDate.getTime()));
                Calendar cal = new Calendar.Builder().setInstant(parsedDate).build();

                int offsetMS = cal.getTimeZone().getOffset(parsedDate.getTime());
                String sign = offsetMS < 0 ? "-" : "+";
                offsetMS = offsetMS < 0 ? -offsetMS : offsetMS;

                int offsetSEC = offsetMS / 1000;
                int offsetMIN = offsetSEC / 60;
                int offsetHRS = offsetMIN / 60;
                offsetMIN %= 60;

                String offsetHrsStr = (offsetHRS < 10 ? "0" : "") + offsetHRS;
                String offsetMinStr = (offsetMIN < 10 ? "0" : "") + offsetMIN;
                // This should look like +HH:mm
                utcOfstSeries.add(sign + offsetHrsStr + ":" + offsetMinStr);

                localDateSeries.add(lclDateFormat.format(parsedDate));
                localTimeSeries.add(lclTimeFormat.format(parsedDate));

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
            for (int end = 1; end < timestamps.size(); end++) {
                // 1 minute delay -> new flight.
                if (timestamps.get(end).getTime() - timestamps.get(end - 1).getTime() > 60000
                        || end == localTimeSeries.size() - 1) {
                    if (end == localTimeSeries.size() - 1) {
                        end += 1;
                    }

                    if (end - start < 60) {
                        start = end;
                        continue;
                    }

                    StringTimeSeries localTime = localTimeSeries.subSeries(start, end);
                    StringTimeSeries localDate = localDateSeries.subSeries(start, end);
                    StringTimeSeries offset = utcOfstSeries.subSeries(start, end);
                    DoubleTimeSeries nlat = lat.subSeries(start, end);
                    DoubleTimeSeries nlon = lon.subSeries(start, end);
                    DoubleTimeSeries nmsl = msl.subSeries(start, end);
                    DoubleTimeSeries nspd = spd.subSeries(start, end);


                    HashMap<String, DoubleTimeSeries> doubleSeries = new HashMap<>();
                    doubleSeries.put("GndSpd", nspd);
                    doubleSeries.put("Longitude", nlon);
                    doubleSeries.put("Latitude", nlat);
                    doubleSeries.put("AltMSL", nmsl);

                    HashMap<String, StringTimeSeries> stringSeries = new HashMap<>();
                    stringSeries.put("Lcl Date", localDate);
                    stringSeries.put("Lcl Time", localTime);
                    stringSeries.put("UTCOfst", offset);

                    FlightMeta meta = new FlightMeta();
                    meta.setFilename(this.filename + ":" + start + "-" + end);
                    meta.setAirframeName(airframeName);
                    meta.setSuggestedTailNumber(nickname);
                    meta.setSystemId(nickname);
                    meta.setAirframeType("Fixed Wing");

                    flights.add(new FlightBuilder(meta, doubleSeries, stringSeries));
                    start = end;
                }
            }

        } catch (ParserConfigurationException | SAXException | ParseException e) {
            throw new FatalFlightFileException("Could not parse GPX data file: " + e.getMessage());
        }

        return flights;
    }
}
