package org.ngafid.processor.events;

import org.ngafid.core.event.CustomEvent;
import org.ngafid.core.event.Event;
import org.ngafid.core.event.EventDefinition;
import org.ngafid.core.flights.Airframes;
import org.ngafid.core.flights.DoubleTimeSeries;
import org.ngafid.core.flights.StringTimeSeries;

import java.util.List;
import java.util.Map;

import static org.ngafid.core.event.CustomEvent.LOW_FUEL_EVENT_THRESHOLDS;
import static org.ngafid.core.flights.Parameters.*;

public class LowEndingFuelScanner extends AbstractEventScanner {
    private final Airframes.Airframe airframe;

    public LowEndingFuelScanner(Airframes.Airframe airframe, EventDefinition eventDefinition) {
        super(eventDefinition);
        this.airframe = airframe;
    }

    @Override
    protected List<String> getRequiredDoubleColumns() {
        return List.of(TOTAL_FUEL);
    }

    @Override
    public List<Event> scan(Map<String, DoubleTimeSeries> doubleTimeSeries, Map<String, StringTimeSeries> stringTimeSeries) throws NullPointerException {
        Double threshold = LOW_FUEL_EVENT_THRESHOLDS.get(definition.getAirframeNameId());
        if (threshold == null) {
            throw new NullPointerException("No low fuel event threshold defined for airframe name id " + definition.getAirframeNameId());
        }

        DoubleTimeSeries fuel = doubleTimeSeries.get(TOTAL_FUEL);
        DoubleTimeSeries unixtime = doubleTimeSeries.get(UNIX_TIME_SECONDS);
        StringTimeSeries utc = stringTimeSeries.get(UTC_DATE_TIME);

        int i = unixtime.getLastValidIndex();
        double endTime = unixtime.get(i);
        String endUTC = utc.get(i);

        double currentTime;
        double duration = 0;
        double fuelSum = 0;
        int fuelValues = 0;

        for (; duration <= 15 && i >= 0; i--) {
            currentTime = unixtime.get(i);
            fuelSum += fuel.get(i);
            fuelValues++;

            duration = endTime - currentTime;
        }

        double average = (fuelSum / fuelValues);
        if (duration >= 15 && average < threshold) {
            return List.of(new CustomEvent(utc.get(i), endUTC, i, i + fuelValues, average, null, definition
            ));
        } else {
            return List.of();
        }
    }

}
