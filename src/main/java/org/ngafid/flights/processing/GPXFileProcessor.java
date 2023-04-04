package org.ngafid.flights.processing;

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

public class GPXFileProcessor implements FileProcessor {
    private static final Logger LOG = Logger.getLogger(GPXFileProcessor.class.getName());

    @Override
    public Flight process(int fleetId, String entry, InputStream stream, Connection connection) throws SQLException, MalformedFlightFileException, IOException, FatalFlightFileException, FlightAlreadyExistsException {
        return null;
    }

    @Override // TODO: Break this method down into smaller methods
    public boolean process(int fleetId, String entry, InputStream stream, Connection connection, List<Flight> flights) throws SQLException, MalformedFlightFileException, IOException, FatalFlightFileException, FlightAlreadyExistsException {
        // BE-GPS-2200
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db;
        try {
            db = dbf.newDocumentBuilder();
            Document doc = db.parse(stream);

        NodeList l = doc.getElementsByTagName("trkseg");
        if (l.getLength() == 0)
            throw new FatalFlightFileException("could not parse GPX data file: failed to find data node.");

        if (l.getLength() != 1)
            throw new FatalFlightFileException("could not parse GPX data file: found multiple data nodes.");

        Node dataNode = l.item(0);
        int len = dataNode.getChildNodes().getLength();

        DoubleTimeSeries lat = new DoubleTimeSeries(connection, "Latitude", "degrees", len);
        DoubleTimeSeries lon = new DoubleTimeSeries(connection, "Longitude", "degrees", len);
        DoubleTimeSeries msl = new DoubleTimeSeries(connection, "AltMSL", "ft", len);
        DoubleTimeSeries spd = new DoubleTimeSeries(connection, "GndSpd", "kt", len);
        ArrayList<Timestamp> timestamps = new ArrayList<Timestamp>(len);
        StringTimeSeries localDateSeries = new StringTimeSeries(connection, "Lcl Date", "yyyy-mm-dd");
        StringTimeSeries localTimeSeries = new StringTimeSeries(connection, "Lcl Time", "hh:mm:ss");
        StringTimeSeries utcOfstSeries = new StringTimeSeries(connection, "UTCOfst", "hh:mm");
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
            spd.add(Double.parseDouble(spdNode.getTextContent()) * 1.94384);

            Node eleNode = elenodes.item(i);
            // Convert meters to feet.
            msl.add(Double.parseDouble(eleNode.getTextContent()) * 3.28084);

            Node d = datanodes.item(i);
            NamedNodeMap attrs = d.getAttributes();

            Node latNode = attrs.getNamedItem("lat");
            lat.add(Double.parseDouble(latNode.getTextContent()));

            Node lonNode = attrs.getNamedItem("lon");
            lon.add(Double.parseDouble(lonNode.getTextContent()));
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

                StringTimeSeries localTime = localTimeSeries.subSeries(connection, start, end);
                StringTimeSeries localDate = localDateSeries.subSeries(connection, start, end);
                StringTimeSeries offset = utcOfstSeries.subSeries(connection, start, end);
                DoubleTimeSeries nlat = lat.subSeries(connection, start, end);
                DoubleTimeSeries nlon = lon.subSeries(connection, start, end);
                DoubleTimeSeries nmsl = msl.subSeries(connection, start, end);
                DoubleTimeSeries nspd = spd.subSeries(connection, start, end);


                HashMap<String, DoubleTimeSeries> doubleSeries = new HashMap<>();
                doubleSeries.put("GndSpd", nspd);
                doubleSeries.put("Longitude", nlon);
                doubleSeries.put("Latitude", nlat);
                doubleSeries.put("AltMSL", nmsl);

                HashMap<String, StringTimeSeries> stringSeries = new HashMap<>();
                stringSeries.put("Lcl Date", localDate);
                stringSeries.put("Lcl Time", localTime);
                stringSeries.put("UTCOfst", offset);

                flights.add(new Flight(fleetId, entry + "-" + start + "-" + end, nickname, airframeName, doubleSeries, stringSeries, connection));
                start = end;
            }
        }

        } catch (ParserConfigurationException | SAXException | ParseException e) {
            throw new FatalFlightFileException("Could not parse GPX data file: " + e.getMessage());
        }

        return true;
    }
}
