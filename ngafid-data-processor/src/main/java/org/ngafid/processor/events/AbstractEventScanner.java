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
