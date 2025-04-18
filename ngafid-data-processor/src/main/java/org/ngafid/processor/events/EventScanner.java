package org.ngafid.processor.events;

import org.ngafid.core.event.Event;
import org.ngafid.core.event.EventDefinition;
import org.ngafid.core.flights.DoubleTimeSeries;
import org.ngafid.core.flights.Parameters;
import org.ngafid.core.flights.StringTimeSeries;
import org.ngafid.core.util.filters.Conditional;
import org.ngafid.core.util.filters.Filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Scans a flight for a normal event, i.e. an event defined with a `Conditional`.
 */
public class EventScanner extends AbstractEventScanner {
    private static final Logger LOG = Logger.getLogger(EventScanner.class.getName());

    private final Conditional conditional;
    private final int startBuffer;
    private final int stopBuffer;

    private int lineNumber;
    private String startTime;
    private String endTime;
    private int startLine;
    private int endLine;

    private int startCount;
    private int stopCount;
    private double severity;

    public EventScanner(EventDefinition eventDefinition) {
        super(eventDefinition);

        Filter filter = eventDefinition.getFilter();
        this.conditional = new Conditional(filter);
        this.startBuffer = eventDefinition.getStartBuffer();
        this.stopBuffer = eventDefinition.getStopBuffer();
    }

    private void reset() {
        lineNumber = 0;
        startTime = null;
        endTime = null;
        startLine = -1;
        endLine = -1;
        startCount = 0;
        stopCount = 0;
        severity = 0;
    }

    @Override
    protected List<String> getRequiredDoubleColumns() {
        return new ArrayList<>(definition.getColumnNames());
    }

    @Override
    public List<Event> scan(Map<String, DoubleTimeSeries> doubleSeries,
                            Map<String, StringTimeSeries> stringSeries) {
        StringTimeSeries utcSeries = stringSeries.get(Parameters.UTC_DATE_TIME);

        reset();

        List<Event> eventList = new ArrayList<>();

        for (int i = 30; i < utcSeries.size(); i++) {
            lineNumber = i;

            conditional.reset();
            for (Map.Entry<String, DoubleTimeSeries> entry : doubleSeries.entrySet()) {
                conditional.set(entry.getKey(), entry.getValue().get(i));
            }

            boolean result = conditional.evaluate();

            if (!result) {
                if (startTime != null) {
                    // we're tracking an event, so increment the stopCount
                    stopCount++;

                    if (stopCount == stopBuffer) {

                        if (startCount >= startBuffer) {
                            // we had enough triggers to reach the start count so create the event
                            Event event = new Event(startTime, endTime, startLine, endLine, definition.getId(), severity);
                            event.setEventDefinitionId(definition.getId());
                            eventList.add(event);
                        }

                        // reset the event values
                        startTime = null;
                        endTime = null;
                        startLine = -1;
                        endLine = -1;

                        // reset the start and stop counts
                        startCount = 0;
                        stopCount = 0;
                    }
                }
            } else {
                // row triggered exceedence

                // startTime is null if an exceedence is not being tracked
                if (startTime == null) {
                    startTime = utcSeries.get(i);
                    startLine = lineNumber;
                    severity = definition.getSeverity(doubleSeries, i);
                }

                endLine = lineNumber;
                endTime = utcSeries.get(i);
                severity = definition.getSeverity(doubleSeries, severity, i);

                // increment the startCount, reset the endCount
                startCount++;
                stopCount = 0;
            }
        }

        if (startTime != null) {
            Event event = new Event(startTime, endTime, startLine, endLine, definition.getId(), severity);
            eventList.add(event);
        }

        return eventList;
    }
}
