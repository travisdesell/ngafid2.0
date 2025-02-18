package org.ngafid.events;

import org.ngafid.common.filters.Conditional;
import org.ngafid.common.filters.Filter;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.Parameters;
import org.ngafid.flights.StringTimeSeries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Used to scan a flight for an event.
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
        StringTimeSeries dateSeries = stringSeries.get(Parameters.LCL_DATE);
        StringTimeSeries timeSeries = stringSeries.get(Parameters.LCL_TIME);

        reset();

        List<Event> eventList = new ArrayList<>();

        // skip the first 30 seconds as that is usually the FDR being initialized
        for (int i = 30; i < dateSeries.size(); i++) {
            // for (i = 0; i < doubleSeries[0].size(); i++) {
            lineNumber = i;

            // LOG.info("Pre-set conditional: " + conditional.toString());

            conditional.reset();
            for (Map.Entry<String, DoubleTimeSeries> entry : doubleSeries.entrySet()) {
                conditional.set(entry.getKey(), entry.getValue().get(i));
            }
            // LOG.info("Post-set conditional: " + conditional.toString());

            boolean result = conditional.evaluate();

            // LOG.info(conditional + ", result: " + result);

            if (!result) {
                if (startTime != null) {
                    // we're tracking an event, so increment the stopCount
                    stopCount++;
                    LOG.info("stopCount: " + stopCount + " with on line: " + lineNumber);

                    if (stopCount == stopBuffer) {
                        System.err.println("Stop count (" + stopCount + ") reached the stop buffer (" + stopBuffer
                                + "), new event created!");

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
                    startTime = dateSeries.get(i) + " " + timeSeries.get(i);
                    startLine = lineNumber;
                    severity = definition.getSeverity(doubleSeries, i);

                    LOG.info("start date time: " + startTime + ", start line number: " + startLine);
                }
                endLine = lineNumber;
                endTime = dateSeries.get(i) + " " + timeSeries.get(i);
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
