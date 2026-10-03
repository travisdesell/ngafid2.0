package org.ngafid.processor.events;

import static org.ngafid.core.flights.Parameters.UNIX_TIME_SECONDS;
import static org.ngafid.core.flights.Parameters.UTC_DATE_TIME;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.ngafid.core.event.Event;
import org.ngafid.core.event.EventDefinition;
import org.ngafid.core.flights.DoubleTimeSeries;
import org.ngafid.core.flights.Flight;
import org.ngafid.core.flights.StringTimeSeries;
import org.ngafid.core.util.ColumnNotAvailableException;

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

    /**
     * Constructs an event scanner for the given event definition.
     *
     * @param eventDefinition the event definition this scanner detects
     */
    public AbstractEventScanner(EventDefinition eventDefinition) {
        this.definition = eventDefinition;
    }

    /**
     * Scans a flight's time series for occurrences of this scanner's event definition and returns the events
     * detected. Implementations walk the supplied series (typically over the indices of the required columns),
     * apply the event's condition and duration/severity rules, and build an {@link Event} for each qualifying
     * span. The maps hold every series available for the flight, keyed by column name; implementations read only
     * the columns they require.
     *
     * @param doubleTimeSeries the flight's double-valued series, keyed by column name
     * @param stringTimeSeries the flight's string-valued series, keyed by column name
     * @return the detected events, in flight order; empty if the event never occurs
     * @throws SQLException if a database lookup performed during scanning fails
     */
    public abstract List<Event> scan(
            Map<String, DoubleTimeSeries> doubleTimeSeries, Map<String, StringTimeSeries> stringTimeSeries)
            throws SQLException;

    /**
     * Loads every time series this scanner requires from the given flight, failing fast if any is absent, so the
     * subsequent {@link #scan} runs against complete data. Each required double and string column is fetched
     * through the flight (which lazily reads it from the database and caches it); the first missing column raises
     * {@link ColumnNotAvailableException} so the caller can skip this scanner for the flight rather than scan with
     * incomplete data.
     *
     * @param connection the database connection used to load the columns
     * @param flight the flight whose required columns are loaded and validated
     * @throws ColumnNotAvailableException if a required double or string column is not present on the flight
     * @throws SQLException if loading a column from the database fails
     */
    public void gatherRequiredColumns(Connection connection, Flight flight)
            throws ColumnNotAvailableException, SQLException {
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
