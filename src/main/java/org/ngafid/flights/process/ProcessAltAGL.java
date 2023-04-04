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

    public ProcessAltAGL(Connection connection, FlightBuilder builder) {
        super(connection, builder);
    }

    public Set<String> getRequiredDoubleColumns() { return REQUIRED_DOUBLE_COLUMNS; }
    public Set<String> getRequiredStringColumns() { return Collections.<String>emptySet(); }
    public Set<String> getRequiredColumns() { return REQUIRED_DOUBLE_COLUMNS; }
    public Set<String> getOutputColumns() { return OUTPUT_COLUMNS; }
    
    public boolean airframeIsValid(String airframe) { return true; }
    public boolean isRequired() { return true; }

    public void compute() throws SQLException, MalformedFlightFileException, FatalFlightFileException {
        DoubleTimeSeries altitudeMSLTS = builder.doubleTimeSeries.get(ALT_MSL);
        DoubleTimeSeries latitudeTS = builder.doubleTimeSeries.get(LATITUDE);
        DoubleTimeSeries longitudeTS = builder.doubleTimeSeries.get(LONGITUDE);

        DoubleTimeSeries altitudeAGLTS = withConnection(connection -> new DoubleTimeSeries(connection, ALT_AGL, UNIT_FT_AGL));

        for (int i = 0; i < altitudeMSLTS.size(); i++) {
            double altitudeMSL = altitudeMSLTS.get(i);
            double latitude = latitudeTS.get(i);
            double longitude = longitudeTS.get(i);

            if (Double.isNaN(altitudeMSL) || Double.isNaN(latitude) || Double.isNaN(longitude)) {
                altitudeAGLTS.add(Double.NaN);
                continue;
            }

            try {
                int altitudeAGL = TerrainCache.getAltitudeFt(altitudeMSL, latitude, longitude);
                altitudeAGLTS.add(altitudeAGL);
            } catch (NoSuchFileException e) {
                throw new MalformedFlightFileException("Could not calculate AGL for this flight as it had latitudes/longitudes outside of the United States.");
            }
        }

        builder.doubleTimeSeries.put(ALT_AGL, altitudeAGLTS);
    }

}
