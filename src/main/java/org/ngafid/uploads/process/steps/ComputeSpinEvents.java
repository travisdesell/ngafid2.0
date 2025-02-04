package org.ngafid.uploads.process.steps;

import org.ngafid.events.CustomEvent;
import org.ngafid.events.calculations.CalculatedDoubleTimeSeries;
import org.ngafid.events.calculations.VSPDRegression;
import org.ngafid.flights.Airframes;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.StringTimeSeries;
import org.ngafid.uploads.process.format.FlightBuilder;

import java.sql.Connection;
import java.util.HashSet;
import java.util.Set;

import static org.ngafid.events.CustomEvent.getHighAltitudeSpin;
import static org.ngafid.events.CustomEvent.getLowAltitudeSpin;
import static org.ngafid.flights.Parameters.*;

public class ComputeSpinEvents extends ComputeStep {
    private static final double ALT_AGL_LIMIT = 250;
    private static final int STOP_DELAY = 1;
    private static final double ALT_CONSTRAINT = 4000.d;
    private static final double AC_NORMAL = 0.1d;

    public ComputeSpinEvents(Connection connection, FlightBuilder builder) {
        super(connection, builder);
    }

    @Override
    public Set<String> getRequiredDoubleColumns() {
        return Set.of(IAS, VSPD_CALCULATED, NORM_AC, LAT_AC, ALT_AGL, ALT_B);
    }

    @Override
    public Set<String> getRequiredStringColumns() {
        return Set.of(LCL_DATE, LCL_TIME);
    }

    @Override
    public Set<String> getRequiredColumns() {
        var set = new HashSet<>(getRequiredDoubleColumns());
        set.addAll(getRequiredStringColumns());
        return set;
    }

    @Override
    public Set<String> getOutputColumns() {
        return Set.of();
    }

    @Override
    public boolean airframeIsValid(Airframes.Airframe airframe) {
        return builder.meta.airframeType.getName().equals("Fixed Wing");
    }

    private DoubleTimeSeries getVSPDDerived() {
        CalculatedDoubleTimeSeries dVSI =
                new CalculatedDoubleTimeSeries(VSPD_CALCULATED, "ft/min", false, null);
        dVSI.create(new VSPDRegression(builder.getDoubleTimeSeries(ALT_B)));
        return dVSI;
    }

    @Override
    public void compute() {
        DoubleTimeSeries ias = builder.getDoubleTimeSeries(IAS);
        DoubleTimeSeries dVSI = getVSPDDerived();
        DoubleTimeSeries normAc = builder.getDoubleTimeSeries(NORM_AC);
        DoubleTimeSeries latAc = builder.getDoubleTimeSeries(LAT_AC);
        DoubleTimeSeries altAGL = builder.getDoubleTimeSeries(ALT_AGL);

        StringTimeSeries dateSeries = builder.getStringTimeSeries(LCL_DATE);
        StringTimeSeries timeSeries = builder.getStringTimeSeries(LCL_TIME);

        boolean airspeedIsLow = false;
        boolean spinStartFound = false;
        boolean altCstrViolated = false;

        double maxNormAc = 0.d;

        int lowAirspeedIndex = -1;
        int endSpinSeconds = 0;

        CustomEvent currentEvent = null;

        for (int i = 0; i < ias.size(); i++) {
            // Get instantaneous altitudes
            double instIAS = ias.get(i);
            double instVSI = dVSI.get(i);
            double instAlt = altAGL.get(i);
            double normAcRel = Math.abs(normAc.get(i));
            double latAcRel = Math.abs(latAc.get(i));

            if (instAlt > ALT_AGL_LIMIT) {
                if (!airspeedIsLow && instIAS < 50) {
                    airspeedIsLow = true;
                    lowAirspeedIndex = i;
                }

                if (airspeedIsLow) {
                    int lowAirspeedIndexDiff = i - lowAirspeedIndex;

                    // check for severity
                    if (normAcRel > maxNormAc) {
                        maxNormAc = normAcRel;
                    }

                    if (instAlt < ALT_CONSTRAINT) {
                        altCstrViolated = true;
                    }

                    if (lowAirspeedIndexDiff <= 2 && instVSI <= -3500) {
                        if (!spinStartFound) {
                            String start = dateSeries.get(lowAirspeedIndex) + " " + timeSeries.get(lowAirspeedIndex);
                            String end = dateSeries.get(i) + " " + timeSeries.get(i);

                            currentEvent = new CustomEvent(start, end, lowAirspeedIndex, i, maxNormAc, null);

                            spinStartFound = true;
                        }
                    }

                    if (spinStartFound && (lowAirspeedIndexDiff > 3 && lowAirspeedIndexDiff <= 30)) {
                        // System.out.println("Looking for end of spin");

                        if (instIAS > 50 && normAcRel < AC_NORMAL && latAcRel < AC_NORMAL) {
                            String endTime = dateSeries.get(i) + " " + timeSeries.get(i);
                            currentEvent.updateEnd(endTime, i);

                            ++endSpinSeconds;
                        }
                    }

                    if (!spinStartFound && lowAirspeedIndexDiff >= 4) {
                        currentEvent = null;
                    }

                    if (endSpinSeconds >= STOP_DELAY || currentEvent == null) {
                        if (currentEvent != null) {
                            if (altCstrViolated) {
                                currentEvent.setDefinition(getLowAltitudeSpin());
                                builder.emitEvent(currentEvent);
                            } else {
                                currentEvent.setDefinition(getHighAltitudeSpin());
                                builder.emitEvent(currentEvent);
                            }
                        }

                        spinStartFound = false;
                        airspeedIsLow = false;
                        altCstrViolated = false;

                        lowAirspeedIndex = -1;
                        endSpinSeconds = 0;
                        maxNormAc = 0.d;
                    }
                }
            }
        }
    }
}
