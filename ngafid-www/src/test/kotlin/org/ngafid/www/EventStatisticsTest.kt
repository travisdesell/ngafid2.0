package org.ngafid.www

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.DriverManager
import java.time.LocalDate

class EventStatisticsTest {
    @Test
    fun monthlyEventCountsUseOneAggregateFlightDenominatorAcrossFleetRows() {
        createConnection().use { connection ->
            createSchema(connection)
            insertMonthlyFlightCounts(connection)
            insertMonthlyEventCounts(connection)
            insertRawAnyEventData(connection)

            val counts =
                EventStatistics.getMonthlyEventCounts(
                    connection,
                    1,
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2025, 3, 1),
                    mapOf(AIRFRAME_ID to AIRFRAME_NAME),
                    mapOf(EVENT_DEFINITION_ID to EVENT_NAME),
                )

            val hardLanding = counts.getValue(EVENT_NAME).getValue(AIRFRAME_NAME)
            assertEquals(listOf("2025-01-01", "2025-02-01"), dates(hardLanding))
            assertArrayEquals(intArrayOf(6, 0), intArray(hardLanding, "aggregateFlightsWithEventCounts"))
            assertArrayEquals(intArrayOf(8, 0), intArray(hardLanding, "aggregateTotalEventsCounts"))
            assertArrayEquals(intArrayOf(300, 30), intArray(hardLanding, "aggregateTotalFlightsCounts"))
            assertArrayEquals(intArrayOf(2, 0), intArray(hardLanding, "flightsWithEventCounts"))
            assertArrayEquals(intArrayOf(3, 0), intArray(hardLanding, "totalEventsCounts"))
            assertArrayEquals(intArrayOf(100, 10), intArray(hardLanding, "totalFlightsCounts"))

            val anyEvent = counts.getValue("ANY Event").getValue(AIRFRAME_NAME)
            assertEquals(listOf("2025-01-01", "2025-02-01"), dates(anyEvent))
            assertArrayEquals(intArrayOf(4, 0), intArray(anyEvent, "aggregateFlightsWithEventCounts"))
            assertArrayEquals(intArrayOf(5, 0), intArray(anyEvent, "aggregateTotalEventsCounts"))
            assertArrayEquals(intArrayOf(300, 30), intArray(anyEvent, "aggregateTotalFlightsCounts"))
            assertArrayEquals(intArrayOf(2, 0), intArray(anyEvent, "flightsWithEventCounts"))
            assertArrayEquals(intArrayOf(3, 0), intArray(anyEvent, "totalEventsCounts"))
            assertArrayEquals(intArrayOf(100, 10), intArray(anyEvent, "totalFlightsCounts"))
        }
    }

    @Test
    fun aggregatePageMonthlyEventCountsDoNotPopulateFleetSpecificAnyEventCounts() {
        createConnection().use { connection ->
            createSchema(connection)
            insertMonthlyFlightCounts(connection)
            insertMonthlyEventCounts(connection)
            insertRawAnyEventData(connection)

            val counts =
                EventStatistics.getMonthlyEventCounts(
                    connection,
                    -1,
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2025, 3, 1),
                    mapOf(AIRFRAME_ID to AIRFRAME_NAME),
                    mapOf(EVENT_DEFINITION_ID to EVENT_NAME),
                )

            val anyEvent = counts.getValue("ANY Event").getValue(AIRFRAME_NAME)
            assertArrayEquals(intArrayOf(4, 0), intArray(anyEvent, "aggregateFlightsWithEventCounts"))
            assertArrayEquals(intArrayOf(5, 0), intArray(anyEvent, "aggregateTotalEventsCounts"))
            assertArrayEquals(intArrayOf(300, 30), intArray(anyEvent, "aggregateTotalFlightsCounts"))
            assertArrayEquals(intArrayOf(0, 0), intArray(anyEvent, "flightsWithEventCounts"))
            assertArrayEquals(intArrayOf(0, 0), intArray(anyEvent, "totalEventsCounts"))
            assertArrayEquals(intArrayOf(0, 0), intArray(anyEvent, "totalFlightsCounts"))
        }
    }

    @Test
    fun anyEventMonthlyCountsIncludeFlightOnlyMonthsWhenNoEventsExistInRange() {
        createConnection().use { connection ->
            createSchema(connection)
            insertMonthlyFlightCounts(connection)

            val counts =
                EventStatistics.getMonthlyEventCounts(
                    connection,
                    1,
                    LocalDate.of(2025, 2, 1),
                    LocalDate.of(2025, 3, 1),
                    mapOf(AIRFRAME_ID to AIRFRAME_NAME),
                    mapOf(EVENT_DEFINITION_ID to EVENT_NAME),
                )

            val anyEvent = counts.getValue("ANY Event").getValue(AIRFRAME_NAME)
            assertEquals(listOf("2025-02-01"), dates(anyEvent))
            assertArrayEquals(intArrayOf(0), intArray(anyEvent, "aggregateFlightsWithEventCounts"))
            assertArrayEquals(intArrayOf(0), intArray(anyEvent, "aggregateTotalEventsCounts"))
            assertArrayEquals(intArrayOf(30), intArray(anyEvent, "aggregateTotalFlightsCounts"))
            assertArrayEquals(intArrayOf(0), intArray(anyEvent, "flightsWithEventCounts"))
            assertArrayEquals(intArrayOf(0), intArray(anyEvent, "totalEventsCounts"))
            assertArrayEquals(intArrayOf(10), intArray(anyEvent, "totalFlightsCounts"))
        }
    }

    private fun createConnection(): Connection =
        DriverManager.getConnection(
            "jdbc:h2:mem:${System.nanoTime()};MODE=MYSQL;NON_KEYWORDS=YEAR,MONTH;DATABASE_TO_UPPER=FALSE",
        )

    private fun createSchema(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.execute(
                """
                CREATE TABLE m_fleet_monthly_flight_counts (
                    fleet_id INT NOT NULL,
                    airframe_id INT NOT NULL,
                    year INT NOT NULL,
                    month INT NOT NULL,
                    count INT NOT NULL
                )
                """.trimIndent(),
            )
            statement.execute(
                """
                CREATE TABLE m_fleet_airframe_monthly_event_counts (
                    fleet_id INT NOT NULL,
                    event_definition_id INT NOT NULL,
                    airframe_id INT NOT NULL,
                    year INT NOT NULL,
                    month INT NOT NULL,
                    event_count INT NOT NULL,
                    flight_count INT NOT NULL
                )
                """.trimIndent(),
            )
            statement.execute(
                """
                CREATE TABLE flights (
                    id INT NOT NULL,
                    airframe_id INT NOT NULL
                )
                """.trimIndent(),
            )
            statement.execute(
                """
                CREATE TABLE events (
                    id INT NOT NULL,
                    fleet_id INT NOT NULL,
                    flight_id INT NOT NULL,
                    start_time TIMESTAMP NOT NULL
                )
                """.trimIndent(),
            )
        }
    }

    private fun insertMonthlyFlightCounts(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.execute(
                """
                INSERT INTO m_fleet_monthly_flight_counts
                    (fleet_id, airframe_id, year, month, count)
                VALUES
                    (1, 10, 2025, 1, 100),
                    (2, 10, 2025, 1, 200),
                    (1, 10, 2025, 2, 10),
                    (2, 10, 2025, 2, 20)
                """.trimIndent(),
            )
        }
    }

    private fun insertMonthlyEventCounts(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.execute(
                """
                INSERT INTO m_fleet_airframe_monthly_event_counts
                    (fleet_id, event_definition_id, airframe_id, year, month, event_count, flight_count)
                VALUES
                    (1, 7, 10, 2025, 1, 3, 2),
                    (2, 7, 10, 2025, 1, 5, 4)
                """.trimIndent(),
            )
        }
    }

    private fun insertRawAnyEventData(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.execute(
                """
                INSERT INTO flights (id, airframe_id)
                VALUES
                    (1, 10),
                    (2, 10),
                    (3, 10),
                    (4, 10)
                """.trimIndent(),
            )
            statement.execute(
                """
                INSERT INTO events (id, fleet_id, flight_id, start_time)
                VALUES
                    (101, 1, 1, '2025-01-05 00:00:00'),
                    (102, 1, 1, '2025-01-06 00:00:00'),
                    (103, 1, 2, '2025-01-07 00:00:00'),
                    (104, 2, 3, '2025-01-08 00:00:00'),
                    (105, 2, 4, '2025-01-09 00:00:00')
                """.trimIndent(),
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun dates(counts: EventStatistics.MonthlyEventCounts): List<String> {
        val field = EventStatistics.MonthlyEventCounts::class.java.getDeclaredField("dates")
        field.isAccessible = true
        return field.get(counts) as List<String>
    }

    private fun intArray(
        counts: EventStatistics.MonthlyEventCounts,
        fieldName: String,
    ): IntArray {
        val field = EventStatistics.EventCountsWithAggregate::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(counts) as IntArray
    }

    private companion object {
        private const val AIRFRAME_ID = 10
        private const val AIRFRAME_NAME = "Cessna 172S"
        private const val EVENT_DEFINITION_ID = 7
        private const val EVENT_NAME = "Hard Landing"
    }
}
