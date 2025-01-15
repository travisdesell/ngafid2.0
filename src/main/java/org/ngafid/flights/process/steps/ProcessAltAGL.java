package org.ngafid.flights.process.steps;

import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.FatalFlightFileException;
import org.ngafid.flights.MalformedFlightFileException;
import org.ngafid.flights.process.FlightBuilder;
import org.ngafid.terrain.TerrainCache;

import java.nio.file.NoSuchFileException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Set;

import static org.ngafid.flights.Parameters.*;

public class ProcessAltAGL extends ProcessStep {
    private static final Set<String> REQUIRED_DOUBLE_COLUMNS = Set.of(ALT_MSL, LATITUDE, LONGITUDE);
    private static final Set<String> OUTPUT_COLUMNS = Set.of(ALT_AGL);

    public ProcessAltAGL(Connection connection, FlightBuilder builder) {
        super(connection, builder);
    }

    public Set<String> getRequiredDoubleColumns() {
        return REQUIRED_DOUBLE_COLUMNS;
    }

    public Set<String> getRequiredStringColumns() {
        return Collections.<String>emptySet();
    }

    public Set<String> getRequiredColumns() {
        return REQUIRED_DOUBLE_COLUMNS;
    }

    public Set<String> getOutputColumns() {
        return OUTPUT_COLUMNS;
    }

    public boolean airframeIsValid(String airframe) {
        return true;
    }

    public void compute() throws SQLException, MalformedFlightFileException, FatalFlightFileException {
        DoubleTimeSeries altitudeMSLTS = builder.getDoubleTimeSeries(ALT_MSL);
        DoubleTimeSeries latitudeTS = builder.getDoubleTimeSeries(LATITUDE);
        DoubleTimeSeries longitudeTS = builder.getDoubleTimeSeries(LONGITUDE);

        DoubleTimeSeries altitudeAGLTS = withConnection(
                connection -> new DoubleTimeSeries(connection, ALT_AGL, Unit.FT_AGL));

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
                throw new MalformedFlightFileException(
                        "Could not calculate AGL for this flight as it had " +
                                "latitudes/longitudes outside of the United States.");
            }
        }

        builder.addTimeSeries(altitudeAGLTS);
    }

}
