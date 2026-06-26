package org.ngafid.core.flights;

import java.util.Optional;

/**
 * Loss of Tail Rotor Effectiveness (LTE) model support for rotorcraft processing.
 *
 * <p>This class is a formula-level Java port of the LTE implementation in
 * {@code faa-raise-etl-glue/loss_of_control/loss_of_control_workflow_job.py}. The RAISE model is a hard-coded
 * feed-forward neural-network calculation:
 *
 * <pre>
 * T1 = [relativeWindBodyFrameDeg, MRCT / sigma, mu, 1]
 * T2 = tanh(0.5 * T1 * layer1Weights)
 * T3 = tanh(0.5 * [T2, 1] * layer2Weights)
 * predictedPedalMargin = [T3, 1] * outputWeights
 * </pre>
 *
 * <p>The classifier is intentionally separated from the neural-network math. The Python source checks
 * {@code PredictedXPTRMPC <= 0} before checking the high-severity yaw condition, which makes severity {@code 3}
 * unreachable. The Java classifier preserves the documented intent of the Python comments by testing the specific
 * event condition before the broader risk condition.
 */
public final class LossOfTailRotorEffectiveness {
    public static final int UNSUPPORTED = -1;
    public static final int NO_RISK = 0;
    public static final int RISK = 2;
    public static final int EVENT = 3;

    public static final double DEFAULT_EVENT_YAW_THRESHOLD_DPS = 60.0;
    public static final double MIN_MRCT_SIGMA = 0.05;
    public static final double MAX_MRCT_SIGMA = 0.1277;
    public static final double MIN_MU = 0.0;
    public static final double MAX_MU = 0.1233;

    private static final double INCH_TO_METER = 0.0254;
    private static final double KNOT_TO_METER_PER_SECOND = 0.514444;
    private static final double POUND_FORCE_TO_NEWTON = 4.4482216153;
    private static final double RPM_TO_RADIAN_PER_SECOND = 0.10472;
    private static final double SEA_LEVEL_AIR_DENSITY_KG_M3 = 1.225;
    /*
     * Neural-network coefficients copied from RAISE's lte_predicted_petal UDF. The Python source builds:
     * T_1 = [relative wind, MRCT/sigma, mu, 1], T_2 from these 10 layer-1 rows, T_3 from the 15 layer-2 rows,
     * then PredictedXPTRMPC from OUTPUT_WEIGHTS. Do not tune these values in NGAFID without SME/model-owner approval.
     */
    private static final double[][] LAYER_1_WEIGHTS = {
            {-0.0089641697471277808, 21.817861246997399, -46.467102680377501, -1.4275890215687099},
            {0.046015910449271502, -26.680817399249101, 30.4645322484254, -14.8257237092024},
            {0.021035327694732198, -16.072597428422899, 44.208732312900899, -4.1531382645369801},
            {0.0172883026345616, 5.6155650787911799, 1.39656818636404, -7.3933838056521504},
            {0.0013322823751157999, -82.249153617565895, -18.058182294228999, 13.6313698025939},
            {-0.044213455194802201, -9.6439700383378906, 15.6447341186991, 11.554587369531101},
            {-0.0077215406534427503, 6.3658954235430203, -10.3941547733269, 3.6688705877586498},
            {-0.0090179583733386495, 5.5205541021447804, -21.31847750595, 3.0960124194469398},
            {0.025110387072421199, 0.65664230056845296, -19.0834501126741, -0.82610698243773695},
            {-0.0174074469314692, -1.02479827520554, 8.1308854778991702, -1.01673286912769},
    };
    private static final double[][] LAYER_2_WEIGHTS = {
            {0.87825228583058002, -2.2116417542138498, -5.2310411006784401, -6.4264099332727902, 8.4800182772842891, -2.1595854587062902, 3.5936821822672602, 0.68280486266727403, -7.0125384204904497, 2.9406784746872301, -1.5634095967805},
            {0.95736400689647405, -1.2426393318597799, -0.38212715317900597, -3.3240260240362098, -1.1522589025525301, -1.3657427473453201, -4.6178834877353898, 0.43744421753506602, -2.8248279772119198, -5.6652971687680198, -1.14863596351259},
            {-3.6907290864489899, 0.66202871399211904, -0.72216947761017203, -2.25404535041335, 0.89214159232018697, 0.41740199847446902, 1.1386603023937201, -3.8584857424779599, -2.7553786677076801, -2.2554318977957899, -1.5614560371566999},
            {-1.4998836635761099, -2.7368690872463399, 1.88409407573306, -3.51073325850161, 1.44331597069281, 1.7245384626350899, 1.61103585065635, 0.222887955985383, 5.4198583247194403, -4.4283139393464301, -1.52198653808183},
            {1.5390231612388301, -0.93279971095381797, -0.55580976086789202, -3.8196931771039702, 0.607112619758021, 5.4014425399451298, -3.4501219681909601, 0.11079558851728701, 6.4868724861120404, -1.0188877902248501, -0.027814420498550398},
            {-0.090576132247396704, -1.03366044606375, -4.49021390450923, 0.0045644886462122098, -0.0799549347437115, 3.8646324561660799, 1.6491537202071, -2.8330226187749998, -0.036094519183306299, -0.33078427070385102, 2.4367977431533001},
            {-2.1844837448312302, -0.863479053810981, -4.5380872637849201, -1.0807656176277001, 2.5840439138503801, -0.88428017359838695, -1.29809064951719, -3.09662973314104, -4.5910558517290996, 1.8001617243000001, -1.9803795778009201},
            {-1.12532917470531, 0.33936178869727301, -0.44951317098995303, 0.0008616971724537, -0.92770797516474601, 1.8387379891433799, -0.502400465000159, 0.0057944428294142, -0.296631766006202, -1.2085352927058299, 0.31573478021183898},
            {-4.6709648474768803, 1.7926816269769501, 0.32419620976289598, 1.8502478400581199, -10.793178734483201, 0.45110248082620003, 5.8369829336635997, 1.2437165133347201, 0.90656146261247095, -0.72357430565228997, 2.03792500257109},
            {1.5025025876720199, -1.3074935605925899, -0.10295770339065199, -1.93624704568454, 0.49706642196677098, -0.83307385042696502, -1.76038226867244, -2.1167874327802201, -0.80060986846200999, -2.66935123234658, -3.1404601130275198},
            {0.057068593502581602, 2.9552653635281501, 0.022256928605490601, -7.5368307981858003, 3.7640678143947999, -0.93636132084317902, -0.21405280893789, -0.368289981907931, -1.3601640435401301, -0.29467012625985001, 1.63064008080769},
            {-3.71603762189241, -3.1551766130818502, -0.79574301077782195, 0.28546416014392201, -1.5706839317614201, -2.3240458031521301, 1.5902996284504001, 6.4581894213883499, -5.1054667802598601, 3.7384848496157299, -0.52589801717768803},
            {-0.901048368087212, 0.68027981246275204, 0.89312486145376302, 1.9218896090039299, -0.22701931574683601, -0.399849220503321, 3.1714167940946001, -1.1497896839626001, -1.50923259677606, -0.62855426939418901, 1.41790662785399},
            {1.21396837538822, -1.8685299115179099, 0.40325376819654202, -2.0573634505287002, 1.20879679712385, -1.7626792551110699, -2.7715344486717801, 2.8253258522142599, 2.9616117593332598, 2.5162455575214402, -2.80065424809978},
            {-0.90296682474246603, -0.950374629098516, -0.63865685892302804, -0.68493759969371804, -2.0364490902386101, -0.062199507704555401, 1.3345032042020499, 0.0060278035497425197, 2.2964680447505299, 0.32021487003629701, -3.1150969826820498},
    };
    private static final double[] OUTPUT_WEIGHTS = {-13.772533358931501, -112.42755409319101, 25.5383697172174, -49.587215660822402, 80.100135394576995, 4.2657336020262502, 15.518040601285101, 0.210863047838805, 26.3393431448469, 79.566749294024703, 51.995109840465503, 11.180602932654701, -43.406978538508397, 69.229592303810605, -92.476022132703903, 33.4455586575002};

    private LossOfTailRotorEffectiveness() {}

    public record HelicopterSpec(
            String airframe,
            double maxGrossWeightLbs,
            double minFlyingWeightLbs,
            double emptyWeightLbs,
            int mainRotorBlades,
            double mainRotorDiameterIn,
            double mainRotorBladeChordIn,
            double mainRotorMaxContinuousRpm) {}

    public record NormalizedInputs(double relativeWindDeg, double mrctSigma, double mu, double yawRateDps) {}

    /**
     * Approximates aircraft operating weight using the same policy as RAISE's {@code helispec_get_weight_lbs(..., 0.75)}:
     *
     * <pre>
     * if minimum flying weight is known:
     *     W_lbs = minFlyingWeight + 0.75 * (maxGrossWeight - minFlyingWeight)
     * else if empty weight is known:
     *     W_lbs = emptyWeight + 0.75 * (maxGrossWeight - emptyWeight)
     * else:
     *     W_lbs = maxGrossWeight
     * </pre>
     *
     * @param spec helicopter specification
     * @return best available operating-weight estimate in pounds-force
     */
    public static double bestWeightLbs(HelicopterSpec spec) {
        if (!Double.isNaN(spec.minFlyingWeightLbs()) && spec.minFlyingWeightLbs() > 0.0) {
            return ((spec.maxGrossWeightLbs() - spec.minFlyingWeightLbs()) * 0.75) + spec.minFlyingWeightLbs();
        }
        if (!Double.isNaN(spec.emptyWeightLbs()) && spec.emptyWeightLbs() > 0.0) {
            return ((spec.maxGrossWeightLbs() - spec.emptyWeightLbs()) * 0.75) + spec.emptyWeightLbs();
        }
        return spec.maxGrossWeightLbs();
    }

    /**
     * Computes the RAISE fallback air-density estimate:
     *
     * <pre>
     * rho = 1.225 * exp((-0.0296 * MSL_ft * 0.3048) / 304.8)
     * </pre>
     *
     * <p>When altitude is unavailable, sea-level standard density is used so LTE can still be evaluated for data sets
     * that provide wind and yaw but not MSL altitude.
     *
     * @param altitudeMslFeet altitude MSL in feet, or {@link Double#NaN}
     * @return air density in kg/m^3
     */
    public static double calculatedAirDensityKgM3(double altitudeMslFeet) {
        if (Double.isNaN(altitudeMslFeet)) {
            return SEA_LEVEL_AIR_DENSITY_KG_M3;
        }
        return SEA_LEVEL_AIR_DENSITY_KG_M3 * Math.exp((-0.0296 * altitudeMslFeet * 0.3048) / 304.8);
    }

    /**
     * Converts wind direction into helicopter body-frame relative wind direction:
     *
     * <pre>
     * relativeWindDeg = (windDirectionDeg - headingDeg) wrapped to [0, 360)
     * </pre>
     *
     * @param headingDeg aircraft heading in degrees
     * @param windDirectionDeg wind direction in degrees
     * @return relative wind direction in degrees, wrapped to {@code [0, 360)}
     */
    public static double relativeWindDirectionBodyFrameDeg(double headingDeg, double windDirectionDeg) {
        double relative = windDirectionDeg - headingDeg;
        relative %= 360.0;
        if (relative < 0.0) {
            relative += 360.0;
        }
        return relative;
    }

    /**
     * Computes the scalar true-airspeed fallback used by the RAISE phase-of-flight code:
     *
     * <pre>
     * TAS_kt = groundspeed_kt + windspeed_kt * cos(relativeWindDeg)
     * </pre>
     *
     * @param groundspeedKt groundspeed in knots
     * @param windSpeedKt wind speed in knots
     * @param relativeWindDeg body-frame relative wind direction in degrees
     * @return calculated true airspeed in knots
     */
    public static double calculatedTrueAirspeedKt(double groundspeedKt, double windSpeedKt, double relativeWindDeg) {
        return groundspeedKt + (windSpeedKt * Math.cos(Math.toRadians(relativeWindDeg)));
    }

    /**
     * Converts aircraft, atmosphere, and flight state into the nondimensional LTE neural-network inputs.
     *
     * <pre>
     * R       = mainRotorDiameterIn / 2 * 0.0254
     * Arotor  = pi * R^2
     * omega   = mainRotorMaxContinuousRpm * 0.10472
     * Vtip    = omega * R
     * c       = mainRotorBladeChordIn * 0.0254
     * Ablade  = c * R * mainRotorBlades
     * sigma   = Ablade / Arotor
     * T       = weight_lbs * 4.4482216153
     * MRCT    = T / (rho * Arotor * Vtip^2)
     * MRCTSIG = MRCT / sigma
     * mu      = trueAirspeed_kt * 0.514444 / Vtip
     * </pre>
     *
     * @param spec helicopter specification
     * @param relativeWindDeg body-frame relative wind direction in degrees
     * @param airDensityKgM3 air density in kg/m^3
     * @param trueAirspeedKt true airspeed in knots
     * @param yawRateDps yaw rate in degrees/second
     * @param airframeGrossWeightLbs measured gross weight in pounds, or {@link Double#NaN} to use the spec estimate
     * @return normalized LTE neural-network inputs, or empty when any required value is invalid
     */
    public static Optional<NormalizedInputs> normalize(
            HelicopterSpec spec,
            double relativeWindDeg,
            double airDensityKgM3,
            double trueAirspeedKt,
            double yawRateDps,
            double airframeGrossWeightLbs) {
        if (!isValidSpec(spec)
                || !isFinite(relativeWindDeg)
                || !isFinite(airDensityKgM3)
                || airDensityKgM3 <= 0.0
                || !isFinite(trueAirspeedKt)
                || !isFinite(yawRateDps)) {
            return Optional.empty();
        }

        double radiusM = spec.mainRotorDiameterIn() / 2.0 * INCH_TO_METER;
        double rotorAreaM2 = Math.PI * radiusM * radiusM;
        double omega = spec.mainRotorMaxContinuousRpm() * RPM_TO_RADIAN_PER_SECOND;
        double tipSpeed = omega * radiusM;
        double chordM = spec.mainRotorBladeChordIn() * INCH_TO_METER;
        double bladeAreaM2 = chordM * radiusM * spec.mainRotorBlades();
        double sigma = bladeAreaM2 / rotorAreaM2;

        if (radiusM <= 0.0 || rotorAreaM2 <= 0.0 || tipSpeed <= 0.0 || sigma <= 0.0) {
            return Optional.empty();
        }

        double weightLbs = isFinite(airframeGrossWeightLbs) && airframeGrossWeightLbs > 0.0
                ? airframeGrossWeightLbs
                : bestWeightLbs(spec);
        double thrustN = weightLbs * POUND_FORCE_TO_NEWTON;
        double mrct = thrustN / (airDensityKgM3 * rotorAreaM2 * tipSpeed * tipSpeed);
        double mrctSigma = mrct / sigma;
        double mu = trueAirspeedKt * KNOT_TO_METER_PER_SECOND / tipSpeed;

        return Optional.of(new NormalizedInputs(relativeWindDeg, mrctSigma, mu, yawRateDps));
    }

    /**
     * Checks the RAISE neural-network validity envelope:
     *
     * <pre>
     * 0.05 <= MRCT / sigma <= 0.1277
     * 0.0  <= mu           <= 0.1233
     * </pre>
     *
     * @param mrctSigma normalized main-rotor thrust coefficient
     * @param mu advance ratio
     * @return true when the neural network is allowed to evaluate the point
     */
    public static boolean isInsideModelEnvelope(double mrctSigma, double mu) {
        return isFinite(mrctSigma)
                && isFinite(mu)
                && mrctSigma >= MIN_MRCT_SIGMA
                && mrctSigma <= MAX_MRCT_SIGMA
                && mu >= MIN_MU
                && mu <= MAX_MU;
    }

    /**
     * Evaluates the hard-coded RAISE LTE neural network and returns the predicted pedal margin
     * ({@code PredictedXPTRMPC} in the Python source).
     *
     * @param relativeWindDeg body-frame relative wind direction in degrees
     * @param mrctSigma normalized main-rotor thrust coefficient
     * @param mu advance ratio
     * @return predicted pedal margin; values {@code <= 0} indicate predicted left-pedal exhaustion/risk
     */
    public static double predictPedalMargin(double relativeWindDeg, double mrctSigma, double mu) {
        double[] input = {relativeWindDeg, mrctSigma, mu, 1.0};
        double[] layer1 = evaluateLayer(input, LAYER_1_WEIGHTS);
        double[] layer1WithBias = appendBias(layer1);
        double[] layer2 = evaluateLayer(layer1WithBias, LAYER_2_WEIGHTS);
        double[] layer2WithBias = appendBias(layer2);

        double output = 0.0;
        for (int i = 0; i < OUTPUT_WEIGHTS.length; i++) {
            output += layer2WithBias[i] * OUTPUT_WEIGHTS[i];
        }
        return output;
    }

    public static int classify(double predictedPedalMargin, double yawRateDps) {
        return classify(predictedPedalMargin, yawRateDps, DEFAULT_EVENT_YAW_THRESHOLD_DPS);
    }

    /**
     * Converts predicted pedal margin and yaw rate into the LTE output code.
     *
     * <pre>
     * -1 = unsupported / not classifiable
     *  0 = no LTE risk
     *  2 = risk: predicted pedal margin <= 0
     *  3 = event: predicted pedal margin <= 0 and yaw rate >= threshold
     * </pre>
     *
     * <p>This branch order intentionally differs from the active Python code because the Python order returns risk
     * before checking the more specific event condition. The order here matches the Python comments describing
     * severity {@code 2} as risk and severity {@code 3} as running out of left pedal while yawing.
     *
     * @param predictedPedalMargin neural-network output
     * @param yawRateDps yaw rate in degrees/second
     * @param eventYawThresholdDps threshold for high-severity LTE event detection
     * @return LTE output code
     */
    public static int classify(double predictedPedalMargin, double yawRateDps, double eventYawThresholdDps) {
        if (!isFinite(predictedPedalMargin) || !isFinite(yawRateDps)) {
            return UNSUPPORTED;
        }
        if (predictedPedalMargin <= 0.0 && yawRateDps >= eventYawThresholdDps) {
            return EVENT;
        }
        if (predictedPedalMargin <= 0.0) {
            return RISK;
        }
        return NO_RISK;
    }

    /**
     * Evaluates LTE end-to-end for already normalized inputs: envelope check, neural-network prediction, then
     * classification.
     *
     * @param inputs normalized LTE model inputs
     * @return LTE output code
     */
    public static int evaluate(NormalizedInputs inputs) {
        if (!isInsideModelEnvelope(inputs.mrctSigma(), inputs.mu())) {
            return UNSUPPORTED;
        }
        return classify(predictPedalMargin(inputs.relativeWindDeg(), inputs.mrctSigma(), inputs.mu()), inputs.yawRateDps());
    }

    /**
     * Validates the helispec fields required by the RAISE LTE formulas. Rows with blank values such as
     * {@code MR_inboard_blade_chord_in} must not be evaluated because they would produce invalid rotor solidity or
     * tip-speed calculations.
     */
    private static boolean isValidSpec(HelicopterSpec spec) {
        return spec != null
                && isFinite(spec.maxGrossWeightLbs())
                && spec.maxGrossWeightLbs() > 0.0
                && spec.mainRotorBlades() >= 2
                && isFinite(spec.mainRotorDiameterIn())
                && spec.mainRotorDiameterIn() > 0.0
                && isFinite(spec.mainRotorBladeChordIn())
                && spec.mainRotorBladeChordIn() > 0.0
                && isFinite(spec.mainRotorMaxContinuousRpm())
                && spec.mainRotorMaxContinuousRpm() > 0.0;
    }

    /**
     * Evaluates one hidden layer neuron-by-neuron using the RAISE transfer function:
     *
     * <pre>
     * output_i = tanh(0.5 * dot(input, weights_i))
     * </pre>
     */
    private static double[] evaluateLayer(double[] input, double[][] weights) {
        double[] output = new double[weights.length];
        for (int row = 0; row < weights.length; row++) {
            double value = 0.0;
            for (int col = 0; col < weights[row].length; col++) {
                value += input[col] * weights[row][col];
            }
            output[row] = Math.tanh(0.5 * value);
        }
        return output;
    }

    /** Appends the neural-network bias value {@code 1.0}. */
    private static double[] appendBias(double[] input) {
        double[] output = new double[input.length + 1];
        System.arraycopy(input, 0, output, 0, input.length);
        output[input.length] = 1.0;
        return output;
    }

    private static boolean isFinite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }
}