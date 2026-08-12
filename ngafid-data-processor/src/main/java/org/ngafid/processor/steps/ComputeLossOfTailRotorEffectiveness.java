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
import org.ngafid.core.flights.LossOfTailRotorEffectiveness;
import org.ngafid.core.flights.LossOfTailRotorEffectiveness.HelicopterSpec;
import org.ngafid.core.flights.LossOfTailRotorEffectiveness.NormalizedInputs;
import org.ngafid.core.flights.MalformedFlightFileException;
import org.ngafid.processor.format.FlightBuilder;

/**
 * Computes Loss of Tail Rotor Effectiveness (LTE) point classifications for rotorcraft flights.
 *
 * <p>The step mirrors the RAISE LTE data flow as closely as the Java NGAFID data model allows:
 *
 * <pre>
 * HDG + WndDr      -> relative wind direction in the body frame
 * GndSpd + WndSpd -> calculated true airspeed fallback
 * AltMSL          -> calculated air-density fallback
 * helicopter spec -> MRCT / sigma and mu
 * neural network  -> predicted pedal margin
 * classifier      -> LTE code (-1, 0, 2, 3)
 * </pre>
 */
public class ComputeLossOfTailRotorEffectiveness extends ComputeStep {
    private static final Set<String> REQUIRED_DOUBLE_COLUMNS = Set.of(GND_SPD, WIND_SPEED, WIND_DIRECTION, HDG, YAW_RATE);
    private static final Set<String> OUTPUT_COLUMNS = Set.of(LTE, LTE_PEDAL_MARGIN, LTE_MRCT_SIGMA, LTE_MU, LTE_RELATIVE_WIND);

    public ComputeLossOfTailRotorEffectiveness(Connection connection, FlightBuilder builder) {
        super(connection, builder);
    }

    @Override
    public Set<String> getRequiredDoubleColumns() {
        return REQUIRED_DOUBLE_COLUMNS;
    }

    @Override
    public Set<String> getRequiredStringColumns() {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getRequiredColumns() {
        return REQUIRED_DOUBLE_COLUMNS;
    }

    @Override
    public Set<String> getOutputColumns() {
        return OUTPUT_COLUMNS;
    }

    @Override
    public boolean airframeIsValid(Airframes.Airframe airframe) {
        return airframe != null
                && airframe.getType() != null
                && "Rotorcraft".equals(airframe.getType().getName());
    }

    @Override
    public void compute() throws SQLException, MalformedFlightFileException, FatalFlightFileException {
        HelicopterSpec spec = withConnection(connection -> RotorcraftAirframeSpecRepository
                .findByAirframeId(connection, builder.meta.getAirframe().getId()))
                .orElseThrow(() -> new MalformedFlightFileException(
                        "No LTE helicopter specification for airframe id "
                                + builder.meta.getAirframe().getId()
                                + " ("
                                + builder.meta.getAirframe().getName()
                                + ")"));

        DoubleTimeSeries groundspeed = builder.getDoubleTimeSeries(GND_SPD);
        DoubleTimeSeries windSpeed = builder.getDoubleTimeSeries(WIND_SPEED);
        DoubleTimeSeries windDirection = builder.getDoubleTimeSeries(WIND_DIRECTION);
        DoubleTimeSeries heading = builder.getDoubleTimeSeries(HDG);
        DoubleTimeSeries yawRate = builder.getDoubleTimeSeries(YAW_RATE);
        DoubleTimeSeries altitudeMsl = builder.getDoubleTimeSeries(ALT_MSL);

        int length = groundspeed.size();
        DoubleTimeSeries lte = withConnection(connection -> new DoubleTimeSeries(connection, LTE, Unit.INDEX, length));
        DoubleTimeSeries pedalMargin = withConnection(connection -> new DoubleTimeSeries(connection, LTE_PEDAL_MARGIN, "pedal margin", length));
        DoubleTimeSeries mrctSigma = withConnection(connection -> new DoubleTimeSeries(connection, LTE_MRCT_SIGMA, Unit.RATIO, length));
        DoubleTimeSeries mu = withConnection(connection -> new DoubleTimeSeries(connection, LTE_MU, Unit.RATIO, length));
        DoubleTimeSeries relativeWind = withConnection(connection -> new DoubleTimeSeries(connection, LTE_RELATIVE_WIND, Unit.DEGREES, length));

        for (int i = 0; i < length; i++) {
            double hdg = heading.get(i);
            double wd = windDirection.get(i);
            double ws = windSpeed.get(i);
            double gs = groundspeed.get(i);
            double yaw = yawRate.get(i);
            double msl = altitudeMsl == null || i >= altitudeMsl.size() ? Double.NaN : altitudeMsl.get(i);

            if (Double.isNaN(hdg) || Double.isNaN(wd) || Double.isNaN(ws) || Double.isNaN(gs) || Double.isNaN(yaw)) {
                addUnsupported(lte, pedalMargin, mrctSigma, mu, relativeWind);
                continue;
            }

            // Formulas below match the RAISE phase-of-flight and LTE formulas:
            // relativeWind = (windDirection - heading) wrapped to [0, 360)
            // TAS_kt = GndSpd_kt + WndSpd_kt * cos(relativeWind)
            // rho = 1.225 * exp((-0.0296 * AltMSL_ft * 0.3048) / 304.8), or sea-level density when AltMSL is absent.
            double relWind = LossOfTailRotorEffectiveness.relativeWindDirectionBodyFrameDeg(hdg, wd);
            double trueAirspeed = LossOfTailRotorEffectiveness.calculatedTrueAirspeedKt(gs, ws, relWind);
            double density = LossOfTailRotorEffectiveness.calculatedAirDensityKgM3(msl);
            Optional<NormalizedInputs> normalized = LossOfTailRotorEffectiveness.normalize(
                    spec, relWind, density, trueAirspeed, yaw, Double.NaN);

            if (normalized.isEmpty()) {
                addUnsupported(lte, pedalMargin, mrctSigma, mu, relativeWind);
                continue;
            }

            NormalizedInputs inputs = normalized.get();
            relativeWind.add(inputs.relativeWindDeg());
            mrctSigma.add(inputs.mrctSigma());
            mu.add(inputs.mu());

            if (!LossOfTailRotorEffectiveness.isInsideModelEnvelope(inputs.mrctSigma(), inputs.mu())) {
                pedalMargin.add(Double.NaN);
                lte.add(LossOfTailRotorEffectiveness.UNSUPPORTED);
                continue;
            }

            // Pedal margin is the RAISE PredictedXPTRMPC neural-network output. Classification maps margin + yaw to
            // LTE severity code: 0 no risk, 2 risk, 3 event, -1 unsupported.
            double margin = LossOfTailRotorEffectiveness.predictPedalMargin(
                    inputs.relativeWindDeg(), inputs.mrctSigma(), inputs.mu());
            pedalMargin.add(margin);
            lte.add(LossOfTailRotorEffectiveness.classify(margin, inputs.yawRateDps()));
        }

        builder.addTimeSeries(lte);
        builder.addTimeSeries(pedalMargin);
        builder.addTimeSeries(mrctSigma);
        builder.addTimeSeries(mu);
        builder.addTimeSeries(relativeWind);
    }

    private static void addUnsupported(
            DoubleTimeSeries lte,
            DoubleTimeSeries pedalMargin,
            DoubleTimeSeries mrctSigma,
            DoubleTimeSeries mu,
            DoubleTimeSeries relativeWind) {
        lte.add(LossOfTailRotorEffectiveness.UNSUPPORTED);
        pedalMargin.add(Double.NaN);
        mrctSigma.add(Double.NaN);
        mu.add(Double.NaN);
        relativeWind.add(Double.NaN);
    }
}