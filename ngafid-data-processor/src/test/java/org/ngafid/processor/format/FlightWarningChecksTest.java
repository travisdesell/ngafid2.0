package org.ngafid.processor.format;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.ngafid.core.flights.Airframes.AIRFRAME_CESSNA_172S;
import static org.ngafid.core.flights.Parameters.*;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.ngafid.core.flights.Airframes;
import org.ngafid.core.flights.DoubleTimeSeries;
import org.ngafid.core.flights.Flight;
import org.ngafid.core.flights.FlightMeta;
import org.ngafid.core.flights.StringTimeSeries;

public class FlightWarningChecksTest {
    @Test
    public void unixTimeAverageIntervalAboveThresholdCreatesWarning() {
        FlightBuilder builder = builderWithDoubleSeries(UNIX_TIME_SECONDS, 0.0, 1.2, 2.4, 3.6);

        FlightWarningChecks.addWarnings(builder, false);

        assertEquals(1, builder.exceptions.size());
        assertEquals(
                "Time series has frequency below 1Hz (avg interval 1.20s)",
                builder.exceptions.get(0).getMessage());
    }

    @Test
    public void unixTimeAverageIntervalAtOneSecondDoesNotCreateWarning() {
        FlightBuilder builder = builderWithDoubleSeries(UNIX_TIME_SECONDS, 0.0, 1.0, 2.0, 3.0);

        FlightWarningChecks.addWarnings(builder, false);

        assertTrue(builder.exceptions.isEmpty());
    }

    @Test
    public void frequencyCheckIgnoresDuplicateAndNanTimestamps() {
        FlightBuilder builder =
                builderWithDoubleSeries(UNIX_TIME_SECONDS, 0.0, 0.0, Double.NaN, 1.2, 1.2, 2.4);

        FlightWarningChecks.addWarnings(builder, false);

        assertEquals(1, builder.exceptions.size());
        assertEquals(
                "Time series has frequency below 1Hz (avg interval 1.20s)",
                builder.exceptions.get(0).getMessage());
    }

    @Test
    public void utcDateTimeFallbackCreatesFrequencyWarning() {
        StringTimeSeries utc = new StringTimeSeries(UTC_DATE_TIME, Unit.UTC_DATE_TIME);
        utc.add("2026-01-01T00:00:00Z");
        utc.add("");
        utc.add("not a timestamp");
        utc.add("2026-01-01T00:00:01.200Z");
        utc.add("2026-01-01T00:00:02.400Z");

        FlightBuilder builder = builderWithStringSeries(UTC_DATE_TIME, utc);

        FlightWarningChecks.addWarnings(builder, false);

        assertEquals(1, builder.exceptions.size());
        assertEquals(
                "Time series has frequency below 1Hz (avg interval 1.20s)",
                builder.exceptions.get(0).getMessage());
    }

    @Test
    public void warningsAppliedBeforeFlightConstructionSetWarningStatus() {
        FlightBuilder builder = builderWithDoubleSeries(UNIX_TIME_SECONDS, 0.0, 1.2, 2.4);

        FlightWarningChecks.addWarnings(builder, false);

        Flight flight = new Flight(
                builder.meta,
                builder.getDoubleTimeSeriesMap(),
                builder.getStringTimeSeriesMap(),
                builder.getItinerary(),
                builder.exceptions,
                builder.getEvents());

        assertEquals(Flight.FlightStatus.WARNING, flight.getStatus());
    }

    @Test
    public void missingFuelQuantityCreatesLegacyWarning() {
        FlightBuilder builder = standardBuilder();
        builder.addTimeSeries(doubleSeries(FUEL_QTY_RIGHT, 1.0, 2.0));

        FlightWarningChecks.addWarnings(builder, true);

        assertWarningPresent(builder, "Cannot calculate 'Total Fuel' as fuel parameter 'FQtyL' was missing.");
    }

    @Test
    public void missingCoordinatesAndAltitudeCreateLegacyWarnings() {
        FlightBuilder builder = standardBuilder();

        FlightWarningChecks.addWarnings(builder, true);

        assertWarningPresent(
                builder,
                "Cannot calculate AGL, flight file had empty or missing 'AltMSL', 'Latitude' and 'Longitude' columns.");
        assertWarningPresent(
                builder,
                "Cannot calculate airport and runway distances, flight file had empty or missing 'Latitude', "
                        + "'Longitude' and 'AltAGL' columns.");
    }

    @Test
    public void missingCessnaDivergenceColumnsCreateLegacyWarning() {
        FlightBuilder builder = standardBuilder();

        FlightWarningChecks.addWarnings(builder, true);

        assertWarningPresent(builder, "Cannot calculate 'E1 CHT Divergence' as parameter 'E1 CHT1' was missing.");
    }

    @Test
    public void parquetBuilderDoesNotEnableLegacyOptionalWarnings() {
        ParquetFlightBuilder builder = new ParquetFlightBuilder(meta(), new HashMap<>(), new HashMap<>());

        FlightWarningChecks.addWarnings(builder, builder.includeLegacyOptionalCalculationWarnings());

        assertTrue(builder.exceptions.isEmpty());
    }

    private static FlightBuilder builderWithDoubleSeries(String name, double... values) {
        Map<String, DoubleTimeSeries> doubleTimeSeries = new HashMap<>();
        doubleTimeSeries.put(name, doubleSeries(name, values));
        return new FlightBuilder(meta(), doubleTimeSeries, new HashMap<>());
    }

    private static FlightBuilder builderWithStringSeries(String name, StringTimeSeries timeSeries) {
        Map<String, StringTimeSeries> stringTimeSeries = new HashMap<>();
        stringTimeSeries.put(name, timeSeries);
        return new FlightBuilder(meta(), new HashMap<>(), stringTimeSeries);
    }

    private static FlightBuilder standardBuilder() {
        return new FlightBuilder(meta(), new HashMap<>(), new HashMap<>());
    }

    private static DoubleTimeSeries doubleSeries(String name, double... values) {
        return new DoubleTimeSeries(name, "double", values);
    }

    private static FlightMeta meta() {
        FlightMeta meta = new FlightMeta();
        meta.setFleetId(1);
        meta.setUploaderId(1);
        meta.setUploadId(1);
        meta.setFilename("warning-test.csv");
        meta.setSystemId("WARNING_TEST");
        meta.setMd5Hash("warning-test-md5");
        meta.setSuggestedTailNumber("N12345");
        meta.setAirframe(new Airframes.Airframe(AIRFRAME_CESSNA_172S, new Airframes.Type("Fixed Wing")));
        meta.setStartDateTime(OffsetDateTime.parse("2026-01-01T00:00:00Z"));
        meta.setEndDateTime(OffsetDateTime.parse("2026-01-01T00:01:00Z"));
        return meta;
    }

    private static void assertWarningPresent(FlightBuilder builder, String expectedMessage) {
        assertTrue(
                builder.exceptions.stream().anyMatch(exception -> expectedMessage.equals(exception.getMessage())),
                "Expected warning was not present: " + expectedMessage);
    }
}
