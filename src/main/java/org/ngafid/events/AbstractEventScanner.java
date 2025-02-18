package org.ngafid.events;

import org.ngafid.common.ColumnNotAvailableException;
import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.flights.Flight;
import org.ngafid.flights.StringTimeSeries;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.ngafid.flights.Parameters.LCL_DATE;
import static org.ngafid.flights.Parameters.LCL_TIME;

public abstract class AbstractEventScanner {

    protected final EventDefinition definition;

    protected List<String> getRequiredDoubleColumns() {
        return List.of();
    }

    protected List<String> getRequiredStringColumns() {
        return List.of(LCL_DATE, LCL_TIME);
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
