package org.ngafid.processor.steps;

import org.ngafid.core.flights.DoubleTimeSeries;
import org.ngafid.core.flights.FatalFlightFileException;
import org.ngafid.core.flights.MalformedFlightFileException;
import org.ngafid.processor.format.FlightBuilder;
import org.ngafid.processor.terrain.TerrainCache;
import org.ngafid.processor.terrain.TerrainUnavailableException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Set;

import static org.ngafid.core.flights.Parameters.*;

/**
 * Computes the altitude above ground level based on the altitude above sea level and lat / long coordinates.
 * <p>
 * This relies on the Terrain data to infer the altitude above sea level at each lat / long coordinate, and the
 * difference between the airplanes MSL and the terrains MSL is the altitude above ground level (ALT_AGL).
 * <p>
 * See {@link TerrainCache} for details.
 */
public class ComputeAltAGL extends ComputeStep {
    private static final Set<String> REQUIRED_DOUBLE_COLUMNS = Set.of(ALT_MSL, LATITUDE, LONGITUDE);
    private static final Set<String> OUTPUT_COLUMNS = Set.of(ALT_AGL);

    public ComputeAltAGL(Connection connection, FlightBuilder builder) {
        super(connection, builder);
    }

    @Override
    public Set<String> getRequiredDoubleColumns() {
        return REQUIRED_DOUBLE_COLUMNS;
    }

    @Override
    public Set<String> getRequiredStringColumns() {
        return Collections.<String>emptySet();
    }

    @Override
    public Set<String> getRequiredColumns() {
        return REQUIRED_DOUBLE_COLUMNS;
    }

    @Override
    public Set<String> getOutputColumns() {
        return OUTPUT_COLUMNS;
    }

    public boolean airframeIsValid(String airframe) {
        return true;
    }

    @Override
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
            } catch (TerrainUnavailableException e) {
                altitudeAGLTS.add(Double.NaN);
            }
        }

        builder.addTimeSeries(altitudeAGLTS);
    }

}
