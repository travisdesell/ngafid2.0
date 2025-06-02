package org.ngafid.processor

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.ngafid.core.Database
import org.ngafid.core.event.Event
import org.ngafid.core.event.EventDefinition
import org.ngafid.core.flights.Flight
import org.ngafid.core.kafka.DisjointConsumer
import org.ngafid.core.kafka.Events
import org.ngafid.core.kafka.Events.EventToCompute
import org.ngafid.core.kafka.Topic
import org.ngafid.core.util.ColumnNotAvailableException
import org.ngafid.core.util.filters.Pair
import org.ngafid.processor.events.AbstractEventScanner
import org.ngafid.processor.events.EventScanner
import org.ngafid.processor.events.LowEndingFuelScanner
import org.ngafid.processor.events.SpinEventScanner
import org.ngafid.processor.events.proximity.ProximityEventScanner
import java.sql.Connection
import java.sql.SQLException
import java.util.function.Consumer
import java.util.logging.Logger

/**
 * The `event` and `event-retry` topics contain events that need to be computed. Most often this should be proximity
 * events, however in the event that we change an event definition and it needs to be re-computed this daemon will
 * do that computation as well.
 *
 *
 * Note that this daemon does NOT scan the database for events that need to be computed -- this is the role of the
 * event observer, which will scan the database for non-computed events and add them to the event topic.
 *
 *
 * You may also manually add events to the queue using the EventHelper program in `ngafid-core`.
 */
class EventConsumer protected constructor(
    mainThread: Thread?,
    consumer: KafkaConsumer<String?, String?>,
    producer: KafkaProducer<String?, String?>?
) :
    DisjointConsumer<String?, String?>(mainThread, consumer, producer) {
    private val objectMapper = ObjectMapper()
    private var eventDefinitionMap: Map<Int, EventDefinition>? = null

    override fun preProcess(records: ConsumerRecords<String?, String?>?) {
        try {
            eventDefinitionMap = getEventDefinitionMap()
        } catch (e: SQLException) {
            throw RuntimeException(e)
        }
    }

    override fun process(record: ConsumerRecord<String?, String?>?): Pair<ConsumerRecord<String?, String?>, Boolean>? {
        val etc: EventToCompute

        try {
            etc = objectMapper.readValue(record!!.value(), EventToCompute::class.java)
        } catch (e: JsonProcessingException) {
            throw RuntimeException(e)
        }

        try {
            Database.getConnection().use { connection ->
                val flight = Flight.getFlight(connection, etc.flightId)
                if (flight == null) {
                    LOG.info("Cannot compute event with definition id " + etc.eventId + " for flight " + etc.flightId + " because the flight does not exist in the database. Assuming this was a stale request")
                    return Pair(record, false)
                }

                val def = eventDefinitionMap!![etc.eventId]
                if (def == null) {
                    LOG.info("Cannot compute event with definition id " + etc.eventId + " for flight " + etc.flightId + " because there is no event with that definition in the database.")
                    return Pair(record, false)
                }

                if (def.airframeNameId > 0 && def.airframeNameId == flight.airframe.id) return Pair(record, false)
                try {
                    clearExistingEvents(connection, flight, eventDefinitionMap!![etc.eventId]!!)
                    val scanner = getScanner(
                        flight,
                        eventDefinitionMap!![etc.eventId]!!
                    )
                    scanner.gatherRequiredColumns(connection, flight)


                    // Scanners may emit events of more than one type -- filter the other events out.
                    val events = scanner
                        .scan(flight.doubleTimeSeriesMap, flight.stringTimeSeriesMap)
                        .stream()
                        .filter { e: Event -> e.eventDefinitionId == etc.eventId }
                        .toList()

                    Event.batchInsertion(connection, flight, events)

                    // Computed okay.
                    return Pair(record, false)
                } catch (e: ColumnNotAvailableException) {
                    // Some other exception happened...
                    e.printStackTrace()
                    LOG.info("A required column was not available so the event could not be computed: " + e.message)
                    return Pair(record, true)
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Retry
                    return Pair(record, true)
                }
            }
        } catch (e: SQLException) {
            // If a sql exception happens, there is likely a bug that needs to addressed or the process should be rebooted. Crash process.
            e.printStackTrace()
            done.set(true)
            mainThread.interrupt()
            throw RuntimeException(e)
        }
    }

    override fun getTopicName(): String {
        return Topic.EVENT.toString()
    }

    override fun getRetryTopicName(): String {
        return Topic.EVENT_RETRY.toString()
    }

    override fun getDLTTopicName(): String {
        return Topic.EVENT_DLQ.toString()
    }

    override fun getMaxPollIntervalMS(): Long {
        return Events.MAX_POLL_INTERVAL_MS
    }

    companion object {
        private val LOG: Logger = Logger.getLogger(EventConsumer::class.java.name)

        @JvmStatic
        fun main(args: Array<String>) {
            val consumer = Events.createConsumer()
            val producer = Events.createProducer()

            EventConsumer(Thread.currentThread(), consumer, producer).run()
        }

        private fun getScanner(flight: Flight, def: EventDefinition): AbstractEventScanner {
            return if (def.id > 0) {
                EventScanner(def)
            } else {
                when (def.id) {
                    -6, -5, -4 -> LowEndingFuelScanner(flight.airframe, def)
                    -3, -2 -> SpinEventScanner(def)
                    -1 -> ProximityEventScanner(flight, def)
                    else -> throw RuntimeException("Cannot create scanner for event with definition id " + def.id + ". Please manually update `org.ngafid.kafka.EventConsumer with the mapping to the scanner.")
                }
            }
        }

        @Throws(SQLException::class)
        private fun clearExistingEvents(connection: Connection, flight: Flight, def: EventDefinition) {
            Event.deleteEvents(connection, flight.id, def.id)
        }

        @Throws(SQLException::class)
        private fun getEventDefinitionMap(): Map<Int, EventDefinition> {
            Database.getConnection().use { connection ->
                val defs: List<EventDefinition> = EventDefinition.getAll(connection)
                val map = HashMap<Int, EventDefinition>(defs.size * 2)
                defs.forEach(Consumer { def: EventDefinition -> map[def.id] = def })
                return map
            }
        }
    }
}
