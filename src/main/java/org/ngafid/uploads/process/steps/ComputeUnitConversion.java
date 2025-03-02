package org.ngafid.uploads.process.steps;

import org.ngafid.flights.DoubleTimeSeries;
import org.ngafid.uploads.process.FatalFlightFileException;
import org.ngafid.uploads.process.MalformedFlightFileException;
import org.ngafid.uploads.process.format.FlightBuilder;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

/**
 * A generic step to convert units from one type to another. As of right now, this is only being done with simple
 * conversions that only require a multiplication factor, but it could be extended to support non-linear transformations.
 */
public class ComputeUnitConversion extends ComputeStep {

    public enum UnitConversion {
        METERS_TO_FEET(3.28084),
        RADIAN_TO_DEGREE(180 / Math.PI);

        private final double value;

        private UnitConversion(double value) {
            this.value = value;
        }

        public double getConversionFactor() {
            return value;
        }

        public String getOutputUnit() {
            return switch (this) {
                case METERS_TO_FEET -> "feet";
                case RADIAN_TO_DEGREE -> "degrees";
            };
        }
    }

    private final String inSeriesName;
    private final String outSeriesName;
    private final UnitConversion unitConversion;

    public ComputeUnitConversion(Connection connection, FlightBuilder builder, String inParam, String outSeries, UnitConversion conversion) {
        super(connection, builder);
        inSeriesName = inParam;
        outSeriesName = outSeries;
        unitConversion = conversion;
    }

    @Override
    public Set<String> getRequiredDoubleColumns() {
        return Set.of(inSeriesName);
    }

    @Override
    public Set<String> getRequiredStringColumns() {
        return Set.of();
    }

    @Override
    public Set<String> getRequiredColumns() {
        return Set.of(inSeriesName);
    }

    @Override
    public Set<String> getOutputColumns() {
        return Set.of(outSeriesName);
    }

    private record UnitConverter(UnitConversion conversion,
                                 DoubleTimeSeries series) implements DoubleTimeSeries.TimeStepCalculation {

        @Override
        public double compute(int i) {
            double value = series.get(i);

            if (Double.isNaN(value))
                return Double.NaN;

            return value * conversion.getConversionFactor();
        }
    }

    @Override
    public void compute() throws SQLException, MalformedFlightFileException, FatalFlightFileException {
        DoubleTimeSeries inputSeries = builder.getDoubleTimeSeries(inSeriesName);
        DoubleTimeSeries convertedSeries = DoubleTimeSeries.computed(outSeriesName, unitConversion.getOutputUnit(), inputSeries.size(), new UnitConverter(unitConversion, inputSeries));
        builder.addTimeSeries(convertedSeries);
    }
}
