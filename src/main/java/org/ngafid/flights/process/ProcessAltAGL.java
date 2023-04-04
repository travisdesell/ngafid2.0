package org.ngafid.flights.process;

import java.util.Set;
import java.util.Collections;
import java.sql.Connection;
import java.sql.SQLException;

import java.nio.file.NoSuchFileException;

import org.ngafid.flights.Flight;
import org.ngafid.terrain.TerrainCache;
import org.ngafid.flights.DoubleTimeSeries;
import static org.ngafid.flights.Parameters.*;
import org.ngafid.flights.FatalFlightFileException;
import org.ngafid.flights.MalformedFlightFileException;

public class ProcessAltAGL extends ProcessStep {
    private static Set<String> REQUIRED_DOUBLE_COLUMNS = Set.of(ALT_MSL, LATITUDE, LONGITUDE);
    private static Set<String> OUTPUT_COLUMNS = Set.of(ALT_AGL);

    public ProcessAltAGL(Connection connection, Flight flight) {
        super(connection, flight);
    }

    public Set<String> getRequiredDoubleColumns() { return REQUIRED_DOUBLE_COLUMNS; }
    public Set<String> getRequiredStringColumns() { return Collections.<String>emptySet(); }
    public Set<String> getRequiredColumns() { return REQUIRED_DOUBLE_COLUMNS; }
    public Set<String> getOutputColumns() { return OUTPUT_COLUMNS; }
    
    public boolean airframeIsValid(String airframe) { return true; }
    public boolean isRequired() { return true; }

    public void compute() throws SQLException, MalformedFlightFileException, FatalFlightFileException {

        flight.addHeader(ALT_AGL, UNIT_FT_AGL);

        DoubleTimeSeries altitudeMSLTS = doubleTimeSeries.get(ALT_MSL);
        DoubleTimeSeries latitudeTS = doubleTimeSeries.get(LATITUDE);
        DoubleTimeSeries longitudeTS = doubleTimeSeries.get(LONGITUDE);

        // TODO: Probably remove this stuff since ths method will only be called if the columns are available
        if (altitudeMSLTS == null || latitudeTS == null || longitudeTS == null) {
            String message = "Cannot calculate AGL, flight file had empty or missing ";

            int count = 0;
            if (altitudeMSLTS == null) {
                message += "'" + ALT_MSL + "'";
                count++;
            }

            if (latitudeTS == null) {
                if (count > 0) message += ", ";
                message += "'" + LATITUDE + "'";
                count++;
            }

            if (longitudeTS == null) {
                if (count > 0) message += " and ";
                message += "'" + LONGITUDE + "'";
                count++;
            }

            message += " column";
            if (count >= 2) message += "s";
            message += ".";

            //should be initialized to false, but lets make sure
            flight.setHasCoords(false);
            flight.setHasAGL(false);
            throw new MalformedFlightFileException(message);
        }
        flight.setHasCoords(true);
        flight.setHasAGL(true);

        DoubleTimeSeries altitudeAGLTS = withConnection(connection -> new DoubleTimeSeries(connection, ALT_AGL, UNIT_FT_AGL));

        for (int i = 0; i < altitudeMSLTS.size(); i++) {
            double altitudeMSL = altitudeMSLTS.get(i);
            double latitude = latitudeTS.get(i);
            double longitude = longitudeTS.get(i);

            //System.err.println("getting AGL for latitude: " + latitude + ", " + longitude);

            if (Double.isNaN(altitudeMSL) || Double.isNaN(latitude) || Double.isNaN(longitude)) {
                altitudeAGLTS.add(Double.NaN);
                //System.err.println("result is: " + Double.NaN);
                continue;
            }

            try {
                int altitudeAGL = TerrainCache.getAltitudeFt(altitudeMSL, latitude, longitude);
                altitudeAGLTS.add(altitudeAGL);
            } catch (NoSuchFileException e) {
                System.err.println("ERROR: could not read terrain file: " + e);

                flight.setHasAGL(false);
                throw new MalformedFlightFileException("Could not calculate AGL for this flight as it had latitudes/longitudes outside of the United States.");
            }

        }

        doubleTimeSeries.put(ALT_AGL, altitudeAGLTS);
    }

}
