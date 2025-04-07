package org.ngafid.events;

import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.StringTimeSeries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.ngafid.events.CustomEvent.getHighAltitudeSpin;
import static org.ngafid.events.CustomEvent.getLowAltitudeSpin;
import static org.ngafid.flights.Parameters.*;

public class SpinEventScanner extends AbstractEventScanner {
    private static final double ALT_AGL_LIMIT = 250;
    private static final int STOP_DELAY = 1;
    private static final double ALT_CONSTRAINT = 4000.d;
    private static final double AC_NORMAL = 0.1d;

    public SpinEventScanner(EventDefinition eventDefinition) {
        super(eventDefinition);
    }

    @Override
    protected List<String> getRequiredDoubleColumns() {
        return List.of(IAS, VSPD_CALCULATED, NORM_AC, LAT_AC, ALT_AGL);
    }

    @Override
    public List<Event> scan(Map<String, DoubleTimeSeries> doubleTimeSeries, Map<String, StringTimeSeries> stringTimeSeries) {
        ArrayList<Event> events = new ArrayList<>();

        DoubleTimeSeries ias = doubleTimeSeries.get(IAS);
        DoubleTimeSeries dVSI = doubleTimeSeries.get(VSPD_CALCULATED);

        DoubleTimeSeries normAc = doubleTimeSeries.get(NORM_AC);
        DoubleTimeSeries latAc = doubleTimeSeries.get(LAT_AC);
        DoubleTimeSeries altAGL = doubleTimeSeries.get(ALT_AGL);

        DoubleTimeSeries unixTime = doubleTimeSeries.get(UNIX_TIME_SECONDS);
        StringTimeSeries utcSeries = stringTimeSeries.get(UTC_DATE_TIME);

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
                            currentEvent = new CustomEvent(utcSeries.get(lowAirspeedIndex), utcSeries.get(i), lowAirspeedIndex, i, maxNormAc, null);
                            spinStartFound = true;
                        }
                    }

                    if (spinStartFound && (lowAirspeedIndexDiff > 3 && lowAirspeedIndexDiff <= 30)) {
                        if (instIAS > 50 && normAcRel < AC_NORMAL && latAcRel < AC_NORMAL) {
                            currentEvent.updateEnd(utcSeries.get(i), i);
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
                                events.add(currentEvent);
                            } else {
                                currentEvent.setDefinition(getHighAltitudeSpin());
                                events.add(currentEvent);
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

        return events;
    }
}
