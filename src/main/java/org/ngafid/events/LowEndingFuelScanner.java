package org.ngafid.events;

import org.ngafid.common.TimeUtils;
import org.ngafid.flights.Airframes;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.StringTimeSeries;

import java.util.List;
import java.util.Map;

import static org.eclipse.jetty.http2.hpack.HpackDecoder.LOG;
import static org.ngafid.events.CustomEvent.LOW_FUEL_EVENT_THRESHOLDS;
import static org.ngafid.flights.Parameters.*;

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
        StringTimeSeries date = stringTimeSeries.get(LCL_DATE);
        StringTimeSeries time = stringTimeSeries.get(LCL_TIME);

        String[] lastValidDateAndIndex = date.getLastValidAndIndex();
        int i = Integer.parseInt(lastValidDateAndIndex[1]);

        String endTime = lastValidDateAndIndex[0] + " " + time.getLastValid();

        String currentTime = endTime;
        double duration = 0;
        double fuelSum = 0;
        int fuelValues = 0;

        for (; duration <= 15 && i >= 0; i--) {
            currentTime = date.get(i) + " " + time.get(i);
            fuelSum += fuel.get(i);
            fuelValues++;

            if (currentTime.equals(" ")) {
                continue;
            }

            LOG.info("DATE = " + currentTime);
            try {
                duration = TimeUtils.calculateDurationInSeconds(currentTime, endTime);
            } catch (TimeUtils.UnrecognizedDateTimeFormatException e) {
                LOG.info("Could not parse date: " + currentTime);
                LOG.info(e.getMessage());
                e.printStackTrace();
                return List.of();
            }
        }

        double average = (fuelSum / fuelValues);
        if (duration >= 15 && average < threshold) {
            return List.of(new CustomEvent(currentTime, endTime, i, i + fuelValues, average, null, definition));
        } else {
            return List.of();
        }
    }

}
