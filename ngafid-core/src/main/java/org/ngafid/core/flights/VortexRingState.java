package org.ngafid.core.flights;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Vortex Ring State (VRS) model support for rotorcraft processing.
 *
 * The core event boundary mirrors the RAISE Johnson-boundary implementation. Inputs are normalized by hover induced
 * velocity {@code Vh}: edgewise velocity {@code Vx/Vh} and axial velocity {@code Vz/Vh}. Severity {@code 3} is the
 * Johnson event boundary; severity {@code 2} is the RAISE risk boundary approximated as points within
 * {@value #DEFAULT_RISK_BUFFER_DISTANCE} nondimensional units of the nondimensional VRS polygon.
 */
public class VortexRingState {
    public static final int UNSUPPORTED = -1;
    public static final int NO_RISK = 0;
    public static final int RISK = 2;
    public static final int EVENT = 3;

    public static final double DEFAULT_RISK_BUFFER_DISTANCE = 0.2;

    private static final double INCH_TO_METER = 0.0254;
    private static final double KNOT_TO_METER_PER_SECOND = 0.514444;
    private static final double FOOT_PER_MINUTE_TO_METER_PER_SECOND = 0.00508;
    private static final double POUND_FORCE_TO_NEWTON = 4.4482216153;
    private static final double SEA_LEVEL_AIR_DENSITY_KG_M3 = 1.225;
    private static final double VXM = 0.95;
    private static final double VZN = -0.45;
    private static final double VZX = -1.5;
    private static final String DEFAULT_POLYGON_RESOURCE = "/vrs/vrs_polygon_non_dim.csv";

    private static volatile Boundary defaultBoundary;

    private VortexRingState() {}

    /**
     * Rotorcraft fields needed to derive hover induced velocity for the VRS model. Missing optional weights are stored
     * as {@link Double#NaN}; max gross weight and main rotor diameter are required.
     */
    public record HelicopterSpec(
            String airframe,
            double maxGrossWeightLbs,
            double minFlyingWeightLbs,
            double emptyWeightLbs,
            double mainRotorDiameterIn) {}

    /** Normalized VRS sample: hover induced velocity plus edgewise and axial components divided by Vh. */
    public record NormalizedInputs(double vhMps, double vxOverVh, double vzOverVh) {}

    /** One nondimensional point in the VRS risk polygon. */
    public record Point(double x, double y) {}

    /** Nondimensional VRS polygon used for the severity-2 risk buffer. */
    public static final class Boundary {
        private final List<Point> points;

        /** Creates an immutable polygon boundary and rejects unusable point sets. */
        private Boundary(List<Point> points) {
            if (points.size() < 3) {
                throw new IllegalArgumentException("VRS boundary must contain at least three points.");
            }
            this.points = List.copyOf(points);
        }

        /** Loads a VRS boundary CSV from the classpath so packaged resources and tests use the same parser. */
        public static Boundary fromCsvResource(String resourcePath) {
            try (InputStream stream = VortexRingState.class.getResourceAsStream(resourcePath)) {
                if (stream == null) {
                    throw new IllegalStateException("Missing VRS boundary resource: " + resourcePath);
                }
                return fromCsv(stream);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to load VRS boundary resource: " + resourcePath, e);
            }
        }

        /** Parses a two-column CSV of nondimensional x/y points into a closed polygon boundary. */
        public static Boundary fromCsv(InputStream stream) throws IOException {
            List<Point> points = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line = reader.readLine();
                if (line == null || !line.contains(",")) {
                    throw new IOException("VRS boundary CSV is empty or missing a header row.");
                }
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) {
                        continue;
                    }
                    String[] parts = trimmed.split(",");
                    if (parts.length < 2) {
                        continue;
                    }
                    points.add(new Point(Double.parseDouble(parts[0]), Double.parseDouble(parts[1])));
                }
            }
            return new Boundary(points);
        }

        /** Returns true when the normalized sample lies inside the polygon using the ray-casting test. */
        public boolean contains(double x, double y) {
            boolean inside = false;
            int j = points.size() - 1;
            for (int i = 0; i < points.size(); i++) {
                Point pi = points.get(i);
                Point pj = points.get(j);
                boolean intersects = ((pi.y() > y) != (pj.y() > y))
                        && (x < (pj.x() - pi.x()) * (y - pi.y()) / (pj.y() - pi.y()) + pi.x());
                if (intersects) {
                    inside = !inside;
                }
                j = i;
            }
            return inside;
        }

        /** Returns zero for points inside the boundary, otherwise the shortest distance to a polygon segment. */
        public double distanceTo(double x, double y) {
            if (contains(x, y)) {
                return 0.0;
            }
            double min = Double.POSITIVE_INFINITY;
            Point previous = points.get(points.size() - 1);
            for (Point current : points) {
                min = Math.min(min, distanceToSegment(x, y, previous, current));
                previous = current;
            }
            return min;
        }

        /** Exposes the loaded point count for resource validation and tests. */
        public int pointCount() {
            return points.size();
        }
    }

    /** Lazily loads and caches the default nondimensional VRS risk polygon resource. */
    public static Boundary defaultBoundary() {
        Boundary cached = defaultBoundary;
        if (cached == null) {
            synchronized (VortexRingState.class) {
                cached = defaultBoundary;
                if (cached == null) {
                    cached = Boundary.fromCsvResource(DEFAULT_POLYGON_RESOURCE);
                    defaultBoundary = cached;
                }
            }
        }
        return cached;
    }

    /** Estimates operating weight as 25% payload above the best available lower weight, matching RAISE behavior. */
    public static double bestWeightLbs(HelicopterSpec spec) {
        if (!Double.isNaN(spec.minFlyingWeightLbs()) && spec.minFlyingWeightLbs() > 0.0) {
            return ((spec.maxGrossWeightLbs() - spec.minFlyingWeightLbs()) * 0.25) + spec.minFlyingWeightLbs();
        }
        if (!Double.isNaN(spec.emptyWeightLbs()) && spec.emptyWeightLbs() > 0.0) {
            return ((spec.maxGrossWeightLbs() - spec.emptyWeightLbs()) * 0.25) + spec.emptyWeightLbs();
        }
        return spec.maxGrossWeightLbs();
    }

    /** Estimates air density from MSL altitude; missing altitude falls back to sea-level standard density. */
    public static double calculatedAirDensityKgM3(double altitudeMslFeet) {
        if (Double.isNaN(altitudeMslFeet)) {
            return SEA_LEVEL_AIR_DENSITY_KG_M3;
        }
        return SEA_LEVEL_AIR_DENSITY_KG_M3 * Math.exp((-0.0296 * altitudeMslFeet * 0.3048) / 304.8);
    }

    /**
     * Converts TAS, vertical speed, density, and rotorcraft specs into Johnson-boundary nondimensional inputs. Returns
     * empty when required values are missing, invalid, or produce a nonphysical hover induced velocity.
     */
    public static Optional<NormalizedInputs> normalize(
            HelicopterSpec spec,
            double trueAirspeedKt,
            double verticalSpeedFpm,
            double airDensityKgM3,
            double airframeGrossWeightLbs) {
        if (!isValidSpec(spec)
                || !isFinite(trueAirspeedKt)
                || !isFinite(verticalSpeedFpm)
                || !isFinite(airDensityKgM3)
                || airDensityKgM3 <= 0.0) {
            return Optional.empty();
        }

        double radiusM = spec.mainRotorDiameterIn() / 2.0 * INCH_TO_METER;
        double rotorAreaM2 = Math.PI * radiusM * radiusM;
        double weightLbs = isFinite(airframeGrossWeightLbs) && airframeGrossWeightLbs > 0.0
                ? airframeGrossWeightLbs
                : bestWeightLbs(spec);
        double weightN = weightLbs * POUND_FORCE_TO_NEWTON;
        double vhMps = Math.sqrt(weightN / (2.0 * rotorAreaM2 * airDensityKgM3));
        if (!isFinite(vhMps) || vhMps <= 0.0) {
            return Optional.empty();
        }

        double vx = trueAirspeedKt * KNOT_TO_METER_PER_SECOND / vhMps;
        double vz = verticalSpeedFpm * FOOT_PER_MINUTE_TO_METER_PER_SECOND / vhMps;
        return Optional.of(new NormalizedInputs(vhMps, vx, vz));
    }

    /** Classifies normalized inputs with the default risk polygon and default severity-2 buffer distance. */
    public static int classify(NormalizedInputs inputs) {
        return classify(inputs, defaultBoundary(), DEFAULT_RISK_BUFFER_DISTANCE);
    }

    /**
     * Classifies one normalized sample as unsupported, no risk, risk, or event using Johnson event logic first and the
     * polygon-distance risk buffer second.
     */
    public static int classify(NormalizedInputs inputs, Boundary riskBoundary, double riskBufferDistance) {
        if (inputs == null || !isFinite(inputs.vxOverVh()) || !isFinite(inputs.vzOverVh())) {
            return UNSUPPORTED;
        }
        double vx = inputs.vxOverVh();
        double vz = inputs.vzOverVh();
        if (isJohnsonEvent(vx, vz)) {
            return EVENT;
        }
        if (riskBoundary != null && riskBoundary.distanceTo(vx, vz) <= riskBufferDistance) {
            return RISK;
        }
        return NO_RISK;
    }

    /** Evaluates the Johnson VRS event envelope for nondimensional edgewise and axial velocity components. */
    public static boolean isJohnsonEvent(double vx, double vz) {
        if (!isFinite(vx) || !isFinite(vz) || vx < 0.0 || vx >= VXM) {
            return false;
        }
        double ratio = vx / VXM;
        double oneMinusRatioSquared = 1.0 - (ratio * ratio);
        if (oneMinusRatioSquared < 0.0) {
            return false;
        }
        double upper = 0.5 * (VZN + VZX) + 0.5 * (VZN - VZX) * Math.pow(oneMinusRatioSquared, 0.2);
        double lower = 0.5 * (VZN + VZX) - 0.5 * (VZN - VZX) * Math.pow(oneMinusRatioSquared, 1.5);
        return vz <= upper && vz >= lower;
    }

    /** Computes shortest Euclidean distance from a normalized sample to a polygon segment. */
    private static double distanceToSegment(double x, double y, Point a, Point b) {
        double dx = b.x() - a.x();
        double dy = b.y() - a.y();
        if (dx == 0.0 && dy == 0.0) {
            return Math.hypot(x - a.x(), y - a.y());
        }
        double t = ((x - a.x()) * dx + (y - a.y()) * dy) / (dx * dx + dy * dy);
        t = Math.max(0.0, Math.min(1.0, t));
        double projectionX = a.x() + t * dx;
        double projectionY = a.y() + t * dy;
        return Math.hypot(x - projectionX, y - projectionY);
    }

    /** Checks only the rotorcraft spec fields that are mandatory for Vh calculation. */
    private static boolean isValidSpec(HelicopterSpec spec) {
        return spec != null
                && isFinite(spec.maxGrossWeightLbs())
                && spec.maxGrossWeightLbs() > 0.0
                && isFinite(spec.mainRotorDiameterIn())
                && spec.mainRotorDiameterIn() > 0.0;
    }

    /** Keeps NaN and infinities from entering geometric and physics calculations. */
    private static boolean isFinite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }
}
