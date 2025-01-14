package org.ngafid.flights;

import org.ngafid.Database;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import java.sql.Connection;
import java.sql.SQLException;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.IOException;

import org.ngafid.WebServer;
import org.ngafid.events.Event;

import static org.ngafid.flights.XPlaneParameters.*;

/**
 * A Class that creates X-Plane FDR files for X-Plane
 *
 * @author <a href = apl1341@cs.rit.edu>Aidan LaBella @ RIT CS</a>
 */

public abstract class XPlaneExport {
    protected String aircraftPath;
    protected StringWriter dataOut;
    protected Flight flight;
    protected Map<String, DoubleTimeSeries> parameters;

    private boolean useMSL;
    private String startDateTime;

    /**
     * Defualt constructor for X-Plane exports
     *
     * @param flightId     the flightId to create the export for
     * @param aircraftPath the path of the aircafts ACF file in XPlane
     * @param useMSL       indicates whether or not to use MSL data to create the
     *                     export
     */
    public XPlaneExport(int flightId, String aircraftPath, boolean useMSL) {
        try (Connection connection = Database.getConnection()) {
            this.aircraftPath = aircraftPath + ",";
            this.useMSL = useMSL;
            this.flight = Flight.getFlight(connection, flightId);
            this.parameters = getSeriesData(connection, flightId, useMSL);
            this.startDateTime = flight.getStartDateTime();
            this.dataOut = this.export();
        } catch (SQLException | IOException e) {
            this.dataOut = new StringWriter();
            this.dataOut.write(e.toString());
        }
    }

    /**
     * Creates a hashmap with references to {@link DoubleTimeSeries} instances
     * for a given flight parameter
     *
     * @param connection the connection to the database
     * @param flightId   the flight for which to retrieve data for
     * @param useMSL     indicates whether or not to use MSL data to create the
     *                   export
     *
     * @return a Map with the pertinent data
     */
    public static Map<String, DoubleTimeSeries> getSeriesData(Connection connection, int flightId, boolean useMSL)
            throws SQLException, IOException {
        Map<String, DoubleTimeSeries> seriesData = new HashMap<>();

        seriesData.put(ALT, DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, (useMSL ? "AltMSL" : "AltAGL")));
        seriesData.put(LATITUDE, DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "Latitude"));
        seriesData.put(LONGITUDE, DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "Longitude"));
        seriesData.put(HEADING, DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "HDG"));
        seriesData.put(PITCH, DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "Pitch"));
        seriesData.put(ROLL, DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "Roll"));
        seriesData.put(IAS, DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "IAS"));
        seriesData.put(E1_RPM, DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "E1 RPM"));
        seriesData.put(E1_EGT, DoubleTimeSeries.getDoubleTimeSeries(connection, flightId, "E1 EGT2"));

        return seriesData;
    }

    /**
     * Converts the time in the database to a time X-Plane will recognize
     * i.e. HH:MM:SS.MS --> HH:MM:SS
     *
     * @return a String with the new formatted time
     */
    private String getTime() {
        System.out.println("FOUND TIME:");
        System.out.println(this.startDateTime);

        String[] timeStrings = this.startDateTime.split(" ");

        String zuluTime = (timeStrings[1].split("\\."))[0];

        System.out.println(zuluTime);
        return zuluTime;
    }

    /**
     * Converts the date in the database to a date X-Plane will recognize
     * i.e. YYYY-MM-DD --> MM/DD/YY
     *
     * @return a String with the new formatted date
     */
    private String getDate() {
        String[] timeStrings = this.startDateTime.split(" ");

        String[] dateParts = timeStrings[0].split("-");

        String year = dateParts[0].substring(2, 4);
        String month = dateParts[1];
        String day = dateParts[2];

        String date = month + "/" + day + "/" + year;

        return date;

    }

    /**
     * Creates a GPS calibration string
     *
     * @return a string in the appropriate X-Plane format
     */
    private String getGPSCalibration() {
        DoubleTimeSeries latitude = parameters.get(LATITUDE);
        DoubleTimeSeries longitude = parameters.get(LONGITUDE);
        DoubleTimeSeries altDTS = parameters.get(ALT);

        for (int i = 0; i < altDTS.size(); i++) {
            double lat = latitude.get(i);
            double lon = longitude.get(i);
            double alt = altDTS.get(i);

            if (!Double.isNaN(lat) && !Double.isNaN(lon) && !Double.isNaN(alt)) {
                return lon + "," + lat + "," + alt;
            }
        }

        System.err.println(
                "couldn't place a calibration header in the export for fiight " + this.flight.toString() + "!");
        return "";
    }

    /**
     * Writes the events to the output file
     *
     * @param scopes the {@link Map} to place the events into
     */
    private void writeEvents(Map<String, Object> scopes) {
        try (Connection connection = Database.getConnection()) {
            ArrayList<Event> events = Event.getAll(connection, this.flight.getId());

            for (Event e : events) {
                System.out.println(e.toString());
                for (int i = e.getStartLine(); i <= e.getEndLine(); i++) {
                    scopes.put(EVNT, EVNT.toUpperCase() + "," + i + ",");
                }
            }
        } catch (Exception se) {
            se.printStackTrace();
        }
    }

    /**
     * Creates an export using MustacheFactory
     *
     * @return an instance of a {@link StringWriter} that contains the export in the
     *         respective *.fdr format
     */
    private StringWriter export() {
        HashMap<String, Object> scopes = new HashMap<String, Object>();

        scopes.put(ENDL, POSIX_ENDL);
        scopes.put(TAIL, TAIL.toUpperCase() + "," + flight.getTailNumber() + ",");
        scopes.put(ACFT, ACFT.toUpperCase() + "," + this.aircraftPath);
        scopes.put(TIME, TIME.toUpperCase() + "," + this.getTime() + ",");
        scopes.put(DATE, DATE.toUpperCase() + "," + this.getDate() + ",");
        // This causes more problems
        // scopes.put(CALI, CALI.toUpperCase() + "," + this.getGPSCalibration() + ",");

        this.writeEvents(scopes);

        StringBuffer sb = new StringBuffer();
        this.writeFlightData(sb, scopes);

        String templateFile = WebServer.MUSTACHE_TEMPLATE_DIR + "template.fdr";

        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache mustache = mf.compile(templateFile);

        scopes.put(DATA, sb.toString());
        scopes.put(COMM, COMM.toUpperCase() + ",Flight " + flight.getId() + ",");

        StringWriter stringOut = new StringWriter();

        try {
            mustache.execute(new PrintWriter(stringOut), scopes).flush();
        } catch (IOException e) {
            stringOut.write(e.toString());
        }

        return stringOut;
    }

    /**
     * Returns a string full of 0's, follwed by a comma, to poplulate the extra as
     * null in the FDR format.
     * If we start tracking other data, we can change this method to include such
     * data
     *
     * @param nZeros the number of 0's
     *
     * @return a {@link String} in the format 0(0),0(1),...0(n),
     */
    protected String getZeros(int nZeros) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nZeros; i++) {
            sb.append(NULL_DATA);
        }
        return sb.toString();
    }

    /**
     * Fills the data block with the flight data for the version of X-Plane
     * requested
     *
     * @param buffer the {@link StringBuffer} to write to
     * @param scopes the scopes used by {@link MustacheFactory}
     */
    protected abstract void writeFlightData(StringBuffer buffer, Map<String, Object> scopes);

    /**
     * Returns the file as a String
     *
     * @return a {@link String} instance of the data
     */
    public String toFdrFile() {
        return dataOut.toString();
    }

    /**
     * Determines if this export uses MSL instead of AGL
     *
     * @return true if MSL is used, false if AGL is used
     */
    protected boolean usesMSL() {
        return this.useMSL;
    }
}
