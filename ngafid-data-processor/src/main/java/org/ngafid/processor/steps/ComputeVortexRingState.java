package org.ngafid.processor.steps;

import static org.ngafid.core.flights.Parameters.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import org.ngafid.core.flights.Airframes;
import org.ngafid.core.flights.DoubleTimeSeries;
import org.ngafid.core.flights.FatalFlightFileException;
import org.ngafid.core.flights.MalformedFlightFileException;
import org.ngafid.core.flights.VortexRingState;
import org.ngafid.core.flights.VortexRingState.NormalizedInputs;
import org.ngafid.processor.format.FlightBuilder;


/**
 * Computes Vortex Ring State (VRS) point classifications for rotorcraft flights.
 *
 * <p>Initial NGAFID VRS support intentionally requires recorded true airspeed. The wind-derived TAS path is left in the
 * code as a disabled TODO until rotorcraft weather enrichment is available and validated.
 */
public class ComputeVortexRingState extends ComputeStep {
    private static final Set<String> REQUIRED_DOUBLE_COLUMNS = Set.of(VSPD);
    private static final Set<String> OUTPUT_COLUMNS = Set.of(VRS, VRS_VH, VRS_VX, VRS_VZ);
    private static final String MISSING_TAS_MESSAGE = "VRS was not calculated because true airspeed is missing. "
            + "Wind-derived TAS calculation requires rotorcraft weather enrichment and is not implemented yet.";

    /** Creates the compute step with the database connection used for specs and output series creation. */
    public ComputeVortexRingState(Connection connection, FlightBuilder builder) {
        super(connection, builder);
    }

    /** Requires vertical speed; TAS is optional so missing TAS can emit a VRS-specific unsupported warning. */
    @Override
    public Set<String> getRequiredDoubleColumns() {
        return REQUIRED_DOUBLE_COLUMNS;
    }

    /** VRS does not require string-valued time-series inputs. */
    @Override
    public Set<String> getRequiredStringColumns() {
        return Collections.emptySet();
    }

    /** Returns all required source columns for the generic compute-step scheduler. */
    @Override
    public Set<String> getRequiredColumns() {
        return REQUIRED_DOUBLE_COLUMNS;
    }

    /** Declares the severity and diagnostic series produced by this step. */
    @Override
    public Set<String> getOutputColumns() {
        return OUTPUT_COLUMNS;
    }

    /** Runs only for rotorcraft airframes because fixed-wing data cannot use this rotor VRS model. */
    @Override
    public boolean airframeIsValid(Airframes.Airframe airframe) {
        return airframe != null
                && airframe.getType() != null
                && "Rotorcraft".equals(airframe.getType().getName());
    }

    /**
     * Loads rotorcraft specs, normalizes each sample with recorded TAS, and emits VRS severity plus Vh/Vx/Vz
     * diagnostics. Missing TAS currently produces unsupported outputs instead of calculating TAS from weather.
     */
    @Override
    public void compute() throws SQLException, MalformedFlightFileException, FatalFlightFileException {
        VortexRingState.HelicopterSpec spec = withConnection(connection -> RotorcraftAirframeSpecRepository
                .findVrsByAirframeId(connection, builder.meta.getAirframe().getId()))
                .orElseThrow(() -> new MalformedFlightFileException(
                        "No VRS helicopter specification for airframe id "
                                + builder.meta.getAirframe().getId()
                                + " ("
                                + builder.meta.getAirframe().getName()
                                + ")"));

        DoubleTimeSeries verticalSpeed = builder.getDoubleTimeSeries(VSPD);
        DoubleTimeSeries trueAirspeed = builder.getDoubleTimeSeries(TAS);
        DoubleTimeSeries altitudeMsl = builder.getDoubleTimeSeries(ALT_MSL);

        /*
         * Wind-derived TAS fallback inputs.
         *
         * These should only be trusted after rotorcraft weather enrichment and/or onboard
         * wind columns have been validated for units, direction convention, and quality.
         */
        //TODO: When rotorcraft weather enrichment is available:

//        DoubleTimeSeries groundspeed = builder.getDoubleTimeSeries(GND_SPD);
//        DoubleTimeSeries windSpeed = builder.getDoubleTimeSeries(WIND_SPEED);
//        DoubleTimeSeries windDirection = builder.getDoubleTimeSeries(WIND_DIRECTION);
//        DoubleTimeSeries heading = builder.getDoubleTimeSeries(HDG);

        int length = verticalSpeed.size();

        DoubleTimeSeries vrs = withConnection(connection -> new DoubleTimeSeries(connection, VRS, Unit.INDEX, length));
        DoubleTimeSeries vh = withConnection(
                connection -> new DoubleTimeSeries(connection, VRS_VH, Unit.METERS_PER_SECOND, length));
        DoubleTimeSeries vx = withConnection(
                connection -> new DoubleTimeSeries(connection, VRS_VX, Unit.RATIO, length));
        DoubleTimeSeries vz = withConnection(
                connection -> new DoubleTimeSeries(connection, VRS_VZ, Unit.RATIO, length));

        //TODO: When rotorcraft weather enrichment is available:

//        boolean canCalculateTasFromWind = hasWindTasInputs(groundspeed, windSpeed, windDirection, heading);

//        if (trueAirspeed == null && !canCalculateTasFromWind) {
        if (trueAirspeed == null) {
            synchronized (builder.exceptions) {
                builder.exceptions.add(new MalformedFlightFileException(MISSING_TAS_MESSAGE));
            }
            addUnsupportedSeries(length, vrs, vh, vx, vz);
            addOutputs(vrs, vh, vx, vz);
            return;
        }

        for (int i = 0; i < length; i++) {
        //TODO: When rotorcraft weather enrichment is available:

//            double tasKt = getTrueAirspeedKt(
//                    i,
//                    trueAirspeed,
//                    groundspeed,
//                    windSpeed,
//                    windDirection,
//                    heading);
            double tasKt = i < trueAirspeed.size() ? trueAirspeed.get(i) : Double.NaN;
            double vspdFpm = verticalSpeed.get(i);
            double msl = altitudeMsl == null || i >= altitudeMsl.size() ? Double.NaN : altitudeMsl.get(i);
            double density = VortexRingState.calculatedAirDensityKgM3(msl);

            Optional<NormalizedInputs> normalized = VortexRingState.normalize(
                    spec, tasKt, vspdFpm, density, Double.NaN);
            if (normalized.isEmpty()) {
                addUnsupportedPoint(vrs, vh, vx, vz);
                continue;
            }

            NormalizedInputs inputs = normalized.get();
            vh.add(inputs.vhMps());
            vx.add(inputs.vxOverVh());
            vz.add(inputs.vzOverVh());
            vrs.add(VortexRingState.classify(inputs));
        }

        addOutputs(vrs, vh, vx, vz);
    }


    /**
     * Returns true when all series required for wind-derived TAS are available.
     */
    private static boolean hasWindTasInputs(
            DoubleTimeSeries groundspeed,
            DoubleTimeSeries windSpeed,
            DoubleTimeSeries windDirection,
            DoubleTimeSeries heading) {
        return groundspeed != null
                && windSpeed != null
                && windDirection != null
                && heading != null;
    }

    /**
     * Uses recorded TAS when present; otherwise falls back to wind-derived TAS.
     */
    private static double getTrueAirspeedKt(
            int index,
            DoubleTimeSeries trueAirspeed,
            DoubleTimeSeries groundspeed,
            DoubleTimeSeries windSpeed,
            DoubleTimeSeries windDirection,
            DoubleTimeSeries heading) {
        if (trueAirspeed != null) {
            return getValueOrNaN(trueAirspeed, index);
        }

        return calculateTasFromWindKt(
                getValueOrNaN(groundspeed, index),
                getValueOrNaN(windSpeed, index),
                getValueOrNaN(windDirection, index),
                getValueOrNaN(heading, index));
    }

    /**
     * Reads one sample safely; missing series values become NaN so normalization can reject them.
     */
    private static double getValueOrNaN(DoubleTimeSeries series, int index) {
        return series == null || index >= series.size() ? Double.NaN : series.get(index);
    }

    /**
     * Calculates approximate TAS from groundspeed and wind component along the aircraft heading.
     *
     * Required unit assumptions:
     * - groundspeedKt is knots
     * - windSpeedKt is knots
     * - windDirectionDeg is degrees
     * - headingDeg is degrees
     *
     * Important: this assumes wind direction convention has been validated.
     */
    private static double calculateTasFromWindKt(
            double groundspeedKt,
            double windSpeedKt,
            double windDirectionDeg,
            double headingDeg) {
        if (!isFinite(groundspeedKt)
                || !isFinite(windSpeedKt)
                || !isFinite(windDirectionDeg)
                || !isFinite(headingDeg)) {
            return Double.NaN;
        }

        double relativeWindDeg = wrap360(windDirectionDeg - headingDeg);
        return groundspeedKt + windSpeedKt * Math.cos(Math.toRadians(relativeWindDeg));
    }

    /** Normalizes degrees to [0, 360); retained for the wind-derived TAS implementation. */
    private static double wrap360(double degrees) {
        double wrapped = degrees % 360.0;
        return wrapped < 0.0 ? wrapped + 360.0 : wrapped;
    }

    /** Fills every sample with unsupported severity and NaN diagnostics when VRS cannot be calculated for the flight. */
    private static void addUnsupportedSeries(
            int length, DoubleTimeSeries vrs, DoubleTimeSeries vh, DoubleTimeSeries vx, DoubleTimeSeries vz) {
        for (int i = 0; i < length; i++) {
            addUnsupportedPoint(vrs, vh, vx, vz);
        }
    }

    /**
     * Keeps NaN and infinities out of the wind-derived TAS calculation.
     */
    private static boolean isFinite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    /** Appends one unsupported sample while preserving output-series alignment with source flight data. */
    private static void addUnsupportedPoint(
            DoubleTimeSeries vrs, DoubleTimeSeries vh, DoubleTimeSeries vx, DoubleTimeSeries vz) {
        vrs.add(VortexRingState.UNSUPPORTED);
        vh.add(Double.NaN);
        vx.add(Double.NaN);
        vz.add(Double.NaN);
    }

    /** Registers all VRS output series together so downstream processing sees a complete diagnostic set. */
    private void addOutputs(DoubleTimeSeries vrs, DoubleTimeSeries vh, DoubleTimeSeries vx, DoubleTimeSeries vz) {
        builder.addTimeSeries(vrs);
        builder.addTimeSeries(vh);
        builder.addTimeSeries(vx);
        builder.addTimeSeries(vz);
    }
}
