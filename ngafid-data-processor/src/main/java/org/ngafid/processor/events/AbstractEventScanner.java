package org.ngafid.processor.events;

import org.ngafid.core.event.Event;
import org.ngafid.core.event.EventDefinition;
import org.ngafid.core.flights.DoubleTimeSeries;
import org.ngafid.core.flights.Flight;
import org.ngafid.core.flights.StringTimeSeries;
import org.ngafid.core.util.ColumnNotAvailableException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.ngafid.core.flights.Parameters.UNIX_TIME_SECONDS;
import static org.ngafid.core.flights.Parameters.UTC_DATE_TIME;

/**
 * Abstract definition of an event scanner. An event scanner is exactly what it sounds like -- it scans flight data for
 * events. This is not done directly on a Flight object so that non-finalized flights can be scanned as a part of the
 * flight processing pipeline. Each event that is applicable to a flight will have a scanner created for it, and this
 * for this scanner a ComputeEvent object will be created and this will be added to the DependencyGraph that will be
 * resolved while processing the flight data. See `Pipeline` for more details.
 */
public abstract class AbstractEventScanner {

    protected final EventDefinition definition;

    protected List<String> getRequiredDoubleColumns() {
        return List.of(UNIX_TIME_SECONDS);
    }

    protected List<String> getRequiredStringColumns() {
        return List.of(UTC_DATE_TIME);
    }

    public AbstractEventScanner(EventDefinition eventDefinition) {
        this.definition = eventDefinition;
    }

    public abstract List<Event> scan(Map<String, DoubleTimeSeries> doubleTimeSeries,
                                     Map<String, StringTimeSeries> stringTimeSeries) throws SQLException;

    public void gatherRequiredColumns(Connection connection, Flight flight) throws ColumnNotAvailableException, SQLException {
        for (var doubleColumnName : getRequiredDoubleColumns()) {
            var col = flight.getDoubleTimeSeries(connection, doubleColumnName);
            if (col == null)
                throw new ColumnNotAvailableException("Required column " + doubleColumnName + " not found");
        }

        for (var stringColumnName : getRequiredStringColumns()) {
            var col = flight.getStringTimeSeries(connection, stringColumnName);
            if (col == null)
                throw new ColumnNotAvailableException("Required column " + stringColumnName + " not found");
        }
    }
}
